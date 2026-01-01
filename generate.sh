cd ~/.m2/repository/dev/anygeneric/blazeftc/
echo "remember to run clean, build, and publish to mvn local"
echo "and set the version in generate.sh!"
find . -type f ! -name '*.asc' -exec sh -c '
  for f do
    md5sum "$f" | cut -d " " -f 1 > "$f.md5"
    sha1sum "$f" | cut -d " " -f 1 > "$f.sha1"
  done
' sh {} +
bsdtar --format=zip -cf ~/archive.zip -s ',^,dev/anygeneric/blazeftc/,'  0.1.2/
rm 0.1.2/*.md5 0.1.2/*.sha1