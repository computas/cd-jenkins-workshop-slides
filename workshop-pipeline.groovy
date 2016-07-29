node {
    stage 'Checkout'
    git url: 'https://github.com/mgfeller/sample-spring-app.git'

    def mvnHome = tool 'M3'

    stage 'Build'
    def version = "${BASE_VERSION}-" +
            currentBuild.number.toString().padLeft(4,'0')
    sh "${mvnHome}/bin/mvn versions:set -DnewVersion=${version}"
    sh "${mvnHome}/bin/mvn clean package -DskipTests"

    stage 'Unit Test'

// getting last author to alert:
    sh "git --no-pager show -s --format='%an <%ae>' > lastAuthor.txt"
    lastAuthor = readFile('lastAuthor.txt').trim()
    echo "Last author: ${lastAuthor}"

    try {
        sh "${mvnHome}/bin/mvn -B test -DpipelineFailOneTest=false"
    } catch (err) {
        step([$class: 'JUnitResultArchiver',
              testResults: '**/target/surefire-reports/TEST-*.xml'])
        mail subject: 'Build Failed - Failed Unit Tests.',
                body: 'The workshop build failed!',
                charset: 'utf-8',
                from: 'vagrant@localhost',
                mimeType: 'text/plain',
                to: 'vagrant@localhost'
        throw err
    }

    stage 'Deploy'
    sh "${mvnHome}/bin/mvn deploy"

}
