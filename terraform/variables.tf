# AWS provider configuration
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

# # Database hacka configuration
# variable "db_hacka_username" {
#   description = "The username for the RDS hacka instance"
#   type        = string
#   sensitive   = true
# }

# variable "db_hacka_password" {
#   description = "The password for the RDS hacka instance"
#   type        = string
#   sensitive   = true
# }

# variable "db_hacka_name" {
#   description = "Database hacka name"
#   type        = string
#   default     = "lanch_cat_db"
# }

# variable "db_hacka_port" {
#   description = "Database hacka port"
#   type        = string
#   default     = "5432"
# }

# variable "db_hacka_identifier" {
#   description = "The identifier for the RDS hacka instance"
#   type        = string
#   default     = "hacka-db"
# }

#Variaveis DockerHUB

variable "dockerhub_username" {
  description = "The username of the dockerhub image to deploy"
  type        = string
}

/*variable "dockerhub_token" {
  description = "The access token of the dockerhub image to deploy"
  type        = string
}*/