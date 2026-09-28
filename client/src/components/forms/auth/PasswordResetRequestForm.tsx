"use client";
import SubmitButton from "@/components/estate/SubmitButton";
import { FormFieldComponent } from "@/components/forms/FormFieldComponent";
import { useResetPasswordRequestMutation } from "@/lib/redux/features/auth/authApiSlice";
import { passwordResetRequestSchema, TPasswordResetRequestSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { Mail } from "lucide-react";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import * as z from "zod";

export default function PasswordResetRequestForm() {
	const [resetPasswordRequest, { isLoading }] = useResetPasswordRequestMutation();

	const {
		register,
		handleSubmit,
		reset,
		formState: { errors },
	} = useForm<TPasswordResetRequestSchema>({
		resolver: zodResolver(passwordResetRequestSchema),
		mode: "all",
		defaultValues: {
			email: "",
		},
	});

	const onSubmit = async (values: z.infer<typeof passwordResetRequestSchema>) => {
		try {
			await resetPasswordRequest(values).unwrap();
			toast.success("Request sent, check your email for the reset link");
			reset();
		} catch (e) {
			const errorMessage = extractErrorMessage(e);
			toast.error(errorMessage || "An error occurred");
		}
	};
	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<FormFieldComponent
				label="Email address"
				name="email"
				register={register}
				errors={errors}
				type="email"
				placeholder="you@example.com"
				startIcon={<Mail aria-hidden="true" />}
				hint="Use the address you registered with."
			/>
			<div className="es-form-actions">
				<p className="es-form-note">The link arrives by email.</p>
				<SubmitButton isLoading={isLoading} busyLabel="Sending…">
					Send reset link
				</SubmitButton>
			</div>
		</form>
	);
}
