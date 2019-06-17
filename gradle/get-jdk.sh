#!/bin/bash

if [ -z "$CI" ]; then

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

  if [ ! -z "$USERPROFILE" ]; then
    # msys
    USERPROFILE_CYG=$(cygpath $USERPROFILE)
    GRADLE_HOME="${USERPROFILE_CYG}/.gradle"
  fi

  OPENJDK_DIR="$GRADLE_HOME/curiostack/openjdk"

  export JAVA_HOME="$OPENJDK_DIR/jdk-11.0.3+7"

  if "$linux" = "true"; then
    SRC="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7/OpenJDK11U-jdk_x64_linux_hotspot_11.0.3_7.tar.gz"
    DEST="$OPENJDK_DIR/OpenJDK11U-jdk_x64_linux_hotspot_11.0.3_7.tar.gz"
  fi

  if "$darwin" = "true"; then
    SRC="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7/OpenJDK11U-jdk_x64_mac_hotspot_11.0.3_7.tar.gz"
    DEST="$OPENJDK_DIR/OpenJDK11U-jdk_x64_mac_hotspot_11.0.3_7.tar.gz"
  fi

  if "$windows" = "true"; then
    SRC="https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7/OpenJDK11U-jdk_x64_windows_hotspot_11.0.3_7.zip"
    DEST="$OPENJDK_DIR/OpenJDK11U-jdk_x64_windows_hotspot_11.0.3_7.zip"
  fi

  if [ ! -d "$JAVA_HOME" ]; then
    mkdir -p "$OPENJDK_DIR"

    echo "Downloading OpenJDK"
    wget -O "$DEST" "$SRC" || curl -L "$SRC" -o "$DEST"

    if "$windows" = "true"; then
      unzip "$DEST" -d "$OPENJDK_DIR"
    else
      tar -xf "$DEST" -C "$OPENJDK_DIR"
    fi
  fi

fi
