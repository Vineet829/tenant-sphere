"use client";

import MyPostCard from "../cards/MyPostCard";
import TabHeading from "./TabHeading";

export default function Posts() {
	return (
		<>
			<TabHeading title="Your posts" note="Everything you have shared with the building." />
			<MyPostCard />
		</>
	);
}
