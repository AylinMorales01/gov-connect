import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';

/** Colores de medalla para los tres primeros puestos. */
const MEDAL_COLORS: Record<number, string> = {
    1: '#F9A825', // oro
    2: '#9E9E9E', // plata
    3: '#A1887F', // bronce
};

interface RankPositionProps {
    position: number;
}

/**
 * Muestra la posición de un ranking: trofeo con color de medalla
 * para los tres primeros puestos y número plano para el resto.
 */
export default function RankPosition({ position }: RankPositionProps) {
    if (position < 1) {
        return null;
    }
    if (position <= 3) {
        return (
            <EmojiEventsIcon
                sx={{ color: MEDAL_COLORS[position], fontSize: 20 }}
            />
        );
    }
    return <>{position}</>;
}
