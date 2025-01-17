"use client";

import { TabsContent } from "@/components/ui/tabs";
import MyPostCard from "../cards/MyPostCard";

export default function Posts() {
	return (
		
		<TabsContent value="posts">
			<div className="grid grid-cols-1 gap-4 p-4 md:grid-cols-2">
				<h3 className="h3-semibold dark:text-platinum"><MyPostCard/></h3>
			</div>
		</TabsContent>
	);
}
