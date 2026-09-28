import { CircleAlert } from "lucide-react";
import type { ReactNode } from "react";

/** A validation message under a form control, announced as soon as it appears. */
export default function FieldError({ id, children }: { id?: string; children: ReactNode }) {
	return (
		<p className="es-field-error" id={id} role="alert">
			<CircleAlert aria-hidden="true" />
			<span>{children}</span>
		</p>
	);
}
