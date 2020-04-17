#!/bin/bash
#
# MIT License
#
# Copyright (c) 2020 Choko (choko@curioswitch.org)
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#


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

export JAVA_HOME="$OPENJDK_DIR/jdk-11.0.4+11"

DEST="$OPENJDK_DIR/jdk-11.0.4+11.tar.gz.or.zip"

if "$linux" = "true"; then
  SRC="https://cdn.azul.com/zulu/bin/11.0.4+11-linux_x64.tar.gz"
fi

if "$darwin" = "true"; then
  SRC="https://cdn.azul.com/zulu/bin/11.0.4+11-macosx_x64.tar.gz"
  export JAVA_HOME="$JAVA_HOME/Contents/Home"
fi

if "$windows" = "true"; then
  SRC="https://cdn.azul.com/zulu/bin/11.0.4+11-win_x64.zip"
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
    mv "${OPENJDK_DIR}/11.0.4+11-win_x64" "$JAVA_HOME"
  else
    mkdir -p "$JAVA_HOME"
    tar --strip-components 1 -xf "$DEST" -C "$JAVA_HOME"
  fi

  rm "$DEST"
fi

set +e
