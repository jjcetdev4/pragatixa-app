pipeline {
    agent any

    tools {
        // Must match the Maven installation name in Manage Jenkins > Tools
        maven 'maven'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building the project'
                dir('decipline_backend-arch_backedn') {
                    sh 'mvn clean compile'
                }
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests'
                dir('decipline_backend-arch_backedn') {
                    sh 'mvn test'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Running SonarQube Analysis'
                dir('decipline_backend-arch_backedn') {
                    withSonarQubeEnv('sonar') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application'
                dir('decipline_backend-arch_backedn') {
                    sh 'mvn package -DskipTests'
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished'
        }
        success {
            echo 'Build and package succeeded!'
        }
        failure {
            echo 'Pipeline failed. Please check the logs.'
        }
    }
}
