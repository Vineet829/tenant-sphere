"use client";

import SubmitButton from "@/components/estate/SubmitButton";
import { useReportTenantMutation } from "@/lib/redux/features/reports/reportApiSlice";
import { reportCreateSchema, TReportCreateSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { AtSign } from "lucide-react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { FormFieldComponent } from "../FormFieldComponent";

export default function CreateReportForm() {
	const router = useRouter();
	const [reportTenant, { isLoading }] = useReportTenantMutation();

	const {
		register,
		handleSubmit,
		reset,
		formState: { errors },
	} = useForm<TReportCreateSchema>({
		resolver: zodResolver(reportCreateSchema),
		mode: "all",
	});

	const onSubmit = async (values: TReportCreateSchema) => {
		try {
			await reportTenant(values).unwrap();
			toast.success("Your report has been received. The management shall take action immediately");
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
				placeholder="Late-night noise"
				hint="A short line that sums up the concern."
			/>

			<FormFieldComponent
				label="Tenant's username"
				name="reported_user_username"
				register={register}
				errors={errors}
				placeholder="username"
				startIcon={<AtSign aria-hidden="true" />}
				link={{ linkText: "Find it in Tenants", linkUrl: "/tenants" }}
			/>

			<FormFieldComponent
				label="Description"
				name="description"
				register={register}
				errors={errors}
				placeholder="What happened, where and when? Stick to what you saw or heard."
				isTextArea
			/>

			<div className="es-form-actions">
				<p className="es-form-note">Your name is never shared with the tenant.</p>
				<SubmitButton isLoading={isLoading} busyLabel="Sending…">
					Send report
				</SubmitButton>
			</div>
		</form>
	);
}
