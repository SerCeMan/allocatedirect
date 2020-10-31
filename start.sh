#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=$(git rev-parse --show-toplevel)
PROG=$(basename "$0")

info() {
  echo "$(date '+[%Y-%m-%d %H:%M:%S]') ${PROG}: INFO: $@"
}

error() {
  echo "$(date '+[%Y-%m-%d %H:%M:%S]') ${PROG}: ERROR: $@"
  exit 1
}

usejdk() {
    local version=$1
    if [[ $version == "8" ]]; then
        version=1.8
    fi
    local jdk
    case $(uname) in
      Darwin)
        jdk=$(/usr/libexec/java_home -v $version)
        ;;
      Linux)
        jdk=/usr/lib/jvm/zulu-13-amd64/
        if [[ ! -d "${jdk}" ]]; then
          cat <<'EOF'
# JDK 13 is not installed, you can install one from Azul by typing:
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9
sudo tee -a "/etc/apt/sources.list.d/azulsystems-stable.list" >/dev/null <<TEE
deb http://repos.azulsystems.com/ubuntu stable main
TEE
sudo apt-get update
sudo apt-get -y install zulu-13
EOF
        fi
        ;;
      *)
        error "unknown OS: $(uname)"
        ;;
    esac
    export JAVA_HOME="${jdk}"
    export PATH="${JAVA_HOME}/bin/:$PATH"
}

main() {
  pushd "${REPO_DIR}" >/dev/null
  ./gradlew build shadowJar
  usejdk 13

  local gc_opts
  local gc=g1
  for arg; do
    case "${arg}" in
      g1)
        gc=g1
        gc_opts=( -XX:+UseG1GC )
        ;;
      shen)
        gc=shenandoah
        gc_opts=( -XX:+UnlockExperimentalVMOptions -XX:+UseZGC )
        ;;
      *)
        error "unknown option: ${arg}"
        ;;
    esac
  done

  if [[ -z "${gc_opts:-}" ]]; then
    info "starting with default gc"
  fi

  mkdir -p /tmp/${gc}
  rm -rf /tmp/${gc}/*
  java -cp build/libs/allocatedirect-1.0-SNAPSHOT-all.jar \
    -Xmx6G \
    -Xms6G \
    -XX:+AlwaysPreTouch \
    ${gc_opts[@]} \
    -Xlog:safepoint,gc*:file=/tmp/${gc}/gc.log:time,level,tags:filecount=6,filesize=4000k \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5001 \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.local.only=true \
    -Dcom.sun.management.jmxremote.port=1100 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    me.serce.allocatedirect.Main
  popd > /dev/null
}

main "$@"
