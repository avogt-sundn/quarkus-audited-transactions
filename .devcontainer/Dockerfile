# See here for image contents: https://github.com/microsoft/vscode-dev-containers/blob/main/containers/java/.devcontainer/base.Dockerfile
# [Choice] Java version (use -bullseye variants on local arm64/Apple Silicon): 8, 11, 17, 8-bullseye, 11-bullseye, 17-bullseye, 8-buster, 11-buster, 17-buster
ARG VARIANT=17-bullseye
FROM mcr.microsoft.com/devcontainers/java:${VARIANT}

# [Option] Install Maven
ARG INSTALL_MAVEN="false"
ARG MAVEN_VERSION=""
# [Option] Install Gradle
ARG INSTALL_GRADLE="false"
ARG GRADLE_VERSION=""
RUN if [ "${INSTALL_MAVEN}" = "true" ]; then su vscode -c "umask 0002 && . /usr/local/sdkman/bin/sdkman-init.sh && sdk install maven \"${MAVEN_VERSION}\""; fi \
    && if [ "${INSTALL_GRADLE}" = "true" ]; then su vscode -c "umask 0002 && . /usr/local/sdkman/bin/sdkman-init.sh && sdk install gradle \"${GRADLE_VERSION}\""; fi

# [Choice] Node.js version: none, lts/*, 16, 14, 12, 10
ARG NODE_VERSION="none"
RUN if [ "${NODE_VERSION}" != "none" ]; then su vscode -c "umask 0002 && . /usr/local/share/nvm/nvm.sh && nvm install ${NODE_VERSION} 2>&1"; fi


# Avoid warnings by switching to noninteractive
# ENV DEBIAN_FRONTEND=noninteractive

ENV IGNORE_NOTICE=true


# BEGIN ------ install common utils: ping, dig, ncat
RUN apt-get update && apt-get install -y iputils-ping dnsutils apache2-utils
#   END ------ install common utils: ping, dig, ncat

# BEGIN ------ install zsh as default shell
ARG INSTALL_ZSH=true
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=$USER_UID
RUN apt-get update && curl -ssL https://raw.githubusercontent.com/microsoft/vscode-dev-containers/main/script-library/common-debian.sh -o /tmp/common-script.sh \
    && /bin/bash /tmp/common-script.sh "$INSTALL_ZSH" "$USERNAME" "$USER_UID" "$USER_GID" \
    && rm /tmp/common-script.sh
ENV SHELL /bin/zsh
#   END ------ install zsh as default shell

#   BEGIN ------ install graalvm
RUN ARCH=$( if [ $(arch) = "aarch64" ]; then echo aarch64; else echo amd64; fi) \
    && echo "Loading GraalVM/Mandrel for $ARCH" && curl -L -o /tmp/mandrel.tgz https://github.com/graalvm/mandrel/releases/download/mandrel-22.2.0.0-Final/mandrel-java17-linux-$ARCH-22.2.0.0-Final.tar.gz \
    && cd /opt && tar xzf /tmp/mandrel.tgz && rm /tmp/mandrel.tgz \
    && ln -s /opt/mandrel-java17-22.2.0.0-Final /opt/mandrel

# BEGIN ------ install common utils: ping, dig, ncat, file
RUN apt-get update && apt-get install -y iputils-ping dnsutils file
#   END ------ install common utils: ping, dig, ncat, file

#   BEGIN ------ install kompose and kubectl and helm
RUN ARCH=$( if [ $(arch) = "aarch64" ]; then echo arm64; else echo amd64; fi) \
    && echo "Loading Kompose for $ARCH" && curl -L -o /tmp/kompose.tgz https://github.com/kubernetes/kompose/releases/download/v1.27.0/kompose-linux-$ARCH.tar.gz \
    && cd /opt && tar xzf /tmp/kompose.tgz && rm /tmp/kompose.tgz \
    && ln -s /opt/kompose-linux-$ARCH /usr/local/bin/kompose
RUN ARCH=$( if [ $(arch) = "aarch64" ]; then echo arm64; else echo amd64; fi) \
    && echo "Loading Kubectl for $ARCH" && curl -sSL -o /usr/local/bin/kubectl https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/$ARCH/kubectl \
    && chmod +x /usr/local/bin/kubectl \
    && curl -s https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash -
RUN cd  /opt/ && git clone https://github.com/ahmetb/kubectx \
    && ln -s /opt/kubectx/kubectx /usr/local/bin/kubectx \
    && ln -s /opt/kubectx/kubens /usr/local/bin/kubens

# Chromium for unit tests
###RUN apt-get update && apt-get install -y chromium
