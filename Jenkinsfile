// =====================================================================
//  NovaBank Transfer Portal - Secure CI/CD pipeline (Team DevNova)
//  Stages: Git Checkout > JUnit Test > Maven Build > Gitleaks >
//          Docker Image Build > Trivy Scan > Docker Run
//  Every security tool is a GATE: if it fails, later stages do not run,
//  so nothing is deployed unless every gate passes.
//  Secrets come from Jenkins Credentials (never written in this file).
// =====================================================================

pipeline {
    agent any

    environment {
        IMAGE_NAME = 'devnova-saisiri'
        IMAGE_TAG  = "${BUILD_NUMBER}"
        APP_PORT   = '8082'     // port the app listens on (matches Dockerfile EXPOSE)
        HOST_PORT  = '8082'     // port published on the server
        CONTAINER  = 'novabank-app'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('Git Checkout') {
            steps {
                // Checks out the team's fork configured in the job (Pipeline script from SCM)
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('JUnit Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Maven Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Gitleaks') {
            steps {
                // GATE: fails the build if any secret is found (values are redacted in logs)
                   sh 'gitleaks detect --no-git --source . --redact --report-format json --report-path gitleaks-report.json --exit-code 1 --verbose'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Image Build') {
            steps {
                sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest .'
            }
        }

        stage('Trivy Scan') {
            steps {
                // Full report (HIGH + CRITICAL) saved as an artifact, does not fail
                sh 'trivy image --severity HIGH,CRITICAL --no-progress --exit-code 0 --format table -o trivy-report.txt ${IMAGE_NAME}:${IMAGE_TAG}'
                // GATE: fail the build if any CRITICAL vulnerability exists
                sh 'trivy image --severity CRITICAL --no-progress --exit-code 1 ${IMAGE_NAME}:${IMAGE_TAG}'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'trivy-report.txt', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Run') {
            steps {
                // Secret is read from Jenkins Credentials (ID: payment-gateway-api-key)
                withCredentials([string(credentialsId: 'payment-gateway-api-key', variable: 'PAYMENT_GATEWAY_API_KEY')]) {
                    sh '''
                        docker rm -f ${CONTAINER} || true
                        docker run -d --name ${CONTAINER} \
                            -p ${HOST_PORT}:${APP_PORT} \
                            -e SPRING_PROFILES_ACTIVE=prod \
                            -e PAYMENT_GATEWAY_API_KEY="$PAYMENT_GATEWAY_API_KEY" \
                            ${IMAGE_NAME}:${IMAGE_TAG}
                    '''
                }
                // Health check: retry for up to ~90 seconds
                sh '''
                    for i in $(seq 1 18); do
                        if curl -fs http://localhost:${HOST_PORT}/actuator/health; then
                            echo ""
                            echo "Application is healthy."
                            docker ps --filter name=${CONTAINER}
                            exit 0
                        fi
                        echo "Waiting for application... ($i/18)"
                        sleep 5
                    done
                    echo "Health check failed. Container logs:"
                    docker logs ${CONTAINER}
                    exit 1
                '''
            }
        }
    }

    post {
        success { echo 'Pipeline passed: all security gates cleared and the app is deployed on port 8082.' }
        failure { echo 'Pipeline failed: check the red stage. Later stages were skipped by design.' }
    }
}
