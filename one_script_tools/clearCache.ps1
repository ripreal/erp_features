
Param (
    [Parameter ()][string]$userFilter = $null
)

$homeDrive = @($env:homedrive) 
$usersFolder = "$homedrive/users"

$users = Get-ChildItem -Path $usersFolder -Exclude "USR1*"

foreach ($userObj in $users) {

    if ($userFilter -ne $null -and $userFilter -ne "" -and $userFilter -ne $userObj.Name) {
        continue
    }

    $userFolder= $userObj.FullName

    $path83local="$userFolder\AppData\Local\1C\1cv8"
    write-output "Removing 1c cache for " $userFolder 
    if((Test-Path $path83local) -eq "true" )
    {get-childitem $path83local|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse} 

    $path83roaming="$userFolder\AppData\Roaming\1C\1cv8"
    if((Test-Path $path83roaming) -eq "true" )
    {get-childitem $path83roaming|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}

    # Для 8.2
    $path82roaming="$userFolder\AppData\Roaming\1C\1cv82"
    if((Test-Path $path82roaming) -eq "true" )
    {get-childitem $path82roaming|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}

    $path82local="$userFolder\AppData\Local\1C\1cv82"
    if((Test-Path $path82local) -eq "true" )
    {get-childitem $path82local|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}

}