import { useQuery } from '@tanstack/react-query';
import { getMonthlyTrend } from '../api/analyticsApi';

export function useMonthlyTrend() {
    return useQuery({
        queryKey: ['monthly-trend'],
        queryFn: getMonthlyTrend,
    });
}