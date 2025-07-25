@echo off
echo Scanning all modules for vulnerabilities...

set modules=registration-module friends-and-chats-module notification-module adoption-and-breeding-module

for %%m in (%modules%) do (
    echo Scanning %%m...
    cd %%m
    mvn org.owasp:dependency-check-maven:check
    cd ..
    echo %%m scan complete
)

echo All dependency scans complete!
echo Check target\dependency-check-report.html in each module
pause