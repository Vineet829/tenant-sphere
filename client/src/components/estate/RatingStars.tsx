import { Star } from "lucide-react";

const FIVE = [0, 1, 2, 3, 4];

function Row() {
	return (
		<>
			{FIVE.map((index) => (
				<Star key={index} />
			))}
		</>
	);
}

export default function RatingStars({ value, showValue = true }: { value?: number | null; showValue?: boolean }) {
	const rating = Math.max(0, Math.min(5, Number(value) || 0));

	if (rating === 0) {
		return <span className="es-stars">Not rated yet</span>;
	}

	return (
		<span className="es-stars" role="img" aria-label={`Rated ${rating.toFixed(1)} out of 5`}>
			<span className="es-stars-track" aria-hidden="true">
				<Row />
				<span className="es-stars-fill" style={{ width: `${(rating / 5) * 100}%` }}>
					<Row />
				</span>
			</span>
			{showValue && <span aria-hidden="true">{rating.toFixed(1)}</span>}
		</span>
	);
}
