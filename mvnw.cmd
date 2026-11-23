@echo off
set "IDEA_MAVEN=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
if exist "%IDEA_MAVEN%" (
    call "%IDEA_MAVEN%" %*
) else (
    call mvn %*
)
