import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import TechnicianCard from "@/components/cards/TechnicianCard";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Technicians — Tenant Sphere",
	description:
		"Tenants can see a list of technicians, what they specialize on and their rating",
};



function TechniciansPage() {
	return (
		<>
			<TechnicianCard />
		</>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><TechniciansPage /></ProtectedRoute>;
}
