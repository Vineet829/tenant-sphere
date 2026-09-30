import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import FormShell, { FormContext } from "@/components/estate/FormShell";
import CreateReportForm from "@/components/forms/report-tenant/CreateReportForm";
import { ShieldCheck } from "lucide-react";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Report a tenant — Tenant Sphere",
	description: "Tenants can report their fellow tenants in cases of misconduct or misbehavior",
};

function ReportTenantPage() {
	return (
		<FormShell
			eyebrow="Get help"
			panelSize="large"
			title={
				<>
					Raise a <em>concern</em>
				</>
			}
			description="When something a neighbour does becomes a problem, let the management know. Say what happened, plainly."
			back={{ href: "/profile", label: "Back to profile" }}
			aside={
				<FormContext icon={<ShieldCheck strokeWidth={1.75} />} label="Anonymous">
					We act on every report, and never disclose who raised it.
				</FormContext>
			}
		>
			<CreateReportForm />
		</FormShell>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><ReportTenantPage /></ProtectedRoute>;
}
