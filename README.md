# youngs

Integration component for [Elasticsearch](https://www.elastic.co/products/elasticsearch) and ISO / OGC standards (catalogues). In one sentence, it allows you to define a mapping from XML-based metadata into an Elasticsearch data model and transfer the contents of an [OGC Catalogue Service](http://www.opengeospatial.org/standards/cat) (CSW) into an Elasticsearch instance.


## Modules

The following features are modularized as Java packages, simply prepend ``org.n52.youngs.`` to find the code you are looking for.

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


## Usage

### Harvest, transform, and load

Youngs provides a single threaded harvester that paginates through all records of a CSW and inserts them into Elasticsearch. To see how this works see class `org.n52.youngs.control.Main.java`.

### Configuration file a.k.a. "mapping file"

The harvesting of a catalog is mainly a mapping of metadata encoded in XML (e.g. CSW + dublin core, or ISO19139, ...) to a flat data model of Elasticsearch fields. This mapping is done in a YAML configuration file, which contains the following information, taken from the file `src/test/resources/mappings/testmapping.yml`. You can find more examples in that directory or in `src/main/resources/mappings/`.

*Mapping Metadata* (defaults are `<unnamed>`, `1`, and `2.0` respectively.

```yaml
name: test
version: 42
xpathversion: 2.0
```

*Applicability test*: This XPath is executed to determine if a mapping should be applied to a provided XML document. Default is `true()`.

```yaml
applicability_xpath: "boolean(//*[local-name()='MD_Metadata']) and boolean(namespace-uri(//*[local-name()='MD_Metadata']) = 'http://www.isotc211.org/2005/gmd')"
```

*Namespaces*: This list of namespaces and prefixes is provided to the XPath evaluation classes and can be used in the XPath definitions throughout the mapping file.

```yaml
namespaces:
    gmd: http://www.isotc211.org/2005/gmd
    csw: http://www.opengis.net/cat/csw/2.0.2
```

*Index configuration*: Settings for the Elasticsearch index, such as name, type to be used for storing records, etc. A string field even contains (in this case YAML) markup that will be send to the node at index creation. Creation of the index with the schema can be enabled/disabld.

```yaml
index:
    create: true
    name: testindex
    type: testrecord
    settings: |
        index:
            number_of_shards: 1 
            number_of_replicas: 1
```

*Mappings*: A map of the actual mappings. If no `index_name` is provided, then the map identifier is used. The contents of the `properties` field are directly inserted into the schema as field properties.

An example mapping definition and corresponding Elasticsearch type deffinition is as follows:

```yaml
mappings:
    id:
        xpath: "//gmd:fileIdentifier"
        isoqueryable: false
        properties:
            type: string
            store: *STORE
            index: analyzed
            #index_name: "id"
            boost: 2.0
```

```json
{
    "mappings":{
        "testrecord":{
            "properties":{
                "id":{
                    "type": "string",
                    "store": "yes",
                    "index": "analyzed",
                    "boost": 2.0
                    "index_name": "id"
                }
            }
        }
    }
}
```

The advantag of a YAML file is that it supports references, e.g. using default values as shown below.

```yaml
defaults:
    store: &STOREDEFAULT true
[...]
mappings:
    id:
        properties:
            store: *STOREDEFAULT
[...]
    title:
        properties:
            store: *STOREDEFAULT
```

#### Spatial field mapping

A special mapping must be used for fields with geospatial data. A ``coordinates`` field with a single field ``points`` is needed. The points field contains an ordered list of latitude and longitude coordinates which form the geometry of the field. The type of the geometry must be given as value of ``coordinates_type``. Currently supported types and the required format for Elasticsearch are as follows:

* [envelope](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_envelope)

The fields `lat` and `lon` must contain numbers to be correctly encoded, which removes trailing zeros.

An example for a bounding box field is given below.

```yaml
location:
    xpath: "//ows:BoundingBox"
    coordinates:
        points:
            -   lon: "number(substring-before(ows:UpperCorner, ' '))"
                lat: "min( ( number(substring-after(ows:UpperCorner, ' ') ),
                number(substring-after(ows:LowerCorner, ' ')) ) )"
            -   lon: "number(substring-before(ows:LowerCorner, ' '))"
                lat: "max( ( number(substring-after(ows:UpperCorner, ' ') ),
                number(substring-after(ows:LowerCorner, ' ')) ) )"
    coordinates_type: envelope
    properties:
        index_name: location
        type: geo_shape
        tree: quadtree
        precision: 1m
```

#### Id field mapping

Exactly _one_ mapping can be marked to be used for the identifier of index entries.

```yaml
id:
    xpath: "//dc:identifier"
    identifier: true
    properties:
        type: string
        boost: 10.0
```

#### Mapping with replacement

A specific replacement can be applied after the evaluation of the XPath to allow complex select expressions in combination with partial field values, e.g. hierachical keywords.

The following example would create a field `` { "classes": [ "A", "B" ] }`` out of XML snippet ``<keyword>class.A</keyword><keyword>class.B</keyword>``. Multiple replacements will be done in order.

```yaml
classes:
    xpath: "//keyword/*[contains(text(), 'class.')]/text()"
    replacements:
        - replace: "class."
          with: ""
    properties:
        store: yes
        type: string
```


#### Mapping with splitting

XPath (even 2.0) cannot create new nodes. So if we want to concatenate two fields and do that multiple times, we need a workaround in Java.

The following configuration uses the ``string-join`` function to concatenate multiple keywords with their types.

```yaml
    typed_keywords:
        # only works for first, because xpath cannot create new nodes: http://stackoverflow.com/questions/9031727/converting-the-result-type-of-string-join-to-nodelist
        xpath: "string-join(for $n in /*/gmd:identificationInfo/*/gmd:descriptiveKeywords/gmd:MD_Keywords
                        return string-join(($n/gmd:type/*/@codeListValue, ':', $n/gmd:keyword/*/text()), ''), '__new_item__')"
        split: "__new_item__"
        properties:
            store: yes
            index: not_analyzed
            include_in_all: false
```

The intermediate string ``theme:Atmospheric conditions__split__socialBenefitArea:Weather__split__place:Archive`` is then split into three items and results in the following field (JSON snippet):

```json
typed_keywords": [
    "theme:Atmospheric conditions",
    "socialBenefitArea:Weather",
    "place:Archive"
]
```

### General notes

* If an XPath expression yields multiple results, the corresponding field will contain an array.
 

### Schema creation and insertion

Youngs will create an Elasticsearch schema based on the mapping file and insert the schema into an Elasticsearch node before inserting the records. This can be controlled in the mappings file:

```
index:
    create: true
```

After insertion, you can inspect the inserted schema using the [Mapping API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html).

* `curl -XGET 'http://localhost:9200/csw/_mapping/record'` to retrieve the mapping for index `csw` and type `record`
* `curl -XGET 'http://localhost:9200/_all/_mapping'` or `curl -XGET 'http://localhost:9200/_mapping'` to retrieve all mappings

### Schema metadata

Youngs creates a second type `mt` to hold metadata for when and which mapping was inserted.

* `curl -XGET 'http://localhost:9200/<indexname>/mt/1'` shows the metadata of the currently inserted schema (just change index name as neccessary)
* `curl -XGET 'http://localhost:9200/<indexname>/_mapping/mt'` shows the mapping for the metadata schema


## Development

See developer documentation file `DEV_README.MD`.


## License

This project is published under The Apache Software License Version 2.0. For details see files NOTICE and LICENSE.


## Contact

* [d.nuest@52north.org](@nuest)
