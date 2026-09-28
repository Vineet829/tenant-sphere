"use client";

import { CardsSkeleton } from "@/components/estate/Skeletons";
import { joined, label, longDate } from "@/components/estate/format";
import { useGetUserProfileQuery } from "@/lib/redux/features/users/usersApiSlice";
import {
	Briefcase,
	Building2,
	CalendarDays,
	Contact,
	DoorOpen,
	Home,
	Layers,
	MapPin,
	Quote,
	UserRound,
	UserRoundCheck,
} from "lucide-react";
import Link from "next/link";
import ProtectedRoute from "../shared/ProtectedRoutes";
import { ProfileItem } from "./ProfileItem";

function AboutContent() {
	const { data, isLoading, isError } = useGetUserProfileQuery();
	const profile = data?.profile;

	if (isLoading) return <CardsSkeleton cards={2} />;
	// The header above already explains the failure and offers a retry.
	if (isError || !profile) return null;

	const home = profile?.apartment;

	return (
		<div className="es-about">
			<section className="es-card es-panel es-about-bio" aria-labelledby="about-bio">
				<h2 className="es-section-title" id="about-bio">
					<Quote aria-hidden="true" strokeWidth={1.75} />
					In your own words
				</h2>
				{profile?.bio ? (
					<blockquote className="es-quote">{profile.bio}</blockquote>
				) : (
					<p className="es-quote" data-empty="true">
						You have not added a bio yet.{" "}
						<Link href="/profile/edit" className="es-field-link">
							Add a few lines
						</Link>{" "}
						so your neighbours know who you are.
					</p>
				)}
			</section>

			<section className="es-card es-panel" aria-labelledby="about-personal">
				<h2 className="es-section-title" id="about-personal">
					<UserRound aria-hidden="true" strokeWidth={1.75} />
					Personal
				</h2>
				<dl className="es-deflist">
					<ProfileItem icon={<Contact />} label="Name" value={profile?.full_name} />
					<ProfileItem icon={<UserRoundCheck />} label="Gender" value={label(profile?.gender)} />
					<ProfileItem icon={<Briefcase />} label="Occupation" value={label(profile?.occupation)} />
					<ProfileItem
						icon={<MapPin />}
						label="From"
						value={joined([profile?.city_of_origin, profile?.country_of_origin])}
					/>
					<ProfileItem icon={<CalendarDays />} label="Joined" value={longDate(profile?.date_joined)} />
				</dl>
			</section>

			<section className="es-card es-panel" aria-labelledby="about-home">
				<h2 className="es-section-title" id="about-home">
					<Home aria-hidden="true" strokeWidth={1.75} />
					Home
				</h2>
				{home ? (
					<dl className="es-deflist">
						<ProfileItem icon={<DoorOpen />} label="Apartment" value={home.unit_number} />
						<ProfileItem icon={<Building2 />} label="Building" value={home.building} />
						<ProfileItem icon={<Layers />} label="Floor" value={`${home.floor}`} />
					</dl>
				) : (
					<p className="es-panel-empty">
						No apartment on your profile yet.{" "}
						<Link href="/apartment" className="es-field-link">
							Add your apartment
						</Link>{" "}
						to report issues for it.
					</p>
				)}
			</section>
		</div>
	);
}

export default function About() {
	return (
		<ProtectedRoute>
			<AboutContent />
		</ProtectedRoute>
	);
}
