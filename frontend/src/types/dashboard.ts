export interface DashboardSummary {
    collectionsThisMonth: number;
    activeContracts: number;
    contractsExpiring: number;
    budgetExecutionPercentage: number;
}

export interface BudgetExecutionItem {
    department: string;
    assignedBudget: number;
    executedBudget: number;
    percentage: number;
}

export interface ExpiringContractItem {
    contractNumber: string;
    contractor: string;
    endDate: string;
    remainingDays: number;
}