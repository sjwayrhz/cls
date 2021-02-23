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
                        'return["xk_write_draft","cls_quote_system"]'
                ]
            ]
        ],
        [$class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select the Project from the Dropdown List',
            filterLength: 1,
            filterable: true,
            name: 'container',
            referencedParameters: 'git',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script:
                        'return[\'Could not get container from git Param\']'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script:
                        ''' if (git.equals("xk_write_draft")){
                                return["write-draft-cbeat","write-draft-cworker","write-draft-draftservice","write-draft-mail","write-draft-pushservice","write-draft-research","write-draft-service"]
                            }
                            else if (git.equals("cls_quote_system")){
                                return["new-quote-celery","new-quote-celery-fund","new-quote-celery-plate","new-quote-patch-mail","new-quote-real-time-first","new-quote-real-time-second","new-quote-service","new-quote-service-test","new-quote-teku"]
                            }
                        '''
                ]
            ]
        ],
        string(name: 'branch', defaultValue: 'master', description: 'The target branch')
    ])
])

def createVersion() {
    return new Date().format('yyyyMMddHHmm') + ".${env.JOB_NAME}" + ".${env.BUILD_ID}"
}

pipeline {
    agent none

    environment {
        tag = createVersion()
        repositry = "registry.cn-beijing.aliyuncs.com/xingkuang"
        sshport = "30022"
        xksship = "123.56.75.169"
        quotesship = "47.95.205.190"
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
                  cd $WORKSPACE/../xkdir/${params.git}
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
                       cd $WORKSPACE/../xkdir/${params.git}
                       docker build -f "Dockerfile-"${params.container} -t ${repositry}/${params.container}:${tag} .
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
                       docker push ${repositry}/${params.container}:${tag}
                       docker rmi ${repositry}/${params.container}:${tag}
                    """
                }
            }
        }
        stage('deploy images to kubernetes') {
            agent any
            steps {
                script {
                    sh """
                       if [[ ${params.git} == "xk_write_draft" ]]; then ssh -p ${sshport} ${xksship} "kubectl set image deployment/${params.container} ${params.container}=${repositry}/${params.container}:${tag}";fi
                       if [[ ${params.git} == "cls_quote_system" ]]; then ssh -p ${sshport} ${quotesship} "kubectl set image deployment/${params.container} ${params.container}=${repositry}/${params.container}:${tag}";fi
                    """
                }
            }
        }
    }
}

