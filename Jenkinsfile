pipeline {
  agent any
  stages {
    stage('Build and test') {
      steps{
        withMaven {
          sh 'mvn clean deploy -f invesdwin-instrument/pom.xml -T4'
        }  
      }
    }
  }
}