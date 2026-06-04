#!/bin/sh
# Maven Wrapper script for Unix/Linux/macOS
# Downloads Maven 3.9.8 on first run and caches it in ~/.m2/wrapper/dists/

MAVEN_VERSION="3.9.8"
WRAPPER_DIR="$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"
MVN_CMD="$MAVEN_HOME/bin/mvn"
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"

if [ ! -f "$MVN_CMD" ]; then
    echo "[mvnw] Maven $MAVEN_VERSION not found. Downloading..."
    mkdir -p "$WRAPPER_DIR"

    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$DOWNLOAD_URL" -o "$WRAPPER_DIR/maven.zip"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$DOWNLOAD_URL" -O "$WRAPPER_DIR/maven.zip"
    else
        echo "[mvnw] Error: curl or wget required."
        exit 1
    fi

    unzip -q "$WRAPPER_DIR/maven.zip" -d "$WRAPPER_DIR"
    rm "$WRAPPER_DIR/maven.zip"
    chmod +x "$MVN_CMD"
    echo "[mvnw] Maven $MAVEN_VERSION installed successfully."
fi

exec "$MVN_CMD" "$@"
