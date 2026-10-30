resource "azurerm_virtual_network" "ecomm" {
  name                = "vnet-ecomm-dev"
  location            = azurerm_resource_group.ecomm.location
  resource_group_name = azurerm_resource_group.ecomm.name
  address_space       = ["10.0.0.0/16"]

  tags = {
    environment = "dev"
  }
}

resource "azurerm_subnet" "aks" {
  name                 = "snet-aks"
  resource_group_name  = azurerm_resource_group.ecomm.name
  virtual_network_name = azurerm_virtual_network.ecomm.name
  address_prefixes     = ["10.0.1.0/24"]
}

resource "azurerm_subnet" "pg" {
  name                 = "snet-pg"
  resource_group_name  = azurerm_resource_group.ecomm.name
  virtual_network_name = azurerm_virtual_network.ecomm.name
  address_prefixes     = ["10.0.2.0/24"]

  delegation {
    name = "postgres"

    service_delegation {
      name    = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  }
}

