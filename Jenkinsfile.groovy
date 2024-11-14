pipeline {
    agent any

    environment {
        MYSQL_ROOT_PASSWORD = 'root'                   // Root password for MySQL container
        MYSQL_DATABASE = 'testdb'                      // Database name to be created in the MySQL container
        SPRING_DATASOURCE_URL = ''                     // Set in Start MySQL stage
        SPRING_DATASOURCE_USERNAME = 'root'            // Username for MySQL
        SPRING_DATASOURCE_PASSWORD = 'root'            // Password for MySQL
    }

    stages {
        stage('Checkout') {
            steps {
                // Clone the repository
                git branch: 'master', url: 'https://github.com/Ahmeddhibi19/e-com-server.git'
            }
        }

        stage('Start MySQL') {
            steps {
                // Start MySQL container for the pipeline
                script {
                    // Run MySQL container and save the container ID for later cleanup
                    def mysqlContainer = docker.image('mysql:8').run("-e MYSQL_ROOT_PASSWORD=${env.MYSQL_ROOT_PASSWORD} -e MYSQL_DATABASE=${env.MYSQL_DATABASE}")
                    env.MYSQL_CONTAINER_ID = mysqlContainer.id

                    // Set the datasource URL with the container's host and port
                    env.SPRING_DATASOURCE_URL = "jdbc:mysql://${mysqlContainer.name}:${mysqlContainer.firstPort}/testdb"

                    // Pause to give MySQL time to initialize
                    sleep(time: 10, unit: 'SECONDS')
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    def mavenHome = tool name: 'Maven 3.8.1', type: 'maven'
                    sh "${mavenHome}/bin/mvn clean package -DskipTests -Dspring.profiles.active=test"
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    def mavenHome = tool name: 'Maven 3.8.1', type: 'maven'
                    sh "${mavenHome}/bin/mvn test -Dspring.profiles.active=test"
                }
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying application...'
                // Add deployment steps here if required (e.g., SCP, SSH)
            }
        }
    }

    post {
        always {
            // Cleanup step: Stop and remove MySQL container after pipeline completes
            script {
                if (env.MYSQL_CONTAINER_ID) {
                    echo 'Stopping MySQL container...'
                    docker.stop(env.MYSQL_CONTAINER_ID)
                    echo 'Removing MySQL container...'
                    docker.rm(env.MYSQL_CONTAINER_ID)
                }
            }
            cleanWs()  // Cleans up the workspace on Jenkins
        }
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}
