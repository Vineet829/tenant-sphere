"use client";

import PersonAvatar from "@/components/estate/PersonAvatar";
import SubmitButton from "@/components/estate/SubmitButton";
import { useGetUserProfileQuery, useUpdateUserProfileMutation } from "@/lib/redux/features/users/usersApiSlice";
import { profileSchema, TProfileSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import axios from "axios";
import { AtSign, Check } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import React, { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import * as z from "zod";
import { FormFieldComponent } from "../FormFieldComponent";
import GenderSelectField from "./GenderSelectField";
import OccupationSelectField from "./OccupationSelectField";

export default function EditProfileForm() {
	const { data } = useGetUserProfileQuery();

	const profile = data?.profile;
	const [avatar, setAvatar] = useState("");
	const [uploading, setUploading] = useState(false);

	const [updateUserProfile, { isLoading }] = useUpdateUserProfileMutation();

	const router = useRouter();

	const {
		register,
		handleSubmit,
		reset,
		setValue,
		control,
		formState: { errors },
	} = useForm<TProfileSchema>();

	useEffect(() => {
		if (profile) {
			reset({ ...profile });
		}
	}, [profile, reset]);

	const uploadFileHandler = async (e: React.ChangeEvent<HTMLInputElement>) => {
		if (!e.target.files) return;

		const file = e.target.files[0];
		const formData = new FormData();
		formData.append("avatar", file);
		setUploading(true);

		try {
			const config = {
				headers: {
					"Content-Type": "multipart/form-data",
				},
			};
			const { data } = await axios.patch("/api/v1/profiles/user/avatar/", formData, config);
			setAvatar(data);
			setUploading(false);
		} catch (error) {
			console.error("Error uploading file:", error);
		} finally {
			setUploading(false);
		}
	};

	const onSubmit = async (values: z.infer<typeof profileSchema>) => {
		try {
			await updateUserProfile(values).unwrap();
			toast.success("Update Successful");
			router.push("/profile");
		} catch (error) {
			const errorMessage = extractErrorMessage(error);
			toast.error(errorMessage || "An error occurred");
		}
	};

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<div className="es-field">
				<div className="es-field-head">
					<label htmlFor="avatar" className="es-label">
						Profile photo
					</label>
				</div>
				<div className="es-upload">
					<PersonAvatar name={profile?.full_name || profile?.username} seed={profile?.username} src={profile?.avatar} size="lg" />
					<div className="es-upload-copy">
						<input accept="image/*" id="avatar" name="avatar" type="file" onChange={uploadFileHandler} />
						<p className="es-upload-status" aria-live="polite">
							{uploading ? (
								<>
									<span className="es-spinner" aria-hidden="true" />
									Uploading…
								</>
							) : avatar ? (
								<>
									<Check aria-hidden="true" />
									Photo received
								</>
							) : (
								"An image file, such as a PNG or JPG."
							)}
						</p>
					</div>
				</div>
			</div>

			<div className="es-form-row">
				<FormFieldComponent
					label="First name"
					name="first_name"
					register={register}
					errors={errors}
					placeholder="First name"
				/>
				<FormFieldComponent
					label="Last name"
					name="last_name"
					register={register}
					errors={errors}
					placeholder="Last name"
				/>
			</div>

			<FormFieldComponent
				label="Username"
				name="username"
				register={register}
				errors={errors}
				placeholder="username"
				startIcon={<AtSign aria-hidden="true" />}
			/>

			<GenderSelectField setValue={setValue} control={control} />
			<OccupationSelectField setValue={setValue} control={control} />

			<div className="es-form-row">
				<FormFieldComponent
					label="City of origin"
					name="city_of_origin"
					register={register}
					errors={errors}
					placeholder="City"
				/>
				<FormFieldComponent
					label="Country of origin"
					name="country_of_origin"
					register={register}
					errors={errors}
					placeholder="Country"
				/>
			</div>

			<FormFieldComponent
				label="Bio"
				name="bio"
				register={register}
				errors={errors}
				placeholder="A few lines about you, for your neighbours."
				isTextArea
			/>

			<div className="es-form-actions">
				<Link href="/profile" className="es-btn es-btn--ghost">
					Cancel
				</Link>
				<SubmitButton isLoading={isLoading} busyLabel="Saving…">
					Save changes
				</SubmitButton>
			</div>
		</form>
	);
}
