@echo off
echo Scanning code for security issues...

set modules=registration-module friends-and-chats-module notification-module adoption-and-breeding-module

for %%m in (%modules%) do (
    echo Scanning code in %%m...
    cd %%m
    mvn clean compile spotbugs:check
    cd ..
    echo %%m code scan complete
)

echo Code scanning complete!
pause