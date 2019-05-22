package io.libs;

// Проверяет соединение к БД и наличие базы
//
// Параметры:
//  dbServer - сервер БД
//  infobase - имя базы на сервере БД
//  sqlUser - Необязательный. админ sql базы
//  sqlPwd - Необязательный. пароль админа sql базы
//
def checkDb(dbServer, infobase, sqlUser, sqlPwd) {
    utils = new Utils()

    sqlUserpath = "" 
    if (sqlUser != null && !sqlUser.isEmpty()) {
        sqlUserpath = "-U ${sqlUser}"
    } else {
        sqlUserpath = "-E"
    }

    sqlPwdPath = "" 
    if (sqlPwd != null && !sqlPwd.isEmpty()) {
        sqlPwdPath = "-P ${sqlPwd}"
    }

    returnCode = utils.cmd("sqlcmd -S ${dbServer} ${sqlUserpath} ${sqlPwdPath} -i \"${env.WORKSPACE}/copy_etalon/error.sql\" -b -v restoreddb =${infobase}");
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при при проверке соединения к sql базе ${dbServer}\\${infobase}. Для подробностей смотрите логи")
    }
}

// Создает бекап базы по пути указанному в параметре backupPath
//
// Параметры:
//  dbServer - сервер БД
//  infobase - имя базы на сервере БД
//  backupPath - каталог бекапов
//  sqlUser - Необязательный. админ sql базы
//  sqlPwd - Необязательный. пароль админа sql базы
//
def backupDb(dbServer, infobase, backupPath, sqlUser, sqlPwd) {
    utils = new Utils()

    sqlUserpath = "" 
    if (sqlUser != null && !sqlUser.isEmpty()) {
        sqlUserpath = "-U ${sqlUser}"
    } else {
        sqlUserpath = "-E"
    }

    sqlPwdPath = "" 
    if (sqlPwd != null && !sqlPwd.isEmpty()) {
        sqlPwdPath = "-P ${sqlPwd}"
    }

    returnCode = utils.cmd("sqlcmd -S ${dbServer} ${sqlUserpath} ${sqlPwdPath} -i \"${env.WORKSPACE}/copy_etalon/backup.sql\" -b -v backupdb =${infobase} -v bakfile=\"${backupPath}\"")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибки при создании бекапа sql базы ${dbServer}\\${infobase}. Для подробностей смотрите логи")
    }
}

// Создает пустую базу на сервере БД
//
// Параметры:
//  dbServer - сервер БД
//  infobase - имя базы на сервере БД
//  sqlUser - Необязательный. админ sql базы
//  sqlPwd - Необязательный. пароль админа sql базы
//
def createEmptyDb(dbServer, infobase, sqlUser, sqlPwd) {

    sqlUserpath = "" 
    if (sqlUser != null && !sqlUser.isEmpty()) {
        sqlUserpath = "-U ${sqlUser}"
    } else {
        sqlUserpath = "-E"
    }

    sqlPwdPath = "" 
    if (sqlPwd != null && !sqlPwd.isEmpty()) {
        sqlPwdPath = "-P ${sqlPwd}"
    }

    utils = new Utils()
    returnCode = utils.cmd("sqlcmd -S ${dbServer} ${sqlUserpath} ${sqlPwdPath} -i \"${env.WORKSPACE}/copy_etalon/error_create.sql\" -b -v restoreddb =${infobase}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при создании пустой sql базы на  ${dbServer}\\${infobase}. Для подробностей смотрите логи")
    }
}

// Восстанавливает базу из бекапа
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  dbServer - сервер БД
//  infobase - имя базы на сервере БД
//  backupPath - каталог бекапов
//  sqlUser - Необязательный. админ sql базы
//  sqlPwd - Необязательный. пароль админа sql базы
//
def restoreDb(dbServer, infobase, backupPath, sqlUser, sqlPwd) {
    utils = new Utils()

    sqlUserpath = "" 
    if (sqlUser != null && !sqlUser.isEmpty()) {
        sqlUserpath = "-U ${sqlUser}"
    } else {
        sqlUserpath = "-E"
    }

    sqlPwdPath = "" 
    if (sqlPwd != null && !sqlPwd.isEmpty()) {
        sqlPwdPath = "-P ${sqlPwd}"
    }

    returnCode = utils.cmd("sqlcmd -S ${dbServer} ${sqlUserpath} ${sqlPwdPath} -i \"${env.WORKSPACE}/copy_etalon/restore.sql\" -b -v restoreddb =${infobase} -v bakfile=\"${backupPath}\"")
    if (returnCode != 0) {
         utils.raiseError("Возникла ошибка при восстановлении базы из sql бекапа ${dbServer}\\${infobase}. Для подробностей смотрите логи")
    } 
}


// Удаляет бекапы из сетевой шары
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  backup_path - путь к бекапам
//
def clearBackups(backup_path) {
    utils = new Utils()
    echo "Deleting file ${backup_path}..."
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/deleteFile.os -file\"${backup_path}\"")
    if (returnCode != 0) {
        echo "Error when deleting file: ${backup_path}"
    }    
}