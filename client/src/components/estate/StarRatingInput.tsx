"use client";

import { Star } from "lucide-react";
import { useState } from "react";

const WORDS = ["", "Poor", "Fair", "Good", "Very good", "Excellent"];
const STARS = [1, 2, 3, 4, 5];

type StarRatingInputProps = {
	name: string;
	value?: number;
	onChange: (value: number) => void;
	onBlur?: () => void;
};

export default function StarRatingInput({ name, value, onChange, onBlur }: StarRatingInputProps) {
	const [hovered, setHovered] = useState(0);
	const chosen = Number(value) || 0;
	const shown = hovered || chosen;

	return (
		<div className="es-starinput">
			<div className="es-starinput-row" onMouseLeave={() => setHovered(0)}>
				{STARS.map((star) => (
					<label
						key={star}
						className="es-starinput-star"
						data-on={star <= shown}
						onMouseEnter={() => setHovered(star)}
					>
						<input
							type="radio"
							name={name}
							value={star}
							checked={chosen === star}
							onChange={() => onChange(star)}
							onBlur={onBlur}
							aria-label={`${star} out of 5, ${WORDS[star]}`}
						/>
						<Star aria-hidden="true" />
					</label>
				))}
			</div>
			<span className="es-starinput-value" aria-hidden="true">
				{shown ? (
					<>
						<strong>{shown} of 5</strong> · {WORDS[shown]}
					</>
				) : (
					"Choose a rating"
				)}
			</span>
		</div>
	);
}
