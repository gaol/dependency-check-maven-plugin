#!/bin/sh
md5=`md5sum maven-metadata.xml | cut -d " " -f1`
sha1=`sha1sum maven-metadata.xml | cut -d " " -f1`
echo "$md5" > maven-metadata.xml.md5
echo "$sha1" > maven-metadata.xml.sha1

