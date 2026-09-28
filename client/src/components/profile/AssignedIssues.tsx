"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import { CardsSkeleton } from "@/components/estate/Skeletons";
import { useGetMyAssignedIssuesQuery } from "@/lib/redux/features/issues/issueApiSlice";
import { ClipboardCheck } from "lucide-react";
import IssueCard from "../cards/IssueCard";
import TabHeading from "./TabHeading";

export default function AssignedIssues() {
	const { data, isLoading, isFetching, isError, refetch } = useGetMyAssignedIssuesQuery("");
	const issues = data?.assigned_issues.results ?? [];

	return (
		<>
			<TabHeading title="Assigned to you" note="Open an issue to update its status." />
			{isLoading ? (
				<CardsSkeleton cards={3} />
			) : isError ? (
				<ErrorState what="your assigned issues" onRetry={refetch} retrying={isFetching} />
			) : issues.length > 0 ? (
				<div className="es-cardgrid es-stagger">
					{issues.map((issue, index) => (
						<IssueCard key={issue.id} issue={issue} index={index} />
					))}
				</div>
			) : (
				<EmptyState icon={ClipboardCheck} description="Issues that are assigned to you will be listed here.">
					Nothing assigned to you
				</EmptyState>
			)}
		</>
	);
}
