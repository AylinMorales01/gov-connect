/** Formatea una fecha ISO (yyyy-mm-dd) a dd/mm/yyyy sin depender de la zona horaria. */
export function formatISODate(iso: string): string {
    if (!iso) return '—';
    const parts = iso.split('-');
    if (parts.length < 3) return iso;
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
}
