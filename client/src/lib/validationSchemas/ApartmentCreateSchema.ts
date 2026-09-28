import * as z from "zod";

export const apartmentCreateSchema = z.object({
	unit_number: z.string().trim().min(1, "Enter the unit number"),
	building: z.string().trim().min(1, "Enter the building name"),
	floor: z
		.number({ required_error: "Enter the floor", invalid_type_error: "Enter the floor as a number" })
		.nonnegative({ message: "The floor cannot be negative" })
		.max(5, { message: "The floor cannot be higher than 5" }),
});

export type TApartmentCreateSchema = z.infer<typeof apartmentCreateSchema>;
