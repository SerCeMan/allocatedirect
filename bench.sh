#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=$(git rev-parse --show-toplevel)
source "${REPO_DIR}/_common.sh"

main() {
  pushd "${REPO_DIR}"/bench >/dev/null
  mvn install >/dev/null
  usejdk 13

  local args=()
  for arg; do
    case "${arg}" in
      alloc1)
        args+=( me.serce.AllocateBuffer1 )
        ;;
      alloc2)
        args+=( -wi 1 -r 120 -p size=128 me.serce.AllocateBuffer1.direct )
        ;;
      alloc3)
        args+=( -wi 1 -r 120 -p size=128 me.serce.AllocateBuffer1.heap )
        ;;
      alloc4)
        args+=( me.serce.AllocateBuffer2 )
        ;;
      *)
        error "unknown benchmark ${arg}"
        ;;
    esac
  done

  info "starting ${args[@]}"
  java -jar target/benchmarks.jar "${args[@]}"
  popd > /dev/null
}

main "$@"
