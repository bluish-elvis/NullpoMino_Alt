#!/usr/bin/env bash

mkdir target nullpomino-core/target nullpomino-run/target

if [ "$(uname)" == "Darwin" ]; then
  # Do something under Mac OS X platform
  xattr -w com.dropbox.ignored 1 target
  xattr -w com.dropbox.ignored 1 nullpomino-core/target
  xattr -w com.dropbox.ignored 1 nullpomino-run/target
else
  # Do something under GNU/Linux platform
  attr -s com.dropbox.ignored -V 1 target
  attr -s com.dropbox.ignored -V 1 nullpomino-core/target
  attr -s com.dropbox.ignored -V 1 nullpomino-run/target
fi
