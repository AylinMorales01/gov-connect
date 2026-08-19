import { useQuery } from '@tanstack/react-query';
import { getCollectionsByPaymentMethod } from '../api/analyticsApi';

export function useCollectionsByPaymentMethod() {
    return useQuery({
        queryKey: ['collections-by-payment-method'],
        queryFn: getCollectionsByPaymentMethod,
    });
}
