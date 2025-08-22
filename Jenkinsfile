pipeline {
    agent any
    environment {
        AWS_ACCOUNT_ID = sh(script: 'aws sts get-caller-identity --query Account --output text', returnStdout: true).trim()
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com"
        ECR_REPOSITORY = "smooth/user-service"
        IMAGE_TAG = "${new Date().format('yyyyMMdd')}-${sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()}"
        AWS_DEFAULT_REGION = "ap-northeast-2"
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: "30", artifactNumToKeepStr: "30"))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Building application]"
                    sh './gradlew clean build'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Building image: ${ECR_REPOSITORY}:${IMAGE_TAG}]"
                    dockerImage = docker.build("${ECR_REPOSITORY}:${IMAGE_TAG}")
                }
            }
        }

        stage('Push to ECR') {
            steps {
                script {
                    echo "----------------------------------------------------------------------------------"
                    echo "[Push Docker Image to ECR]"
                    sh """
                        aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                        docker tag ${ECR_REPOSITORY}:${IMAGE_TAG} ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
                        docker tag ${ECR_REPOSITORY}:${IMAGE_TAG} ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest
                        docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
                        docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                echo "----------------------------------------------------------------------------------"
                echo "[Cleaning up local Docker images and workspace.]"
                sh """
                    docker rmi ${ECR_REPOSITORY}:${IMAGE_TAG} || true
                    docker rmi ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG} || true
                    docker rmi ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest || true
                """
                cleanWs()

                def discordFooter = (currentBuild.currentResult == 'SUCCESS') ? "빌드 성공 ✅" : "빌드 실패 ❌"
                withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK_URL')]) {
                    discordSend description: "Jenkins Job: ${env.JOB_NAME}",
                        footer: discordFooter,
                        link: env.BUILD_URL,
                        title: "User Service Build Result",
                        webhookURL: DISCORD_WEBHOOK_URL
                }
            }
        }
        success {
            echo "----------------------------------------------------------------------------------"
            echo "[Pipeline succeeded! Image pushed: ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}]"
        }
        failure {
            echo "----------------------------------------------------------------------------------"
            echo "[Pipeline failed!]"
        }
    }
}
