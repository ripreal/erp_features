cd /d %~dp0

"C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" -file drop_db.ps1
if NOT %ERRORLEVEL% == 0 (
    echo "Script failed  when executing 64-bit powershell drop_db script. Let's try 32-bit..."
    "C:\Windows\SysWOW64\WindowsPowerShell\v1.0\powershell.exe" -file drop_db.ps1
)

rem 32-bit PowerShell 	C:\Windows\SysWOW64\WindowsPowerShell\v1.0\powershell.exe
rem 64-bit PowerShell 	C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe