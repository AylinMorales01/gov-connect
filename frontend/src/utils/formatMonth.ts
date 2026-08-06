export const formatMonth = (dateString: string): string => {
    if (!dateString) return '';

    // Soporta formatos como "2026-01" o "2026-01-01"
    const parts = dateString.split('-');
    if (parts.length < 2) return dateString;

    const monthIndex = parseInt(parts[1], 10) - 1;
    const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

    return months[monthIndex] || dateString;
};