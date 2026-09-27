#!/bin/bash
SRC="app/src/main/res/drawable/ic_launcher_foreground.png"

mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

convert -resize 48x48 "$SRC" app/src/main/res/mipmap-mdpi/ic_launcher.png
cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png

convert -resize 72x72 "$SRC" app/src/main/res/mipmap-hdpi/ic_launcher.png
cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png

convert -resize 96x96 "$SRC" app/src/main/res/mipmap-xhdpi/ic_launcher.png
cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png

convert -resize 144x144 "$SRC" app/src/main/res/mipmap-xxhdpi/ic_launcher.png
cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png

convert -resize 192x192 "$SRC" app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
cp app/src/main/res/mipmap-xxxhdpi/ic_launcher.png app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png

echo "Legacy icons generated."
