resource "azurerm_user_assigned_identity" "aks" {
  location            = azurerm_resource_group.ecomm.location
  name                = "id-aks-ecomm-dev"
  resource_group_name = azurerm_resource_group.ecomm.name
}

resource "azurerm_role_assignment" "aks_network" {
  scope                = azurerm_subnet.aks.id
  role_definition_name = "Network Contributor"
  principal_id         = azurerm_user_assigned_identity.aks.principal_id
}

resource "azurerm_kubernetes_cluster" "ecomm" {
  name                = "aks-ecomm-dev"
  location            = azurerm_resource_group.ecomm.location
  resource_group_name = azurerm_resource_group.ecomm.name
  dns_prefix          = "aks-ecomm-dev"

  default_node_pool {
    name           = "default"
    node_count     = 1
    vm_size        = "Standard_D4as_v7"
    vnet_subnet_id = azurerm_subnet.aks.id
  }

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.aks.id]
  }

  network_profile {
    network_plugin      = "azure"
    network_plugin_mode = "overlay"
    pod_cidr            = "10.244.0.0/16"
    service_cidr        = "10.1.0.0/16"
    dns_service_ip      = "10.1.0.10"
  }

  node_provisioning_profile {
    default_node_pools = "Auto"
  }

  depends_on = [azurerm_role_assignment.aks_network]
}

resource "azurerm_role_assignment" "example" {
  scope                = azurerm_container_registry.ecomm.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_kubernetes_cluster.ecomm.kubelet_identity[0].object_id
}