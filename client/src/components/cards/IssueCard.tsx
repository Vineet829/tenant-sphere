import { IssueProgressMini } from "@/components/estate/IssueProgress";
import { PriorityPill, StatusPill } from "@/components/estate/Pills";
import { ArrowRight, DoorOpen } from "lucide-react";
import Link from "next/link";
import type { CSSProperties } from "react";

interface Issue {
	id: string;
	title: string;
	description: string;
	apartment_unit: string;
	status: string;
	priority: string;
}

interface IssueCardProps {
	issue: Issue;
	index?: number;
}

export default function IssueCard({ issue, index = 0 }: IssueCardProps) {
	return (
		<Link
			href={`/issue/${issue.id}`}
			className="es-card es-card--interactive es-issue"
			style={{ "--i": Math.min(index, 8) } as CSSProperties}
			aria-label={`${issue.title}. Open issue`}
		>
			<div className="es-issue-top">
				<StatusPill status={issue.status} />
				<PriorityPill priority={issue.priority} />
			</div>

			<h3 className="es-card-title">{issue.title}</h3>
			<p className="es-issue-text">{issue.description}</p>

			<IssueProgressMini status={issue.status} />

			<div className="es-issue-foot">
				<span>
					<DoorOpen aria-hidden="true" strokeWidth={1.75} />
					Apartment {issue.apartment_unit}
				</span>
				<span className="es-issue-open">
					Open
					<ArrowRight aria-hidden="true" />
				</span>
			</div>
		</Link>
	);
}
