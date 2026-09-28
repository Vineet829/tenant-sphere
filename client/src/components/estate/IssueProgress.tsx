import { Check } from "lucide-react";
import type { CSSProperties } from "react";

const STEPS = [
	{ key: "reported", label: "Reported" },
	{ key: "in_progress", label: "In progress" },
	{ key: "resolved", label: "Resolved" },
];

function stateOf(index: number, current: number) {
	if (current === STEPS.length - 1) return "done";
	if (index < current) return "done";
	if (index === current) return "current";
	return "upcoming";
}

/** The reported → in progress → resolved sequence, drawn from the issue's real status. */
export default function IssueProgress({ status }: { status?: string }) {
	const current = Math.max(
		0,
		STEPS.findIndex((step) => step.key === status),
	);

	return (
		<ol className="es-progress" aria-label="Progress">
			{STEPS.map((step, index) => {
				const state = stateOf(index, current);
				return (
					<li
						key={step.key}
						className="es-progress-step"
						data-state={state}
						style={{ "--i": index } as CSSProperties}
						aria-current={index === current ? "step" : undefined}
					>
						<div className="es-progress-track">
							<span className="es-progress-node">
								{state === "done" ? <Check aria-hidden="true" /> : String(index + 1).padStart(2, "0")}
							</span>
							{index < STEPS.length - 1 && (
								<span className="es-progress-line" aria-hidden="true">
									<span />
								</span>
							)}
						</div>
						<span className="es-progress-label">{step.label}</span>
					</li>
				);
			})}
		</ol>
	);
}

export function IssueProgressMini({ status }: { status?: string }) {
	const current = Math.max(
		0,
		STEPS.findIndex((step) => step.key === status),
	);
	return (
		<div className="es-progress es-progress--mini" aria-hidden="true">
			{STEPS.map((step, index) => (
				<span key={step.key} className="es-progress-bar" data-on={index <= current} />
			))}
		</div>
	);
}
