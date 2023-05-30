#!/bin/sh

# Compile Spark classes
sbt compile
sbt package
mv target/scala-2.12/spark_transformations.jar jars/spark_transformations.jar

# Download HadoopAWS 
if [ ! -f ./jars/hadoop-aws-3.3.2.jar ]
then
    echo "No HadoopAWS found in ./jars/ folder. Downloading..."
    curl -o jars/hadoop-aws-3.3.2.jar https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/3.3.2/hadoop-aws-3.3.2.jar
    echo "HadoopAWS downloaded."
else
    echo "HadoopAWS found."
fi

# Download AWS Java SDK bundle
if [ ! -f ./jars/hadoop-aws-3.3.2.jar ]
then
    echo "No AWS Java SDK bundle found in ./jars/ folder. Downloading..."
    curl -o jars/aws-java-sdk-bundle-1.11.1026.jar https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/1.11.1026/aws-java-sdk-bundle-1.11.1026.jar
    echo "AWS Java SDK bundle downloaded."
else
    echo "AWS Java SDK bundle found."
fi