#!/bin/bash

set -e

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
  MSYS_BASH_PATH="$(cygpath -w "$(which bash)")"
  export MSYS_BASH_PATH
fi

OPENJDK_DIR="$GRADLE_HOME/curiostack/openjdk"

export JAVA_HOME="$OPENJDK_DIR/jdk-zulu13.29.9-ca-jdk13.0.2"

DEST="$OPENJDK_DIR/jdk-zulu13.29.9-ca-jdk13.0.2.tar.gz.or.zip"

if "$linux" = "true"; then
  SRC="https://cdn.azul.com/zulu/bin/zulu13.29.9-ca-jdk13.0.2-linux_x64.tar.gz"
fi

if "$darwin" = "true"; then
  SRC="https://cdn.azul.com/zulu/bin/zulu13.29.9-ca-jdk13.0.2-macosx_x64.tar.gz"
  export JAVA_HOME="$JAVA_HOME/Contents/Home"
fi

if "$windows" = "true"; then
  SRC="https://cdn.azul.com/zulu/bin/zulu13.29.9-ca-jdk13.0.2-win_x64.zip"
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
    mv "${OPENJDK_DIR}/zulu13.29.9-ca-jdk13.0.2-win_x64" "$JAVA_HOME"
  else
    mkdir -p "$JAVA_HOME"
    tar --strip-components 1 -xf "$DEST" -C "$JAVA_HOME"
  fi

  rm "$DEST"
fi

set +e
