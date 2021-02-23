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
                        'return["cls-web","in-app-views","cailianpress-admin-web","h5quotes", \
                        "cls-order","cailianpress-wap","cailianpress-statics","stib-cailianpress-web","stib-in-app-views","lj-website", \
                        "ljwap","ljcms","hongan-listed-company-web"]'
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
                  cd $WORKSPACE/../cailianfe/${params.git}
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
                       cd $WORKSPACE/../cailianfe/${params.git}
                       if [[ ${params.git} == "cailianpress-wap" ]] || [[ ${params.git} == "cls-order" ]] ||  [[ ${params.git} == "cailianpress-admin-web" ]]; then echo "${params.git}"; else rm -f dist.tar.gz && npm run build && tar -czf dist.tar.gz dist;fi;
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

