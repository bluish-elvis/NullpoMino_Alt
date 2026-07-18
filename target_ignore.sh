#!/usr/bin/env bash

mkdir {,nullpomino-core/,nullpomino-run/}{build,out,target}

if [ "$(uname)" == "Darwin" ]; then
  # Do something under Mac OS X platform
  xattr -w com.dropbox.ignored 1 .idea/workspace.xml
  xattr -w -r com.dropbox.ignored 1 {.git,.gradle}
  xattr -w -r com.dropbox.ignored 1 {,nullpomino-core/,nullpomino-run/}{build,out,target}
else
  # Do something under GNU/Linux platform
  attr -s com.dropbox.ignored -V 1 .idea/workspace.xml
  attr -s com.dropbox.ignored -V 1 {.git,.gradle}
  attr -s com.dropbox.ignored -V 1 {,nullpomino-core/,nullpomino-run/}{build,out,target}
fi
