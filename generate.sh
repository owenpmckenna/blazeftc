#!/bin/bash
cd ~/.m2/repository/dev/anygeneric/blazeftc/
echo "remember to run clean, build, and publish to mvn local"
echo "and set the version in generate.sh!"
VERSION=0.1.36

cd $VERSION
find . -type f ! -name '*.asc' ! -name "*.md5" ! -name "*.sha1" -exec sh -c '
  for f do
    md5sum "$f" | cut -d " " -f 1 > "$f.md5"
    sha1sum "$f" | cut -d " " -f 1 > "$f.sha1"
  done
' sh {} +
cd ..

bsdtar --format=zip -cf ~/archive.zip -s ',^,dev/anygeneric/blazeftc/,'  $VERSION/
rm $VERSION/*.md5 $VERSION/*.sha1
