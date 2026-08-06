import { useQuery } from '@tanstack/react-query';
import { getDepartmentRanking } from '../api/analyticsApi';

export function useDepartmentRanking() {
    return useQuery({
        queryKey: ['department-ranking'],
        queryFn: getDepartmentRanking,
    });
}