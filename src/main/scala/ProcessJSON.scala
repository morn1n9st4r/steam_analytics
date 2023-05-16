import org.apache.spark.sql.{SparkSession}

object ProcessJSON extends App {

    val spark = SparkSession.builder()
        .appName("Process JSON")
        .master("local[*]")
        .getOrCreate()
        
    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.access.key", "your key")

    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.secret.key", "your key")

    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.endpoint", "s3.amazonaws.com")
    
    val df = spark.read.json("s3a://steam-json-bucket/steam_simple.json")
    df.show()
}
