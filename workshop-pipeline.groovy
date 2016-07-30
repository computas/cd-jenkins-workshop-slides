node {
    stage 'Checkout'
    git url: 'git@github.com:mgfeller/sample-spring-app.git'

    def mvnHome = tool 'M3'

    stage 'Build'
    def version = "${BASE_VERSION}-" +
            currentBuild.number.toString().padLeft(4,'0')
    sh "${mvnHome}/bin/mvn versions:set -DnewVersion=${version}"
    sh "${mvnHome}/bin/mvn clean package -DskipTests"

    stage 'Unit Test / License Check'

    sh "git --no-pager show -s --format='%an <%ae>' > lastAuthor.txt"
    lastAuthor = readFile('lastAuthor.txt').trim()
    echo "Last author: ${lastAuthor}"

    parallel(
            'licenseCheck': { sh "${mvnHome}/bin/mvn license:check" },
            'unitTest': {
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
            }
    )

    stage 'Deploy'
    timeout(time: 40, unit: 'SECONDS') {
        input message: 'Do you want to release version, ' +  version + '?', ok: 'Release'
    }

    sh "git config user.name \"Vagrant Builder\""
    sh "git config user.email vagrant@localhost"
    def comment = "\"Automatically created tag ${version}\""
    sh "git tag -a -m ${comment} ${version}"
    sh "git push origin ${version}"

    sh "${mvnHome}/bin/mvn deploy"

}
