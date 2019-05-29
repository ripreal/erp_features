# Скрипт удаления баз из кластера и СУБД

# --- Ввод параметров подключения ---

Param (
    [Parameter()][string]$server1c = "localhost",
    [Parameter()][string]$serverSql = "localhost",
    [Parameter()][string]$agentPort = "1550",
    [Parameter()][string]$infobase = "test_temp",
    [Parameter()][string]$user = "",
    [Parameter()][string]$passw = "",
    [Parameter()][string]$sqluser = "",
    [Parameter()][string]$sqlPwd = "",
    [Parameter()][string]$fulldrop = $false
)

$baseuser = $user
$basepassw = $passw
$SrvAddr=$server1c + ":$agentPort"
$baseFound = $false

# --- Рабочая часть скрипта ---

try {
    $V83Com=New-Object -ComObject "V83.ComConnector"
    $ServerAgent = $V83Com.ConnectAgent($SrvAddr)
} catch {
    throw $_.Exception.Message
}

$Clusters = $ServerAgent.GetClusters()

$Cluster = $Clusters[0]
$ServerAgent.Authenticate($Cluster,"","")

$WorkingProcesses = $ServerAgent.GetWorkingProcesses($Cluster);
$CurrentWorkingProcess = $V83Com.ConnectWorkingProcess("tcp://"+$server1c+":" + $WorkingProcesses[0].MainPort)
$CurrentWorkingProcess.AddAuthentication($baseuser, $basepassw)
$BaseInfo = $CurrentWorkingProcess.GetInfoBases()
$BaseInfo | ForEach-Object {
    if ($_.Name -eq $infobase) {
        $baseFound = $true
        $Base = $_
    }    
}  

if ($baseFound -eq $true) {

    #блокируем РЗ
    $Base.ScheduledJobsDenied = $true
    $Base.SessionsDenied = $true
    $CurrentWorkingProcess.UpdateInfoBase($Base)

    $connections = $CurrentWorkingProcess.GetInfoBaseConnections($Base)
    foreach ($conn in $connections) {
        try {
            write-host "Reset connection " $conn.AppID "for base " $infobase
            $CurrentWorkingProcess.Disconnect($conn) 
        } 
        catch {

        }
    }

    # Получаем список сессий кластера и прерываем их
     foreach ($CurrCluster in $Clusters) {
        $Sessions = $ServerAgent.GetSessions($CurrCluster)
        if (!($Sessions.Count -eq 0))
        {
            foreach ($Session in $Sessions)
            {
                if ($Session.Infobase.Name -eq $infobase) {
                    write-host "Reset session" $Session.AppID "with user" $Session.UserName
                    try {
                        $ServerAgent.TerminateSession($Cluster, $Session)
                    } catch {
                        write-output $_.Exception.Message
                    }
                }
            }
        }
    }

    # Удаляем базу из кластера
    write-output "Removing database from cluster..."
    #удаляем базу
    try {
        $dir =  $PSScriptRoot 
        $sqluserline = ""
        if ($sqluser.Length -gt 0) {
            $sqluserline = "-U $sqluser"
        } else {
            $sqluserline = "-E"
        }

        $sqlpwdline = ""
        if ($sqlPwd.Length -gt 0) {
            $sqlpwdline = "-P $sqlPwd"
        }

        
        $cmd_text = "sqlcmd -S $serverSql $sqluserline $sqlpwdline -i $dir\set_offline_db.sql -b -v infobase =$infobase"
        cmd.exe /c $cmd_text 
        write-output "Drop base..."
        $CurrentWorkingProcess.DropInfoBase($Base, 0)
        $cmd_text = "sqlcmd -S $serverSql $sqluserline $sqlpwdline -i $dir\set_online_db.sql -b -v infobase =$infobase"
        cmd.exe /c $cmd_text 

        if ($fulldrop -eq $true) {
            $cmd_text = "sqlcmd -S $serverSql $sqluserline $sqlpwdline -i $dir\remove_db.sql -b -v infobase =$infobase"
            cmd.exe /c $cmd_text 
        }

        $users = @($env:UserName)

        # Очищаем кэш для 8.3
        foreach ($user in $users){

            $user = "C:\Users\" + $user

            $path83local="$user\AppData\Local\1C\1cv8"
            write-output "Removing 1c cache for " $user 
            if((Test-Path $path83local) -eq "true" )
            {get-childitem $path83local|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse} 

            $path83roaming="$user\AppData\Roaming\1C\1cv8"
            if((Test-Path $path83roaming) -eq "true" )
            {get-childitem $path83roaming|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}

            # Для 8.2
            $path82roaming="$user\AppData\Roaming\1C\1cv82"
            if((Test-Path $path82roaming) -eq "true" )
            {get-childitem $path82roaming|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}

            $path82local="$user\AppData\Local\1C\1cv82"
            if((Test-Path $path82local) -eq "true" )
            {get-childitem $path82local|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}

        }
    } catch {
        throw $_.Exception.Message
    }
} else {
    write-output "Infobase $infobase to delete not found in cluster 1c"
}

