#!/bin/bash

# Based on https://gist.github.com/asmaloney/55d96a8c3558b2f92cb3

VERSION="$1"
DMG_BACKGROUND_IMG="src/main/resources/assemblies/mac/background.png"
 
APP_EXE="CayenneModeler.app/Contents/MacOS/CayenneModeler"
 
VOL_NAME="cayenne-$1-macosx"
DMG_TMP="target/${VOL_NAME}-temp.dmg"
DMG_FINAL="target/${VOL_NAME}.dmg"
STAGING_DIR="target/dmg-staging"

# clear out any old data
rm -rf "${STAGING_DIR}" "${DMG_TMP}" "${DMG_FINAL}"
 
# copy over the stuff we want in the final disk image to our staging dir
mkdir -p "${STAGING_DIR}"
cp -rpf "target/${VOL_NAME}/CayenneModeler.app" "${STAGING_DIR}"
cp -rpf "target/${VOL_NAME}/README.txt"         "${STAGING_DIR}"
cp -rpf "target/${VOL_NAME}/cayenne-${VERSION}" "${STAGING_DIR}"

# figure out how big our DMG needs to be
#  assumes our contents are at least 1M!
SIZE=`du -sh "${STAGING_DIR}" | sed 's/\([0-9]*\)M\(.*\)/\1/'`
SIZE=`echo "${SIZE} + 1.0" | bc | awk '{print int($20+0.5)}'`
 
if [ $? -ne 0 ]; then
   echo "Error: Cannot compute size of staging dir"
   exit
fi

# create the temp DMG file
hdiutil create -srcfolder "${STAGING_DIR}" -volname "${VOL_NAME}" -fs HFS+ \
      -fsargs "-c c=64,a=16,e=16" -format UDRW -size ${SIZE}M "${DMG_TMP}"
 
echo "Created DMG: ${DMG_TMP}"
 
# mount it and save the device
DEVICE=$(hdiutil attach -readwrite -noverify "${DMG_TMP}" | \
         egrep '^/dev/' | sed 1q | awk '{print $1}')

sleep 2

# add a link to the Applications dir
pushd /Volumes/"${VOL_NAME}"
ln -s /Applications
popd
 
# add a background image
mkdir /Volumes/"${VOL_NAME}"/.background
cp "${DMG_BACKGROUND_IMG}" /Volumes/"${VOL_NAME}"/.background/

# tell the Finder to resize the window, set the background,
#  change the icon size, place the icons in the right position, etc.
echo '
   tell application "Finder"
     tell disk "'${VOL_NAME}'"
           open
           set current view of container window to icon view
           set toolbar visible of container window to false
           set statusbar visible of container window to false
           set the bounds of container window to {100, 100, 600, 450}
           set viewOptions to the icon view options of container window
           set arrangement of viewOptions to not arranged
           set icon size of viewOptions to 70
           set background picture of viewOptions to file ".background:background.png"
           set position of item "cayenne-'${VERSION}'" of container window to {50, 240}
           set position of item "README.txt" of container window to {360, 240}
           set position of item "CayenneModeler.app" of container window to {50, 100}
           set position of item "Applications" of container window to {360, 100}
           close
           open
           update without registering applications
           delay 2
     end tell
   end tell
' | osascript
 
sync
# unmount it
hdiutil detach "${DEVICE}"
 
# now make the final image a compressed disk image
echo "Creating compressed image"
hdiutil convert "${DMG_TMP}" -format UDZO -imagekey zlib-level=9 -o "${DMG_FINAL}"
 
echo 'Done.'
 
exit