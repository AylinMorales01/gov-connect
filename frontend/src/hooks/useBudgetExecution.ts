import { useQuery } from '@tanstack/react-query';
import { getBudgetExecution } from '../api/dashboardApi';

export function useBudgetExecution() {
    return useQuery({
        queryKey: ['budget-execution'],
        queryFn: getBudgetExecution,
    });
}
