#!/usr/bin/env bash

set -Eeuo pipefail

readonly MINECRAFT_VERSION='1.20.1'
readonly NEOFORGE_VERSION='1.20.1-47.1.106'
readonly INSTALLER_SHA256='c1ea1c3be532c4444004efd7f9cc71e1590d010ff44845b93effba61e3bd1526'
readonly SERVER_SHA1='84194a2f286ef7c14ed7ce0090dba59902951553'
readonly INSTALLER_URL="https://maven.neoforged.net/releases/net/neoforged/forge/${NEOFORGE_VERSION}/forge-${NEOFORGE_VERSION}-installer.jar"
readonly SERVER_URL="https://piston-data.mojang.com/v1/objects/${SERVER_SHA1}/server.jar"
readonly CANDIDATE_NAME='smart-resource-multiplier-neoforge-1.3.0+mc1.20.1.jar'
readonly READY_MARKER='For help, type "help"'
readonly READY_TIMEOUT_SECONDS=300
readonly SHUTDOWN_TIMEOUT_SECONDS=120
readonly INSTALL_ATTEMPTS=4

readonly REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly BUILD_ROOT="${REPOSITORY_ROOT}/neoforge/build"
readonly CACHE_DIRECTORY="${BUILD_ROOT}/production-server-cache"
readonly RUN_DIRECTORY="${BUILD_ROOT}/production-server-smoke"
readonly INSTALLER_PATH="${CACHE_DIRECTORY}/forge-${NEOFORGE_VERSION}-installer.jar"
readonly SERVER_CACHE_PATH="${CACHE_DIRECTORY}/server-${MINECRAFT_VERSION}.jar"
readonly INSTALLED_SERVER_PATH="${RUN_DIRECTORY}/libraries/net/minecraft/server/${MINECRAFT_VERSION}/server-${MINECRAFT_VERSION}.jar"
readonly CANDIDATE_PATH="${BUILD_ROOT}/libs/${CANDIDATE_NAME}"
readonly COPIED_CANDIDATE_PATH="${RUN_DIRECTORY}/mods/${CANDIDATE_NAME}"
readonly CONSOLE_LOG="${RUN_DIRECTORY}/production-server-console.log"
readonly LATEST_LOG="${RUN_DIRECTORY}/logs/latest.log"
readonly COMMAND_PIPE="${RUN_DIRECTORY}/server-console-input"

SERVER_PID=''
COMMAND_FD_OPEN='false'

process_is_running() {
    local pid="$1"
    kill -0 "${pid}" 2>/dev/null
}

terminate_process_group() {
    local pid="${1:-}"
    if [[ -z "${pid}" ]]; then
        return
    fi
    kill -TERM -- "-${pid}" 2>/dev/null || true
    for _ in $(seq 1 20); do
        if ! process_is_running "${pid}"; then
            break
        fi
        sleep 1
    done
    kill -KILL -- "-${pid}" 2>/dev/null || true
    wait "${pid}" 2>/dev/null || true
}

cleanup() {
    local status=$?
    trap - EXIT INT TERM
    if [[ "${COMMAND_FD_OPEN}" == 'true' ]]; then
        exec 3>&- || true
    fi
    terminate_process_group "${SERVER_PID}"
    if (( status != 0 )) && [[ -f "${CONSOLE_LOG}" ]]; then
        printf '%s\n' '--- production Forge 47 server console (last 250 lines) ---' >&2
        tail -n 250 "${CONSOLE_LOG}" >&2 || true
    fi
    exit "${status}"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

hash_matches() {
    local algorithm="$1"
    local expected="$2"
    local path="$3"
    local actual
    case "${algorithm}" in
        sha256) actual="$(sha256sum "${path}" | awk '{print $1}')" ;;
        sha1) actual="$(sha1sum "${path}" | awk '{print $1}')" ;;
        *) printf 'Unsupported digest algorithm: %s\n' "${algorithm}" >&2; return 2 ;;
    esac
    [[ "${actual,,}" == "${expected,,}" ]]
}

download_verified() {
    local url="$1"
    local destination="$2"
    local algorithm="$3"
    local expected="$4"
    local partial="${destination}.part"

    if [[ -f "${destination}" ]] && hash_matches "${algorithm}" "${expected}" "${destination}"; then
        return
    fi
    rm -f -- "${destination}"
    curl --fail --location --show-error \
        --retry 8 --retry-delay 2 --retry-all-errors \
        --connect-timeout 30 --continue-at - \
        --output "${partial}" "${url}"
    if ! hash_matches "${algorithm}" "${expected}" "${partial}"; then
        printf '%s digest mismatch for %s.\n' "${algorithm}" "${url}" >&2
        rm -f -- "${partial}"
        return 1
    fi
    mv -- "${partial}" "${destination}"
}

wait_for_marker() {
    local marker="$1"
    local timeout_seconds="$2"
    local deadline=$((SECONDS + timeout_seconds))
    while (( SECONDS < deadline )); do
        if grep -Fq -- "${marker}" "${CONSOLE_LOG}"; then
            return
        fi
        if ! process_is_running "${SERVER_PID}"; then
            local status=0
            wait "${SERVER_PID}" || status=$?
            SERVER_PID=''
            printf 'Production Forge 47 server exited before readiness (status %s).\n' "${status}" >&2
            return 1
        fi
        sleep 1
    done
    printf 'Production Forge 47 server did not become ready within %s seconds.\n' \
        "${timeout_seconds}" >&2
    return 1
}

wait_for_shutdown() {
    local deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
    while process_is_running "${SERVER_PID}"; do
        if (( SECONDS >= deadline )); then
            printf 'Production Forge 47 server did not stop within %s seconds.\n' \
                "${SHUTDOWN_TIMEOUT_SECONDS}" >&2
            return 1
        fi
        sleep 1
    done
    local status=0
    wait "${SERVER_PID}" || status=$?
    SERVER_PID=''
    if (( status != 0 )); then
        printf 'Production Forge 47 server exited with status %s.\n' "${status}" >&2
        return 1
    fi
}

cd "${REPOSITORY_ROOT}"

command -v curl >/dev/null
command -v setsid >/dev/null
command -v sha1sum >/dev/null
command -v sha256sum >/dev/null
command -v unzip >/dev/null

JAVA_COMMAND="${JAVA_HOME:+${JAVA_HOME}/bin/java}"
if [[ -z "${JAVA_COMMAND}" || ! -x "${JAVA_COMMAND}" ]]; then
    JAVA_COMMAND="$(command -v java)"
fi
JAVA_VERSION_TEXT="$(${JAVA_COMMAND} -version 2>&1)"
if [[ ! "${JAVA_VERSION_TEXT}" =~ version\ \"17[.] ]]; then
    printf 'The production Forge 47 smoke requires Java 17; found:\n%s\n' \
        "${JAVA_VERSION_TEXT}" >&2
    exit 1
fi

if [[ ! -f "${CANDIDATE_PATH}" ]]; then
    printf 'Missing final NeoForge candidate: %s\n' "${CANDIDATE_PATH}" >&2
    exit 1
fi
python3 tools/validate_neoforge_jar.py

mkdir -p "${CACHE_DIRECTORY}"
download_verified "${INSTALLER_URL}" "${INSTALLER_PATH}" sha256 "${INSTALLER_SHA256}"
download_verified "${SERVER_URL}" "${SERVER_CACHE_PATH}" sha1 "${SERVER_SHA1}"

case "${RUN_DIRECTORY}" in
    "${REPOSITORY_ROOT}"/neoforge/build/production-server-smoke) ;;
    *) printf 'Refusing unsafe production-smoke cleanup target: %s\n' "${RUN_DIRECTORY}" >&2; exit 1 ;;
esac
rm -rf -- "${RUN_DIRECTORY}"
mkdir -p "$(dirname "${INSTALLED_SERVER_PATH}")"
cp -- "${SERVER_CACHE_PATH}" "${INSTALLED_SERVER_PATH}"

install_status=1
for attempt in $(seq 1 "${INSTALL_ATTEMPTS}"); do
    if (cd "${RUN_DIRECTORY}" && "${JAVA_COMMAND}" -jar "${INSTALLER_PATH}" --installServer .); then
        install_status=0
        break
    fi
    printf 'Forge 47 installer attempt %s/%s failed; retrying verified partial downloads.\n' \
        "${attempt}" "${INSTALL_ATTEMPTS}" >&2
done
if (( install_status != 0 )); then
    printf 'Official Forge 47 installer failed after %s attempts.\n' "${INSTALL_ATTEMPTS}" >&2
    exit 1
fi

if ! hash_matches sha1 "${SERVER_SHA1}" "${INSTALLED_SERVER_PATH}"; then
    printf 'Installer server JAR no longer matches the official Minecraft %s digest.\n' \
        "${MINECRAFT_VERSION}" >&2
    exit 1
fi
for required in \
        "${RUN_DIRECTORY}/libraries/net/neoforged/forge/${NEOFORGE_VERSION}/unix_args.txt" \
        "${RUN_DIRECTORY}/run.sh" \
        "${RUN_DIRECTORY}/user_jvm_args.txt"; do
    if [[ ! -f "${required}" ]]; then
        printf 'Official server installation omitted %s.\n' "${required}" >&2
        exit 1
    fi
done

mkdir -p "${RUN_DIRECTORY}/mods"
cp -- "${CANDIDATE_PATH}" "${COPIED_CANDIDATE_PATH}"
if ! cmp -s -- "${CANDIDATE_PATH}" "${COPIED_CANDIDATE_PATH}"; then
    printf '%s\n' 'Copied production candidate is not byte-identical.' >&2
    exit 1
fi
mapfile -t installed_mods < <(find "${RUN_DIRECTORY}/mods" -mindepth 1 -maxdepth 1 -type f -name '*.jar' -print)
if (( ${#installed_mods[@]} != 1 )) || [[ "${installed_mods[0]}" != "${COPIED_CANDIDATE_PATH}" ]]; then
    printf 'Production server must contain exactly the final candidate JAR; found %s.\n' \
        "${installed_mods[*]:-none}" >&2
    exit 1
fi

cat >"${RUN_DIRECTORY}/eula.txt" <<'EOF'
eula=true
EOF
cat >"${RUN_DIRECTORY}/server.properties" <<'EOF'
allow-flight=true
enable-command-block=false
enable-rcon=false
enable-status=false
enforce-secure-profile=false
gamemode=creative
generate-structures=false
level-name=production-smoke-world
max-players=1
motd=Smart Resource Multiplier production Forge 47 smoke
online-mode=false
server-ip=127.0.0.1
server-port=0
simulation-distance=5
spawn-protection=0
sync-chunk-writes=true
view-distance=5
EOF
cat >"${RUN_DIRECTORY}/user_jvm_args.txt" <<'EOF'
-Xms512M
-Xmx1536M
EOF

: >"${CONSOLE_LOG}"
mkfifo "${COMMAND_PIPE}"
exec 3<>"${COMMAND_PIPE}"
COMMAND_FD_OPEN='true'
(
    cd "${RUN_DIRECTORY}"
    exec setsid "${JAVA_COMMAND}" \
        '@user_jvm_args.txt' \
        "@libraries/net/neoforged/forge/${NEOFORGE_VERSION}/unix_args.txt" \
        nogui <"${COMMAND_PIPE}" >"${CONSOLE_LOG}" 2>&1
) &
SERVER_PID=$!

wait_for_marker "${READY_MARKER}" "${READY_TIMEOUT_SECONDS}"
printf '%s\n' 'smartdrops status' 'smartdrops validate' 'stop' >&3
exec 3>&-
COMMAND_FD_OPEN='false'
wait_for_shutdown

if [[ ! -f "${LATEST_LOG}" ]]; then
    printf 'Production Forge 47 server produced no latest.log: %s\n' "${LATEST_LOG}" >&2
    exit 1
fi
for marker in \
        "Launching target 'forgeserver'" \
        "Found mod file \"${CANDIDATE_NAME}\" of type MOD" \
        'NeoForge mod loading, version 47.1.106, for MC 1.20.1' \
        'Smart Resource Multiplier initialized' \
        'Done (' \
        'Smart Resource Multiplier: ON' \
        'Smart Resource Multiplier Validation' \
        'Status: Valid' \
        'No validation issues found.' \
        'No configuration or world data was changed.' \
        'Stopping server' \
        'All dimensions are saved'; do
    if ! grep -Fq -- "${marker}" "${CONSOLE_LOG}"; then
        printf 'Production Forge 47 evidence is missing: %s\n' "${marker}" >&2
        exit 1
    fi
done
if grep -Fq -- 'forgeserveruserdev' "${CONSOLE_LOG}"; then
    printf '%s\n' 'Production smoke launched a userdev target.' >&2
    exit 1
fi
for forbidden in \
        '/ERROR]' '/FATAL]' 'Game crashed!' \
        'MixinApplyError' 'InvalidInjectionException' \
        'NoClassDefFoundError' 'NoSuchMethodError' 'VerifyError' \
        'ModLoadingException' 'Failed to load mods' \
        'Missing metadata in pack mod:smart_resource_drops' \
        'Missing data pack mod:smart_resource_drops' \
        'Reference map' 'could not be read' \
        'JarJar selection failure'; do
    if grep -Fq -- "${forbidden}" "${CONSOLE_LOG}"; then
        printf 'Production Forge 47 console contains forbidden failure: %s\n' \
            "${forbidden}" >&2
        exit 1
    fi
done
if [[ ! -s "${RUN_DIRECTORY}/config/smart_resource_drops.json" ]]; then
    printf '%s\n' 'Production Forge 47 server did not persist the canonical config.' >&2
    exit 1
fi

SERVER_PID=''
trap - EXIT INT TERM
printf 'Production Forge 47 server smoke passed with Java 17, installer %s, and candidate SHA-256 %s.\n' \
    "${INSTALLER_SHA256}" "$(sha256sum "${CANDIDATE_PATH}" | awk '{print $1}')"
