"use client";

import ErrorState from "@/components/estate/ErrorState";
import PersonAvatar from "@/components/estate/PersonAvatar";
import RatingStars from "@/components/estate/RatingStars";
import { joined, label, monthYear } from "@/components/estate/format";
import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import { useGetUserProfileQuery } from "@/lib/redux/features/users/usersApiSlice";
import { BadgeCheck, Briefcase, Building2, CalendarDays, DoorOpen, MapPin, Pencil, Plus, Star } from "lucide-react";
import Link from "next/link";
import type { CSSProperties } from "react";

function HeaderSkeleton() {
	return (
		<section className="es-card es-profile-hero" role="status">
			<span className="es-sr">Loading your profile…</span>
			<div className="es-profile-cover" />
			<div className="es-profile-body">
				<span className="es-avatar es-avatar--xl es-skeleton es-skeleton--circle" />
				<div className="es-profile-id">
					<span className="es-skeleton h-[40px] w-[min(320px,70%)]" />
					<span className="es-skeleton mt-[14px] h-[14px] w-[min(420px,90%)]" />
				</div>
			</div>
		</section>
	);
}

function HeaderContent() {
	const { data, isLoading, isFetching, isError, refetch } = useGetUserProfileQuery();
	const profile = data?.profile;

	if (isError) return <ErrorState what="your profile" onRetry={refetch} retrying={isFetching} />;
	if (isLoading || !profile) return <HeaderSkeleton />;

	const home = profile.apartment;
	const reputation = Math.max(0, Math.min(100, Number(profile.reputation) || 0));
	const rating = Number(profile.average_rating) || 0;

	return (
		<section className="es-card es-profile-hero" aria-label="Your profile">
			<div className="es-profile-cover" aria-hidden="true" />

			<div className="es-profile-body">
				<PersonAvatar name={profile.full_name || profile.username} seed={profile.username} src={profile.avatar} size="xl" />

				<div className="es-profile-id">
					<h1 className="es-profile-name">{profile.full_name}</h1>
					<ul className="es-profile-meta">
						<li className="es-profile-handle">@{profile.username}</li>
						{profile.occupation && (
							<li>
								<Briefcase aria-hidden="true" strokeWidth={1.75} />
								{label(profile.occupation)}
							</li>
						)}
						{(profile.city_of_origin || profile.country_of_origin) && (
							<li>
								<MapPin aria-hidden="true" strokeWidth={1.75} />
								{joined([profile.city_of_origin, profile.country_of_origin])}
							</li>
						)}
						{profile.date_joined && (
							<li>
								<CalendarDays aria-hidden="true" strokeWidth={1.75} />
								Joined {monthYear(profile.date_joined)}
							</li>
						)}
					</ul>
				</div>

				<div className="es-profile-actions">
					<Link href="/profile/edit" className="es-btn es-btn--outline">
						<Pencil aria-hidden="true" />
						Edit profile
					</Link>
					<Link href="/apartment" className={`es-btn ${home ? "es-btn--ghost" : "es-btn--primary"}`}>
						<Plus aria-hidden="true" />
						{home ? "Add apartment" : "Add your apartment"}
					</Link>
				</div>
			</div>

			<dl className="es-stats">
				<div className="es-stat-tile">
					<dt>
						<BadgeCheck aria-hidden="true" strokeWidth={1.75} />
						Reputation
					</dt>
					<dd>
						<span className="es-stat-value">{reputation}</span>
						<span>out of 100</span>
						<span className="es-meter" aria-hidden="true">
							<span className="w-[var(--meter-fill)]" style={{ "--meter-fill": `${reputation}%` } as CSSProperties} />
						</span>
					</dd>
				</div>

				<div className="es-stat-tile">
					<dt>
						<Star aria-hidden="true" strokeWidth={1.75} />
						Average rating
					</dt>
					<dd>
						{rating > 0 ? (
							<>
								<span className="es-stat-value">{rating.toFixed(1)}</span>
								<RatingStars value={rating} showValue={false} />
							</>
						) : (
							<span className="es-stat-value es-stat-value--text">Not rated yet</span>
						)}
					</dd>
				</div>

				<div className="es-stat-tile">
					<dt>
						<DoorOpen aria-hidden="true" strokeWidth={1.75} />
						Apartment
					</dt>
					<dd>
						{home ? (
							<>
								<span className="es-stat-value">{home.unit_number}</span>
								<span>Floor {home.floor}</span>
							</>
						) : (
							<span className="es-stat-value es-stat-value--text">Not added yet</span>
						)}
					</dd>
				</div>

				<div className="es-stat-tile">
					<dt>
						<Building2 aria-hidden="true" strokeWidth={1.75} />
						Building
					</dt>
					<dd>
						<span className="es-stat-value es-stat-value--text">{home?.building || "Not added yet"}</span>
					</dd>
				</div>
			</dl>
		</section>
	);
}

export default function Header() {
	return (
		<ProtectedRoute>
			<HeaderContent />
		</ProtectedRoute>
	);
}
