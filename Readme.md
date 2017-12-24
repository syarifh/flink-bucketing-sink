This Flink Project is used to consume kafka topics from kafka producers and save to bucket (sink directory based on time and size).
To use this repo, git clone the source and follow the instruction:
1. Open project main directory
2. Type "sbt run" to run directly
3. To run as a service, type "sbt assembly" , it will create fat jar inside folder target/scala/ . Run "java -jar transaction-log-sink-gs-assembly.jar" to run
4. check directory /tmp/output/transaction-log/ to see the logs which already consumed.
