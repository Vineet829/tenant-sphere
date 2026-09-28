"use client";

import EmptyState from "@/components/estate/EmptyState";
import ErrorState from "@/components/estate/ErrorState";
import PageHeader from "@/components/estate/PageHeader";
import PersonCard, { type PersonFact } from "@/components/estate/PersonCard";
import { PeopleSkeleton } from "@/components/estate/Skeletons";
import { joined, label, monthYear } from "@/components/estate/format";
import { useUserProfile } from "@/hooks/useUseProfile";
import { useGetAllUsersQuery } from "@/lib/redux/features/users/usersApiSlice";
import { useAppSelector } from "@/lib/redux/hooks/typedHooks";
import type { Profile } from "@/types";
import { Building2, CalendarDays, DoorOpen, MapPin, SearchX, UsersRound } from "lucide-react";
import PaginationSection from "../shared/PaginationSection";
import ProtectedRoute from "../shared/ProtectedRoutes";
import UsersSearch from "../shared/search/UsersSearch";

const PAGE_SIZE = 9;

function factsFor(tenant: Profile): PersonFact[] {
	const home = tenant.apartment;
	return [
		{
			icon: DoorOpen,
			label: "Apartment",
			value: home ? joined([home.unit_number, `Floor ${home.floor}`], " · ") : "Not added yet",
		},
		{ icon: Building2, label: "Building", value: home?.building || "Not added yet" },
		{
			icon: MapPin,
			label: "From",
			value: joined([tenant.city_of_origin, tenant.country_of_origin]) || "Not shared",
		},
		{ icon: CalendarDays, label: "Joined", value: monthYear(tenant.date_joined) || "Not available" },
	];
}

function TenantCardContent() {
	const searchTerm = useAppSelector((state) => state.user.searchTerm);
	const page = useAppSelector((state) => state.user.page);

	const { data, isLoading, isFetching, isError, refetch } = useGetAllUsersQuery({ searchTerm, page });
	const ready = !isLoading && !isError;
	const { profile: me } = useUserProfile();

	const tenants = data?.profiles.results ?? [];
	const totalCount = data?.profiles.count || 0;
	const totalPages = Math.ceil(totalCount / PAGE_SIZE);

	return (
		<div className="es-page es-page--wide">
			<PageHeader
				eyebrow="Directory"
				title={
					<>
						Your <em>neighbours</em>
					</>
				}
				count={ready ? totalCount : undefined}
				description="The people who share your building. Put a name to the face you pass on the stairs."
			/>

			<div className="es-toolbar">
				<UsersSearch placeholder="Search tenants by name or username" label="Search tenants" />
			</div>

			{isLoading ? (
				<PeopleSkeleton cards={6} />
			) : isError ? (
				<ErrorState what="the tenants" onRetry={refetch} retrying={isFetching} />
			) : tenants.length > 0 ? (
				<div className="es-people es-stagger" aria-busy={isFetching}>
					{tenants.map((tenant, index) => (
						<PersonCard
							key={tenant.id}
							index={index}
							name={tenant.full_name}
							username={tenant.username}
							slug={tenant.slug}
							avatar={tenant.avatar}
							bio={tenant.bio}
							badge={
								tenant.username === me?.username
									? "You"
									: tenant.occupation && tenant.occupation !== "tenant"
										? label(tenant.occupation)
										: undefined
							}
							facts={factsFor(tenant)}
							summaryFacts={factsFor(tenant).slice(0, 2)}
						/>
					))}
				</div>
			) : searchTerm ? (
				<EmptyState icon={SearchX} description="Check the spelling, or try a first name, last name or username.">
					No tenants match &ldquo;{searchTerm}&rdquo;
				</EmptyState>
			) : (
				<EmptyState icon={UsersRound} description="Tenants appear here once they join the building.">
					No tenants yet
				</EmptyState>
			)}

			<PaginationSection totalPages={totalPages} entityType="user" />
		</div>
	);
}

export default function TenantCard() {
	return (
		<ProtectedRoute>
			<TenantCardContent />
		</ProtectedRoute>
	);
}
