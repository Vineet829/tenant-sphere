"use client";

import { useState } from "react";
import { ArrowLeft, ArrowRight, ArrowUpRight, Loader2, Users } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";
import { Button } from "@/components/ui/button";
import LoginForm from "@/components/forms/auth/LoginForm";
import RegisterForm from "@/components/forms/auth/RegisterForm";
import { useRedirectIfAuthenticated } from "@/hooks";
import { useLoginUserMutation } from "@/lib/redux/features/auth/authApiSlice";
import { useAppDispatch } from "@/lib/redux/hooks/typedHooks";
import { setAuth } from "@/lib/redux/features/auth/authSlice";
import { UseGoogle } from "@/utils";
import AuthAside from "./AuthAside";
import { authErrorMessage } from "./authError";
import styles from "./auth.module.css";

export default function AuthExperience({ mode }: { mode: "login" | "register" }) {
	useRedirectIfAuthenticated();
	const isRegister = mode === "register";
	const router = useRouter();
	const dispatch = useAppDispatch();
	const [loginGuest, { isLoading: guestPending }] = useLoginUserMutation();
	const [formPending, setFormPending] = useState(false);
	const [googlePending, setGooglePending] = useState(false);
	const [alternativeError, setAlternativeError] = useState<string | null>(null);
	const busy = formPending || guestPending || googlePending;

	const handleGuest = async () => {
		if (busy) return;
		setAlternativeError(null);
		try {
			await loginGuest({ email: "testuser@gmail.com", password: "example1234" }).unwrap();
			dispatch(setAuth());
			toast.success("Login Successful");
			router.push("/welcome");
		} catch (error) {
			setAlternativeError(authErrorMessage(error));
		}
	};

	const handleGoogle = async () => {
		if (busy) return;
		setGooglePending(true);
		setAlternativeError(null);
		try { await UseGoogle(); } finally { setGooglePending(false); }
	};

	return (
		<div className={styles.frame}>
			<a href="#auth-form" className={styles.skipLink}>Skip to form</a>
			<header className={styles.header}>
				<Link href="/" className={styles.wordmark} aria-label="Tenant Sphere home">tenant<span>sphere</span><span className={styles.wordmarkDot}>.</span></Link>
				<Link href="/" className={styles.backLink}><ArrowLeft size={15} aria-hidden="true" />Back to home</Link>
			</header>

			<main className={`${styles.layout} ${isRegister ? styles.registerLayout : ""}`}>
				<AuthAside mode={mode} />

				<section id="auth-form" tabIndex={-1} className={styles.panel} aria-labelledby="auth-title">
					<div className={styles.formHeading}>
						<p className={styles.formEyebrow}>{isRegister ? "A NEW BEGINNING" : "WELCOME HOME"}</p>
						<h1 id="auth-title">{isRegister ? "Create your account" : "Come on in."}</h1>
						<p>{isRegister ? "A few details, and you’re one step closer." : "Log in to your Tenant Sphere community."}</p>
					</div>

					{isRegister ? <RegisterForm disabled={guestPending || googlePending} onBusyChange={setFormPending} /> : <LoginForm disabled={guestPending || googlePending} onBusyChange={setFormPending} />}

					<div className={styles.divider}><span>or continue with</span></div>
					<div className={styles.alternatives}>
						<Button type="button" variant="outline" className={styles.alternativeButton} disabled={busy} onClick={handleGoogle} aria-busy={googlePending}>{googlePending ? <Loader2 size={17} className={styles.spinner} aria-hidden="true" /> : <ArrowUpRight size={17} aria-hidden="true" />}{googlePending ? "Connecting…" : "Google"}</Button>
						<Button type="button" variant="outline" className={styles.alternativeButton} disabled={busy} onClick={handleGuest} aria-busy={guestPending}>{guestPending ? <Loader2 size={17} className={styles.spinner} aria-hidden="true" /> : <Users size={17} aria-hidden="true" />}{guestPending ? "Logging in…" : "Try as a guest"}</Button>
					</div>
					{alternativeError && <p className={styles.formError} role="alert">{alternativeError}</p>}
					<div className={styles.switchPage}>
						<span>{isRegister ? "Already a neighbour?" : "New to Tenant Sphere?"}</span>
						<Link href={isRegister ? "/login" : "/register"} prefetch={false}>{isRegister ? "Log in" : "Create an account"}<ArrowRight size={16} aria-hidden="true" /></Link>
					</div>
				</section>
			</main>

			<footer className={styles.footer}><span>© {new Date().getFullYear()} Tenant Sphere</span><span>Good things start with good neighbours.</span></footer>
		</div>
	);
}
