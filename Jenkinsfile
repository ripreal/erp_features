@Library("shared-libraries")
import io.libs.SqlUtils
import io.libs.ProjectHelpers

def sqlUtils = new SqlUtils()
def backupTasks = [:]
def dropdbTasks = [:]
def createDbTasks = [:]
def runHandlers1cTasks = [:]
def updateDbTasks = [:]

pipeline {

    parameters {
        string(defaultValue: "localhost", description: 'Сервер 1С', name: 'server1c')
        string(defaultValue: "", description: 'Платформа 1С, например 8.3.12.1685. Если пустой - то будет использована последняя версия, среди установленных', name: 'platform1c')
        string(defaultValue: "localhost", description: 'Сервер ms sql', name: 'serverSql')
        string(defaultValue: "Administrator", description: 'Имя администратора 1С', name: 'admin1cUser')
        string(defaultValue: "", description: 'Пароль администратора 1С', name: 'admin1cPwd')
        string(defaultValue: "sa", description: 'Имя sql пользователя', name: 'sqlUser')
        string(defaultValue: "", description: 'Пароль sql пользователя', name: 'sqlPwd')
        string(defaultValue: "", description: 'Базы для тестирования, через запятую. Например work_erp,work_upp', name: 'templatebases')
        string(defaultValue: "", description: 'Хранилища баз для тестирования, через запятую. Количество должно соответсвовать базам для тестирования. Например D:/temp/storage1c/erp,D:/temp/storage1c/upp', name: 'storages1cPath')
        string(description: 'Имя пользователя хранилищ 1С. Не может быть пустым', name: 'storageUser')
        string(description: 'Пароль пользователя хранилищ 1с. Не может быть пустым', name: 'storagePwd')
        string(defaultValue: "master", description: 'Нода дженкинса для запуска', name: 'jenkinsAgent')
    }

    agent {
        label "master"
    }
    options {
        timeout(time: 24, unit: 'HOURS') 
        buildDiscarder(logRotator(numToKeepStr:'10'))
    }
    stages {
        stage("Подготовка параметров") {
            steps {
                timestamps {
                    script {
                        assert storageUser
                        assert storagePwd
                    }
                }
            }
        }
        stage("Подготовка параметров") {
            steps {
                timestamps {
                    script {

                        agentPort = "1551"
                        templatebasesList = templatebases.toLowerCase().replaceAll("\\s", "").split(",")
                        storages1cPathList = storages1cPathList.toLowerCase().replaceAll("\\s", "").split(",")

                        assert storages1cPathList.size() == templatebasesList.size()

                        for (i = 0;  i < templatebasesList.size(); i++) {
                            templateDb = templatebasesList[i]
                            storage1cPath = storages1cPathList[i]

                            if (templateDb.isEmpty()) {
                                continue
                            }

                            testbase = "temp_${templateDb}"

                            dropDbTasks["dropDbTask_${testbase}"] = dropDbTask(
                                server1c, 
                                agentPort, 
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
                    }
                }
            }
        }

        stage("Запуск") {
            steps {
                timestamps {
                    script {
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
        stage("Выполнение bdd тестирования") {
            steps {
                timestamps {
                    script {

                        if (testbase == null) {
                            return
                        }

                        utils.cmd("""runner vanessa --settings tools/vrunner.json 
                            --v8version ${platform1c} 
                            --ibconnection "/S${server1c}\\${testbase}"
                            --db-user ${admin1cUser} 
                            --db-pwd ${admin1cPwd} 
                            --pathvanessa tools/add/bddRunner.epf"""
                        )
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
                    writeFile file:'environment.properties', text:"Build=${jenkinsIntegration.buildURL()}"
                }

                allure includeProperties: false, jdk: '', results: [[path: 'build/out/allure']]
                
                publishHTML target: [
                    allowMissing: false, 
                    alwaysLinkToLastBuild: true, 
                    keepAll: false, 
                    reportDir: 'build/out', 
                    reportFiles: 'allure-report/index.html',                 
                    reportName: 'HTML Report', 
                    reportTitles: ''
                ]
            }
        }
    }
}


def dropDbTask(server1c, agentPort, serverSql, infobase, admin1cUser, admin1cPwd, sqluser, sqlPwd) {
    return {
        timestamps {
            stage("Удаление тестовой базы ${infobase}") {
                def projectHelpers = new ProjectHelpers()
                def utils = new Utils()

                projectHelpers.dropDb(server1c, agentPort, serverSql, infobase, admin1cUser, admin1cPwd, sqluser, sqlPwd)
            }
        }
    }
}

def createDbTask(server1c, serverSql, platform1c, infobase) {
    return {
        stage("Создание тестовой базы ${infobase}") {
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
        stage("Резервное копирование ${infobase}") {
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
        stage("Резервное копирование ${infobase}") {
            timestamps {
                projectHelpers = new ProjectHelpers()
                projectHelpers.unlocking1cBase(projectHelpers.getConnString(server1c, infobase), admin1cUser, admin1cPwd)
            }
        }
    }
}

def restoreTask(serverSql, infobase, backupPath) {
    return {
        stage("Восстановление базы ${infobase}") {
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
