resource "azurerm_resource_group" "tfstate" {
  name     = "rg-ecomm-tfstate"
  location = "France Central"
}

resource "azurerm_storage_account" "tfstate" {
  name                     = "saecommtfstatev0"
  resource_group_name      = azurerm_resource_group.tfstate.name
  location                 = azurerm_resource_group.tfstate.location
  account_tier             = "Standard"
  account_replication_type = "LRS"

  tags = {
    environment = "dev"
  }
}

resource "azurerm_storage_container" "tfstate" {
  name                  = "sc-ecomm-tfstate"
  storage_account_id    = azurerm_storage_account.tfstate.id
  container_access_type = "private"
}