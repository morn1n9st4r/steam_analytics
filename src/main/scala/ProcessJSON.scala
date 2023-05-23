import org.apache.spark.sql.{SparkSession}
import org.apache.spark.sql.functions.{col}
object ProcessJSON extends App {

    val spark = SparkSession.builder()
        .appName("Process JSON")
        .master("local[*]")
        .getOrCreate()
        
    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.access.key", "key")

    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.secret.key", "key")

    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.endpoint", "s3.amazonaws.com")
    
    val df = spark.read.option("inferSchema", "true")
                       .json("s3a://steam-json-bucket/steam_complex.json")
    
    df.printSchema()


    val df_flattened = df.selectExpr("appid","tags.*")
    df_flattened.registerTempTable("tags")
    
    val prefixedData = df_flattened.columns.foldLeft(df_flattened) { (df, column) =>
        df.withColumnRenamed(column, "tag_" + column)
    }



    val newdf = df.drop(col("tags"))
                  .join(prefixedData, df("appid") === prefixedData("tag_appid"))
                  .drop(col("tag_appid"))
    newdf.printSchema()
    // df.write.parquet("s3a://steam-json-bucket/parquet/steam_complex.parquet")
}
