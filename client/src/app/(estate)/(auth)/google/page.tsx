"use client";

import Spinner from "@/components/shared/Spinner";
import { useSocialAuth } from "@/hooks";
import { useSocialAuthenticationMutation } from "@/lib/redux/features/auth/authApiSlice";
import { Suspense } from "react";

function SigningIn() {
	return (
		<div className="es-page es-page--wide">
			<div className="es-statuspage">
				<Spinner size="lg" label="Signing you in with Google…" />
				<h1 className="es-statuspage-title">Signing you in</h1>
				<p className="es-statuspage-text">One moment while we finish signing you in with Google.</p>
			</div>
		</div>
	);
}

export default function GoogleLoginPage() {
	return (
		<Suspense fallback={<SigningIn />}>
			<GoogleLoginContent />
		</Suspense>
	);
}

function GoogleLoginContent() {
	const [googleAuthenticate] = useSocialAuthenticationMutation();
	useSocialAuth(googleAuthenticate, "google-oauth2");
	return <SigningIn />;
}
