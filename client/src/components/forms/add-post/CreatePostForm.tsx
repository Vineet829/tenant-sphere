"use client";

import SubmitButton from "@/components/estate/SubmitButton";
import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import { useCreatePostMutation } from "@/lib/redux/features/posts/postApiSlice";
import { postSchema, TPostSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { useRouter } from "next/navigation";
import React from "react";
import { Controller, useForm } from "react-hook-form";
import { toast } from "react-toastify";
import FieldError from "@/components/estate/FieldError";
import { FormFieldComponent } from "../FormFieldComponent";

function CreatePostFormContent() {
	const [createPost, { isLoading }] = useCreatePostMutation();
	const router = useRouter();

	const {
		register,
		handleSubmit,
		control,
		setValue,
		formState: { errors },
	} = useForm<TPostSchema>({
		resolver: zodResolver(postSchema),
		mode: "all",
		defaultValues: {
			title: "",
			tags: [],
			body: "",
		},
	});

	const handleInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, field: any) => {
		if (e.key === "Enter" && field.name === "tags") {
			e.preventDefault();

			const target = e.target as HTMLInputElement;
			const tagInput = target.value.trim();

			if (tagInput !== "" && !field.value.includes(tagInput)) {
				setValue("tags", [...field.value, tagInput], { shouldValidate: true });

				target.value = "";
			}
		}
	};

	const handleRemoveTag = (tagToRemove: string, field: any) => {
		const updatedTags = field.value.filter((tag: string) => tag !== tagToRemove);
		setValue("tags", updatedTags, { shouldValidate: true });
	};

	const onSubmit = async (values: TPostSchema) => {
		try {
			await createPost(values).unwrap();
			toast.success("Your Post has been added");
			router.push("/welcome");
		} catch (error) {
			const errorMessage = extractErrorMessage(error);
			toast.error(errorMessage || "An error occurred");
		}
	};

	const tagsError = errors.tags?.message;

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<FormFieldComponent
				label="Title"
				name="title"
				register={register}
				errors={errors}
				placeholder="Anyone know a good plumber nearby?"
			/>
			<FormFieldComponent
				label="Body"
				name="body"
				register={register}
				errors={errors}
				placeholder="Ask a question or share what you have in mind…"
				isTextArea
			/>

			<div className="es-field" data-invalid={Boolean(tagsError)}>
				<div className="es-field-head">
					<label htmlFor="tags" className="es-label">
						Tags
					</label>
				</div>
				<Controller
					name="tags"
					control={control}
					render={({ field }) => (
						<div className="es-tagbox">
							{field.value.map((tag: string) => (
								<span key={tag} className="es-chip es-chip--tag es-chip--accent">
									{tag}
									<button
										type="button"
										className="es-chip-remove"
										onClick={() => handleRemoveTag(tag, field)}
										aria-label={`Remove the tag ${tag}`}
									>
										<X aria-hidden="true" />
									</button>
								</span>
							))}
							<input
								type="text"
								id="tags"
								placeholder={field.value.length > 0 ? "Add another…" : "Type a tag and press Enter"}
								onKeyDown={(e) => handleInputKeyDown(e, field)}
								autoComplete="off"
								aria-describedby="tags-hint"
							/>
						</div>
					)}
				/>
				{tagsError ? (
					<FieldError>{tagsError}</FieldError>
				) : (
					<p className="es-hint" id="tags-hint">
						Press Enter after each tag. Tags help neighbours find your post.
					</p>
				)}
			</div>

			<div className="es-form-actions">
				<p className="es-form-note">Everyone in the building can read and reply.</p>
				<SubmitButton isLoading={isLoading} busyLabel="Publishing…">
					Publish post
				</SubmitButton>
			</div>
		</form>
	);
}

export default function CreatePostForm() {
	return (
		<ProtectedRoute>
			<CreatePostFormContent />
		</ProtectedRoute>
	);
}
