import { api } from './axios';
import type { ContractItem, ContractSummary } from '../types/contracts';

export const getContracts = async (status?: string, search?: string): Promise<ContractItem[]> => {
    const response = await api.get<{ data: ContractItem[] }>('/contracts', {
        params: { status: status || undefined, search: search || undefined },
    });
    return response.data.data;
};

export const getContractsSummary = async (): Promise<ContractSummary> => {
    const response = await api.get<{ data: ContractSummary }>('/contracts/summary');
    return response.data.data;
};
