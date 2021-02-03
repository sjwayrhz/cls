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
                        'return["paw-api","paw-cron","paw-server","store-cron","store-server", \
                        "paw-sole-spider","paw-spider-app","paw-spider-hudongpingtai","paw-spider-pipeline","paw-spider-web", \
                        "paw-frontend","paw-mercury-parser","store-web"]'
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
        repositry = "swr.cn-east-2.myhuaweicloud.com/lanjing"
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
                  cd $WORKSPACE/../${params.git}
                  pwd
                  if [[ ${params.git} == "paw-server" ]];then cd "djangoy";fi
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
                       cd $WORKSPACE/../${params.git}
                       if [[ ${params.git} == "paw-server" ]];then cd "djangoy";fi
                       if [[ ${params.git} == "paw-frontend" || ${params.git} == "paw-mercury-parser"|| ${params.git} == "store-web" ]];then rm -f dist.tar.gz && npm run build && tar -czf dist.tar.gz dist;fi
                       docker build -t ${repositry}/${params.git}:${tag} .
                       """
                }
            }
        }
        stage('list docker images') {
            agent any
            steps {
                script {
                    sh '''
                       docker images
                    '''
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
                       ssh -p 30022 122.112.229.98 "kubectl set image deployment/${params.git} ${params.git}=${repositry}/${params.git}:${tag}"
                    """
                }
            }
        }
    }
}

