resource "aws_s3_bucket" "s3_steam_json_data" {
    bucket = "${var.bucket_name}"

    tags = {
        Name        = "S3 bucket for json aquired from Steam API."
        Environment = "Dev"
    }
}


resource "aws_s3_bucket_lifecycle_configuration" "archive_week_old_data" {
    bucket = aws_s3_bucket.s3_steam_json_data.id

    rule {
        id      = "move-to-glacier"
        status  = "Enabled"
        prefix  = ""

        transition {
            days          = 7
            storage_class = "GLACIER"
        }
    }
}