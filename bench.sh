#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=$(git rev-parse --show-toplevel)
source "${REPO_DIR}/_common.sh"

main() {
  pushd "${REPO_DIR}"/bench >/dev/null
  mvn install >/dev/null
  usejdk 13

  local clz
  local args=()
  for arg; do
    case "${arg}" in
      alloc1)
        clz=me.serce.AllocateBuffer1
        ;;
      alloc2)
        clz=me.serce.AllocateBuffer2
        args+=( -prof perfasm )
        ;;
      *)
        error "unknown benchmark ${arg}"
        ;;
    esac
  done

  info "starting ${clz}"
  java -jar target/benchmarks.jar "${clz}" "${args[@]}"
  popd > /dev/null
}

main "$@"
