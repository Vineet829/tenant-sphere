# Frontend styling

Tailwind CSS 3 owns the app's styling. `tailwind.theme.ts` defines the shared
palette, scoped theme variables, fonts, radii, shadows, animations and responsive
breakpoints. `tailwind.config.ts` extends the existing Tailwind configuration and
registers the variables for the community shell and its portalled surfaces.

Use utility classes directly for local styling. Repeated component styles use
`@apply` in the existing component stylesheets. The homepage and authentication
CSS modules retain local selector scoping and also use Tailwind utilities. This
keeps the existing selector precedence, responsive rules and interactive states.
Container queries remain named CSS containers so forms respond to the available
content width, including when the navigation is visible.

Prefer named theme utilities such as `text-estate-text`, `rounded-estate-sm` and
`animate-estate-page-in`. Arbitrary values preserve the existing pixel dimensions,
gradients, shorthand resets and zoom calculations. Update the theme when changing
shared visual values; avoid adding another parallel palette.

Runtime values such as rating percentages, skeleton widths and animation indices
are passed as CSS variables and consumed by Tailwind utilities. Do not construct
utility class names from runtime numbers: Tailwind must see complete class names.

The occupation selector uses react-select's unstyled mode and Tailwind component
classes. Its keyboard navigation and positioning remain with the library. Vendor
styles such as React Toastify's animation stylesheet stay with their package.
