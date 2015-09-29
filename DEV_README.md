# youngs developer documentation

Developer documentation for the software **youngs**. For user documentation, contact and license please see the file `README.MD`.

## Unit tests

Unit tests should be developed for all standalone functionality. The are automatically run during `mvn test`.


## Integration test

Integration tests for sources and/or sinks require an internet connection to an existing catalogue. An Elasticsearch sink can automatically be started using the system property `test.start.elasticsearch`, see `pom.xml` and class `org.n52.youngs.test.ElasticsearchServer`.

To run integration tests, activate the profile `integration-test`: `mvn verify -Pintegration-test`.


## Index management

During development, the following commands can be handy to delete indices and types when running an external Elasticsearch database. Commands are tested in [Cmder](http://cmder.net/).

* `curl -XDELETE 'http://localhost:9200/testindex/'` (see [here](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-index.html)
