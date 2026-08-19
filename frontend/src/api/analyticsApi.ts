import { api } from './axios';
import type {
    MonthlyTrendItem,
    DepartmentRankingItem,
    FinancialOverview,
    ConceptBreakdownItem,
    PaymentMethodBreakdownItem,
    ContractDepartmentBreakdownItem,
    TopContractorItem,
} from '../types/analytics';

export const getMonthlyTrend = async (): Promise<MonthlyTrendItem[]> => {
    const response = await api.get<{ data: MonthlyTrendItem[] }>('/analytics/monthly-trend');
    return response.data.data;
};

export const getDepartmentRanking = async (): Promise<DepartmentRankingItem[]> => {
    const response = await api.get<{ data: DepartmentRankingItem[] }>('/analytics/department-ranking');
    return response.data.data;
};

export const getFinancialOverview = async (): Promise<FinancialOverview> => {
    const response = await api.get<{ data: FinancialOverview }>('/analytics/financial-overview');
    return response.data.data;
};

export const getCollectionsByConcept = async (): Promise<ConceptBreakdownItem[]> => {
    const response = await api.get<{ data: ConceptBreakdownItem[] }>('/analytics/collections-by-concept');
    return response.data.data;
};

export const getCollectionsByPaymentMethod = async (): Promise<PaymentMethodBreakdownItem[]> => {
    const response = await api.get<{ data: PaymentMethodBreakdownItem[] }>('/analytics/collections-by-payment-method');
    return response.data.data;
};

export const getContractsValueByDepartment = async (): Promise<ContractDepartmentBreakdownItem[]> => {
    const response = await api.get<{ data: ContractDepartmentBreakdownItem[] }>('/analytics/contracts-value-by-department');
    return response.data.data;
};

export const getTopContractors = async (): Promise<TopContractorItem[]> => {
    const response = await api.get<{ data: TopContractorItem[] }>('/analytics/top-contractors');
    return response.data.data;
};
