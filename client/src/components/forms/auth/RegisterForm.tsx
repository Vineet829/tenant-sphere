"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Check, Loader2, Mail } from "lucide-react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { useRegisterUserMutation } from "@/lib/redux/features/auth/authApiSlice";
import { registerUserSchema, type TRregisterUserSchema } from "@/lib/validationSchemas";
import { Button } from "@/components/ui/button";
import AuthField from "@/components/auth/AuthField";
import { authErrorMessage } from "@/components/auth/authError";
import styles from "@/components/auth/auth.module.css";

type Props = { disabled?: boolean; onBusyChange?: (busy: boolean) => void };

export default function RegisterForm({ disabled = false, onBusyChange }: Props) {
	const [registerUser, { isLoading }] = useRegisterUserMutation();
	const [serverError, setServerError] = useState<string | null>(null);
	const router = useRouter();
	const { register, handleSubmit, reset, watch, formState: { errors } } = useForm<TRregisterUserSchema>({
		resolver: zodResolver(registerUserSchema), mode: "onTouched",
		defaultValues: { username: "", first_name: "", last_name: "", email: "", password: "", re_password: "" },
	});
	const password = watch("password");
	const requirements = [
		{ label: "8+ characters", met: password.length >= 8 },
		{ label: "Upper & lowercase", met: /[a-z]/.test(password) && /[A-Z]/.test(password) },
		{ label: "A number", met: /\d/.test(password) },
	];

	const onSubmit = async (values: TRregisterUserSchema) => {
		if (disabled || isLoading) return;
		setServerError(null);
		onBusyChange?.(true);
		try {
			await registerUser(values).unwrap();
			toast.success("Check your email for an activation link, then log in to your account.");
			router.push("/login");
			reset();
		} catch (error) {
			setServerError(authErrorMessage(error));
		} finally {
			onBusyChange?.(false);
		}
	};

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className={styles.form} aria-label="Create your account" aria-busy={isLoading}>
			{serverError && <p className={styles.formError} role="alert">{serverError}</p>}
			<fieldset disabled={disabled || isLoading} className={styles.formFields}>
				<legend className={styles.visuallyHidden}>Your account details</legend>
				<div className={styles.fieldGrid}>
					<AuthField id="register-first-name" label="First name" autoComplete="given-name" placeholder="First name" required maxLength={60} {...register("first_name")} error={errors.first_name?.message} />
					<AuthField id="register-last-name" label="Last name" autoComplete="family-name" placeholder="Last name" required maxLength={60} {...register("last_name")} error={errors.last_name?.message} />
					<AuthField id="register-username" label="Username" autoComplete="username" autoCapitalize="none" spellCheck={false} placeholder="your_username" required maxLength={60} {...register("username")} error={errors.username?.message} />
					<AuthField id="register-email" label="Email address" type="email" autoComplete="email" autoCapitalize="none" spellCheck={false} placeholder="you@example.com" required maxLength={254} {...register("email")} error={errors.email?.message} />
					<AuthField id="register-password" label="Password" type="password" autoComplete="new-password" placeholder="Create a password" required {...register("password")} error={errors.password?.message} aria-describedby="password-requirements" />
					<AuthField id="register-confirm-password" label="Confirm password" type="password" autoComplete="new-password" placeholder="Repeat password" required {...register("re_password")} error={errors.re_password?.message} />
				</div>
				<ul id="password-requirements" className={styles.requirements} aria-label="Password requirements">{requirements.map(({ label, met }) => <li key={label} className={met ? styles.requirementMet : ""}><Check size={12} aria-hidden="true" /><span>{label}<span className={styles.visuallyHidden}>{met ? ": met" : ": required"}</span></span></li>)}</ul>
				<Button type="submit" className={styles.submitButton} disabled={disabled || isLoading}>{isLoading ? <><Loader2 size={18} className={styles.spinner} aria-hidden="true" />Creating your account…</> : <>Create account<ArrowRight size={18} aria-hidden="true" /></>}</Button>
			</fieldset>
			<p className={styles.activationNote}><Mail size={15} aria-hidden="true" />We’ll email you a link to activate your account.</p>
		</form>
	);
}
