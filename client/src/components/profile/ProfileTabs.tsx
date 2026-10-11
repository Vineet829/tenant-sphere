"use client";

import { useGetMyAssignedIssuesQuery, useGetMyIssuesQuery } from "@/lib/redux/features/issues/issueApiSlice";
import { useGetMyPostsQuery } from "@/lib/redux/features/posts/postApiSlice";
import { useGetMyReportsQuery } from "@/lib/redux/features/reports/reportApiSlice";
import * as Tabs from "@radix-ui/react-tabs";
import { ClipboardCheck, Flag, MessagesSquare, UserRound, Wrench } from "lucide-react";
import About from "./About";
import AssignedIssues from "./AssignedIssues";
import Issues from "./Issues";
import Posts from "./Posts";
import Reports from "./Reports";

function Count({ value }: { value?: number }) {
	if (typeof value !== "number") return null;
	return <span className="es-tab-count">{value}</span>;
}

export default function ProfileTabs() {
	// The first page of each list carries its full count.
	const { data: posts } = useGetMyPostsQuery(1);
	const { data: issues } = useGetMyIssuesQuery(1);
	const { data: reports } = useGetMyReportsQuery(1);
	const { data: assigned } = useGetMyAssignedIssuesQuery(1);

	return (
		<Tabs.Root className="es-tabs" defaultValue="about">
			<Tabs.List aria-label="Profile sections">
				<Tabs.Trigger value="about">
					<UserRound aria-hidden="true" strokeWidth={1.75} />
					About
				</Tabs.Trigger>
				<Tabs.Trigger value="posts">
					<MessagesSquare aria-hidden="true" strokeWidth={1.75} />
					Posts
					<Count value={posts?.my_posts.count} />
				</Tabs.Trigger>
				<Tabs.Trigger value="my-issues">
					<Wrench aria-hidden="true" strokeWidth={1.75} />
					My issues
					<Count value={issues?.my_issues.count} />
				</Tabs.Trigger>
				<Tabs.Trigger value="my-reports">
					<Flag aria-hidden="true" strokeWidth={1.75} />
					My reports
					<Count value={reports?.reports.count} />
				</Tabs.Trigger>
				<Tabs.Trigger value="assigned-issues">
					<ClipboardCheck aria-hidden="true" strokeWidth={1.75} />
					Assigned to me
					<Count value={assigned?.assigned_issues.count} />
				</Tabs.Trigger>
			</Tabs.List>

			<Tabs.Content value="about">
				<About />
			</Tabs.Content>
			<Tabs.Content value="posts">
				<Posts />
			</Tabs.Content>
			<Tabs.Content value="my-issues">
				<Issues />
			</Tabs.Content>
			<Tabs.Content value="my-reports">
				<Reports />
			</Tabs.Content>
			<Tabs.Content value="assigned-issues">
				<AssignedIssues />
			</Tabs.Content>
		</Tabs.Root>
	);
}
