"use client";

import { useEffect, useState, type ReactNode } from "react";

type ConfirmButtonProps = {
	onConfirm: () => void;
	children: ReactNode;
	/** Shown after the first click, while waiting for the second. */
	confirmLabel: ReactNode;
	className?: string;
	armedClassName?: string;
	disabled?: boolean;
	ariaLabel?: string;
};

/** A destructive action that asks for a second click before it runs. */
export default function ConfirmButton({
	onConfirm,
	children,
	confirmLabel,
	className = "",
	armedClassName = "",
	disabled = false,
	ariaLabel,
}: ConfirmButtonProps) {
	const [armed, setArmed] = useState(false);

	useEffect(() => {
		if (!armed) return;
		const timer = window.setTimeout(() => setArmed(false), 4000);
		return () => window.clearTimeout(timer);
	}, [armed]);

	const handleClick = () => {
		if (!armed) {
			setArmed(true);
			return;
		}
		setArmed(false);
		onConfirm();
	};

	return (
		<button
			type="button"
			className={`${className} ${armed ? armedClassName : ""}`.trim()}
			data-armed={armed}
			onClick={handleClick}
			onBlur={() => setArmed(false)}
			disabled={disabled}
			aria-label={armed ? undefined : ariaLabel}
		>
			{armed ? confirmLabel : children}
			<span className="es-sr" aria-live="polite">
				{armed ? "Press again to confirm." : ""}
			</span>
		</button>
	);
}
