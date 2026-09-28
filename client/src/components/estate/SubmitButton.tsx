import { ArrowRight } from "lucide-react";
import type { ReactNode } from "react";

type SubmitButtonProps = {
	children: ReactNode;
	/** Shown while the request is in flight. */
	busyLabel?: string;
	isLoading?: boolean;
	disabled?: boolean;
	arrow?: boolean;
	className?: string;
};

export default function SubmitButton({
	children,
	busyLabel = "Saving…",
	isLoading = false,
	disabled = false,
	arrow = true,
	className = "",
}: SubmitButtonProps) {
	return (
		<button
			type="submit"
			className={`es-btn es-btn--primary ${className}`.trim()}
			disabled={isLoading || disabled}
			aria-busy={isLoading}
		>
			{isLoading ? (
				<>
					<span className="es-spinner" aria-hidden="true" />
					{busyLabel}
				</>
			) : (
				<>
					{children}
					{arrow && <ArrowRight className="es-btn-arrow" aria-hidden="true" />}
				</>
			)}
		</button>
	);
}
