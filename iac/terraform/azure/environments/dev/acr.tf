resource "azurerm_container_registry" "ecomm" {
  name                = "EcommContainerRegistry1"
  resource_group_name = azurerm_resource_group.ecomm.name
  location            = azurerm_resource_group.ecomm.location
  sku                 = "Basic"
  admin_enabled       = false
}