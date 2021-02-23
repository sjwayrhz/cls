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
                        'return["lj-website","ljcms","ljwap","ljweb"]'
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
        repositry = "swr.cn-east-2.myhuaweicloud.com/riskflow"
        sshport = "30022"
        sship = "119.3.23.176"
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
                  cd $WORKSPACE/../risk/${params.git}
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
                       cd $WORKSPACE/../risk/${params.git}
                       npm run build
                       if [[ ${params.git} == "lj-website" ]] || [[ ${params.git} == "lj-wap" ]]; then echo "${params.git}"; else rm -f dist.tar.gz && npm run build && tar -czf dist.tar.gz build;fi;
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
                       ssh -p ${sshport} ${sship} "kubectl set image deployment/${params.git} ${params.git}=${repositry}/${params.git}:${tag}"
                    """
                }
            }
        }
    }
}

