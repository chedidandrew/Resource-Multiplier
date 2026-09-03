#!/usr/bin/env bash

set -Eeuo pipefail

readonly READY_TIMEOUT_SECONDS=180
readonly CLIENT_TIMEOUT_SECONDS=300
readonly SERVER_SHUTDOWN_TIMEOUT_SECONDS=60
readonly SERVER_READY_MARKER='For help, type "help"'
readonly CLIENT_PASS_MARKER='Fabric multiplayer client smoke passed: real non-op denial, operator Apply/Reset revisions, disconnect cleanup, channel renegotiation, and reconnect'
readonly SERVER_PASS_MARKER='Fabric multiplayer server smoke passed: non-op denial, operator promotion, authoritative patch/reset, disconnect cleanup, and reconnect'

readonly REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly LOG_DIRECTORY="${REPOSITORY_ROOT}/build/fabric-multiplayer-smoke-logs"
readonly PREFLIGHT_CONSOLE_LOG="${LOG_DIRECTORY}/preflight-console.log"
readonly SERVER_CONSOLE_LOG="${LOG_DIRECTORY}/server-console.log"
readonly CLIENT_CONSOLE_LOG="${LOG_DIRECTORY}/client-console.log"

SERVER_PID=''
ACTIVE_PID=''

mkdir -p "${LOG_DIRECTORY}"
: >"${PREFLIGHT_CONSOLE_LOG}"
: >"${SERVER_CONSOLE_LOG}"
: >"${CLIENT_CONSOLE_LOG}"

process_is_running() {
    local pid="$1"
    kill -0 "${pid}" 2>/dev/null
}

terminate_process_group() {
    local pid="${1:-}"
    [[ -z "${pid}" ]] && return
    kill -TERM -- "-${pid}" 2>/dev/null || true
    for _ in $(seq 1 20); do
        process_is_running "${pid}" || break
        sleep 1
    done
    kill -KILL -- "-${pid}" 2>/dev/null || true
    wait "${pid}" 2>/dev/null || true
}

cleanup() {
    local status=$?
    trap - EXIT INT TERM
    terminate_process_group "${ACTIVE_PID}"
    terminate_process_group "${SERVER_PID}"
    exit "${status}"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

print_failure_logs() {
    printf '%s\n' '--- Fabric multiplayer preflight console (last 100 lines) ---' >&2
    tail -n 100 "${PREFLIGHT_CONSOLE_LOG}" >&2 || true
    printf '%s\n' '--- Fabric multiplayer server console (last 200 lines) ---' >&2
    tail -n 200 "${SERVER_CONSOLE_LOG}" >&2 || true
    printf '%s\n' '--- Fabric multiplayer client console (last 200 lines) ---' >&2
    tail -n 200 "${CLIENT_CONSOLE_LOG}" >&2 || true
}

wait_for_server_readiness() {
    local deadline=$((SECONDS + READY_TIMEOUT_SECONDS))
    while (( SECONDS < deadline )); do
        grep -Fq -- "${SERVER_READY_MARKER}" "${SERVER_CONSOLE_LOG}" && return 0
        if ! process_is_running "${SERVER_PID}"; then
            local status=0
            wait "${SERVER_PID}" || status=$?
            SERVER_PID=''
            printf 'Fabric multiplayer server exited before readiness (status %s).\n' "${status}" >&2
            return 1
        fi
        sleep 1
    done
    printf 'Fabric multiplayer server did not become ready within %s seconds.\n' \
        "${READY_TIMEOUT_SECONDS}" >&2
    return 1
}

run_client_with_timeout() {
    setsid xvfb-run -a ./gradlew --no-daemon --console=plain \
        runMultiplayerClientSmoke >"${CLIENT_CONSOLE_LOG}" 2>&1 &
    ACTIVE_PID=$!
    local deadline=$((SECONDS + CLIENT_TIMEOUT_SECONDS))
    while process_is_running "${ACTIVE_PID}"; do
        if ! process_is_running "${SERVER_PID}" \
                && ! grep -Fq -- "${CLIENT_PASS_MARKER}" "${CLIENT_CONSOLE_LOG}"; then
            printf '%s\n' 'Fabric server exited while the client smoke was still running.' >&2
            terminate_process_group "${ACTIVE_PID}"
            ACTIVE_PID=''
            return 125
        fi
        if (( SECONDS >= deadline )); then
            printf 'Fabric multiplayer client exceeded its %s-second timeout.\n' \
                "${CLIENT_TIMEOUT_SECONDS}" >&2
            terminate_process_group "${ACTIVE_PID}"
            ACTIVE_PID=''
            return 124
        fi
        sleep 1
    done
    local status=0
    wait "${ACTIVE_PID}" || status=$?
    ACTIVE_PID=''
    return "${status}"
}

wait_for_server_shutdown() {
    local deadline=$((SECONDS + SERVER_SHUTDOWN_TIMEOUT_SECONDS))
    while process_is_running "${SERVER_PID}"; do
        if (( SECONDS >= deadline )); then
            printf 'Fabric server did not stop within %s seconds after client exit.\n' \
                "${SERVER_SHUTDOWN_TIMEOUT_SECONDS}" >&2
            terminate_process_group "${SERVER_PID}"
            SERVER_PID=''
            return 124
        fi
        sleep 1
    done
    local status=0
    wait "${SERVER_PID}" || status=$?
    SERVER_PID=''
    return "${status}"
}

cd "${REPOSITORY_ROOT}"
command -v setsid >/dev/null
command -v xvfb-run >/dev/null

./gradlew --no-daemon --console=plain compileClienttestJava \
    2>&1 | tee "${PREFLIGHT_CONSOLE_LOG}"

setsid ./gradlew --no-daemon --console=plain \
    runMultiplayerServerSmoke >"${SERVER_CONSOLE_LOG}" 2>&1 &
SERVER_PID=$!

if ! wait_for_server_readiness; then
    print_failure_logs
    exit 1
fi

client_status=0
run_client_with_timeout || client_status=$?
server_status=0
wait_for_server_shutdown || server_status=$?

client_marker_status=0
grep -Fq -- "${CLIENT_PASS_MARKER}" "${CLIENT_CONSOLE_LOG}" || client_marker_status=$?
server_marker_status=0
grep -Fq -- "${SERVER_PASS_MARKER}" "${SERVER_CONSOLE_LOG}" || server_marker_status=$?

if (( client_status != 0 \
        || server_status != 0 \
        || client_marker_status != 0 \
        || server_marker_status != 0 )); then
    printf 'Fabric multiplayer smoke failed: client_status=%s, server_status=%s, client_marker=%s, server_marker=%s.\n' \
        "${client_status}" "${server_status}" "${client_marker_status}" "${server_marker_status}" >&2
    print_failure_logs
    exit 1
fi

SERVER_PID=''
trap - EXIT INT TERM
printf '%s\n' 'Fabric multiplayer client/server smoke completed successfully.'
