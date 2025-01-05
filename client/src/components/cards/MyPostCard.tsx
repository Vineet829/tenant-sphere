"use client";

import { useGetMyPostsQuery } from "@/lib/redux/features/posts/postApiSlice";
import Spinner from "../shared/Spinner";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "../ui/card";
import { formatDate, getRepliesText, getViewText } from "@/utils";
import { formatDistanceToNow, parseISO } from "date-fns";
import Link from "next/link";
import { Button } from "../ui/button";
import { EyeIcon, MessageSquareQuoteIcon } from "lucide-react";

export default function MyPostCard() {
	const { data, isLoading } = useGetMyPostsQuery();

	if (isLoading) {
		return (
			<div className="flex-center pt-32">
				<Spinner size="xl" />
			</div>
		);
	}

	const posts = data?.my_posts.results || [];

	return (
		<div>
			<h1 className="flex-center font-robotoSlab dark:text-pumpkin text-3xl sm:text-4xl">
				My Posts - ({posts.length})
			</h1>
			<div className="mt-6 grid grid-cols-1 sm:grid-cols-[400px_400px] gap-6 w-full">
				{posts.length > 0 ? (
					posts.map((post) => (
						<Card key={post.id} className="dark:border-gray rounded-lg border">
							<CardHeader className="dark:text-platinum w-full pb-4">
								<CardTitle className="font-robotSlab text-center text-xl sm:text-2xl mb-4">
									{post.title.length > 25
										? `${post.title.substring(0, 25)}....`
										: post.title}
								</CardTitle>
								<CardDescription>
									<div className="flex flex-col sm:flex-row justify-between">
										<div className="mb-2 sm:mb-0">
											<span>Posted on</span>
											<span className="dark:text-pumpkin ml-1">
												{formatDate(post.created_at).toString()}
											</span>
										</div>
										<div>
											<span>Last Updated</span>
											<span className="dark:text-pumpkin ml-1">
												{formatDistanceToNow(parseISO(post.updated_at), {
													addSuffix: true,
												})}
											</span>
										</div>
									</div>
								</CardDescription>
							</CardHeader>
							<CardContent className="border-t-deepBlueGrey dark:border-gray border-y py-4 text-sm">
								<p className="dark:text-platinum">
									{post.body.length > 65
										? `${post.body.substring(0, 65)}....`
										: post.body}
								</p>
							</CardContent>

							<div className="flex flex-col sm:flex-row items-center justify-between p-2">
								<Link href={`/post/${post.slug}`} className="w-full sm:w-auto">
									<Button size="sm" className="lime-gradient text-babyPowder w-full">
										View Post
									</Button>
								</Link>
								<div className="flex-row-center dark:text-platinum mt-2 sm:mt-0">
									<EyeIcon className="post-icon text-electricIndigo mr-1" />
									{getViewText(post.view_count)}
								</div>
								<div className="flex-row-center dark:text-platinum mt-2 sm:mt-0">
									<MessageSquareQuoteIcon className="post-icon text-electricIndigo mr-1" />
									<span>{getRepliesText(post.replies_count ?? 0)}</span>
								</div>
							</div>
						</Card>
					))
				) : (
					<p className="h2-semibold dark:text-lime-500">No Posts Found!</p>
				)}
			</div>
		</div>
	);
}
