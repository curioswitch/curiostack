#!/bin/bash

# OS specific support (must be 'true' or 'false').
windows=false
darwin=false
linux=false
case "`uname`" in
  CYGWIN* )
    windows=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    windows=true
    ;;
  MSYS* )
    windows=true
    ;;
  Linux* )
    linux=true
    ;;
esac

GRADLE_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"

if [ "$windows" ] && [ -n "$USERPROFILE" ]; then
  # msys
  USERPROFILE_CYG=$(cygpath $USERPROFILE)
  GRADLE_HOME="${USERPROFILE_CYG}/.gradle"
fi

OPENJDK_DIR="$GRADLE_HOME/curiostack/openjdk"

export JAVA_HOME="$OPENJDK_DIR/jdk-11.0.4+11"

DEST="$OPENJDK_DIR/jdk-11.0.4+11.tar.gz.or.zip"

if "$linux" = "true"; then
  SRC="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.4%2B11/OpenJDK11U-jdk_x64_linux_hotspot_11.0.4_11.tar.gz"
fi

if "$darwin" = "true"; then
  SRC="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.4%2B11/OpenJDK11U-jdk_x64_mac_hotspot_11.0.4_11.tar.gz"
  export JAVA_HOME="$JAVA_HOME/Contents/Home"
fi

if "$windows" = "true"; then
  SRC="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.4%2B11/OpenJDK11U-jdk_x64_windows_hotspot_11.0.4_11.zip"
fi

if [ ! -d "$JAVA_HOME" ]; then
  mkdir -p "$OPENJDK_DIR"

  echo "Downloading OpenJDK to $JAVA_HOME"
  if command -v wget >/dev/null 2>&1; then
    wget --quiet -O "$DEST" "$SRC"
  else
    curl --silent -L "$SRC" -o "$DEST"
  fi

  if "$windows" = "true"; then
    unzip "$DEST" -d "$OPENJDK_DIR"
    mv "${OPENJDK_DIR}/zulu13.28.11-ca-jdk13.0.1-win_x64" "$JAVA_HOME"
  else
    tar -xf --strip-components 1 "$DEST" -C "$JAVA_HOME"
  fi

  rm "$DEST"
fi
