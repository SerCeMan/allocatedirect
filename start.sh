#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=$(git rev-parse --show-toplevel)
source "${REPO_DIR}/_common.sh"

main() {
  pushd "${REPO_DIR}" >/dev/null
  ./gradlew build shadowJar --no-daemon
  usejdk 13

  local gc_opts
  local gc=g1
  local args=()
  for arg; do
    case "${arg}" in
      g1)
        gc=g1
        gc_opts=( -XX:+UseG1GC )
        ;;
      shen)
        gc=shenandoah
        gc_opts=( -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC )
        ;;
      *)
        args+=( "${arg}" )
        ;;
    esac
  done

  info "starting with ${gc} gc"

  mkdir -p /tmp/${gc}
  rm -rf /tmp/${gc}/*
  java -cp build/libs/allocatedirect-1.0-SNAPSHOT-all.jar \
    -Xmx4G \
    -Xms4G \
    -XX:+AlwaysPreTouch \
    ${gc_opts[@]} \
    -Xlog:safepoint,gc*:file=/tmp/${gc}/gc.log:time,level,tags:filecount=6,filesize=4000k \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5001 \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.local.only=true \
    -Dcom.sun.management.jmxremote.rmi.port=1100 \
    -Djava.rmi.server.hostname=127.0.0.1 \
    -Dcom.sun.management.jmxremote.port=1100 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    me.serce.allocatedirect.Main "${args[@]}"
  popd > /dev/null
}

main "$@"
