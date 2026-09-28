"use client";

import { useGetMyApartmentQuery } from "@/lib/redux/features/apartment/apartmentApiSlice";
import { useGetSingleIssueQuery } from "@/lib/redux/features/issues/issueApiSlice";
import { useGetAllTechniciansQuery } from "@/lib/redux/features/users/usersApiSlice";
import { useAppSelector } from "@/lib/redux/hooks/typedHooks";
import { DoorOpen, Wrench } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { FormContext } from "./FormShell";
import PersonAvatar from "./PersonAvatar";
import { StatusPill } from "./Pills";
import RatingStars from "./RatingStars";
import { joined, label } from "./format";

/** The apartment a new issue will be filed against. */
export function ApartmentContext() {
	const { data, isLoading } = useGetMyApartmentQuery();
	const apartment = data?.apartment;

	if (isLoading) {
		return (
			<div className="es-context" role="status">
				<span className="es-sr">Loading your apartment…</span>
				<span className="es-skeleton" style={{ width: 38, height: 38, borderRadius: 11 }} />
				<span className="es-skeleton" style={{ width: 180, height: 14 }} />
			</div>
		);
	}

	return (
		<FormContext icon={<DoorOpen strokeWidth={1.75} />} label={apartment ? "Reporting for" : "Before you start"}>
			{apartment ? (
				joined([`Apartment ${apartment.unit_number}`, apartment.building], " · ")
			) : (
				<>
					<Link href="/apartment">Add your apartment</Link> to your profile first.
				</>
			)}
		</FormContext>
	);
}

/** The issue whose status is about to change. */
export function IssueContext({ id }: { id: string }) {
	const { data } = useGetSingleIssueQuery(id);
	const issue = data?.issue;

	if (!issue) return null;

	return (
		<div className="es-context es-context--stack">
			<div className="es-context-copy">
				<span className="es-context-label">Apartment {issue.apartment_unit}</span>
				<span className="es-context-value">{issue.title}</span>
			</div>
			<StatusPill status={issue.status} />
		</div>
	);
}

/** The technician being rated, when they are in the directory we already hold. */
export function TechnicianContext() {
	const searchTerm = useAppSelector((state) => state.user.searchTerm);
	const page = useAppSelector((state) => state.user.page);
	const { data } = useGetAllTechniciansQuery({ searchTerm, page });
	const [username, setUsername] = useState("");

	useEffect(() => {
		setUsername(new URLSearchParams(window.location.search).get("username") ?? "");
	}, []);

	if (!username) return null;

	const technician = data?.non_tenant_profiles.results.find((profile) => profile.username === username);

	if (!technician) {
		return (
			<FormContext icon={<Wrench strokeWidth={1.75} />} label="You are rating">
				@{username}
			</FormContext>
		);
	}

	return (
		<div className="es-context">
			<PersonAvatar name={technician.full_name} seed={technician.username} src={technician.avatar} size="md" />
			<div className="es-context-copy">
				<span className="es-context-label">You are rating</span>
				<span className="es-context-value">
					{joined([technician.full_name, label(technician.occupation)], " · ")}
				</span>
				<RatingStars value={technician.average_rating} />
			</div>
		</div>
	);
}
