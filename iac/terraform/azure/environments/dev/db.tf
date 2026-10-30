variable "db_psswd" {
  type      = string
  sensitive = true
}

resource "azurerm_private_dns_zone" "db" {
  name                = "ecomm.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.ecomm.name
}

resource "azurerm_private_dns_zone_virtual_network_link" "db" {
  name                = "postgres-vnet-link"
  private_dns_zone_id = azurerm_private_dns_zone.db.id
  virtual_network_id  = azurerm_virtual_network.ecomm.id
}

resource "azurerm_postgresql_flexible_server" "ecomm" {
  name                          = "pg-ecomm-devv0"
  resource_group_name           = azurerm_resource_group.ecomm.name
  location                      = azurerm_resource_group.ecomm.location
  version                       = "16"
  delegated_subnet_id           = azurerm_subnet.pg.id
  private_dns_zone_id           = azurerm_private_dns_zone.db.id
  public_network_access_enabled = false
  administrator_login           = "ecommadmin"
  administrator_password        = var.db_psswd

  storage_mb = 32768

  sku_name   = "B_Standard_B1ms"
  depends_on = [azurerm_private_dns_zone_virtual_network_link.db]

}

resource "azurerm_postgresql_flexible_server_database" "ecomm" {

  for_each = toset([
    "ecomm_product_db",
    "ecomm_order_db",
    "ecomm_payment_db"
  ])


  name      = each.value
  server_id = azurerm_postgresql_flexible_server.ecomm.id
  collation = "en_US.utf8"
  charset   = "UTF8"

}