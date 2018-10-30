#!/bin/bash

MARKER=||REPO_PATH||/.gradle/is-curiostack.txt

if [ -f "$MARKER" ]; then
  ||TOOL_EXE_PATH|| "$@"
else
  export PATH=$(p=$(echo $PATH | tr ":" "\n" | grep -v "||SHIMS_PATH||" | tr "\n" ":"); echo ${p%:})
  ||EXE_NAME|| "$@"
fi
