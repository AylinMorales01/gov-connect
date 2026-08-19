import { useQuery } from '@tanstack/react-query';
import { getCollectionsByConcept } from '../api/analyticsApi';

export function useCollectionsByConcept() {
    return useQuery({
        queryKey: ['collections-by-concept'],
        queryFn: getCollectionsByConcept,
    });
}
