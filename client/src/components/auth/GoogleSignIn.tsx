"use client";

import { Check } from "lucide-react";
import { Suspense } from "react";
import { useSocialAuth } from "@/hooks";
import { useSocialAuthenticationMutation } from "@/lib/redux/features/auth/authApiSlice";
import styles from "./auth.module.css";

function SigningIn({ signedIn = false }: { signedIn?: boolean }) {
	return (
		<div className={styles.frame}>
			<header className={styles.header}>
				<span className={styles.wordmark}>tenant<span>sphere</span><span className={styles.wordmarkDot}>.</span></span>
			</header>

			<main className={styles.statusLayout}>
				<section className={`${styles.panel} ${styles.statusPanel}`} aria-labelledby="google-title">
					<span className={`${styles.statusIcon} ${signedIn ? styles.statusIconDone : ""}`} aria-hidden="true">
						{signedIn && <Check size={24} strokeWidth={2.5} />}
					</span>
					<div className={styles.formHeading}>
						<p className={styles.formEyebrow}>GOOGLE SIGN-IN</p>
						<h1 id="google-title">{signedIn ? "You’re in." : "Signing you in"}</h1>
						<p role="status">{signedIn ? "Opening your community…" : "Confirming your Google account…"}</p>
					</div>
				</section>
			</main>

			<footer className={styles.footer}><span>© {new Date().getFullYear()} Tenant Sphere</span><span>Good things start with good neighbours.</span></footer>
		</div>
	);
}

function GoogleSignInContent() {
	const [googleAuthenticate] = useSocialAuthenticationMutation();
	const signedIn = useSocialAuth(googleAuthenticate, "google-oauth2");
	return <SigningIn signedIn={signedIn} />;
}

export default function GoogleSignIn() {
	return (
		<Suspense fallback={<SigningIn />}>
			<GoogleSignInContent />
		</Suspense>
	);
}
