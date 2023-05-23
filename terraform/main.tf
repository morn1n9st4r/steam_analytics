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

data "aws_availability_zones" "available" {}

resource "aws_vpc" "redshift-vpc" {
  cidr_block           = var.redshift_vpc_cidr
  enable_dns_hostnames = true
  
  tags = {
    Name        = "redshift-vpc"
    Environment = "dev"
  }
}


resource "aws_subnet" "redshift-subnet-az1" {
  vpc_id            = aws_vpc.redshift-vpc.id
  cidr_block        = var.redshift_subnet_1_cidr
  availability_zone = data.aws_availability_zones.available.names[0]
  
  tags = {
    Name        = "redshift-subnet-az1"
    Environment = "dev"
  }
}


resource "aws_subnet" "redshift-subnet-az2" {
  vpc_id            = aws_vpc.redshift-vpc.id
  cidr_block        = var.redshift_subnet_2_cidr
  availability_zone = data.aws_availability_zones.available.names[1]
  
  tags = {
    Name        = "redshift-subnet-az2"
    Environment = "dev"
  }
}

resource "aws_redshift_subnet_group" "redshift-subnet-group" {
  depends_on = [
    aws_subnet.redshift-subnet-az1,
    aws_subnet.redshift-subnet-az2,
  ]

  name       = "redshift-subnet-group"
  subnet_ids = [aws_subnet.redshift-subnet-az1.id, aws_subnet.redshift-subnet-az2.id]

  tags = {
    Name        = "redshift-subnet-group"
    Environment = "dev"
  }
}


resource "aws_internet_gateway" "redshift-igw" {
  vpc_id = aws_vpc.redshift-vpc.id

  tags = {
    Name        = "redshift-igw"
    Environment = "dev"
  }
}

resource "aws_route_table" "redshift-rt-igw" {
  vpc_id = aws_vpc.redshift-vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.redshift-igw.id
  }  

  tags = {
    Name        = "redshift-public-route-igw"
    Environment = "dev"
  }
}

resource "aws_route_table_association" "redshift-subnet-rt-association-igw-az1" {
  subnet_id      = aws_subnet.redshift-subnet-az1.id
  route_table_id = aws_route_table.redshift-rt-igw.id
}

resource "aws_route_table_association" "redshift-subnet-rt-association-igw-az2" {
  subnet_id      = aws_subnet.redshift-subnet-az2.id
  route_table_id = aws_route_table.redshift-rt-igw.id
}

resource "aws_default_security_group" "redshift_security_group" {
  depends_on = [aws_vpc.redshift-vpc]
  
  vpc_id = aws_vpc.redshift-vpc.id
  
  ingress {
    description = "Redshift Port"
    from_port   = 5439
    to_port     = 5439
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] 
  }
  
  tags = {
    Name        = "redshift-security-group"
    Environment = "dev"
  }
}

resource "aws_iam_role" "redshift-role" {
  name = "redshift-role" 
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "redshift.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF

 tags = {
    Name        = "redshift-role"
    Environment = "dev"
  }
}


resource "aws_iam_role_policy" "redshift-s3-full-access-policy" {
  name = "${var.app_name}-${var.app_environment}-redshift-role-s3-policy"
  role = aws_iam_role.redshift-role.id

policy = <<EOF
{
   "Version": "2012-10-17",
   "Statement": [
     {
       "Effect": "Allow",
       "Action": "s3:*",
       "Resource": "*"
      }
   ]
}
EOF
}

resource "aws_redshift_cluster" "redshift-cluster" {
  depends_on = [
    aws_vpc.redshift-vpc,
    aws_redshift_subnet_group.redshift-subnet-group,
    aws_iam_role.redshift-role
  ]

  cluster_identifier = var.redshift_cluster_identifier
  database_name      = var.redshift_database_name
  master_username    = var.redshift_admin_username
  master_password    = var.redshift_admin_password
  node_type          = var.redshift_node_type
  cluster_type       = var.redshift_cluster_type
  number_of_nodes    = var.redshift_number_of_nodes

  iam_roles = [aws_iam_role.redshift-role.arn]

  cluster_subnet_group_name = aws_redshift_subnet_group.redshift-subnet-group.id
  
  skip_final_snapshot = true

  tags = {
    Name        = "redshift-cluster"
    Environment = var.app_environment
  }
}