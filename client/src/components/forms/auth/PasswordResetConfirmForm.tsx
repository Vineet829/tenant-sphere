"use client";
import SubmitButton from "@/components/estate/SubmitButton";
import { FormFieldComponent } from "@/components/forms/FormFieldComponent";
import { useResetPasswordConfirmMutation } from "@/lib/redux/features/auth/authApiSlice";
import { passwordResetConfirmSchema, TPasswordResetConfirmSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { useParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import * as z from "zod";

export default function PasswordResetConfirmForm() {
	const router = useRouter();
	const { uid, token } = useParams();

	const [resetPasswordConfirm, { isLoading }] = useResetPasswordConfirmMutation();

	const {
		register,
		handleSubmit,
		reset,
		formState: { errors },
	} = useForm<TPasswordResetConfirmSchema>({
		mode: "all",
		defaultValues: {
			uid: uid as string,
			token: token as string,
			new_password: "",
			re_new_password: "",
		},
	});

	const onSubmit = async (values: z.infer<typeof passwordResetConfirmSchema>) => {
		try {
			await resetPasswordConfirm({
				...values,
				uid: uid as string,
				token: token as string,
			}).unwrap();
			router.push("/login");
			toast.success("Your password was reset successfully");
			reset();
		} catch (e) {
			const errorMessage = extractErrorMessage(e);
			toast.error(errorMessage || "An error occurred");
		}
	};

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<FormFieldComponent
				label="New password"
				name="new_password"
				register={register}
				errors={errors}
				placeholder="New password"
				isPassword={true}
			/>

			<FormFieldComponent
				label="Confirm password"
				name="re_new_password"
				register={register}
				errors={errors}
				placeholder="Type it once more"
				isPassword={true}
			/>
			<div className="es-form-actions">
				<p className="es-form-note">You will log in with it next.</p>
				<SubmitButton isLoading={isLoading} busyLabel="Saving…">
					Save new password
				</SubmitButton>
			</div>
		</form>
	);
}
