"use client";

import ConfirmButton from "@/components/estate/ConfirmButton";
import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import { isMissing } from "@/components/estate/apiError";
import IssueProgress from "@/components/estate/IssueProgress";
import { PriorityPill, StatusPill } from "@/components/estate/Pills";
import { DetailSkeleton } from "@/components/estate/Skeletons";
import { label, plural } from "@/components/estate/format";
import { useDeleteIssueMutation, useGetSingleIssueQuery } from "@/lib/redux/features/issues/issueApiSlice";
import { useGetUserProfileQuery } from "@/lib/redux/features/users/usersApiSlice";
import { extractErrorMessage } from "@/utils";
import {
	ArrowLeft,
	DoorOpen,
	Eye,
	FileQuestion,
	Gauge,
	ListChecks,
	RefreshCw,
	ScrollText,
	Trash2,
	UserRound,
	Wrench,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";

interface IssueDetailsProps {
	params: {
		id: string;
	};
}

function BackLink() {
	return (
		<Link href="/profile" className="es-backlink">
			<ArrowLeft aria-hidden="true" />
			Back to profile
		</Link>
	);
}

function progressNote(status?: string, assignedTo?: string) {
	if (status === "resolved") return "This issue has been resolved.";
	if (status === "in_progress") {
		return assignedTo ? `${assignedTo} is working on this.` : "Work on this issue is under way.";
	}
	return assignedTo
		? `Reported and assigned to ${assignedTo}. Work has not started yet.`
		: "Reported. Waiting to be assigned to a technician.";
}

export default function IssueDetails({ params }: IssueDetailsProps) {
	const id = params.id;
	const { data, isLoading, isFetching, isError, error, refetch } = useGetSingleIssueQuery(id);
	const issue = data?.issue;
	const router = useRouter();

	const { data: currentUser } = useGetUserProfileQuery();

	const canUpdate = issue?.assigned_to === currentUser?.profile.full_name;

	const canDelete = issue?.reported_by === currentUser?.profile.full_name;

	const [deleteIssue, { isLoading: isDeleting }] = useDeleteIssueMutation();

	const handleDeleteIssue = async () => {
		if (issue?.id) {
			try {
				await deleteIssue(issue.id).unwrap();
				router.push("/profile");
				toast.success("Your Issue was deleted");
			} catch (e) {
				const errorMessage = extractErrorMessage(e);
				toast.error(errorMessage || "An error occurred");
			}
		}
	};

	if (isLoading) {
		return (
			<div className="es-page es-page--wide">
				<BackLink />
				<DetailSkeleton />
			</div>
		);
	}

	if (isError && !isMissing(error)) {
		return (
			<div className="es-page es-page--wide">
				<BackLink />
				<ErrorState what="this issue" onRetry={refetch} retrying={isFetching} />
			</div>
		);
	}

	if (isError || !issue) {
		return (
			<div className="es-page es-page--wide">
				<BackLink />
				<EmptyState
					icon={FileQuestion}
					description="It may have been deleted, or the link might be out of date."
					action={
						<Link href="/profile" className="es-btn es-btn--primary">
							Go to your profile
						</Link>
					}
				>
					We could not find that issue
				</EmptyState>
			</div>
		);
	}

	return (
		<div className="es-page es-page--wide">
			<BackLink />

			<header className="es-issuehead">
				<p className="es-eyebrow">Issue · Apartment {issue.apartment_unit}</p>
				<h1 className="es-pagehead-title">{issue.title}</h1>
				<div className="es-issuehead-pills">
					<StatusPill status={issue.status} />
					<PriorityPill priority={issue.priority} />
				</div>
			</header>

			<div className="es-detailgrid">
				<div className="es-detailstack">
					<section className="es-card es-progresscard" aria-labelledby="issue-progress">
						<h2 className="es-section-title" id="issue-progress">
							<ListChecks aria-hidden="true" strokeWidth={1.75} />
							Where things stand
						</h2>
						<IssueProgress status={issue.status} />
						<p className="es-progresscard-note">{progressNote(issue.status, issue.assigned_to)}</p>
					</section>

					<section className="es-card es-panel" aria-labelledby="issue-description">
						<h2 className="es-section-title" id="issue-description">
							<ScrollText aria-hidden="true" strokeWidth={1.75} />
							What was reported
						</h2>
						<p className="es-prose">{issue.description}</p>
					</section>
				</div>

				<aside className="es-card es-panel" aria-labelledby="issue-details">
					<h2 className="es-section-title" id="issue-details">
						<Wrench aria-hidden="true" strokeWidth={1.75} />
						Details
					</h2>
					<dl className="es-deflist">
						<div className="es-defrow">
							<dt>
								<DoorOpen aria-hidden="true" strokeWidth={1.75} />
								Apartment
							</dt>
							<dd>{issue.apartment_unit}</dd>
						</div>
						<div className="es-defrow">
							<dt>
								<UserRound aria-hidden="true" strokeWidth={1.75} />
								Reported by
							</dt>
							<dd>{issue.reported_by}</dd>
						</div>
						<div className="es-defrow">
							<dt>
								<Wrench aria-hidden="true" strokeWidth={1.75} />
								Assigned to
							</dt>
							<dd data-empty={!issue.assigned_to}>{issue.assigned_to || "Not assigned yet"}</dd>
						</div>
						<div className="es-defrow">
							<dt>
								<Gauge aria-hidden="true" strokeWidth={1.75} />
								Priority
							</dt>
							<dd>{label(issue.priority)}</dd>
						</div>
						<div className="es-defrow">
							<dt>
								<Eye aria-hidden="true" strokeWidth={1.75} />
								Seen
							</dt>
							<dd>{plural(issue.view_count, "view")}</dd>
						</div>
					</dl>

					{(canUpdate || canDelete) && (
						<div className="es-detail-actions">
							{canUpdate && (
								<Link href={`/issue/update-issue/${id}`} className="es-btn es-btn--primary es-btn--block">
									<RefreshCw aria-hidden="true" />
									Update status
								</Link>
							)}

							{canDelete && (
								<ConfirmButton
									className="es-btn es-btn--danger es-btn--block"
									onConfirm={handleDeleteIssue}
									disabled={isDeleting}
									confirmLabel={
										<>
											<Trash2 aria-hidden="true" />
											Delete for good?
										</>
									}
								>
									<Trash2 aria-hidden="true" />
									Delete issue
								</ConfirmButton>
							)}
						</div>
					)}
				</aside>
			</div>
		</div>
	);
}
