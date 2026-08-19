import { useQuery } from '@tanstack/react-query';
import { getContractsValueByDepartment } from '../api/analyticsApi';

export function useContractsValueByDepartment() {
    return useQuery({
        queryKey: ['contracts-value-by-department'],
        queryFn: getContractsValueByDepartment,
    });
}
