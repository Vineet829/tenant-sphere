# Tenant Sphere

Welcome to the **Tenant Sphere**! This application is designed to facilitate communication and management between tenants and property management. It provides various features for tenants to manage their apartments, report issues, and interact with other tenants and technicians.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [License](#license)
- [Contact](#contact)

## Features

- User authentication and profile management
- View and manage posts by tenants
- Report issues related to apartments
- Report other tenants for misconduct
- Interact with posts: upvote, downvote, reply, and bookmark
- View tenants and technicians in dedicated tabs
- Access to bookmarked posts and posts by tags
- **Tenant Reporting:** Users can report other tenants for various misconducts.

### Tech Stack

- **Backend:** Django, Django REST Framework, Python 3, Type Hints
- **Frontend:** React, Next.js 14, TypeScript, Redux, Redux Toolkit
- **Containerization:** Docker, NGINX, Shell Scripting

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
- **Register a New User:** Create a new account to start using the app.
- **Login:** Securely log in with your registered credentials.
- **Logout:** End your session securely to protect your account.
- **Password Reset:** Request a password reset if you've forgotten your password.

### Tenant and Technician Management
- **View Tenants:** Access a list of all tenants in the tenants tab.
- **View Technicians:** Access a list of all technicians in the technicians tab.

### Profile Management Features
- **View Profile:** Access your profile to view personal information.
- **User Posts:** View all your created posts, including details such as creation and update timestamps.
- **View Issues and Reports:** Access and manage your reported issues and assigned issues.

### Post Management Features
- **View Posts:** Browse all posts made by tenants.
- **Bookmark Posts:** Save posts for later access in the bookmark tab.
- **View by Tags:** Filter posts by tags for easier navigation.
- **Interact with Posts:** Upvote, downvote, reply to, and delete your own posts.
- **Post Metadata:** See when a post was created and last updated.

### Issue Reporting
- **Report Issues:** Report issues related to your apartment directly through the app.
- **Report Other Tenants:** Users can report other tenants for misconduct. The reported tenants will receive a warning from the admin via email, ensuring a safe and respectful community.

   
