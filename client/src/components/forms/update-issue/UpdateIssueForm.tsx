"use client";

import SegmentedField from "@/components/estate/SegmentedField";
import SubmitButton from "@/components/estate/SubmitButton";
import { useUpdateIssueMutation } from "@/lib/redux/features/issues/issueApiSlice";
import { TIssueUpdateSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { ISSUE_STATUS_OPTIONS } from "../report-issue/CreateIssueForm";

interface UpdateParamsProps {
	params: {
		id: string;
	};
}

export default function UpdateIssueForm({ params }: UpdateParamsProps) {
	const issueId = params.id;
	const [updateIssue, { isLoading }] = useUpdateIssueMutation();
	const router = useRouter();

	const {
		handleSubmit,
		control,
		reset,
		formState: { errors },
	} = useForm<TIssueUpdateSchema>();

	const onSubmit = async (formValues: TIssueUpdateSchema) => {
		if (issueId) {
			const valuesWithIssueId = {
				...formValues,
				issueId,
			};

			try {
				await updateIssue(valuesWithIssueId).unwrap();
				toast.success(
					"The Issue assigned to you has been updated. A confirmation email has been sent to the tenant",
				);
				reset();
				router.push("/profile");
			} catch (error) {
				const errorMessage = extractErrorMessage(error);
				toast.error(errorMessage || "An error occurred");
			}
		}
	};
	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<Controller
				name="status"
				control={control}
				render={({ field: { onChange, onBlur, value, ref } }) => (
					<SegmentedField
						legend="New status"
						name="status"
						options={ISSUE_STATUS_OPTIONS}
						value={value}
						onChange={onChange}
						onBlur={onBlur}
						inputRef={ref}
						hint="Pick where the work stands right now."
						error={errors.status?.message}
					/>
				)}
			/>

			<div className="es-form-actions">
				<p className="es-form-note">The tenant is told by email.</p>
				<SubmitButton isLoading={isLoading} busyLabel="Updating…">
					Update status
				</SubmitButton>
			</div>
		</form>
	);
}
