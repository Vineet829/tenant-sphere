import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

type EmptyStateProps = {
	icon: LucideIcon;
	/** Headline. */
	children: ReactNode;
	description?: ReactNode;
	action?: ReactNode;
};

export default function EmptyState({ children, icon: Icon, description, action }: EmptyStateProps) {
	return (
		<div className="es-empty">
			<span className="es-empty-icon" aria-hidden="true">
				<Icon strokeWidth={1.5} />
			</span>
			<p className="es-empty-title">{children}</p>
			{description && <p className="es-empty-text">{description}</p>}
			{action}
		</div>
	);
}
