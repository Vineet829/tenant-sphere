import PersonAvatar from "@/components/estate/PersonAvatar";
import SubmitButton from "@/components/estate/SubmitButton";
import { useUserProfile } from "@/hooks/useUseProfile";
import { useGetSinglePostQuery, useReplyToPostMutation } from "@/lib/redux/features/posts/postApiSlice";
import { replyCreateSchema, TReplyCreateSchema } from "@/lib/validationSchemas";
import { extractErrorMessage } from "@/utils";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import { FormFieldComponent } from "../FormFieldComponent";

interface ReplyFormProps {
	slug: string | undefined;
	postId?: string;
}

export default function CreateReplyForm({ slug }: ReplyFormProps) {
	const [replyToPost, { isLoading }] = useReplyToPostMutation();
	const { data } = useGetSinglePostQuery(slug || "");
	const { profile } = useUserProfile();
	const postId = data?.post.id;
	const router = useRouter();

	const {
		register,
		handleSubmit,
		reset,
		formState: { errors },
	} = useForm<TReplyCreateSchema>({
		resolver: zodResolver(replyCreateSchema),
		mode: "all",
		defaultValues: {
			body: "",
		},
	});

	const onSubmit = async (values: TReplyCreateSchema) => {
		try {
			await replyToPost({ postId, ...values }).unwrap();
			toast.success("Your reply was created.");
			router.push(`/post/${slug}`);
			reset();
		} catch (error) {
			const errorMessage = extractErrorMessage(error);
			toast.error(errorMessage || "An error occurred");
		}
	};
	return (
		<div className="es-replyform">
			<PersonAvatar name={profile?.full_name || profile?.username} seed={profile?.username} src={profile?.avatar} size="sm" />
			<form noValidate onSubmit={handleSubmit(onSubmit)}>
				<label htmlFor="body" className="es-sr">
					Your reply
				</label>
				<FormFieldComponent
					name="body"
					register={register}
					errors={errors}
					placeholder="Add your reply…"
					isTextArea
				/>
				<div className="es-replyform-foot">
					<SubmitButton isLoading={isLoading} busyLabel="Posting…" arrow={false} className="es-btn--sm">
						Reply
					</SubmitButton>
				</div>
			</form>
		</div>
	);
}
