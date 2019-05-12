@Library("shared-libraries")
import io.libs.SqlUtils
import io.libs.ProjectHelpers
import io.libs.Utils

def sqlUtils = new SqlUtils()
def utils = new Utils()
def backupTasks = [:]
def restoreTasks = [:]
def dropDbTasks = [:]
def createDbTasks = [:]
def runHandlers1cTasks = [:]
def updateDbTasks = [:]

pipeline {

    parameters {
        string(defaultValue: "${env.server1c}", description: 'Имя сервера 1с, по умолчанию localhost', name: 'server1c')
        string(defaultValue: "${env.server1cPort}", description: 'Порт агента сервера 1с. По умолчанию 1541', name: 'server1cPort')
        string(defaultValue: "${env.platform1c}", description: 'Версия платформы 1с, например 8.3.12.1685. По умолчанию будет использована последня версия среди установленных', name: 'platform1c')
        string(defaultValue: "${env.serverSql}", description: 'Имя сервера MS SQL. По умолчанию localhost', name: 'serverSql')
        string(defaultValue: "${env.admin1cUser}", description: 'Имя администратора базы тестирования 1с. Должен быть одинаковым для всех баз', name: 'admin1cUser')
        string(defaultValue: "${env.admin1cPwd}", description: 'Пароль администратора базы тестирования 1C. Должен быть одинаковым для всех баз', name: 'admin1cPwd')
        string(defaultValue: "${env.sqlUser}", description: 'Имя администратора сервера MS SQL. Если пустой, то используется доменная  авторизация', name: 'sqlUser')
        string(defaultValue: "${env.sqlPwd}", description: 'Пароль администратора MS SQL.  Если пустой, то используется доменная  авторизация', name: 'sqlPwd')
        string(defaultValue: "${env.templatebases}", description: 'Список баз для тестирования через запятую. Например work_erp,work_upp', name: 'templatebases')
        string(defaultValue: "${env.storages1cPath}", description: 'Необязательный. Пути к хранилищам 1С для обновления копий баз тестирования через запятую. Число хранилищ (если указаны), должно соответствовать числу баз тестирования. Например D:/temp/storage1c/erp,D:/temp/storage1c/upp', name: 'storages1cPath')
        string(defaultValue: "${env.storageUser}", description: 'Необязательный. Администратор хранилищ  1C. Должен быть одинаковым для всех хранилищ', name: 'storageUser')
        string(defaultValue: "${env.storagePwd}", description: 'Необязательный. Пароль администратора хранилищ 1c', name: 'storagePwd')
        string(defaultValue: "master", description: 'Нода дженкинса, на которой запускать пайплайн. По умолчанию master', name: 'jenkinsAgent')
    }

    agent {
        label "master"
    }
    options {
        timeout(time: 8, unit: 'HOURS') 
        buildDiscarder(logRotator(numToKeepStr:'10'))
    }
    stages {
        stage("Preparing environment") {
            steps {
                timestamps {
                    script {
                        assert storageUser
                        //assert storagePwd
                        templatebasesList = utils.lineToArray(templatebases.toLowerCase())
                        storages1cPathList = utils.lineToArray(storages1cPath.toLowerCase())

                        assert storages1cPathList.size() == templatebasesList.size()

                        server1c = server1c.isEmpty() ? "localhost" : server1c
                        serverSql = serverSql.isEmpty() ? "localhost" : serverSql
                        server1cPort = server1cPort.isEmpty() ? "1541" : server1cPort
                        sqlUser = sqlUser.isEmpty() ? "sa" : sqlUser
                        testbase = null
                    }
                }
            }
        }
        stage("Launch") {
            steps {
                timestamps {
                    script {

                        for (i = 0;  i < templatebasesList.size(); i++) {
                            templateDb = templatebasesList[i]
                            storage1cPath = storages1cPathList[i]

                            if (templateDb.isEmpty()) {
                                continue
                            }

                            testbase = "temp_${templateDb}"

                            dropDbTasks["dropDbTask_${testbase}"] = dropDbTask(
                                server1c, 
                                server1cPort, 
                                serverSql, 
                                testbase, 
                                admin1cUser, 
                                admin1cPwd,
                                sqluser,
                                sqlPwd
                            )

                            backupTasks["backupTask_${templateDb}"] = backupTask(
                                serverSql, 
                                templateDb, 
                                backupPath
                            )

                            restoreTasks["restoreTask_${testbase}"] = restoreTask(
                                serverSql, 
                                testbase, 
                                backupPath
                            )

                            createDbTasks["createDbTask_${testbase}"] = createDbTask(
                                server1c,
                                serverSql,
                                platform1c,
                                testbase
                            )

                            updateDbTasks["updateTask_${testbase}"] = updateDbTask(
                                platform1c, 
                                storage1cPath, 
                                storageUser, 
                                storagePwd, 
                                connString, 
                                admin1cUser, 
                                admin1cPwd
                            )

                            runHandlers1cTasks["runHandlers1cTask_${testbase}"] = runHandlers1cTask(
                                server1c, 
                                testbase, 
                                admin1cUser, 
                                admin1cPwd
                            )
                        }

                        parallel dropDbTasks
                        parallel backupTasks
                        parallel restoreTasks
                        parallel createDbTasks
                        parallel updateDbTasks
                        parallel runHandlers1cTasks
                    }
                }
            }
        }
        stage("Executing bdd tests") {
            steps {
                timestamps {
                    script {

                        if (templatebasesList.size() == 0) {
                            return
                        }

                        returnCode = utils.cmd("""runner vanessa --settings tools/vrunner.json 
                            --v8version ${platform1c} 
                            --ibconnection "/S${server1c}\\${testbase}"
                            --db-user ${admin1cUser} 
                            --db-pwd ${admin1cPwd} 
                            --pathvanessa tools/add/bddRunner.epf"""
                        )
                        if (returnCode != 0) {
                            utils.raiseError("Возникла ошибка при запуске ADD на сервере ${server1c} и базе ${testbase}")
                        }
                    }
                }
            }
        }
    }   
    post {
        always {
            script {
                if (currentBuild.result == "ABORTED") {
                    return
                }

                dir ('build/out/allure') {
                    writeFile file:'environment.properties', text:"Build=${env.BUILD_URL}"
                }

                allure includeProperties: false, jdk: '', results: [[path: 'build/out/allure']]
            }
        }
    }
}


def dropDbTask(server1c, server1cPort, serverSql, infobase, admin1cUser, admin1cPwd, sqluser, sqlPwd) {
    return {
        timestamps {
            stage("Deleting base ${infobase} from previous build") {
                def projectHelpers = new ProjectHelpers()
                def utils = new Utils()

                projectHelpers.dropDb(server1c, server1cPort, serverSql, infobase, admin1cUser, admin1cPwd, sqluser, sqlPwd)
            }
        }
    }
}

def createDbTask(server1c, serverSql, platform1c, infobase) {
    return {
        stage("Creating new base  ${infobase}") {
            timestamps {
                projectHelpers = new ProjectHelpers()
                try {
                    projectHelpers.createDb(platform1c, server1c, serversql, infobase, null, false)
                } catch (excp) {
                    echo "Error happened when creating base ${infobase}. Probably base already exists in the ibases.v8i list. Skip the error"
                }
            }
        }
    }
}

def backupTask(serverSql, infobase, backupPath) {
    return {
        stage("Backup ${infobase}") {
            timestamps {
                sqlUtils = new SqlUtils()

                sqlUtils.checkDb(serverSql, infobase)
                sqlUtils.backupDb(serverSql, infobase, backupPath)
            }
        }
    }
}

def runHandlers1cTask(server1c, infobase, admin1cUser, admin1cPwd) {
    return {
        stage("Backup ${infobase}") {
            timestamps {
                projectHelpers = new ProjectHelpers()
                projectHelpers.unlocking1cBase(projectHelpers.getConnString(server1c, infobase), admin1cUser, admin1cPwd)
            }
        }
    }
}

def restoreTask(serverSql, infobase, backupPath) {
    return {
        stage("Restore from backup ${infobase}") {
            timestamps {
                sqlUtils = new SqlUtils()

                sqlUtils.createEmptyDb(serverSql, infobase)
                sqlUtils.restoreDb(serverSql, infobase, backupPath)
                sqlUtils.clearBackups(backupPath)
            }
        }
    }
}

def updateDbTask(platform1c, storage1cPath, storageUser, storagePwd, connString, admin1cUser, admin1cPwd) {
    return {
        stage("loading from storage ${userBase}") {
            timestamps {
                echo "Executing updating from storage..."
                prHelpers = new ProjectHelpers()
          
                echo "Loading from 1C storage..."
                prHelpers.loadCfgFrom1CStorage(storage1cPath, storageUser, storagePwd, userBaseConnString, admin1cUser, admin1cPwd, platform1c)
                prHelpers.updateInfobase(connString, admin1cUser, admin1cPwd, platform1c)
            }
        }
    }
}
