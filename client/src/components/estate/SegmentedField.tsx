import FieldError from "./FieldError";
import type { CSSProperties, ReactNode } from "react";

export type SegmentOption = {
	value: string;
	label: string;
	tone?: "info" | "warn" | "ok" | "danger";
};

type SegmentedFieldProps = {
	legend: string;
	name: string;
	options: SegmentOption[];
	value?: string;
	onChange: (value: string) => void;
	onBlur?: () => void;
	hint?: ReactNode;
	error?: string;
	/** Leave out the coloured dots when the options carry no status. */
	plain?: boolean;
};

/** A short, mutually exclusive choice shown in full rather than hidden in a menu. */
export default function SegmentedField({
	legend,
	name,
	options,
	value,
	onChange,
	onBlur,
	hint,
	error,
	plain = false,
}: SegmentedFieldProps) {
	return (
		<fieldset className="es-field es-fieldset" data-invalid={Boolean(error)}>
			<legend className="es-label">{legend}</legend>
			<div className="es-segmented" data-plain={plain} style={{ "--es-options": options.length } as CSSProperties}>
				{options.map((option) => (
					<label className="es-segment" data-tone={option.tone} key={option.value}>
						<input
							type="radio"
							name={name}
							value={option.value}
							checked={value === option.value}
							onChange={() => onChange(option.value)}
							onBlur={onBlur}
						/>
						<span className="es-segment-label">{option.label}</span>
					</label>
				))}
			</div>
			{hint && !error && <p className="es-hint">{hint}</p>}
			{error && <FieldError>{error}</FieldError>}
		</fieldset>
	);
}
