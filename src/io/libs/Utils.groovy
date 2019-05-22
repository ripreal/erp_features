package io.libs

import java.util.Date;
import java.text.SimpleDateFormat;
import groovy.json.JsonSlurper

// Выполняет команду в среде ОС Windows (batch) или Linux (bash) и возвращает статус операции
//
// Параметры:
//  command - строка команды, которую нужно выполнить
//  workDir  - рабочий каталог для команды 
//
// Возвращаемое значение
//  Integer - код выполнения операции
//
def cmd(command, workDir = "") {
    
    if (!workDir.isEmpty()) {
        command = "${getWorkspaceLine(workDir)} ${command}"
    }

    def returnCode = 0
    if (isUnix()) {
        returnCode = sh script: "${command}", returnStatus: true
    } else {
        returnCode = bat script: "chcp 65001\n${command}", returnStatus: true
    }
    return returnCode
}

// Выполняет команду в среде ОС Windows (batch) или Linux (bash) и возвращает вывод
//
// Параметры:
//  command - строка команды, которую нужно выполнить
//
// Возвращаемое значение
//  String - вывод в консоль
//
def cmdOut(command) {
    if (isUnix()) {
        return sh (script: command, returnStdout: true)
    } else {
        return bat (script: command, returnStdout: true)
    }
}

// Получает сокращенный номер платформы 1С без номера сборки. Например
// 8.3.12.1529 => 8.3.12
//
// Параметры:
//  fullPlatformName - номер платформы 1С
//
// Возвращаемое значение
//  String - сокращенный номер платформы
//
def shortPlatformName(fullPlatformName) {
    return fullPlatformName.substring(0, 6)
} 

// Возвращает Timestamp вида yyyyMMdddss
//
// Возвращаемое значение
//  String - сгенерированный timestamp
//
def currentDateStamp() {
    dateFormat = new SimpleDateFormat("yyyyMMdddss");
    date = new Date();
    return  dateFormat.format(date);
}

// Возвращает отформатированную текущую дату
//
// Параметры:
//  format - формат даты, например yyyyMMdddss
//
// Возвращаемое значение
//  String - текущая дата
//
def currentDateWithFormat(format) {
    dateFormat = new SimpleDateFormat(format);
    date = new Date();
    return  dateFormat.format(date);
}

// Выполняет клонирование репозитория со всеми сабмодулями
//
// Параметры:
//  repo - url к репозиторию, включая @ - имя пользователя авторизации
//  credentialsId - SHH-key bitbucket-а или имя пользователя при обычной авторизации
//  targetDir - каталог, куда клонировать репозиторий 
//  branch - имя ветки, которую клонировать
//
def checkoutSCM(repo, targetDir, branch) {
    checkout changelog: false,
    poll: false,
    scm: [$class: 'GitSCM', 
        branches: [[name: branch]],
        doGenerateSubmoduleConfigurations: false,
        submoduleCfg: [], 
        userRemoteConfigs: [[credentialsId: "bitbuket_user", url: repo]],
        extensions: [
            [$class: 'CleanBeforeCheckout'],
            [$class: 'RelativeTargetDirectory', 
                relativeTargetDir: targetDir],
            [$class: 'SubmoduleOption',
                disableSubmodules: false,
                parentCredentials: true,
                recursiveSubmodules: true,
                trackingSubmodules: true,
                reference: '',
                trackingSubmodules: false]]]
}

// Проверяет доступность хоста через TCP IP протокол
//
// Параметры:
//  server - имя сервер, который нужно проверить
//  pingNode - если true, тогда проверяется еще нода дженкинса
//
// Возвращаемое значение
//  boolean - если true, то сервер доступен
//
def pingServer(server, pingNode = false) {
    established = true;
    try {
        InetAddress.getByName("${server}.bit-erp.loc");
    } catch (error) {
        established = false
    }
    if (established && pingNode) {
        established = pingJenkinsAgent(server)
    }
    return established;
}


// Проверяет наличие ноды дженкинса через API
//
// Параметры:
//  serlabelver - имя ноды дженкинса, который нужно проверить
//
// Возвращаемое значение
//  boolean - если true, то такая нода существует
//
def pingJenkinsAgent(label) {
    def labelObj = Jenkins.instance.getLabel(label)
    return (labelObj.nodes.size() + labelObj.clouds.size()) > 0
}

// Парсит json в объектную модель 
//
// Параметры:
//  json - строка с json
//
// Возвращаемое значение
//  Object - объект с json
//
def parseJSON(json) {
    return new JsonSlurper().parseText(json)
}

// Переходит в указанный каталог ОС. На данный момент поддерживается только Windows
//
// Параметры:
//  destFolder - относительный путь, по которому нужно перейти
//
def moveToFolder(destFolder) {
    if (isUnix()) {
        raiseError("not implemented")
    } else {
        cmd("cd ${destFolder}")
    }
}

// Удаляет произвольный файл или каталог. Если файл не найден, то ошибка не вызывается.
//
// Параметры:
//  filename - путь к файлу или каталогу
//
def deleteFile(filename) {
    echo "Deleting file ${filename}..."
    returnCode = cmd("oscript ${env.WORKSPACE}/one_script_tools/deleteFile.os -file ${filename}")
    if (returnCode != 0) {
        raiseError("Возникла ошибка при удалении файла: ${filename}")
    }
}

// Записывает произвольную строку в файл. Записываются только уникальные строки
//
// Параметры:
//  file - путь к файлу
//  line - строка текста
//
def writeLineToFile(file, line) {
    utils = new Utils()
    retunStatus = utils.cmd("oscript one_script_tools/writeLineToFile.os -file ${file} -line \"${line}\"")
    if (retunStatus != 0) {
        raiseError("Возникла ошибка при записи строки в файл ${file}. Для подробностей смотрите логи")
    }
}

//Возвращает сроку случайных символов заданной длинны
//Парвметры:
//len - длинна строки
//   

def giveMeKey(len = 8) {
    letter = "01234ZXCVBNML56789qwertyuiKJHGFDSAQWERToplkjhgfdsazxcYUIOPvbnm".toCharArray()
    randomString = org.apache.commons.lang.RandomStringUtils.random(len, 0, 40, false, false, letter)
    return randomString
}

// Создает и прикрепляет артефакт к сборке в виде текстового файла. Каждый вызов метода перезатирает артефакт.
//
// Параметры:
//  text - текст для помещения в артефакт
//
def setBuildResultMessage(text){
    def fileName = 'BuildResultMessage.txt'
    writeFile(file: fileName, text: text, encoding: "UTF-8")
    step([$class: 'ArtifactArchiver', artifacts: fileName, fingerprint: true])
}

// Блокирует поток выполнения кода на переданное число секунд. Работает только под Windows
//
// Параметры:
//  sec - секунд, на которое нужно заблокировать поток.
//
def pauseThread(sec) {
    if (isUnix()) {
        raiseError("not implemented")
    } else {
        cmd("ping 127.0.0.1 -n ${sec} -w 1000 >NUL")
    }
}

// Копирует файл в файловой системе ноды дженкинса.
//
// Параметры:
//  sourceFile - файл источник
//  destFile - файл приемник
//
def copyFile(sourceFile, destFile) {
    cmd("oscript one_script_tools/copyFile.os -sourcefile ${sourceFile} -destfile ${destFile}")
}

// Вызывает ошибку, которая прекращает исполнение кода и прикрепляет текст ошибки архивом к сборке
//
// Параметры:
//
//  errorText - читаемое описание ошибки
//
def raiseError(errorText) {
    utils = new Utils()
    utils.setBuildResultMessage(errorText)
    error errorText
}

// Возвращает команду консоли для выполнения батника в указанном каталоге.
// Пример использования: utils.cmd("${getWorkspaceLine(workspace)} git init")
//
// Параметры:
//
//  workspace - каталог, в который нужно перейти
//
def getWorkspaceLine(workspace = "") {
    return workspace.isEmpty() ? "" : "cd ${workspace} &"
}

// Возвращает список файлов и каталогов, находящихся внутри каталога. Метод работает без рекурсии
//
// Параметры:
//
//  path - каталог, в котором нужно искать файлы
//
@NonCPS
def getAllFiles(path) {
    
    if (env['NODE_NAME'].equals("master")) {
        File localPath = new File(path)
        rootPath = new hudson.FilePath(localPath);
    } else {
        rootPath = new hudson.FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), path);
    }
    
    def list = []
    for (subPath in rootPath.list()) {
        list.add(subPath)
    }
    return list
}

// Конвертирует строку в массив по сплиттеру
//
// Параметры:
//  line - строка с разделителями
//
// Возвращаемое значение
//  Array - массив строк
//
def lineToArray(line, splitter = ",") {
    dirtArray = line.replaceAll("\\s", "").split(",")
    cleanArray = []
    for (item in dirtArray) {
        if (!item.isEmpty()) {
            cleanArray.add(item)
        }
    }
    return cleanArray
}