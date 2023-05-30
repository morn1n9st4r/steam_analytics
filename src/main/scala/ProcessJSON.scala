import org.apache.spark.sql.{SparkSession}
import org.apache.spark.sql.functions.{col, udf, length, concat_ws, coalesce, lit, when}

import scala.util.Try

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
                       .option("mode", "DROPMALFORMED")
                       .json("s3a://steam-json-bucket/steam_complex.json")


    df.printSchema()


/*
    //TODO: fix column names for tags and leave only top 10 

    val df_flattened = df.selectExpr("appid","tags.*")
    df_flattened.registerTempTable("tags")
    val prefixed_tags_all = df_flattened.columns.foldLeft(df_flattened) { (df, column) =>
        df.withColumnRenamed(column, "tag_" + column)
    }
    val prefixed_tags = prefixed_tags_all.withColumnRenamed("tag_appid", "tagged_appid")
    val oldColumns = prefixed_tags.columns
    val sanitizedColumns = prefixed_tags.columns.map(_.replaceAll("[^a-zA-Z0-9_]", "_"))
    val newColumnsExpr = oldColumns.zip(sanitizedColumns).map { case (oldCol, newCol) =>
        col(oldCol).as(newCol)
    }
    val updatedDF = prefixed_tags.select(newColumnsExpr: _*)
    val tagColumns = sanitizedColumns.filter(_.startsWith("tag_"))

    val joinedTags = updatedDF.withColumn(
        "tags",
        concat_ws(", ", tagColumns.map(c => when(col(c).isNotNull, c)).filter(_ != null): _*)
    )
    val resultDF = joinedTags.select("tagged_appid", "tags")
    resultDF.show()
    println(resultDF.columns.length)*/

    
    val newdf = df.drop(col("tags"))
                  //.join(prefixedData, df("appid") === resultDF("tagged_appid"))
                  //.drop(col("tagged_appid"))

    val languageAbbreviations = Map(
      "English" -> "EN",
      "Spanish" -> "ES",
      "Spanish - Spain" -> "ES",
      "Spanish - Latin America" -> "ES",
      "Chinese" -> "ZH",
      "Simplified Chinese" -> "ZH",
      "Traditional Chinese" -> "ZH",
      "Hindi" -> "HI",
      "Arabic" -> "AR",
      "Portuguese" -> "PT",
      "Portuguese - Brazil" -> "PT",
      "Portuguese - Portugal" -> "PT",
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
    updatedData.write.parquet("s3a://steam-json-bucket/parquet/steam_complex.parquet")
}
