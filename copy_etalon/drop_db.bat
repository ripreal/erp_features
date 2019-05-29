cd /d %~dp0

set server1c=%1
set agentPort=%2
set serverSql=%3
set base=%4
set admin1cUser=%5
set admin1cPwd=%6
set sqluser=%7
set sqlpassw=%8
set fulldrop=%9

if "%admin1cUser%" == "" (
    set admin1cUserLine=""
)else (
    set admin1cUserLine= -user %admin1cUser%
)

if "%admin1cPwd%" == "" (
    set admin1cPwdLine=""
)else (
    set admin1cPwdLine= -passw %admin1cPwd%
)


if "%sqluser%" == "" (
    set sqluserLine=""
)else (
    set sqluserLine= -sqluser %sqluser%
)

if "%sqlpassw%" == "" (
    set sqlpasswLine=""
)else (
    set sqlpasswLine= -sqlPwd %sqlpassw%
)

if "%fulldrop%" == "" (
    set fulldropLine =""
)else (
    set fulldropLine = -fulldrop true
)

"C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" -file drop_db.ps1 -server1c %server1c% -agentPort %agentPort% -serverSql %serverSql% -infobase %base% %admin1cUserLine% %admin1cPwdLine% %sqluserLine% %sqlpasswLine% %fulldropLine%
if NOT %ERRORLEVEL% == 0 (
    echo "Script failed  when executing 64-bit powershell drop_db script. Let's try 32-bit..."
    "C:\Windows\SysWOW64\WindowsPowerShell\v1.0\powershell.exe" -file drop_db.ps1 -server1c %server1c% -agentPort %agentPort% -serverSql %serverSql% -infobase %base% %admin1cUserLine% %admin1cPwdLine% %sqluserLine% %sqlpasswLine% %fulldropLine%
)

rem 32-bit PowerShell 	C:\Windows\SysWOW64\WindowsPowerShell\v1.0\powershell.exe
rem 64-bit PowerShell 	C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe