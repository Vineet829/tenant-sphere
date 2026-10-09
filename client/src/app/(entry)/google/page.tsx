import type { Metadata } from "next";
import GoogleSignIn from "@/components/auth/GoogleSignIn";

export const metadata: Metadata = { title: "Signing in — Tenant Sphere" };

export default function GooglePage() {
	return <GoogleSignIn />;
}
