export function formatPercentage(value: number): string {
    return new Intl.NumberFormat('es-CO', {
        style: 'percent',
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
    }).format(value / 100);
}