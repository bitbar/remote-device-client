FROM ubuntu:16.04

ARG PLATFORM_TOOLS_VERSION=r28.0.1
ARG PLATFORM_TOOLS_URL=https://nexus.wro.int.bitbar.com/repository/raw-hosted/android/platform-tools_${PLATFORM_TOOLS_VERSION}-linux.zip

ADD ${PLATFORM_TOOLS_URL} /platform-tools_${PLATFORM_TOOLS_VERSION}-linux.zip

RUN apt-get update && \
    apt-get install -y --no-install-recommends unzip && \
    rm -rf /var/lib/apt/lists/*

RUN unzip -j -d /usr/bin /platform-tools_${PLATFORM_TOOLS_VERSION}-linux.zip platform-tools/adb && \
    rm /platform-tools_${PLATFORM_TOOLS_VERSION}-linux.zip
RUN chmod +x /usr/bin/adb

RUN /usr/bin/adb version

RUN echo "#!/bin/bash" >/usr/local/bin/entrypoint.sh
RUN echo "exec \$@" >>/usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT [ "/usr/local/bin/entrypoint.sh" ]
