import org.apache.spark.sql.{SparkSession}
import org.apache.spark.sql.functions.{col, udf, length}

object ProcessJSON extends App {

    val spark = SparkSession.builder()
        .appName("Process JSON")
        .master("local[*]")
        .getOrCreate()
        
    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.access.key", sys.env.getOrElse("AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY_ID"))

    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.secret.key", sys.env.getOrElse("AWS_SECRET_ACCESS_KEY", "AWS_SECRET_ACCESS_KEY"))

    spark.sparkContext
        .hadoopConfiguration
        .set("fs.s3a.endpoint", "s3.amazonaws.com")
    
    val df = spark.read.option("inferSchema", "true")
                       .json("s3a://steam-json-bucket/steam_complex.json")
    
    df.printSchema()

    val df_flattened = df.selectExpr("appid","tags.*")
                         .na.fill(0)
    df_flattened.registerTempTable("tags")

    val prefixedData = df_flattened.columns.foldLeft(df_flattened) { (df, column) =>
        df.withColumnRenamed(column, "tag_" + column)
    }


    val newdf = df.drop(col("tags"))
                  .join(prefixedData, df("appid") === prefixedData("tag_appid"))
                  .drop(col("tag_appid"))
   
    val languageAbbreviations = Map(
      "English" -> "EN",
      "Spanish" -> "ES",
      "Chinese" -> "ZH",
      "Hindi" -> "HI",
      "Arabic" -> "AR",
      "Portuguese" -> "PT",
      "Bengali" -> "BN",
      "Russian" -> "RU",
      "Japanese" -> "JA",
      "Punjabi" -> "PA",
      "German" -> "DE",
      "Javanese" -> "JV",
      "Malay" -> "MS",
      "Telugu" -> "TE",
      "Vietnamese" -> "VI",
      "Korean" -> "KO",
      "French" -> "FR",
      "Marathi" -> "MR",
      "Tamil" -> "TA",
      "Urdu" -> "UR",
      "Turkish" -> "TR",
      "Italian" -> "IT",
      "Thai" -> "TH",
      "Persian" -> "FA",
      "Polish" -> "PL",
      "Pashto" -> "PS",
      "Kannada" -> "KN",
      "Dutch" -> "NL",
      "Swedish" -> "SV",
      "Greek" -> "EL",
      "Czech" -> "CS",
      "Romanian" -> "RO",
      "Hungarian" -> "HU",
      "Danish" -> "DA",
      "Finnish" -> "FI",
      "Bulgarian" -> "BG",
      "Slovak" -> "SK",
      "Lithuanian" -> "LT",
      "Latvian" -> "LV",
      "Estonian" -> "ET",
      "Slovenian" -> "SL",
      "Croatian" -> "HR",
      "Icelandic" -> "IS",
      "Norwegian" -> "NO",
      "Irish" -> "GA",
      "Albanian" -> "SQ"
    )

    val replaceLanguageNames = udf((languages: String) => {
      languages.split(",")
        .map(_.trim)
        .map(language => languageAbbreviations.getOrElse(language, language))
        .mkString(",")
    })

    // Apply the UDF to replace language names in the DataFrame
    val updatedData = newdf
        .withColumn("languagesAbbs", replaceLanguageNames(col("languages")))
        .drop(col("languages"))

    updatedData.printSchema()
    updatedData.show()
    //val lengthData = updatedData.withColumn("len", length(col("languagesAbbs")))
    //lengthData.select(col("languagesAbbs"), col("len")).show()
    updatedData.write.parquet("s3a://steam-json-bucket/parquet/steam_complex.parquet")
}
