call mvn clean install
rmdir .deploy /s /q
mkdir .deploy
xcopy api\target\albus-api.jar .deploy\
xcopy core\target\albus-core.jar .deploy\
xcopy plus\target\albus-plus.jar .deploy\
