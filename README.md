# youngs

Integration component for [Elasticsearch](https://www.elastic.co/products/elasticsearch) and ISO / OGC standards (catalogues). In one sentence, it allows you to define a mapping from XML-based metadata into an Elasticsearch data model and transfer the contents of an [OGC Catalogue Service](http://www.opengeospatial.org/standards/cat) (CSW) into an Elasticsearch instance.

## Modules

The following modules are realized as Java packages, simply prepend ``org.n52.youngs.`` to find the code you are looking for.

* ``control`` contains executable Java classes to control the harvesting, transformation, and loading
* ``harvest`` contains Java classes to read all records or a subset of records from an OGC CSW
* ``transform`` contains Java and configuration classes to turn an XML metadata record into a Java class that can be loaded into Elasticsearch
* ``load`` contains Java classes to insert the records into an Elasticsearch cluster

## The name

[Young's modulus](https://en.wikipedia.org/wiki/Young's_modulus), also called modulus of elasticity, is a "standard" for (simplified) describing the elasticity of solid materials. OGC and ISO standards might be some people actually be described as solid with both positive and negative connotations. In this project we use the name **youngs** simply because it combines the two words "elastic" for the document search engine Elasticsearch with the notion of a common standard, in our case OGC and ISO standards for describing geospatial metadata.

## Build

Run ``mvn clean install`` - that's it!

## Requirements

* Java 8
* Maven

## License

This 
