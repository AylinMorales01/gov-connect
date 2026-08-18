import { api } from './axios';
import type { DashboardSummary, ExpiringContractItem, AutomationLogItem } from '../types/dashboard';

export const getDashboardSummary = async (): Promise<DashboardSummary> => {
    const response = await api.get<{ data: DashboardSummary }>('/dashboard/summary');
    return response.data.data;
};

export const getExpiringContracts = async (): Promise<ExpiringContractItem[]> => {
    const response = await api.get<{ data: ExpiringContractItem[] }>('/dashboard/expiring-contracts');
    return response.data.data;
};

export const getAutomationLogs = async (): Promise<AutomationLogItem[]> => {
    const response = await api.get<{ data: AutomationLogItem[] }>('/automation/logs');
    return response.data.data;
};