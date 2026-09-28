"use client";

import SubmitButton from "@/components/estate/SubmitButton";
import { useGetSinglePostQuery, useUpdatePostMutation } from "@/lib/redux/features/posts/postApiSlice";
import { postUpdateSchema, TPostUpdateSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import * as z from "zod";
import { FormFieldComponent } from "../FormFieldComponent";

interface UpdateParamsProps {
	params: {
		slug: string;
	};
}

export default function UpdatePostForm({ params }: UpdateParamsProps) {
	const postSlug = params.slug;
	const { data } = useGetSinglePostQuery(postSlug);
	const post = data?.post;
	const [updatePost, { isLoading }] = useUpdatePostMutation();
	const router = useRouter();

	const {
		register,
		handleSubmit,
		reset,
		formState: { errors },
	} = useForm<TPostUpdateSchema>({
		resolver: zodResolver(postUpdateSchema),
	});

	useEffect(() => {
		if (post) {
			reset({
				...post,
			});
		}
	}, [post, reset]);

	const onSubmit = async (formValues: z.infer<typeof postUpdateSchema>) => {
		try {
			await updatePost({ postSlug, ...formValues }).unwrap();
			toast.success("Post updated successfully");
			router.push(`/post/${postSlug}`);
		} catch (error) {
			toast.error(extractErrorMessage(error) || "Failed to update post");
		}
	};

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<FormFieldComponent label="Title" name="title" register={register} errors={errors} />

			<FormFieldComponent label="Body" name="body" register={register} errors={errors} isTextArea />

			<div className="es-form-actions">
				<Link href={`/post/${postSlug}`} className="es-btn es-btn--ghost">
					Cancel
				</Link>
				<SubmitButton isLoading={isLoading} busyLabel="Saving…">
					Save changes
				</SubmitButton>
			</div>
		</form>
	);
}
