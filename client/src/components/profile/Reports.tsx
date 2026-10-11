"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import { CardsSkeleton } from "@/components/estate/Skeletons";
import { longDate, timeAgo } from "@/components/estate/format";
import { useGetMyReportsQuery } from "@/lib/redux/features/reports/reportApiSlice";
import { CalendarDays, Flag, Plus } from "lucide-react";
import Link from "next/link";
import { useEffect, useState, type CSSProperties } from "react";
import PageControls from "../shared/PageControls";
import TabHeading from "./TabHeading";

export default function Reports() {
	const [page, setPage] = useState(1);
	const { currentData: data, isLoading, isFetching, isError, refetch } = useGetMyReportsQuery(page);
	const totalPages = data?.reports.total_pages ?? page;
	useEffect(() => {
		if (data && page > Math.max(totalPages, 1)) setPage(Math.max(totalPages, 1));
	}, [data, page, totalPages]);
	const reports = data?.reports.results ?? [];

	return (
		<>
			<TabHeading
				title="Reports you sent"
				note="Reports stay anonymous. Only you can see this list."
				action={
					<Link href="/report-tenant" className="es-btn es-btn--outline es-btn--sm">
						<Plus aria-hidden="true" />
						Report a tenant
					</Link>
				}
			/>
			{isLoading || (!data && isFetching) ? (
				<CardsSkeleton cards={3} />
			) : isError ? (
				<ErrorState what="your reports" onRetry={refetch} retrying={isFetching} />
			) : reports.length > 0 ? (
				<div className="es-cardgrid es-stagger">
					{reports.map((report, index) => (
						<article
							key={report.id}
							className="es-card es-report"
							style={{ "--i": Math.min(index, 8) } as CSSProperties}
						>
							<time className="es-report-date" dateTime={report.created_at} title={timeAgo(report.created_at)}>
								<CalendarDays aria-hidden="true" strokeWidth={1.75} />
								{longDate(report.created_at)}
							</time>
							<h3 className="es-card-title">{report.title}</h3>
							<p>{report.description}</p>
						</article>
					))}
				</div>
			) : (
				<EmptyState icon={Flag} description="If a neighbour's behaviour becomes a problem, you can raise it with the management.">
					No reports sent
				</EmptyState>
			)}
			<PageControls page={page} totalPages={totalPages} onChange={setPage} pending={isFetching} />
		</>
	);
}
