"use client";

import SubmitButton from "@/components/estate/SubmitButton";
import { useCreateApartmentMutation } from "@/lib/redux/features/apartment/apartmentApiSlice";
import { apartmentCreateSchema, TApartmentCreateSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { Building2, DoorOpen } from "lucide-react";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { toast } from "react-toastify";
import * as z from "zod";
import FieldError from "@/components/estate/FieldError";
import { FormFieldComponent } from "../FormFieldComponent";

export default function ApartmentCreateForm() {
	const [createApartment, { isLoading }] = useCreateApartmentMutation();
	const router = useRouter();

	const {
		register,
		handleSubmit,
		control,
		reset,
		formState: { errors },
	} = useForm<TApartmentCreateSchema>({
		resolver: zodResolver(apartmentCreateSchema),
		mode: "all",
		defaultValues: {
			unit_number: "",
			building: "",
			floor: 1,
		},
	});

	const onSubmit = async (values: z.infer<typeof apartmentCreateSchema>) => {
		try {
			await createApartment(values).unwrap();
			toast.success("Apartment Added");
			router.push("/profile");
			reset();
		} catch (error) {
			const errorMessage = extractErrorMessage(error);
			toast.error(errorMessage || "An error occurred");
		}
	};

	const floorError = errors.floor?.message;

	return (
		<form noValidate onSubmit={handleSubmit(onSubmit)} className="es-form">
			<FormFieldComponent
				label="Building"
				name="building"
				register={register}
				errors={errors}
				placeholder="Cedar Court"
				startIcon={<Building2 aria-hidden="true" />}
			/>

			<div className="es-form-row">
				<FormFieldComponent
					label="Unit number"
					name="unit_number"
					register={register}
					errors={errors}
					placeholder="C-204"
					startIcon={<DoorOpen aria-hidden="true" />}
				/>

				<div className="es-field" data-invalid={Boolean(floorError)}>
					<div className="es-field-head">
						<label htmlFor="floor" className="es-label">
							Floor
						</label>
					</div>
					<Controller
						name="floor"
						control={control}
						render={({ field }) => (
							<input
								{...field}
								id="floor"
								type="number"
								inputMode="numeric"
								min={0}
								max={5}
								className="es-input"
								onChange={(e) => field.onChange(parseInt(e.target.value))}
								aria-invalid={Boolean(floorError)}
							/>
						)}
					/>
					{floorError ? <FieldError>{floorError}</FieldError> : <p className="es-hint">A number from 0 to 5.</p>}
				</div>
			</div>

			<div className="es-form-actions">
				<p className="es-form-note">It appears on your profile straight away.</p>
				<SubmitButton isLoading={isLoading} busyLabel="Saving…">
					Add apartment
				</SubmitButton>
			</div>
		</form>
	);
}
