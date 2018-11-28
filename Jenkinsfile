#!/usr/bin/env groovy

node('linux && maven') {
    ansiColor('xterm') {
        stage('Checkout') {
            git([
                url: 'https://github.com/bitbar/remote-device-client.git',
                poll: true
            ])
        }

        stage('Build') {
            sh('mvn clean package')
        }
        
        stage('Results') {
            junit([
                testResults: 'target/surefire-reports/TEST-*.xml',
                allowEmptyResults: true
            ])
            archiveArtifacts 'target/*.jar'
        }
    }
}
