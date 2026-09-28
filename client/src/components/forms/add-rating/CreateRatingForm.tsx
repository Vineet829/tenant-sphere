"use client";

import EmptyState from "@/components/estate/EmptyState";
import StarRatingInput from "@/components/estate/StarRatingInput";
import SubmitButton from "@/components/estate/SubmitButton";
import { useAddRatingMutation } from "@/lib/redux/features/rating/ratingApiSlice";
import { ratingCreateSchema, TRatingCreateSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { AtSign, Wrench } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { toast } from "react-toastify";
import FieldError from "@/components/estate/FieldError";
import { FormFieldComponent } from "../FormFieldComponent";

export default function CreateRatingForm() {
	const router = useRouter();
	const [addRating, { isLoading }] = useAddRatingMutation();

	const {
		register,
		handleSubmit,
		setValue,
		control,
		formState: { errors },
	} = useForm<TRatingCreateSchema>({
		resolver: zodResolver(ratingCreateSchema),
		mode: "all",
	});

	const [username, setUsername] = useState("");
	const [checked, setChecked] = useState(false);

	useEffect(() => {
		const queryParams = new URLSearchParams(window.location.search);
		const ratedUserUsername = queryParams.get("username");
		if (ratedUserUsername) {
			setValue("rated_user_username", ratedUserUsername);
			setUsername(ratedUserUsername);
		}
		setChecked(true);
	}, [setValue]);

	const onSubmit = async (data: TRatingCreateSchema) => {
		try {
			await addRating(data).unwrap();
			toast.success("Your Rating has been added!");
			router.push("/technicians");
		} catch (error) {
			const errorMessage = extractErrorMessage(error);
			toast.error(errorMessage || "An error occurred");
		}
	};

	const ratingError = errors.rating?.message;

	// A rating always belongs to someone. Without a technician there is nothing to submit.
	if (checked && !username) {
		return (
			<EmptyState
				icon={Wrench}
				description="Choose who did the work from the technicians directory, then rate them from their card."
				action={
					<Link href="/technicians" className="es-btn es-btn--primary">
						Choose a technician
					</Link>
				}
			>
				Who are you rating?
			</EmptyState>
		);
	}

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<FormFieldComponent
				label="Technician"
				name="rated_user_username"
				register={register}
				errors={errors}
				startIcon={<AtSign aria-hidden="true" />}
				disabled
				hint="Filled in from the technician you chose."
			/>

			<div className="es-field" data-invalid={Boolean(ratingError)}>
				<div className="es-field-head">
					<span className="es-label" id="rating-label">
						Rating
					</span>
				</div>
				<Controller
					name="rating"
					control={control}
					render={({ field }) => (
						<div role="radiogroup" aria-labelledby="rating-label">
							<StarRatingInput name="rating" value={field.value} onChange={field.onChange} onBlur={field.onBlur} />
						</div>
					)}
				/>
				{ratingError && <FieldError>{ratingError}</FieldError>}
			</div>

			<FormFieldComponent
				label="Comment"
				name="comment"
				errors={errors}
				register={register}
				placeholder="What went well, and what could have been better?"
				isTextArea
				hint="A few honest lines help the next neighbour choose."
			/>

			<div className="es-form-actions">
				<p className="es-form-note">Ratings count towards the technician&apos;s average.</p>
				<SubmitButton isLoading={isLoading} busyLabel="Saving…">
					Add rating
				</SubmitButton>
			</div>
		</form>
	);
}
