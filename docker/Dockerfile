# docker build --no-cache -f docker/Dockerfile -t redis-env:local .

FROM ubuntu:22.04

WORKDIR /

# Install development utilities
RUN apt-get update && \
    apt-get install -y build-essential && \
    apt-get install -y wget unzip curl && \
    apt-get install -y gpg coreutils && \
    apt-get install -y git clang-format && \
    apt-get install -y lsb-release vim && \
    apt-get install -y python3-pip && \
    apt-get install -y language-pack-en-base && \
    apt-get clean;

RUN echo "Upgrade cmake ..." && \
    pip3 install cmake --upgrade

RUN echo "Installing openjdk-17 ..."; \
    set -ex; \
    mkdir /openjdk && cd /openjdk; \
    wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz; \
    tar -xvf openjdk-17.*; \
    cd jdk-17.0.2; \
    mkdir -p /usr/local/openjdk-17; \
    mv * /usr/local/openjdk-17; \
    rm -rf /openjdk;

RUN echo "Installing Maven 3.9.4 ..."; \
    set -ex; \
    mkdir /maven && cd /maven; \
    wget https://dlcdn.apache.org/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz; \
    tar xzvf apache-maven-3.9.4-bin.tar.gz; \
    cd apache-maven-3.9.4; \
    mkdir -p /usr/local/maven; \
    mv * /usr/local/maven; \
    rm -rf /maven;

RUN echo "Installing flatbuffers ..."; \
    set -ex; \
    mkdir /flatbuffers && cd /flatbuffers; \
    wget https://github.com/google/flatbuffers/archive/refs/tags/v23.5.26.tar.gz; \
    tar xzvf v23.5.26.tar.gz; \
    cd flatbuffers-23.5.26; \
    mkdir build; \
    cd build; \
    cmake -G "Unix Makefiles" -DFLATBUFFERS_BUILD_TESTS=OFF -DCMAKE_BUILD_TYPE=RelWithDebInfo ..; \
    make -j4 install; \
    rm -rf /flatbuffers;

RUN echo "Installing redis ..."; \
    set -ex; \
    mkdir /redis && cd /redis; \
    wget https://github.com/redis/redis/archive/refs/tags/7.2.1.tar.gz; \
    tar xzvf 7.2.1.tar.gz; \
    cd redis-7.2.1/deps/jemalloc; \
    ./configure && make -j4 && make install; \
    cd ../..; \
    make -j4 && make install; \
    rm -rf /redis;

RUN echo "Cleanup docker image..."; \
    set -ex; \
    apt-get autoremove -y; \
    apt-get clean -y; \
    rm -rf /var/lib/apt/lists/*;

 RUN echo "export ENV VARIABLES ...";
 RUN echo '\
 export LD_LIBRARY_PATH=/usr/local/lib \n\
 export JAVA_HOME=/usr/local/openjdk-17 \n\
 export M2_HOME=/usr/local/maven \n\
 export M2=${M2_HOME}/bin \n\
 export PATH=${JAVA_HOME}/bin:${M2}:${PATH};' >> /root/.bashrc
