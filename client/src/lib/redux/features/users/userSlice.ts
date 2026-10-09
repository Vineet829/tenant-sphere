import { createSlice, PayloadAction } from "@reduxjs/toolkit";

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
});


export const { setSearchTerm, setCurrentPage, setTechSearchTerm, setTechCurrentPage } = userSlice.actions;
export default userSlice.reducer;
