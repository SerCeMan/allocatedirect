#!/usr/bin/env bash
set -euo pipefail

main() {
    wget https://github.com/openjdk/jdk/archive/jdk-13+32.tar.gz
    tar xf jdk-13+32.tar.gz
    cd jdk-jdk-13-32/src/utils/hsdis/
    wget https://ftp.gnu.org/gnu/binutils/binutils-2.31.1.tar.gz
    tar -xzf binutils-2.31.1.tar.gz
    sudo apt-get install libgmp-dev libmpfr-dev libmpc-dev g++-multilib
    # add the following to get intel syntax on line 437 of hsdis.c, see
    # https://bugs.openjdk.java.net/browse/JDK-8234583
    # ```
    # strncpy(iop, "intel", 5);
    # iop += 5;
    # ```
    make BINUTILS=binutils-2.31.1 ARCH=amd64
    sudo cp build/linux-amd64/hsdis-amd64.so /usr/lib/jvm/zulu-13-amd64/lib/server/
}

main "$@"
