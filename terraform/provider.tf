# provider.tf
terraform {
  required_providers {
    kafka = {
      source  = "mongey/kafka"
      version = "~> 0.5.3"
    }
  }
}

provider "kafka" {
  bootstrap_servers = ["host.docker.internal:9092"]
}