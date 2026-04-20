pipeline {
    agent any

    environment {
        BACKEND_IMAGE  = 'cyionk8s/chat-backend:latest'
        FRONTEND_IMAGE = 'cyionk8s/chat-frontend:latest'
        SONAR_HOST_URL = 'http://sonarqube:9000'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend: Test') {
            steps {
                dir('backend') {
                    sh './mvnw test'
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Backend: SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    dir('backend') {
                        sh '''
                            ./mvnw sonar:sonar \
                                -Dsonar.projectKey=chat \
                                -Dsonar.projectName="Chat App" \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.token=${SONAR_TOKEN} \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('Frontend: Lint') {
            steps {
                dir('frontend') {
                    sh 'npm ci && npm run lint'
                }
            }
        }

        stage('Docker: Build & Push Backend') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                        docker build -t ${BACKEND_IMAGE} ./backend
                        docker push ${BACKEND_IMAGE}
                        docker logout
                    '''
                }
            }
        }

        stage('Docker: Build & Push Frontend') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                        docker build -t ${FRONTEND_IMAGE} ./frontend
                        docker push ${FRONTEND_IMAGE}
                        docker logout
                    '''
                }
            }
        }
    }

    post {
        failure {
            echo 'Pipeline failed. Check the stage logs above for details.'
        }
        success {
            echo 'All stages passed. Images pushed to DockerHub.'
        }
    }
}
