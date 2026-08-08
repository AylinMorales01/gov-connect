import { useQuery } from '@tanstack/react-query';
import { getAutomationLogs } from '../api/dashboardApi';

export function useAutomationLogs() {
    return useQuery({
        queryKey: ['automation-logs'],
        queryFn: getAutomationLogs,
        refetchInterval: 60_000,
        refetchIntervalInBackground: false,
    });
}
