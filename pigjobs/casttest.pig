REGISTER '/usr/lib/hbase/hbase-0.90.4-cdh3u3.jar'
REGISTER '/usr/lib/hbase/lib/guava-r06.jar'
REGISTER '/usr/lib/zookeeper/zookeeper-3.3.5-cdh3u6.jar'
REGISTER './logp-common-1.7.jar'
REGISTER './pig-to-json.jar'
REGISTER './json_simple-1.1.jar'

A = LOAD '/user/sbalasubramanian/data' AS fld:chararray;
DUMP A;
B = FOREACH A GENERATE (bag{tuple(long)})fld; 
DESCRIBE B;
DUMP B;
