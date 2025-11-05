@echo off
echo Scanning Docker containers...

echo Building containers...
docker-compose build

echo Scanning containers...
trivy image --severity HIGH,CRITICAL registration-module:latest
trivy image --severity HIGH,CRITICAL friends-module:latest
trivy image --severity HIGH,CRITICAL notification-module:latest
trivy image --severity HIGH,CRITICAL adoption-and-breeding-module:latest

echo 🎉 Container scanning complete!
pause