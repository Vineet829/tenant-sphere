import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { setLogout } from "@/lib/redux/features/auth/authSlice";

const initialState = {
	searchTerm: "",
	page: 1,
	techSearchTerm: "",
	techPage: 1,
};

const userSlice = createSlice({
	name: "user",
	initialState,
	reducers: {
		setSearchTerm: (state, action: PayloadAction<string>) => {
			state.searchTerm = action.payload;
		},
		setCurrentPage: (state, action: PayloadAction<number>) => {
			state.page = action.payload;
		},
		setTechSearchTerm: (state, action: PayloadAction<string>) => {
			state.techSearchTerm = action.payload;
		},
		setTechCurrentPage: (state, action: PayloadAction<number>) => {
			state.techPage = action.payload;
		},
	},
	// The next person to sign in starts without anyone else's search or page.
	extraReducers: (builder) => {
		builder.addCase(setLogout, () => initialState);
	},
});


export const { setSearchTerm, setCurrentPage, setTechSearchTerm, setTechCurrentPage } = userSlice.actions;
export default userSlice.reducer;
