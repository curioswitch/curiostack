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

  export JAVA_HOME="$OPENJDK_DIR/jdk-12.0.1+12"

  if "$linux" = "true"; then
    SRC="https://github.com/AdoptOpenJDK/openjdk12-binaries/releases/download/jdk-12.0.1%2B12/OpenJDK12U-jdk_x64_linux_hotspot_12.0.1_12.tar.gz"
    DEST="$OPENJDK_DIR/OpenJDK12U-jdk_x64_linux_hotspot_12.0.1_12.tar.gz"
  fi

  if "$darwin" = "true"; then
    SRC="https://github.com/AdoptOpenJDK/openjdk12-binaries/releases/download/jdk-12.0.1%2B12/OpenJDK12U-jdk_x64_mac_hotspot_12.0.1_12.tar.gz"
    DEST="$OPENJDK_DIR/OpenJDK12U-jdk_x64_mac_hotspot_12.0.1_12.tar.gz"
  fi

  if "$windows" = "true"; then
    SRC="https://github.com/AdoptOpenJDK/openjdk12-binaries/releases/download/jdk-12.0.1%2B12/OpenJDK12U-jdk_x64_windows_hotspot_12.0.1_12.zip"
    DEST="$OPENJDK_DIR/OpenJDK12U-jdk_x64_windows_hotspot_12.0.1_12.zip"
  fi

  if [ ! -d "$JAVA_HOME" ]; then
    mkdir -p "$OPENJDK_DIR"

    echo "Downloading OpenJDK"
    curl -L "$SRC" -o "$DEST"

    if "$windows" = "true"; then
      unzip "$DEST" -d "$OPENJDK_DIR"
    else
      tar -xf "$DEST" -C "$OPENJDK_DIR"
    fi
  fi

fi
