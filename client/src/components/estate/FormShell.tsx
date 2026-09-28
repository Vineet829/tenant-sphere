import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

type FormShellProps = {
	eyebrow: string;
	title: ReactNode;
	description?: ReactNode;
	back?: { href: string; label: string };
	/** Supporting content under the description: context, steps, reassurance. */
	aside?: ReactNode;
	/** Enlarge the form panel independently of its supporting column. */
	panelSize?: "default" | "large";
	children: ReactNode;
};

/** Two columns: what the form is for on the left, the form itself on the right. */
export default function FormShell({ eyebrow, title, description, back, aside, panelSize = "default", children }: FormShellProps) {
	return (
		<div className="es-page es-page--wide">
			{back && (
				<Link href={back.href} className="es-backlink">
					<ArrowLeft aria-hidden="true" />
					{back.label}
				</Link>
			)}
			<div className="es-formpage">
				<header className="es-formaside">
					<p className="es-eyebrow">{eyebrow}</p>
					<h1 className="es-formaside-title">{title}</h1>
					{description && <p className="es-formaside-text">{description}</p>}
					{aside}
				</header>
				<section className="es-card es-formpanel" data-size={panelSize}>{children}</section>
			</div>
		</div>
	);
}

type Step = { title: string; text: string };

/** Numbered notes in the style of the public pages. */
export function FormSteps({ steps, label }: { steps: Step[]; label?: string }) {
	return (
		<ol className="es-steps" aria-label={label}>
			{steps.map((step, index) => (
				<li key={step.title} className="es-step">
					<span className="es-step-number" aria-hidden="true">
						{String(index + 1).padStart(2, "0")}
					</span>
					<div>
						<p className="es-step-title">{step.title}</p>
						<p className="es-step-text">{step.text}</p>
					</div>
				</li>
			))}
		</ol>
	);
}

type FormNoteProps = {
	icon: ReactNode;
	label: string;
	children: ReactNode;
};

/** A small card for one fact that matters while filling in the form. */
export function FormContext({ icon, label, children }: FormNoteProps) {
	return (
		<div className="es-context">
			<span className="es-context-icon" aria-hidden="true">
				{icon}
			</span>
			<div className="es-context-copy">
				<span className="es-context-label">{label}</span>
				<span className="es-context-value">{children}</span>
			</div>
		</div>
	);
}
