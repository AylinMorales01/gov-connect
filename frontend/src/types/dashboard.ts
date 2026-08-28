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

export interface AutomationLogItem {
    id: number;
    userId: number | null;
    process: string;
    status: string;
    message: string;
    executionTimeMs: number | null;
    createdAt: string;
}

/** Resultado de una ejecución manual de la alerta de contratos por vencer. */
export interface ExpiringContractsAlertResult {
    contractsFound: number;
    emailsSent: number;
    recipients: string[];
    message: string;
}