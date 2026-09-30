"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import PageHeader from "@/components/estate/PageHeader";
import PersonCard, { type PersonFact } from "@/components/estate/PersonCard";
import { PeopleSkeleton } from "@/components/estate/Skeletons";
import { joined, label, monthYear } from "@/components/estate/format";
import { useGetAllTechniciansQuery } from "@/lib/redux/features/users/usersApiSlice";
import { useAppSelector } from "@/lib/redux/hooks/typedHooks";
import type { Profile, UserState } from "@/types";
import { CalendarDays, MapPin, SearchX, Wrench } from "lucide-react";
import PaginationSection from "../shared/PaginationSection";
import UsersSearch from "../shared/search/UsersSearch";


function factsFor(technician: Profile): PersonFact[] {
	return [
		{
			icon: MapPin,
			label: "From",
			value: joined([technician.city_of_origin, technician.country_of_origin]) || "Not shared",
		},
		{ icon: CalendarDays, label: "Joined", value: monthYear(technician.date_joined) || "Not available" },
	];
}

export default function TechnicianCard() {
	const searchTerm = useAppSelector((state: UserState) => state.user.searchTerm);
	const page = useAppSelector((state: UserState) => state.user.page);
	const { data, isLoading, isFetching, isError, refetch } = useGetAllTechniciansQuery({ searchTerm, page });
	const ready = !isLoading && !isError;

	const technicians = data?.non_tenant_profiles.results ?? [];
	const totalCount = data?.non_tenant_profiles.count || 0;
	const totalPages = data?.non_tenant_profiles.total_pages ?? 0;

	return (
		<div className="es-page es-page--wide">
			<PageHeader
				eyebrow="Directory"
				title={
					<>
						The people who <em>fix things</em>
					</>
				}
				count={ready ? totalCount : undefined}
				description="Find the right pair of hands for the job, and rate the work once it is done."
			/>

			<div className="es-toolbar">
				<UsersSearch placeholder="Search technicians by name or username" label="Search technicians" />
			</div>

			{isLoading ? (
				<PeopleSkeleton cards={6} />
			) : isError ? (
				<ErrorState what="the technicians" onRetry={refetch} retrying={isFetching} />
			) : technicians.length > 0 ? (
				<div className="es-people es-stagger" aria-busy={isFetching}>
					{technicians.map((technician, index) => (
						<PersonCard
							key={technician.id}
							index={index}
							name={technician.full_name}
							username={technician.username}
							slug={technician.slug}
							avatar={technician.avatar}
							bio={technician.bio}
							badge={label(technician.occupation)}
							facts={factsFor(technician)}
							summaryFacts={factsFor(technician).slice(0, 1)}
							profileType="technician"
							rating={technician.average_rating}
						/>
					))}
				</div>
			) : searchTerm ? (
				<EmptyState icon={SearchX} description="Check the spelling, or try a first name, last name or username.">
					No technicians match &ldquo;{searchTerm}&rdquo;
				</EmptyState>
			) : (
				<EmptyState icon={Wrench} description="Technicians appear here once they join the building.">
					No technicians yet
				</EmptyState>
			)}

			<PaginationSection totalPages={totalPages} entityType="user" />
		</div>
	);
}
