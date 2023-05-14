resource "aws_s3_bucket" "s3_steam_json_data" {
    bucket = "${var.bucket_name}"

    tags = {
        Name        = "S3 bucket for json aquired from Steam API."
        Environment = "Dev"
    }
}
