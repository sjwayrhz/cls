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
                        'return["cls-es","cls-sync"]'
                ]
            ]
        ],
        [$class: 'StringParameterDefinition', defaultValue: 'master', description: '', name: 'branch']
    ])
])

def createVersion() {
    return new Date().format('yyyyMMddHHmm') + ".${env.BUILD_ID}"
}

pipeline {
    agent none

    environment {
        tag = createVersion()
        repositry = "swr.cn-east-2.myhuaweicloud.com/yb7"
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
                    pwd
                    git reset --hard HEAD
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
                        if  [[ ${params.git} == "cls-sync" ]];then cd "sync/deploy";fi
                        if  [[ ${params.git} == "cls-es" ]];then cd "es/deploy";fi
                        rm -f ${params.git}
                        CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${params.git} ../main.go
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
                       ssh -p 30022 122.112.150.241 "kubectl set image deployment/${params.git} ${params.git}=${repositry}/${params.git}:${tag}"
                    """
                }
            }
        }
    }
}

