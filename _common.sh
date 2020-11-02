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
          cat <<EOF
# JDK ${version} is not installed, you can install one from Azul by typing:
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9
sudo tee -a "/etc/apt/sources.list.d/azulsystems-stable.list" >/dev/null <<TEE
deb http://repos.azulsystems.com/ubuntu stable main
TEE
sudo apt-get update
sudo apt-get -y install zulu-${version}
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
