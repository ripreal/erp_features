package io.bit

// Создает базу в кластере через RAS или пакетный режим. Для пакетного режима есть возможность создать базу с конфигурацией
//
// Параметры:
//  platform - номер платформы 1С, например 8.3.12.1529
//  server1c - сервер 1c
//  serversql - сервер 1c 
//  base - имя базы на сервере 1c и sql
//  cfdt - файловый путь к dt или cf конфигурации для загрузки. Только для пакетного режима!
//  isras - если true, то используется RAS для скрипта, в противном случае - пакетный режим
//
def createDb(platform, server1c, serversql, base, cfdt, isras) {
    utils = new Utils()

    cfdtpath = ""
    if (cfdt != null && !cfdt.isEmpty()) {
        cfdtpath = "-cfdt ${cfdt}"
    }

    israspath = ""
    if (isras) {
        israspath = "-isras true"
    }
    returnCode = utils.cmd("oscript one_script_tools/dbcreator.os -platform ${platform} -server1c ${server1c} -serversql ${serversql} -base ${base} ${cfdtpath} ${israspath}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при создании базы ${base} в кластере ${serversql}")
    }
}

// Убирает в 1С базу окошки с тем, что база перемещена, интернет поддержкой, очищает настройки ванессы
//
// Параметры:
//  сonnection_string - путь к 1С базе.
//  admin_1c_user - имя админа 1С базы
//  admin_1c_password - пароль админа 1С базы
//
def unlocking1cBase(utils, onnection_string, admin_1c_user, admin_1c_password) {
    utils = new Utils()
    utils.cmd("runner run --execute ${env.WORKSPACE}/one_script_tools/unlockBase1C.epf --command \"-locktype unlock -usersettingsprovider FILE\" --db-user ${admin_1c_user} --db-pwd ${admin_1c_password} --ibconnection=${onnection_string}")
}

def getConnString(server1c, infobase) {
    return "/S${server1c}\\${infobase}"
}

// Удаляет базу из кластера через powershell.
//
// Параметры:
//  shortPlatform - номер платформы 1С, например 8.3.12
//  baseServer - сервер sql 
//  base - имя базы на сервере sql
//  admin1cUser - имя администратора 1С в кластере для базы
//  admin1cPwd - пароль администратора 1С в кластере для базы
//  sqluser - юзер sql
//  sqlPwd - пароль sql
//  fulldrop - если true, то удаляется база из кластера 1С и sql сервера
//
def dropDb(server1c, agentPort, serverSql, base, admin1cUser, admin1cPwd, sqluser, sqlPwd, fulldrop = false) {

    utils = new Utils()

    fulldropLine = "";
    if (fulldrop) {
        fulldropLine = "-fulldrop true"
    }

    sqluserLine = "";
    if (sqluser != null && !sqluser.isEmpty()) {
        sqluserLine = "-sqluser ${sqluser}"
    }

    sqlpasswLine = "";
    if (sqlPwd != null && !sqlPwd.isEmpty()) {
        sqlpasswLine = "-sqlPwd ${sqlPwd}"
    }

    returnCode = utils.cmd("powershell -file ${env.WORKSPACE}/copy_etalon/drop_db.ps1 -server1c ${server1c} -serverSql ${serverSql} -infobase ${base} -user ${admin1cUser} -passw ${admin1cPwd} ${sqluserLine} ${sqlpasswLine} -fulldrop ${fulldropLine}")
    if (returnCode != 0) { 
        eror "error when deleting base with COM ${server1c}\\${base}. See logs above fore more information."
    }
}

// Загружает в базу конфигурацию из 1С хранилища. Базу желательно подключить к хранилищу под загружаемыйм пользователем,
//  т.к. это даст буст по скорости загрузки.
//
// Параметры:
//
//
def loadCfgFrom1CStorage(storageTCP, storageUser, storagePwd, connString, admin1cUser, admin1cPassword, platform) {
    utils = new Utils()

    returnCode = utils.cmd("runner loadrepo --storage-name ${storageTCP} --storage-user ${storageUser} --storage-pwd ${storagePwd} --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword} --v8version ${platform}")
    if (returnCode != 0) {
         utils.raiseError("Загрузка конфигурации из 1С хранилища  ${storageTCP} завершилась с ошибкой. Для подробностей смотрите логи.")
    }
}

// Обновляет базу в режиме конфигуратора. Аналог нажатия кнопки f7
//
// Параметры:
//
//  connString - строка соединения, например /Sdevadapter\template_adapter_adapter
//  platform - полный номер платформы 1с
//  admin1cUser - администратор базы
//  admin1cPassword - пароль администратора базы
//
def updateInfobase(connString, admin1cUser, admin1cPassword, platform) {

    utils = new Utils()
    admin1cUserLine = "";
    if (!admin1cUser.isEmpty()) {
        admin1cUserLine = "--db-user ${admin1cUser}"
    }
    admin1cPassLine = "";
    if (!admin1cPassword.isEmpty()) {
        admin1cPassLine = "--db-pwd ${admin1cPassword}"
    }

    returnCode = utils.cmd("runner updatedb --ibconnection ${connString} ${admin1cUserLine} ${admin1cPassLine} --v8version ${platform}")
    if (returnCode != 0) {
        utils.raiseError("Обновление базы ${connString} в режиме конфигуратора завершилось с ошибкой. Для дополнительной информации смотрите логи")
    }
}
