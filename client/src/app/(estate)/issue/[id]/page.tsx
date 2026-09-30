import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import IssueDetails from "@/components/issue/IssueDetails";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Issue — Tenant Sphere",
	description:
		"Authenticated uses can get the details of the issue they have raised. They can also delete the issue",
};

interface ParamsProps {
	params: {
		id: string;
	};
}

function IssueDetailPage({ params }: ParamsProps) {
	return <IssueDetails params={params} />;
}

export default async function ProtectedPage({ params }: { params: Promise<ParamsProps["params"]> }) {
	const props = { params: await params };
	return <ProtectedRoute><IssueDetailPage {...props} /></ProtectedRoute>;
}
