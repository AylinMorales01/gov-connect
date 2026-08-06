export function formatCompactCurrency(value: number): string {
    // Si es mayor o igual a mil millones (1.000.000.000) -> Billones / Miles de millones
    if (value >= 1_000_000_000) {
        const formatted = (value / 1_000_000_000).toFixed(1).replace('.0', '');
        return `$${formatted} B`;
    }

    // Si es mayor o igual a un millón (1.000.000) -> Millones
    if (value >= 1_000_000) {
        const formatted = (value / 1_000_000).toFixed(1).replace('.0', '');
        return `$${formatted} M`;
    }

    // Si es menor a un millón, usamos formato de miles simple (ej. $500 K o formato normal)
    if (value >= 1_000) {
        const formatted = (value / 1_000).toFixed(0);
        return `$${formatted} K`;
    }

    return `$${value}`;
}