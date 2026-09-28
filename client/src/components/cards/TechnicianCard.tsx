"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import PageHeader from "@/components/estate/PageHeader";
import PersonCard, { type PersonFact } from "@/components/estate/PersonCard";
import RatingStars from "@/components/estate/RatingStars";
import { PeopleSkeleton } from "@/components/estate/Skeletons";
import { joined, label, monthYear } from "@/components/estate/format";
import { useGetAllTechniciansQuery } from "@/lib/redux/features/users/usersApiSlice";
import { useAppSelector } from "@/lib/redux/hooks/typedHooks";
import type { Profile, UserState } from "@/types";
import { CalendarDays, MapPin, SearchX, Star, Wrench } from "lucide-react";
import Link from "next/link";
import PaginationSection from "../shared/PaginationSection";
import UsersSearch from "../shared/search/UsersSearch";

const PAGE_SIZE = 9;

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
	const totalPages = Math.ceil(totalCount / PAGE_SIZE);

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
							avatar={technician.avatar}
							bio={technician.bio}
							badge={label(technician.occupation)}
							facts={factsFor(technician)}
							footer={
								<>
									<RatingStars value={technician.average_rating} />
									<Link
										href={`/add-rating?username=${technician.username}`}
										className="es-btn es-btn--outline es-btn--sm"
										aria-label={`Rate ${technician.full_name}`}
									>
										<Star aria-hidden="true" />
										Rate
									</Link>
								</>
							}
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
