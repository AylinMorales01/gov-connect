import { api } from './axios';
import type { MonthlyTrendItem, DepartmentRankingItem } from '../types/analytics';

export const getMonthlyTrend = async (): Promise<MonthlyTrendItem[]> => {
    const response = await api.get<{ data: MonthlyTrendItem[] }>('/analytics/monthly-trend');
    return response.data.data;
};

export const getDepartmentRanking = async (): Promise<DepartmentRankingItem[]> => {
    const response = await api.get<{ data: DepartmentRankingItem[] }>('/analytics/department-ranking');
    return response.data.data;
};