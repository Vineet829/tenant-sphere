import {
	BookmarkedPostsResponse,
	BookmarkResponse,
	MyPostsResponse,
	PopularTagResponse,
	PostData,
	PostDeleteResponse,
	PostQueryParams,
	PostResponse,
	PostsByTagResponse,
	PostsResponse,
	ReplyPostData,
	ReplyResponse,
	TopPostsResponse,
	UpdatePostData,
	UpvoteDownvoteResponse,
	PostVoteResponse,
	SetPostVoteData,
} from "@/types";
import { baseApiSlice } from "../api/baseApiSlice";
import { usersApiSlice } from "../users/usersApiSlice";


const postQueriesApi = baseApiSlice.injectEndpoints({
	endpoints: (builder) => ({
		createPost: builder.mutation<PostResponse, PostData>({
			query: (postData) => ({
				url: "/posts/create/",
				method: "POST",
				body: postData,
			}),
			invalidatesTags: ["Post"],
		}),
		getAllPosts: builder.query<PostsResponse, PostQueryParams>({
			query: (params = {}) => {
				const queryString = new URLSearchParams();

				if (params.page) {
					queryString.append("page", params.page.toString());
				}
				return `/posts/?${queryString.toString()}`;
			},
			providesTags: ["Post"],
		}),
		getMyPosts: builder.query<MyPostsResponse, void>({
			query: () => "/posts/my-posts/",
			providesTags: ["Post"],
		}),
		getSinglePost: builder.query<PostResponse, string>({
			query: (postSlug) => `/posts/${postSlug}/`,
			providesTags: ["Post"],
		}),
		updatePost: builder.mutation<PostResponse, UpdatePostData>({
			query: ({ postSlug, ...postData }) => ({
				url: `/posts/${postSlug}/update/`,
				method: "PATCH",
				body: postData,
			}),
			invalidatesTags: ["Post"],
		}),
		upvotePost: builder.mutation<UpvoteDownvoteResponse, string>({
			query: (postId) => ({
				url: `/posts/${postId}/upvote/`,
				method: "PATCH",
			}),
			invalidatesTags: ["Post"],
		}),
		deletePost: builder.mutation<PostDeleteResponse, string>({
            query: (postId) => ({
                url: `/posts/${postId}/delete/`,
                method: "DELETE",
            }),
			invalidatesTags: [{ type: 'Post', id: 'LIST' }],
        
		}),
		downvotePost: builder.mutation<UpvoteDownvoteResponse, string>({
			query: (postId) => ({
				url: `/posts/${postId}/downvote/`,
				method: "PATCH",
			}),
			invalidatesTags: ["Post"],
		}),
		bookmarkPost: builder.mutation<BookmarkResponse, string>({
			query: (postSlug) => ({
				url: `/posts/${postSlug}/bookmark/`,
				method: "PATCH",
			}),
			invalidatesTags: ["Post"],
		}),
		unBookmarkPost: builder.mutation<BookmarkResponse, string>({
			query: (postSlug) => ({
				url: `/posts/${postSlug}/unbookmark/`,
				method: "PATCH",
			}),
			invalidatesTags: ["Post"],
		}),
		getAllMyBookmarks: builder.query<BookmarkedPostsResponse, void>({
			query: () => "/posts/bookmarked/posts/",
			providesTags: ["Post"],
		}),
		getTopPosts: builder.query<TopPostsResponse, void>({
			query: () => "/posts/top-posts/",
			providesTags: ["Post"],
		}),
		getPopularTags: builder.query<PopularTagResponse, void>({
			query: () => "/posts/popular-tags/",
			providesTags: ["Post"],
		}),
		getAllReplies: builder.query<PostResponse, string>({
			query: (postId) => `/posts/${postId}/replies/`,
			providesTags: ["Post"],
		}),
		getPostsByTag: builder.query<PostsByTagResponse, string>({
			query: (tagSlug) => `/posts/tags/${tagSlug}/`,
			providesTags: ["Post"],
		}),
		replyToPost: builder.mutation<ReplyResponse, ReplyPostData>({
			query: ({ postId, ...replyData }) => ({
				url: `/posts/${postId}/reply/`,
				method: "POST",
				body: replyData,
			}),
			invalidatesTags: ["Post"],
		}),
	}),
});

type VotablePost = { id: string; upvotes: number; downvotes: number; is_upvoted?: boolean; is_downvoted?: boolean };

function applyConfirmedVote(posts: VotablePost[], result: PostVoteResponse) {
	for (const post of posts) {
		if (post.id !== result.id) continue;
		post.upvotes = result.upvotes;
		post.downvotes = result.downvotes;
		if ("is_upvoted" in post) post.is_upvoted = result.is_upvoted;
		if ("is_downvoted" in post) post.is_downvoted = result.is_downvoted;
	}
}

export const postApiSlice = postQueriesApi.injectEndpoints({
	endpoints: (builder) => ({
		setPostVote: builder.mutation<PostVoteResponse, SetPostVoteData>({
			query: ({ postId, vote }) => ({ url: `/posts/${postId}/vote/`, method: "PUT", body: { vote } }),
			invalidatesTags: (result) => result ? ["Post"] : [],
			async onQueryStarted(_, { dispatch, getState, queryFulfilled }) {
				try {
					const { data } = await queryFulfilled;
					const state = getState() as Parameters<typeof postQueriesApi.util.selectCachedArgsForQuery>[0];
					for (const args of postQueriesApi.util.selectCachedArgsForQuery(state, "getSinglePost")) {
						dispatch(postQueriesApi.util.updateQueryData("getSinglePost", args, (draft) => applyConfirmedVote([draft.post], data)));
					}
					for (const args of postQueriesApi.util.selectCachedArgsForQuery(state, "getAllPosts")) {
						dispatch(postQueriesApi.util.updateQueryData("getAllPosts", args, (draft) => applyConfirmedVote(draft.posts.results, data)));
					}
					for (const args of postQueriesApi.util.selectCachedArgsForQuery(state, "getMyPosts")) {
						dispatch(postQueriesApi.util.updateQueryData("getMyPosts", args, (draft) => applyConfirmedVote(draft.my_posts.results, data)));
					}
					for (const args of postQueriesApi.util.selectCachedArgsForQuery(state, "getAllMyBookmarks")) {
						dispatch(postQueriesApi.util.updateQueryData("getAllMyBookmarks", args, (draft) => applyConfirmedVote(draft.bookmarked_posts.results, data)));
					}
					for (const args of postQueriesApi.util.selectCachedArgsForQuery(state, "getPostsByTag")) {
						dispatch(postQueriesApi.util.updateQueryData("getPostsByTag", args, (draft) => applyConfirmedVote(draft.posts_by_tag.results, data)));
					}
					for (const args of postQueriesApi.util.selectCachedArgsForQuery(state, "getTopPosts")) {
						dispatch(postQueriesApi.util.updateQueryData("getTopPosts", args, (draft) => applyConfirmedVote(draft.top_posts.results, data)));
					}
					const profileState = getState() as Parameters<typeof usersApiSlice.util.selectCachedArgsForQuery>[0];
					for (const args of usersApiSlice.util.selectCachedArgsForQuery(profileState, "getProfilePosts")) {
						dispatch(usersApiSlice.util.updateQueryData("getProfilePosts", args, (draft) => applyConfirmedVote(draft.profile_posts.results, data)));
					}
				} catch {
					// Preserve the confirmed state on failure; the caller offers a safe retry.
				}
			},
		}),
	}),
});

export const {
	useCreatePostMutation,
	useGetAllPostsQuery,
	useGetAllMyBookmarksQuery,
	useGetPostsByTagQuery,
	useGetMyPostsQuery,
	useGetTopPostsQuery,
	useGetPopularTagsQuery,
	useGetSinglePostQuery,
	useUpvotePostMutation,
	useDownvotePostMutation,
	useSetPostVoteMutation,
	useUpdatePostMutation,
	useBookmarkPostMutation,
	useUnBookmarkPostMutation,
	useReplyToPostMutation,
	useGetAllRepliesQuery,
	useDeletePostMutation
} = postApiSlice;
