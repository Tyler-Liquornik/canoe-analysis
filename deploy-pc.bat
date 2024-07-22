@ECHO OFF

rem A batch script to easily deploy a self contained PC app for PADDL
rem Modified from source: https://github.com/dlemmermann/JPackageScriptFX

rem You need to have the WIX toolset installed for this script to work
rem Download https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip ]
rem Then add the binaries folder to the PATH env variable

rem ------ ENVIRONMENT --------------------------------------------------------

rem Environment variables must be set properly for this script to work.
rem A generic example:
rem $env:PROJECT_VERSION="1.0-SNAPSHOT"
rem $env:APP_VERSION="1.0.0"
rem $env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-21.0.2"
rem $env:PATH="$env:JAVA_HOME\bin;$env:PATH"

set JAVA_VERSION=21
set MAIN_JAR=CanoeAnalysis-%PROJECT_VERSION%.jar
set INSTALLER_TYPE=exe
set APP_NAME=PADDL

echo java home: %JAVA_HOME%
echo project version: %PROJECT_VERSION%
echo app version: %APP_VERSION%
echo main JAR file: %MAIN_JAR%
ech %PATH%

rem ------ SETUP DIRECTORIES AND FILES ----------------------------------------

IF EXIST target\java-runtime rmdir /S /Q target\java-runtime
IF EXIST target\installer rmdir /S /Q target\installer

mkdir target\installer\input\libs
mkdir target\installer\input\resources

xcopy /S /Q target\libs\* target\installer\input\libs\
copy target\%MAIN_JAR% target\installer\input\libs\
xcopy /S /Q src\main\resources\* target\installer\input\resources\

rem ------ REQUIRED MODULES ---------------------------------------------------

echo detecting required modules

"%JAVA_HOME%\bin\jdeps" ^
  -q ^
  --multi-release %JAVA_VERSION% ^
  --ignore-missing-deps ^
  --class-path "target\installer\input\libs\*" ^
  --print-module-deps target\classes\com\wecca\canoeanalysis\Main.class > temp.txt

set /p detected_modules=<temp.txt

echo detected modules: %detected_modules%

rem ------ MANUAL MODULES -----------------------------------------------------

set manual_modules=,jdk.localedata,jdk.unsupported,java.xml,java.scripting,jdk.zipfs,java.rmi
echo manual modules: %manual_modules%

rem ------ RUNTIME IMAGE ------------------------------------------------------

echo creating java runtime image

call "%JAVA_HOME%\bin\jlink" ^
  --strip-native-commands ^
  --no-header-files ^
  --no-man-pages ^
  --compress=2 ^
  --strip-debug ^
  --add-modules %detected_modules%%manual_modules% ^
  --include-locales=en,de ^
  --output target\java-runtime

rem ------ PACKAGING ----------------------------------------------------------

echo creating bundle

call "%JAVA_HOME%\bin\jpackage" ^
  --type app-image ^
  --dest target\installer ^
  --input target\installer\input\libs ^
  --name %APP_NAME% ^
  --main-class com.wecca.canoeanalysis.Main ^
  --main-jar CanoeAnalysis-%PROJECT_VERSION%.jar ^
  --java-options -Xmx2048m ^
  --java-options --add-opens=java.base/java.lang.reflect=ALL-UNNAMED ^
  --java-options --add-opens=java.base/java.lang=ALL-UNNAMED ^
  --java-options --add-opens=java.base/java.lang.reflect=ALL-UNNAMED ^
  --runtime-image target\java-runtime ^
  --resource-dir target\installer\input\resources ^
  --app-version %APP_VERSION% ^
  --copyright "Copyright © 2024 WECCA"

set RESOURCES_DIR=target\installer\%APP_NAME%
mkdir %RESOURCES_DIR%
echo Copying resources to %RESOURCES_DIR%
echo Source directory: target\installer\input\resources

rem Note: This can be changed to include other folders with writable resources later if needed
xcopy /S /Q target\installer\input\resources\com\wecca\canoeanalysis\css %RESOURCES_DIR%\
xcopy /S /Q target\installer\input\resources\com\wecca\canoeanalysis\font %RESOURCES_DIR%\
xcopy /S /Q target\installer\input\resources\com\wecca\canoeanalysis\settings %RESOURCES_DIR%\

rem ------ PACKAGE INTO INSTALLER ------------------------------------------------

echo creating EXE installer

call "%JAVA_HOME%\bin\jpackage" ^
  --type %INSTALLER_TYPE% ^
  --dest target\installer ^
  --name %APP_NAME% ^
  --app-image target\installer\%APP_NAME% ^
  --app-version %APP_VERSION% ^
  --copyright "Copyright © 2024 WECCA" ^
  --win-dir-chooser ^
  --win-shortcut ^
  --win-per-user-install ^
  --win-menu