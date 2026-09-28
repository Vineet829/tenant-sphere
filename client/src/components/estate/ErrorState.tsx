"use client";

import { CloudOff, RotateCw } from "lucide-react";

type ErrorStateProps = {
	/** What failed to load, in plain words: "posts", "your profile". */
	what: string;
	onRetry?: () => void;
	retrying?: boolean;
};

/** Shown when a request fails, so a failure never reads as "nothing here". */
export default function ErrorState({ what, onRetry, retrying = false }: ErrorStateProps) {
	return (
		<div className="es-empty es-empty--error" role="alert">
			<span className="es-empty-icon" aria-hidden="true">
				<CloudOff strokeWidth={1.5} />
			</span>
			<p className="es-empty-title">We could not load {what}</p>
			<p className="es-empty-text">Check your connection and try again. If it keeps happening, the service may be down.</p>
			{onRetry && (
				<button type="button" className="es-btn es-btn--outline" onClick={onRetry} disabled={retrying}>
					{retrying ? <span className="es-spinner" aria-hidden="true" /> : <RotateCw aria-hidden="true" />}
					{retrying ? "Trying again…" : "Try again"}
				</button>
			)}
		</div>
	);
}
