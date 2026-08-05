package com.govconnect.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DashboardSummaryDTO {

    private BigDecimal collectionsThisMonth;
    private Integer activeContracts;
    private Integer contractsExpiring;
    private BigDecimal budgetExecutionPercentage;
    private LocalDateTime lastUpdated;

    // Constructor vacío
    public DashboardSummaryDTO() {}

    // Constructor completo mapeando el orden de la vista
    public DashboardSummaryDTO(BigDecimal collectionsThisMonth, Integer activeContracts,
                               Integer contractsExpiring, BigDecimal budgetExecutionPercentage,
                               LocalDateTime lastUpdated) {
        this.collectionsThisMonth = collectionsThisMonth;
        this.activeContracts = activeContracts;
        this.contractsExpiring = contractsExpiring;
        this.budgetExecutionPercentage = budgetExecutionPercentage;
        this.lastUpdated = lastUpdated;
    }

    // Getters y Setters
    public BigDecimal getCollectionsThisMonth() { return collectionsThisMonth; }
    public void setCollectionsThisMonth(BigDecimal collectionsThisMonth) { this.collectionsThisMonth = collectionsThisMonth; }

    public Integer getActiveContracts() { return activeContracts; }
    public void setActiveContracts(Integer activeContracts) { this.activeContracts = activeContracts; }

    public Integer getContractsExpiring() { return contractsExpiring; }
    public void setContractsExpiring(Integer contractsExpiring) { this.contractsExpiring = contractsExpiring; }

    public BigDecimal getBudgetExecutionPercentage() { return budgetExecutionPercentage; }
    public void setBudgetExecutionPercentage(BigDecimal budgetExecutionPercentage) { this.budgetExecutionPercentage = budgetExecutionPercentage; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}