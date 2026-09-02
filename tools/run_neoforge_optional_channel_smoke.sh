#!/usr/bin/env bash

set -Eeuo pipefail

readonly READY_TIMEOUT_SECONDS=180
readonly CLIENT_TIMEOUT_SECONDS=300
readonly SERVER_SHUTDOWN_TIMEOUT_SECONDS=60
readonly SERVER_READY_MARKER='For help, type "help"'
readonly CLIENT_ONLY_CLIENT_MARKER='NeoForge optional-channel client passed: client-only installation connected, server-bound channels unavailable, and config route failed closed'
readonly CLIENT_ONLY_SERVER_MARKER='NeoForge optional-channel server probe passed: clientOnly installation and clean disconnect'
readonly SERVER_ONLY_CLIENT_MARKER='NeoForge optional-channel client passed: production-unmodded client remained connected to the server-only installation'
readonly SERVER_ONLY_SERVER_MARKER='NeoForge optional-channel server probe passed: serverOnly installation and clean disconnect'

readonly REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly LOG_DIRECTORY="${REPOSITORY_ROOT}/neoforge/build/optional-channel-smoke-logs"
readonly PREFLIGHT_CONSOLE_LOG="${LOG_DIRECTORY}/preflight-console.log"

SERVER_PID=''
ACTIVE_PID=''
SERVER_CONSOLE_LOG=''
CLIENT_CONSOLE_LOG=''

mkdir -p "${LOG_DIRECTORY}"
: >"${PREFLIGHT_CONSOLE_LOG}"

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
    terminate_process_group "${ACTIVE_PID}"
    terminate_process_group "${SERVER_PID}"
    exit "${status}"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

print_failure_logs() {
    printf '%s\n' '--- NeoForge optional-channel preflight console (last 100 lines) ---' >&2
    tail -n 100 "${PREFLIGHT_CONSOLE_LOG}" >&2 || true
    printf '%s\n' '--- NeoForge optional-channel server console (last 200 lines) ---' >&2
    tail -n 200 "${SERVER_CONSOLE_LOG}" >&2 || true
    printf '%s\n' '--- NeoForge optional-channel client console (last 200 lines) ---' >&2
    tail -n 200 "${CLIENT_CONSOLE_LOG}" >&2 || true
}

wait_for_server_readiness() {
    local deadline=$((SECONDS + READY_TIMEOUT_SECONDS))
    while (( SECONDS < deadline )); do
        if grep -Fq -- "${SERVER_READY_MARKER}" "${SERVER_CONSOLE_LOG}"; then
            return 0
        fi
        if ! process_is_running "${SERVER_PID}"; then
            local status=0
            wait "${SERVER_PID}" || status=$?
            SERVER_PID=''
            printf 'NeoForge optional-channel server exited before readiness (status %s).\n' "${status}" >&2
            return 1
        fi
        sleep 1
    done

    printf 'NeoForge optional-channel server did not become ready within %s seconds.\n' \
        "${READY_TIMEOUT_SECONDS}" >&2
    return 1
}

run_client_with_timeout() {
    local client_task="$1"
    setsid xvfb-run -a ./gradlew -p neoforge --no-daemon --console=plain \
        "${client_task}" >"${CLIENT_CONSOLE_LOG}" 2>&1 &
    ACTIVE_PID=$!

    local deadline=$((SECONDS + CLIENT_TIMEOUT_SECONDS))
    while process_is_running "${ACTIVE_PID}"; do
        if ! process_is_running "${SERVER_PID}"; then
            printf '%s\n' 'NeoForge optional-channel server exited while its client was still running.' >&2
            terminate_process_group "${ACTIVE_PID}"
            ACTIVE_PID=''
            return 125
        fi
        if (( SECONDS >= deadline )); then
            printf 'NeoForge optional-channel client exceeded its %s-second timeout.\n' \
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
            printf 'NeoForge optional-channel server did not stop within %s seconds.\n' \
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

run_pair() {
    local label="$1"
    local server_task="$2"
    local client_task="$3"
    local client_marker="$4"
    local server_marker="$5"

    SERVER_CONSOLE_LOG="${LOG_DIRECTORY}/${label}-server-console.log"
    CLIENT_CONSOLE_LOG="${LOG_DIRECTORY}/${label}-client-console.log"
    : >"${SERVER_CONSOLE_LOG}"
    : >"${CLIENT_CONSOLE_LOG}"

    setsid ./gradlew -p neoforge --no-daemon --console=plain \
        "${server_task}" >"${SERVER_CONSOLE_LOG}" 2>&1 &
    SERVER_PID=$!

    if ! wait_for_server_readiness; then
        print_failure_logs
        return 1
    fi

    local client_status=0
    run_client_with_timeout "${client_task}" || client_status=$?

    local server_status=0
    wait_for_server_shutdown || server_status=$?

    local client_marker_status=0
    grep -Fq -- "${client_marker}" "${CLIENT_CONSOLE_LOG}" || client_marker_status=$?
    local server_marker_status=0
    grep -Fq -- "${server_marker}" "${SERVER_CONSOLE_LOG}" || server_marker_status=$?

    if (( client_status != 0 \
            || server_status != 0 \
            || client_marker_status != 0 \
            || server_marker_status != 0 )); then
        printf 'NeoForge optional-channel %s pair failed: client_status=%s, server_status=%s, client_marker=%s, server_marker=%s.\n' \
            "${label}" \
            "${client_status}" \
            "${server_status}" \
            "${client_marker_status}" \
            "${server_marker_status}" >&2
        print_failure_logs
        return 1
    fi

    SERVER_PID=''
    printf 'NeoForge optional-channel %s pair completed successfully.\n' "${label}"
}

cd "${REPOSITORY_ROOT}"

command -v setsid >/dev/null
command -v xvfb-run >/dev/null

# Compile every participating source set before overlapping Gradle invocations.
./gradlew -p neoforge --no-daemon --console=plain \
    compileClienttestJava compileOptionalchanneltestJava \
    2>&1 | tee "${PREFLIGHT_CONSOLE_LOG}"

run_pair \
    'client-only' \
    'runOptionalClientOnlyServerTest' \
    'runOptionalClientOnlyClientTest' \
    "${CLIENT_ONLY_CLIENT_MARKER}" \
    "${CLIENT_ONLY_SERVER_MARKER}"

run_pair \
    'server-only' \
    'runOptionalServerOnlyServerTest' \
    'runOptionalServerOnlyClientTest' \
    "${SERVER_ONLY_CLIENT_MARKER}" \
    "${SERVER_ONLY_SERVER_MARKER}"

trap - EXIT INT TERM
printf '%s\n' 'NeoForge optional-channel installation matrix completed successfully.'
