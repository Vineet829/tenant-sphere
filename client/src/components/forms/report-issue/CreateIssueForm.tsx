"use client";

import SegmentedField, { type SegmentOption } from "@/components/estate/SegmentedField";
import SubmitButton from "@/components/estate/SubmitButton";
import { useGetMyApartmentQuery } from "@/lib/redux/features/apartment/apartmentApiSlice";
import { useReportIssueMutation } from "@/lib/redux/features/issues/issueApiSlice";
import { issueCreateSchema, TIssueCreateSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { FormFieldComponent } from "../FormFieldComponent";

export const ISSUE_STATUS_OPTIONS: SegmentOption[] = [
	{ value: "reported", label: "Reported", tone: "info" },
	{ value: "in_progress", label: "In progress", tone: "warn" },
	{ value: "resolved", label: "Resolved", tone: "ok" },
];

const PRIORITY_OPTIONS: SegmentOption[] = [
	{ value: "low", label: "Low" },
	{ value: "medium", label: "Medium", tone: "warn" },
	{ value: "high", label: "High", tone: "danger" },
];

export default function CreateIssueForm() {
	const { data } = useGetMyApartmentQuery();
	const apartment = data?.apartment;

	const [reportIssue, { isLoading }] = useReportIssueMutation();
	const router = useRouter();

	const {
		register,
		handleSubmit,
		control,
		reset,
		formState: { errors },
	} = useForm<TIssueCreateSchema>({
		resolver: zodResolver(issueCreateSchema),
		mode: "all",
	});

	const onSubmit = async (formValues: TIssueCreateSchema) => {
		if (!apartment?.id) {
			toast.error("Create your apartment first in your profile, before creating an issue");
			return;
		}

		const valuesWithApartmentId = {
			...formValues,
			apartmentId: apartment.id,
		};

		try {
			await reportIssue(valuesWithApartmentId).unwrap();
			toast.success("Your Issue has been reported. A confirmation email has been sent to you.");
			reset();
			router.push("/profile");
		} catch (error) {
			const errorMessage = extractErrorMessage(error);
			toast.error(errorMessage || "An error occurred");
		}
	};

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<FormFieldComponent
				label="Title"
				name="title"
				register={register}
				errors={errors}
				placeholder="Kitchen tap keeps dripping"
				hint="A short line that names the problem."
			/>
			<FormFieldComponent
				label="Description"
				name="description"
				register={register}
				errors={errors}
				placeholder="What is happening, where, and since when? Mention the best time for a visit."
				isTextArea
			/>

			<Controller
				name="status"
				control={control}
				render={({ field: { onChange, onBlur, value } }) => (
					<SegmentedField
						legend="Status"
						name="status"
						options={ISSUE_STATUS_OPTIONS}
						value={value}
						onChange={onChange}
						onBlur={onBlur}
						hint="Choose Reported when you are raising a new issue."
						error={errors.status?.message}
					/>
				)}
			/>

			<Controller
				name="priority"
				control={control}
				render={({ field: { onChange, onBlur, value } }) => (
					<SegmentedField
						legend="Priority"
						name="priority"
						options={PRIORITY_OPTIONS}
						value={value}
						onChange={onChange}
						onBlur={onBlur}
						hint="How soon does this need attention?"
						error={errors.priority?.message}
					/>
				)}
			/>

			<div className="es-form-actions">
				<p className="es-form-note">You will find it under My issues on your profile.</p>
				<SubmitButton isLoading={isLoading} busyLabel="Sending…">
					Report issue
				</SubmitButton>
			</div>
		</form>
	);
}
