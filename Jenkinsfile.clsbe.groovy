properties([
    parameters([
        [$class: 'ChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select the git Name from the Dropdown List',
            filterLength: 1,
            filterable: true,
            name: 'git',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script:
                        'return[\'Could not get git name\']'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script:
                        'return["cls-es","cls-sync","cailian-update-service","cailianpress-credits-mall","cailianpress-openapi","cailianpress-admin","cailianpress-api", \
                        "cailianpress-cron","cailianpress-featured-api","cailianpress-live","cailianpress-vod","core-service","cailianpress-search","cailianpress-vod"]'
                ]
            ]
        ],
        [$class: 'StringParameterDefinition', defaultValue: 'master', description: '', name: 'branch']
    ])
])

def createVersion() {
    return new Date().format('yyyyMMddHHmm') + ".${env.JOB_NAME}" + ".${env.BUILD_ID}"
}

pipeline {
    agent none

    environment {
        tag = createVersion()
        repositry = "swr.cn-east-2.myhuaweicloud.com/yb7"
        sshport = "30022"
        sship = "122.112.212.229"
    }

    stages {
        stage ('define tags') {
            agent any
            steps {
                echo "${tag}"
            }
        }
        stage ('git pull') {
            agent any
            steps {
                sh """                    
                    cd $WORKSPACE/../cailian
                    if  [[ ${params.git} == "cls-sync" ]];then cd "sync";fi
                    if  [[ ${params.git} == "cls-es" ]];then cd "es";fi
                    if  [[ ${params.git} == "cailian-update-service" ]];then cd "update-service";fi
                    if  [[ ${params.git} == "cailianpress-credits-mall" ]];then cd "credits-mall";fi
                    if  [[ ${params.git} == "cailianpress-openapi" ]];then cd "openapi";fi
                    if  [[ ${params.git} == "cailianpress-admin" ]];then cd "admin";fi
                    if  [[ ${params.git} == "cailianpress-api" ]];then cd "api";fi
                    if  [[ ${params.git} == "cailianpress-cron" ]];then cd "cron";fi
                    if  [[ ${params.git} == "cailianpress-featured-api" ]];then cd "featured-api";fi
                    if  [[ ${params.git} == "cailianpress-live" ]];then cd "live_streaming";fi
                    if  [[ ${params.git} == "cailianpress-vod" ]];then cd "vod";fi
                    if  [[ ${params.git} == "cailianpress-search" ]];then cd "vod";fi
                    if  [[ ${params.git} == "core-service" ]];then cd "core";fi
                    pwd
                    git reset --hard HEAD
                    git checkout -B ${params.branch}
                    git checkout -b ${tag}
                    git branch -D ${params.branch}
                    git checkout -b ${params.branch}
                    git branch -D ${tag}
                    git pull origin ${params.branch}
                  """                
            }
        }
        stage('build docker image') {
            agent any
            steps {
                script {
                    sh """
                        cd $WORKSPACE/../cailian
                        if  [[ ${params.git} == "cls-es" ]];then cd "es/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cls-sync" ]];then cd "sync/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailian-update-service" ]];then cd "update-service/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-credits-mall" ]];then cd "credits-mall/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-openapi" ]];then cd "openapi/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-admin" ]];then cd "admin/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-api" ]];then cd "api/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-cron" ]];then cd "cron/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-featured-api" ]];then cd "featured-api/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-live" ]];then cd "live_streaming/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-vod" ]];then cd "vod/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "cailianpress-search" ]];then cd "search/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go;fi
                        if  [[ ${params.git} == "core-service" ]];then cd "core/deploy" && rm -f ${params.git} && CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../core.go;fi
                        docker build -t ${repositry}/${params.git}:${tag} .
                        docker images
                       """
                }
            }
        }
        stage('push docker images') {
            agent any
            steps {
                script {
                    sh """
                       docker push ${repositry}/${params.git}:${tag}
                       docker rmi ${repositry}/${params.git}:${tag}
                    """
                }
            }
        }
        stage('deploy images to kubernetes') {
            agent any
            steps {
                script {
                    sh """
                       ssh -p ${sshport} ${sship} "kubectl set image deployment/${params.git} ${params.git}=${repositry}/${params.git}:${tag}"
                    """
                }
            }
        }
    }
}

