variable "bucket_name" {
    type        = string
    description = "The name of the S3 bucket to create."
    default     = "steam-json-bucket"
}

variable "app_name" {
  type        = string
  description = "Application name"
}

variable "app_environment" {
  type        = string
  description = "Application environment"
}

variable "redshift_vpc_cidr" {
  type        = string
  description = "VPC IPv4 CIDR"
}

variable "redshift_subnet_1_cidr" {
  type        = string
  description = "IPv4 CIDR for Redshift subnet 1"
}

variable "redshift_subnet_2_cidr" {
  type        = string
  description = "IPv4 CIDR for Redshift subnet 2"
}

variable "redshift_cluster_identifier" {
  type        = string
  description = "Redshift Cluster Identifier"
}

variable "redshift_database_name" { 
  type        = string
  description = "Redshift Database Name"
}

variable "redshift_admin_username" {
  type        = string
  description = "Redshift Admin Username"
}

variable "redshift_admin_password" { 
  type        = string
  description = "Redshift Admin Password"
}

variable "redshift_node_type" { 
  type        = string
  description = "Redshift Node Type"
  default     = "dc2.large"
}

variable "redshift_cluster_type" { 
  type        = string
  description = "Redshift Cluster Type"
  default     = "single-node"  // options are single-node or multi-node
}

variable "redshift_number_of_nodes" {
  type        = number
  description = "Redshift Number of Nodes in the Cluster"
  default     = 1
}