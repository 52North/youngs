# youngs developer documentation

Developer documentation for the software **youngs**. For user documentation, contact and license please see the file `README.MD`.

## Unit tests

Unit tests should be developed for all standalone functionality. The are automatically run during `mvn test`.


## Integration test

Integration tests for sources and/or sinks require an internet connection to an existing catalogue. An Elasticsearch sink can automatically be started using the system property `test.start.elasticsearch`, see `pom.xml` and class `org.n52.youngs.test.ElasticsearchServer`.

To run integration tests, activate the profile `integration-test`: `mvn verify -Pintegration-test`.


## Index management

During development, the following commands can be handy to delete indices and types when running an external Elasticsearch database. Commands are tested in [Cmder](http://cmder.net/).

* `curl -v -Method Delete 'http://localhost:9200/testindex/'` (see [here](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-index.html)


## Perform a release

youngs uses the 52°North parent POM (https://github.com/52North/maven-parents), so all required configuration is readily available.

### Preparations

Put your [Sonatype OSS](https://oss.sonatype.org/) credentials into your `.m2/settings.xml` like this:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>ossrh</id>
            <username>$username</username>
            <password>$password</password>
        </server>
    </servers>
</settings>
```

You also need a public key on a keyserver (see [here](https://www.debian-administration.org/article/451/Submitting_your_GPG_key_to_a_keyserver) for upload instructions), which you then use to sign the release. Check the if the signing works by running `mvn clean install -Psign`.

### Release

First switch to a special branch to prepare the release. Afterward you can merge this into the master or develop branch.

```sh
git checkout -b release-prepare
```

Now start the release process with

```sh
mvn release:prepare
mvn release:perform -P sign
```

The `release` commands are interactive and allow you to set the release version and next development version.

After performing the release on the command line, log in to Sonatype Nexus at https://oss.sonatype.org/ and complete the following steps:

* Locate the project in the the staging repository: https://oss.sonatype.org/#stagingRepositories
* Check it contains the required files (pom, asc, sources, javadoc, ...)
* "Close" the staging repository
* Click on "release"
* Wait... then refresh - the staging repo should be gone.
* Check after a short delay for the published modules at http://repo1.maven.org/maven2/org/n52/youngs/


