import type { ReactNode } from "react";

type PageHeaderProps = {
	eyebrow?: string;
	title: ReactNode;
	count?: number;
	description?: ReactNode;
	actions?: ReactNode;
	compact?: boolean;
};

export default function PageHeader({ eyebrow, title, count, description, actions, compact = false }: PageHeaderProps) {
	return (
		<header className={`es-pagehead${compact ? " es-pagehead--compact" : ""}`}>
			<div className="es-pagehead-copy">
				{eyebrow && <p className="es-eyebrow">{eyebrow}</p>}
				<h1 className="es-pagehead-title">
					<span>{title}</span>
					{typeof count === "number" && (
						<span className="es-count" aria-label={`${count} in total`}>
							{count}
						</span>
					)}
				</h1>
				{description && <p className="es-pagehead-lede">{description}</p>}
			</div>
			{actions && <div className="es-pagehead-actions">{actions}</div>}
		</header>
	);
}
