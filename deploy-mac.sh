#!/bin/bash

# A bash script to easily deploy a self contained macOS app for PADDL
# Modified from source: https://github.com/dlemmermann/JPackageScriptFX

# ------ ENVIRONMENT --------------------------------------------------------

# Environment variables must be set properly for this script to work.
# A generic example:
# export PROJECT_VERSION="1.0-SNAPSHOT"
# export APP_VERSION="1.0.0"
# export JAVA_HOME=$(/usr/libexec/java_home -v 21)
# export PATH=$JAVA_HOME/bin:$PATH

JAVA_VERSION=21
MAIN_JAR="CanoeAnalysis-$PROJECT_VERSION.jar"
INSTALLER_TYPE=dmg
APP_NAME="PADDL"

echo "java home: $JAVA_HOME"
echo "project version: $PROJECT_VERSION"
echo "app version: $APP_VERSION"
echo "main JAR file: $MAIN_JAR"

# ------ SETUP DIRECTORIES AND FILES ----------------------------------------

# Note : resources are moved to an external location outside the JAR for writability

rm -rfd ./target/java-runtime/
rm -rfd target/installer/

mkdir -p target/installer/input/libs/
mkdir target/installer/input/resources/

cp target/libs/* target/installer/input/libs/
cp target/"${MAIN_JAR}" target/installer/input/libs/
cp -r src/main/resources/* target/installer/input/resources/

# ------ REQUIRED MODULES ---------------------------------------------------

echo "detecting required modules"

# shellcheck disable=SC2006
detected_modules=`"$JAVA_HOME"/bin/jdeps \
-q \
--multi-release $JAVA_VERSION \
--ignore-missing-deps \
--print-module-deps \
--class-path "target/installer/input/libs/*" \
  target/classes/com/wecca/canoeanalysis/Main.class`
echo "detected modules: $detected_modules"

# ------ MANUAL MODULES -----------------------------------------------------

manual_modules=,jdk.localedata,jdk.unsupported,java.xml,java.scripting,jdk.zipfs,java.rmi
echo "manual modules: $manual_modules"

# ------ RUNTIME IMAGE ------------------------------------------------------

echo "creating java runtime image"
"$JAVA_HOME"/bin/jlink \
--strip-native-commands \
--no-header-files \
--no-man-pages  \
--compress=2  \
--strip-debug \
--add-modules "${detected_modules}${manual_modules}" \
--include-locales=en,de \
--output target/java-runtime

# ------ CREATE APP BUNDLE --------------------------------------------------

echo "Creating .app bundle"

APP_NAME=PADDL

"$JAVA_HOME"/bin/jpackage \
--type app-image \
--dest target/installer \
--input target/installer/input/libs \
--name "$APP_NAME" \
--main-class com.wecca.canoeanalysis.Main \
--main-jar "$MAIN_JAR" \
--java-options -Xmx2048m \
--java-options --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--java-options --add-opens=java.base/java.lang=ALL-UNNAMED \
--java-options --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--runtime-image target/java-runtime \
--icon src/main/resources/com/wecca/canoeanalysis/images/canoe.icns \
--app-version "$APP_VERSION" \
--vendor "WECCA" \
--copyright "Copyright © 2024 WECCA" \
--mac-package-identifier com.wecca.canoeanalysis \
--mac-package-name PADDL

APP_BUNDLE="target/installer/$APP_NAME.app"
RESOURCES_DIR="$APP_BUNDLE/Contents/Resources"
echo "Copying resources to $RESOURCES_DIR"
echo "Source directory: target/installer/input/resources"

# Note: This can be changed to include other folders with writable resources later if needed
cp -r target/installer/input/resources/com/wecca/canoeanalysis/css "$RESOURCES_DIR/"
cp -r target/installer/input/resources/com/wecca/canoeanalysis/font "$RESOURCES_DIR/"
cp -r target/installer/input/resources/com/wecca/canoeanalysis/settings "$RESOURCES_DIR/"

# ------ PACKAGE INTO DMG ---------------------------------------------------

echo "Creating DMG installer"

"$JAVA_HOME"/bin/jpackage \
--type $INSTALLER_TYPE \
--dest target/installer \
--name PADDL \
--app-image "$APP_BUNDLE" \
--icon src/main/resources/com/wecca/canoeanalysis/images/canoe.icns \
--app-version "$APP_VERSION" \
--vendor "WECCA" \
--copyright "Copyright © 2024 WECCA" \
--mac-package-identifier com.wecca.canoeanalysis \
--mac-package-name PADDL