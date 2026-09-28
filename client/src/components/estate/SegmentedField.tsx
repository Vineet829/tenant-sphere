"use client";

import FieldError from "./FieldError";
import { useId, type CSSProperties, type ReactNode, type Ref } from "react";

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
	inputRef?: Ref<HTMLInputElement>;
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
	inputRef,
	hint,
	error,
	plain = false,
}: SegmentedFieldProps) {
	const id = useId();
	const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;
	const focusValue = options.some((option) => option.value === value) ? value : options[0]?.value;

	return (
		<fieldset className="es-field es-fieldset" data-invalid={Boolean(error)} role="radiogroup" aria-invalid={Boolean(error)} aria-describedby={describedBy}>
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
							ref={option.value === focusValue ? inputRef : undefined}
							aria-describedby={describedBy}
						/>
						<span className="es-segment-label">{option.label}</span>
					</label>
				))}
			</div>
			{hint && !error && <p className="es-hint" id={`${id}-hint`}>{hint}</p>}
			{error && <FieldError id={`${id}-error`}>{error}</FieldError>}
		</fieldset>
	);
}
