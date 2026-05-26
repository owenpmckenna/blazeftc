#!/bin/bash
cd ~/.m2/repository/dev/anygeneric/blazeftc/
echo "remember to run clean, build, and publish to mvn local"
echo "and set the version in generate.sh!"
VERSION=0.1.24
#generate the signatures of the maven-metadata files
#find . -type f ! -name '*.asc' ! -name "*.md5" ! -name "*.sha1" ! -name "*.zip" -exec sh -c '
#  for f do
#    md5sum "$f" | cut -d " " -f 1 > "$f.md5"
#    sha1sum "$f" | cut -d " " -f 1 > "$f.sha1"
#  done
#' sh {} +

#cd $VERSION
#find . -type f ! -name '*.asc' ! -name "*.md5" ! -name "*.sha1" -exec sh -c '
#  for f do
#    md5sum "$f" | cut -d " " -f 1 > "$f.md5"
#    sha1sum "$f" | cut -d " " -f 1 > "$f.sha1"
#  done
#' sh {} +
#cd ..

bsdtar --format=zip -cf ~/archive.zip -s ',^,dev/anygeneric/blazeftc/,'  $VERSION/
rm 0.1.2/*.md5 0.1.2/*.sha1
