import Header from "@/components/profile/Header";
import ProfileTabs from "@/components/profile/ProfileTabs";
import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Your profile — Tenant Sphere",
	description: "Signed in users can view all their profile information",
};

function ProfilePageContent() {
	return (
		<div className="es-page es-page--wide">
			<Header />
			<ProfileTabs />
		</div>
	);
}

export default function ProfilePage() {
	return (
		<ProtectedRoute>
			<ProfilePageContent />
		</ProtectedRoute>
	);
}
