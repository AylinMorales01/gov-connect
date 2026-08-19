import { useQuery } from '@tanstack/react-query';
import { getFinancialOverview } from '../api/analyticsApi';

export function useFinancialOverview() {
    return useQuery({
        queryKey: ['financial-overview'],
        queryFn: getFinancialOverview,
    });
}
