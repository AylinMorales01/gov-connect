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