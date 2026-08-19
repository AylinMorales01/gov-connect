export interface MonthlyTrendItem {
    month: string;
    amount: number;
}

export interface DepartmentRankingItem {
    rank: number | null;
    department: string;
    executionPercentage: number;
    totalCollections: number;
    score: number;
}

export interface FinancialOverview {
    bestCollectionMonth: string;
    bestCollectionAmount: number;
    worstCollectionMonth: string;
    worstCollectionAmount: number;
    averageMonthlyCollection: number;
    lastMonthGrowthPercentage: number;
    trend: string;
}

export interface ConceptBreakdownItem {
    concept: string;
    totalAmount: number;
    totalCount: number;
}

export interface PaymentMethodBreakdownItem {
    paymentMethod: string;
    totalAmount: number;
    totalCount: number;
}

export interface ContractDepartmentBreakdownItem {
    department: string;
    totalValue: number;
    totalCount: number;
}

export interface TopContractorItem {
    contractor: string;
    totalValue: number;
    contractCount: number;
}