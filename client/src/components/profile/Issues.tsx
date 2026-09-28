"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import { CardsSkeleton } from "@/components/estate/Skeletons";
import { useGetMyIssuesQuery } from "@/lib/redux/features/issues/issueApiSlice";
import { Plus, Wrench } from "lucide-react";
import Link from "next/link";
import IssueCard from "../cards/IssueCard";
import TabHeading from "./TabHeading";

export default function Issues() {
	const { data, isLoading, isFetching, isError, refetch } = useGetMyIssuesQuery();
	const issues = data?.my_issues.results ?? [];

	return (
		<>
			<TabHeading
				title="Issues you reported"
				note="Follow each one from reported to resolved."
				action={
					<Link href="/report-issue" className="es-btn es-btn--outline es-btn--sm">
						<Plus aria-hidden="true" />
						Report an issue
					</Link>
				}
			/>
			{isLoading ? (
				<CardsSkeleton cards={3} />
			) : isError ? (
				<ErrorState what="your issues" onRetry={refetch} retrying={isFetching} />
			) : issues.length > 0 ? (
				<div className="es-cardgrid es-stagger">
					{issues.map((issue, index) => (
						<IssueCard key={issue.id} issue={issue} index={index} />
					))}
				</div>
			) : (
				<EmptyState icon={Wrench} description="When something at your apartment needs attention, report it and track it here.">
					No issues reported
				</EmptyState>
			)}
		</>
	);
}
