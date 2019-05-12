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
        string(defaultValue: "${env.server1c}", description: '1c server hostname', name: 'server1c')
        string(defaultValue: "${env.server1cPort}", description: '1c server agent port. By default 1541', name: 'server1cPort')
        string(defaultValue: "${env.platform1c}", description: 'Platform 1c version, for example 8.3.12.1685. If empty then last version available on host  will be used', name: 'platform1c')
        string(defaultValue: "${env.serverSql}", description: 'MS SQL server hostname', name: 'serverSql')
        string(defaultValue: "${env.admin1cUser}", description: 'Infobase administrator user name', name: 'admin1cUser')
        string(defaultValue: "${env.admin1cPwd}", description: 'Infobase administrator 1C password', name: 'admin1cPwd')
        string(defaultValue: "${env.sqlUser}", description: 'MS SQL user name', name: 'sqlUser')
        string(defaultValue: "${env.sqlPwd}", description: 'MS SQL user password', name: 'sqlPwd')
        string(defaultValue: "${env.templatebases}", description: 'List of bases for testing via comma. For example work_erp,work_upp', name: 'templatebases')
        string(defaultValue: "${env.storages1cPath}", description: 'Storages 1c mapped with infobases for testing via comma Amount of storages 1c must conform to amount of infobases. For example D:/temp/storage1c/erp,D:/temp/storage1c/upp', name: 'storages1cPath')
        string(defaultValue: "${env.storageUser}", description: 'Storage 1C username. Must be single for every storage', name: 'storageUser')
        string(defaultValue: "${env.storagePwd}", description: 'Storage 1c user password. Cannot be empty', name: 'storagePwd')
        string(defaultValue: "master", description: 'Jenkins node to launch pipeline', name: 'jenkinsAgent')
    }

    agent {
        label "master"
    }
    options {
        timeout(time: 24, unit: 'HOURS') 
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
                        server1cPort = server1cPort.isEmpty() ? "1551" : server1cPort
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

                        def utils = new Utils()

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
                /*
                publishHTML target: [
                    allowMissing: false, 
                    alwaysLinkToLastBuild: true, 
                    keepAll: false, 
                    reportDir: 'build/out', 
                    reportFiles: 'allure-report/index.html',                 
                    reportName: 'HTML Report', 
                    reportTitles: ''
                ]
                */
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
