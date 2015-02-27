
jmeter-analyzer
===============

A small utility to parse large jtl files.

Usage
-----

Compile using maven:  
  `mvn clean package`

Run with the jtl file as parameter:  
  `java -jar ./target/analyzer-1.0-SNAPSHOT.jar result.jtl`

The analyzer will create a new folder `result` in which a (formatted) text file `results.txt` will be saved. This file contains the results of all aggregations from the Config class.
The same folder will also contain a sqlite database `data.sqlite` which has a table for every thread group of the result file with the imported information (in case you want to do your own analyzing).

License
-------

See LICENSE


