import type { ReactNode } from "react";

/**
 * Inline facts separated by a middle dot. When the list wraps, the dot that would
 * start a new line is clipped instead of being left hanging.
 */
export default function MetaList({ children, className = "" }: { children: ReactNode; className?: string }) {
	return (
		<span className={`es-meta ${className}`.trim()}>
			<span className="es-meta-row">{children}</span>
		</span>
	);
}
