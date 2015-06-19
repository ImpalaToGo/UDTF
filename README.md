# UDTF
UDTF and data transformation functions for ImpalaToGo

###SequenceFileExploder
Utility to emulate Hive explode() on [Hadoop Sequence File](http://wiki.apache.org/hadoop/SequenceFile) values, 
see [Hive explode()]( https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF#LanguageManualUDF-explode).

To build with maven, run :
```sh
mvn -Dhadoop.version=2.6.0-cdh5.5.0-SNAPSHOT clean install
```
