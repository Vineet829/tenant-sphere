import FieldError from "@/components/estate/FieldError";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/ui/password-input";
import { Textarea } from "@/components/ui/textarea";
import Link from "next/link";
import React from "react";
import { FieldErrors, FieldValues, Path, UseFormRegister } from "react-hook-form";

type FormFieldComponentProps<TFieldValues extends FieldValues> = {
	label?: string;
	name: Path<TFieldValues>;
	register: UseFormRegister<TFieldValues>;
	errors: FieldErrors<TFieldValues>;
	type?: React.HTMLInputTypeAttribute;
	placeholder?: string;
	required?: boolean;
	startIcon?: React.ReactNode;
	endIcon?: React.ReactNode;
	isPassword?: boolean;
	isTextArea?: boolean;
	link?: {
		linkText: string;
		linkUrl: string;
	};
	/** Supporting text under the control. */
	hint?: React.ReactNode;
	className?: string;
	disabled?: boolean;
};

export function FormFieldComponent<TFieldValues extends FieldValues>({
	label,
	name,
	register,
	disabled = false,
	errors,
	type = "text",
	placeholder,
	required = false,
	startIcon,
	endIcon,
	link,
	hint,
	className,
	isPassword = false,
	isTextArea = false,
}: FormFieldComponentProps<TFieldValues>) {
	const errorMessage = errors[name]?.message as unknown as string;
	const describedBy = errorMessage ? `${name}-error` : hint ? `${name}-hint` : undefined;

	const renderInputComponent = () => {
		if (isTextArea) {
			return (
				<Textarea
					id={name}
					{...register(name, { required })}
					placeholder={placeholder}
					className={className}
					disabled={disabled}
					aria-invalid={Boolean(errorMessage)}
					aria-describedby={describedBy}
				/>
			);
		} else if (isPassword) {
			return (
				<PasswordInput
					id={name}
					{...register(name, { required })}
					placeholder={placeholder}
					className={className}
					disabled={disabled}
					aria-invalid={Boolean(errorMessage)}
					aria-describedby={describedBy}
				/>
			);
		} else {
			return (
				<Input
					id={name}
					{...register(name, { required })}
					type={type}
					placeholder={placeholder}
					startIcon={startIcon}
					endIcon={endIcon}
					disabled={disabled}
					className={className}
					aria-invalid={Boolean(errorMessage)}
					aria-describedby={describedBy}
				/>
			);
		}
	};

	return (
		<div className="es-field" data-invalid={Boolean(errorMessage)}>
			{(label || link) && (
				<div className="es-field-head">
					{label && (
						<label htmlFor={name} className="es-label">
							{label}
						</label>
					)}
					{link && (
						<Link href={link.linkUrl} className="es-field-link">
							{link.linkText}
						</Link>
					)}
				</div>
			)}
			<div className="es-control">{renderInputComponent()}</div>
			{hint && !errorMessage && (
				<p className="es-hint" id={`${name}-hint`}>
					{hint}
				</p>
			)}
			{errorMessage && <FieldError id={`${name}-error`}>{errorMessage}</FieldError>}
		</div>
	);
}
