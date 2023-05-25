import org.apache.spark.sql.{SparkSession}
import org.apache.spark.sql.functions.{col}

object ProcessJSON extends App {

    val spark = SparkSession.builder()
        .appName("Process JSON")
        .master("local[*]")
        .getOrCreate()
        
    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.access.key", "AKIA2JZCCPMHCOW3NI3F")

    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.secret.key", "rdV6io0psHaIrQZUS6JANqA10ICCj0ZymjKH1l57")

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
                  // replace as abbreviations
                  .drop(col("languages"))

    newdf.printSchema()
    newdf.write.parquet("s3a://steam-json-bucket/parquet/steam_complex.parquet")
}
