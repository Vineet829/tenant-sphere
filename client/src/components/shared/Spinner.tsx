interface SpinnerProps {
	size?: "sm" | "md" | "lg" | "xl";
	/** Announced to screen readers. */
	label?: string;
}

export default function Spinner({ size = "md", label = "Loading…" }: SpinnerProps) {
	return (
		<div role="status" className={`es-loader es-loader--${size}`}>
			<span className="es-loader-ring" aria-hidden="true" />
			<span className="es-sr">{label}</span>
		</div>
	);
}
