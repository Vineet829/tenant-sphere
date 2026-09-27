"use client";

import { forwardRef, useState, type ReactNode } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Input, type InputProps } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import styles from "./auth.module.css";

interface AuthFieldProps extends InputProps {
	id: string;
	label: string;
	error?: string;
	hint?: string;
	labelAccessory?: ReactNode;
}

const AuthField = forwardRef<HTMLInputElement, AuthFieldProps>(function AuthField(
	{ id, label, error, hint, labelAccessory, type = "text", disabled, "aria-describedby": describedBy, ...props },
	ref,
) {
	const [visible, setVisible] = useState(false);
	const password = type === "password";
	const descriptions = [describedBy, hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(" ");
	return (
		<div className={styles.field}>
			<div className={styles.labelRow}>
				<Label htmlFor={id} className={styles.label}>{label}</Label>
				{labelAccessory}
			</div>
			<div className={styles.control}>
				<Input {...props} ref={ref} id={id} disabled={disabled} type={password && visible ? "text" : type} className={`${styles.input} ${password ? styles.passwordInput : ""}`} aria-invalid={Boolean(error)} aria-describedby={descriptions || undefined} />
				{password && (
					<Button type="button" variant="ghost" size="icon" className={styles.revealButton} onClick={() => setVisible(!visible)} disabled={disabled} aria-label={`${visible ? "Hide" : "Show"} ${label.toLowerCase()}`} aria-pressed={visible}>
						{visible ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
					</Button>
				)}
			</div>
			{hint && <p id={`${id}-hint`} className={styles.fieldHint}>{hint}</p>}
			{error && <p id={`${id}-error`} className={styles.fieldError} aria-live="polite">{error}</p>}
		</div>
	);
});

export default AuthField;
