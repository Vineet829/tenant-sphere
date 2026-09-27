"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Loader2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { useLoginUserMutation } from "@/lib/redux/features/auth/authApiSlice";
import { useAppDispatch } from "@/lib/redux/hooks/typedHooks";
import { setAuth } from "@/lib/redux/features/auth/authSlice";
import { loginUserSchema, type TLoginUserSchema } from "@/lib/validationSchemas";
import { Button } from "@/components/ui/button";
import AuthField from "@/components/auth/AuthField";
import { authErrorMessage } from "@/components/auth/authError";
import styles from "@/components/auth/auth.module.css";

type Props = { disabled?: boolean; onBusyChange?: (busy: boolean) => void };

export default function LoginForm({ disabled = false, onBusyChange }: Props) {
	const [loginUser, { isLoading }] = useLoginUserMutation();
	const [serverError, setServerError] = useState<string | null>(null);
	const router = useRouter();
	const dispatch = useAppDispatch();
	const { register, handleSubmit, reset, formState: { errors } } = useForm<TLoginUserSchema>({
		resolver: zodResolver(loginUserSchema), mode: "onTouched", defaultValues: { email: "", password: "" },
	});

	const onSubmit = async (values: TLoginUserSchema) => {
		if (disabled || isLoading) return;
		setServerError(null);
		onBusyChange?.(true);
		try {
			await loginUser(values).unwrap();
			dispatch(setAuth());
			toast.success("Login Successful");
			router.push("/welcome");
			reset();
		} catch (error) {
			setServerError(authErrorMessage(error));
		} finally {
			onBusyChange?.(false);
		}
	};

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className={styles.form} aria-label="Log in" aria-busy={isLoading}>
			{serverError && <p className={styles.formError} role="alert">{serverError}</p>}
			<fieldset disabled={disabled || isLoading} className={styles.formFields}>
				<legend className={styles.visuallyHidden}>Your login details</legend>
				<AuthField id="login-email" label="Email address" type="email" autoComplete="email" autoCapitalize="none" spellCheck={false} placeholder="you@example.com" required {...register("email")} error={errors.email?.message} />
				<AuthField id="login-password" label="Password" type="password" autoComplete="current-password" placeholder="Enter your password" required {...register("password")} error={errors.password?.message} labelAccessory={<Link href="/forgot-password" prefetch={false} className={styles.forgotLink}>Forgot password?</Link>} />
				<Button type="submit" className={styles.submitButton} disabled={disabled || isLoading}>{isLoading ? <><Loader2 size={18} className={styles.spinner} aria-hidden="true" />Logging in…</> : <>Log in<ArrowRight size={18} aria-hidden="true" /></>}</Button>
			</fieldset>
		</form>
	);
}
