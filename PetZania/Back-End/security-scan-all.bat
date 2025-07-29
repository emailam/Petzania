@echo off
echo Starting comprehensive security scan for Petzania Backend
echo =============================================================

echo Step 1: Scanning dependencies...
call scan-dependencies.bat

echo Step 2: Scanning code...
call scan-code.bat

echo Step 3: Scanning containers...
call scan-containers.bat

echo Security scan complete!
echo Check the following reports:
echo    - Dependency reports: [module]\target\dependency-check-report.html
echo    - Code reports: [module]\target\site\spotbugs.html
echo    - Container reports: In terminal output above

pause