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
    apt-get clean;

RUN echo "Upgrade cmake ..." && \
    pip3 install cmake --upgrade

RUN echo "Installing openjdk-17 ..."; \
    set -ex; \
    wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz; \
    tar -xvf openjdk-17.*; \
    cd jdk-17.0.2; \
    mkdir -p /usr/local/openjdk-17; \
    mv * /usr/local/openjdk-17; \
    rm -rf /openjdk-17.*;

RUN echo "Installing Maven 3.9.4 ..."; \
    set -ex; \
    wget https://dlcdn.apache.org/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz; \
    tar xzvf apache-maven-3.9.4-bin.tar.gz; \
    cd apache-maven-3.9.4; \
    mkdir -p /usr/local/maven; \
    mv * /usr/local/maven; \
    rm -rf /apache-maven-3.9.4-bin.tar.gz;

RUN echo "Installing flatbuffers ..."; \
    set -ex; \
    git clone https://github.com/google/flatbuffers.git --depth 1; \
    cd flatbuffers; \
    mkdir build; \
    cd build; \
    cmake -G "Unix Makefiles" -DFLATBUFFERS_BUILD_TESTS=OFF -DCMAKE_BUILD_TYPE=RelWithDebInfo ..; \
    make -j4 install; \
    rm -rf /flatbuffers;

RUN echo "Installing redis ..."; \
    set -ex; \
    git clone https://github.com/redis/redis.git --depth 1; \
    cd redis/deps/jemalloc; \
    ./configure && make -j4 && make install; \
    cd /redis; \
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
