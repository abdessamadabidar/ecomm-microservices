terraform {
  backend "azurerm" {
    resource_group_name  = "rg-ecomm-tfstate"
    storage_account_name = "saecommtfstatev0"
    container_name       = "sc-ecomm-tfstate"
    key                  = "dev.terraform.tfstate"
  }
}