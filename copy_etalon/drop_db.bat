cd /d %~dp0

set server1c=%1
set agentPort=%2
set serverSql=%3
set base=%4
set admin1cUserLine=%5
set sqluserLine=%6
set sqlpasswLine=%7
set fulldropLine=%8


"C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" -file drop_db.ps1 -server1c %server1c% -agentPort %agentPort% -serverSql %serverSql% -infobase %base% %admin1cUserLine% %admin1cPwdLine% %sqluserLine% %sqlpasswLine% %fulldropLin%
if NOT %ERRORLEVEL% == 0 (
    echo "Script failed  when executing 64-bit powershell drop_db script. Let's try 32-bit..."
    "C:\Windows\SysWOW64\WindowsPowerShell\v1.0\powershell.exe" -file drop_db.ps1 -server1c %server1c% -agentPort %agentPort% -serverSql %serverSql% -infobase %base% %admin1cUserLine% %admin1cPwdLine% %sqluserLine% %sqlpasswLine% %fulldropLin%
)

rem 32-bit PowerShell 	C:\Windows\SysWOW64\WindowsPowerShell\v1.0\powershell.exe
rem 64-bit PowerShell 	C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe