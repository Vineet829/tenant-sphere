# Tenant Sphere

Welcome to the **Tenant Sphere**! This application is designed to facilitate communication and management between tenants and property management. It provides various features for tenants to manage their apartments, report issues, and interact with other tenants and technicians.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [License](#license)
- [Contact](#contact)

### Tech Stack Overview

### Tech Stack Overview

- **Docker:** 
  - Utilized for creating consistent development and production environments, facilitating deployment with Docker Compose.

- **NGINX:** 
  - Implemented as a reverse proxy for the Django API and React/Next.js frontend, enhancing security and managing traffic effectively.

- **Django REST Framework:** 
  - Employed to build RESTful APIs that support user authentication and various app features.

- **Djoser:** 
  - Integrated to provide views for user registration, login, and password management seamlessly.

- **Celery & Redis:** 
  - Leveraged for handling asynchronous tasks and background job processing, with monitoring capabilities via Flower.

- **PostgreSQL:** 
  - Chosen as the primary database for storing user data, posts, and issues efficiently.

- **React & Next.js 14:** 
  - Used to power the frontend, delivering a dynamic user experience with server-side rendering capabilities.

- **Redux & Redux Toolkit:** 
  - Applied to manage application state across React components effectively.

- **Tailwind CSS:** 
  - Adopted for styling components using utility-first CSS, enabling rapid UI development.

- **Git:** 
  - Utilized for version control, allowing for effective tracking of code changes and collaboration.

- **Cloudinary:** 
  - Employed to manage image uploads and storage seamlessly.

- **DigitalOcean:** 
  - Selected for hosting the application and managing SSL certificates.


## Installation

### Prerequisites

- Node.js (v14.x or later)
- npm (v6.x or later)
- MongoDB (v4.x or later)
- Docker (for containerization)

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/tenant-app.git
   cd tenant-app


## Backend Setup

2. Install backend dependencies:
```bash
cd backend
npm install
```

3. Set up the environment variables:
Create a `.env` file in the root directory and add the following:

```env
SITE_NAME=""
DJANGO_SECRET_KEY=""
DJANGO_ADMIN_URL=""
EMAIL_PORT=""
EMAIL_HOST=""
DEFAULT_FROM_EMAIL=""
CELERY_FLOWER_USER=""
CELERY_FLOWER_PASSWORD=""
CELERY_BROKER_URL=""
CELERY_RESULT_BACKEND=""
POSTGRES_HOST=""
POSTGRES_PORT=""
POSTGRES_DB=""
POSTGRES_USER=""
POSTGRES_PASSWORD=""
CLOUDINARY_CLOUD_NAME=""
CLOUDINARY_API_KEY=""
CLOUDINARY_API_SECRET=""
COOKIE_SECURE=""
SIGNING_KEY=""
GOOGLE_CLIENT_ID=""
GOOGLE_CLIENT_SECRET=""
REDIRECT_URIS=""
```

4. Start the backend application:
```bash
npm start
```

## Frontend Setup

5. Install frontend dependencies:
```bash
cd frontend
npm install
```

6. Start the frontend application:
```bash
npm start
```

## Usage

### Authentication Features
- **Register a New User:** Easily create a new account to start using the app.
- **Login:** Securely log in with your registered credentials or use Google OAuth for quick access.
- **Logout:** End your session securely to protect your account from unauthorized access.
- **Password Reset:** Quickly request a password reset if you forget your password, ensuring you can regain access to your account.

### Tenant and Technician Management
- **View Tenants:** Access a comprehensive list of all tenants in the designated tab for easy management.
- **View Technicians:** Browse through a list of all technicians in the corresponding tab for quick reference and ratings based on performance.

### Profile Management Features
- **View Profile:** Access and update your profile information, including managing your avatar.
- **User Posts:** View all posts you have created, complete with timestamps for better tracking.
- **View Issues and Reports:** Manage reported issues and view assigned issues for effective oversight.

### Post Management Features
- **View Posts:** Explore all posts made by tenants, including top posts to stay informed about community discussions.
- **Bookmark Posts:** Save posts for easy access later in the bookmark tab to ensure you don’t miss important information.
- **View by Tags:** Filter posts by tags to streamline navigation and find relevant content quickly.
- **Interact with Posts:** Engage with posts by upvoting, downvoting, replying to comments, and deleting your own posts as needed.
- **Post Metadata:** Access detailed information about when a post was created and last updated for better context.
- **Pagination and Search:** Utilize pagination and search functionality to easily navigate through posts and tenants.

### Issue Reporting
- **Report Issues:** Directly report any issues related to your apartment through the app for prompt attention.
- **Report Other Tenants:** Users can report other tenants for various misconducts, with reported individuals receiving a warning from the admin via email. This process helps maintain a safe and respectful community environment.



   
