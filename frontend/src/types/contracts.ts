export interface ContractItem {
    id: number;
    contractNumber: string;
    contractorName: string;
    object: string;
    contractValue: number;
    startDate: string;
    endDate: string;
    status: string;
    department: string;
}

export interface ContractSummary {
    totalContracts: number;
    totalValue: number;
    activeContracts: number;
    suspendedContracts: number;
    finishedContracts: number;
}
