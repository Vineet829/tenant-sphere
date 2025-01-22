# Tenant Sphere


In my current apartment complex, I’ve encountered several frustrating issues with maintenance and services. Here are some of the main problems:

- **Communication Issues**: We have to report problems through a shared WhatsApp account, but messages often get lost. This leads to many issues going unresolved for too long.
  
- **Unresponsive Technicians**: Sometimes, technicians ignore our requests or come up with excuses, making it hard to get the help we really need.
  
- **Reporting Problem Neighbors**: It’s difficult to report noisy or troublesome neighbors, which can negatively impact the vibe of our community.



To tackle these challenges, I created an app that makes it easy for tenants to report issues and bad behavior. The app is designed to:

- **Simplify Communication**: Users can easily report apartment problems and tenant misconduct through the app, ensuring quick responses and efficient issue resolution.

- **Hold Technicians Accountable**: Specific tasks can be assigned to technicians to monitor how they handle reported issues.

- **Create a Safer Community**: It is simple to report noisy or troublesome behavior and send warnings to those involved, helping everyone feel safe and comfortable at home.




## Table of Contents

- [Tech Stack](#techstack)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [License](#license)
- [Contact](#contact)



### Tech Stack Overview

- **Docker**: Used to create consistent development and production environments, facilitating deployment with Docker Compose.

- **NGINX**: Acts as a reverse proxy for the Django API and React/Next.js frontend, enhancing security and managing traffic effectively.

- **Django REST Framework**: Builds RESTful APIs that support user authentication and various app features.

- **Djoser**: Provides views for user registration, login, and password management seamlessly.

- **Celery & Redis**: Handles asynchronous tasks and background job processing, with monitoring capabilities via Flower.

- **PostgreSQL**: Serves as the primary database for efficiently storing user data, posts, and issues.

- **React & Next.js 14**: Powers the frontend, delivering a dynamic user experience with server-side rendering capabilities.

- **Redux & Redux Toolkit**: Manages application state across React components effectively.

- **Tailwind CSS**: Styles components using utility-first CSS, enabling rapid UI development.

- **Git**: Tracks code changes and facilitates collaboration through version control.

- **Cloudinary**: Manages image uploads and storage seamlessly.


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



<p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/11.png" alt="alt text 1" width="850" />

</p>
<p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/12.png" alt="alt text 1" width="850" />

</p>
<p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/1.png" alt="alt text 1" width="850" />

</p>
<p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/2.png" alt="alt text 1" width="850" />

</p>



<p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/15.jpeg" alt="alt text 1" width="400" />

</p>

### Tenant and Technician Management
- **View Tenants:** Access a comprehensive list of all tenants in the designated tab for easy management.
- **View Technicians:** Browse through a list of all technicians in the corresponding tab for quick reference and ratings based on performance.





<p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/5.png" alt="alt text 1" width="850" />

</p>
 <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/13.jpeg" alt="alt text" width="400" height="800">

### Profile Management Features
- **View Profile:** Access and update your profile information, including managing your avatar.
- **User Posts:** View all posts you have created, complete with timestamps for better tracking.
- **View Issues and Reports:** Manage reported issues and view assigned issues for effective oversight.



 <p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/4.png" alt="alt text 1" width="850" />

</p>

### Post Management Features
- **View Posts:** Explore all posts made by tenants, including top posts to stay informed about community discussions.
- **Bookmark Posts:** Save posts for easy access later in the bookmark tab to ensure you don’t miss important information.
- **View by Tags:** Filter posts by tags to streamline navigation and find relevant content quickly.
- **Interact with Posts:** Engage with posts by upvoting, downvoting, replying to comments, and deleting your own posts as needed.
- **Post Metadata:** Access detailed information about when a post was created and last updated for better context.
- **Pagination and Search:** Utilize pagination and search functionality to easily navigate through posts and tenants.


 <p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/3.png" alt="alt text 1" width="850" />

</p>

 <p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/9.png" alt="alt text 1" width="850" />

</p>
 <p align="center">
<img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/14.jpeg" alt="alt text" width="400" height="600">
</p>

### Issue Reporting
- **Report Issues:** Directly report any issues related to your apartment through the app for prompt attention.
- **Report Other Tenants:** Users can report other tenants for various misconducts, with reported individuals receiving a warning from the admin via email. This process helps maintain a safe and respectful community environment.


 <p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/7.png" alt="alt text 1" width="850" />

</p>

 <p align="center">
    <img src="https://github.com/Vineet829/tenant-sphere/blob/main/imgs/8.png" alt="alt text 1" width="850" />

</p>

   
