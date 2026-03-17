# 🚀 Jenkins CI/CD Deployment — Complete Guide

> **Project:** Rev Password Manager  
> **Stack:** Spring Boot · Angular · MySQL · Docker · AWS ECR · AWS EC2 · AWS SSM  
> **Jenkins Mode:** Runs inside a Docker container on your local machine

---

## 📋 Table of Contents

1. [Prerequisites — One-Time Setup](#1-prerequisites--one-time-setup)
2. [Jenkins Configuration — One-Time](#2-jenkins-configuration--one-time)
3. [Full Pipeline Flow](#3-full-pipeline-flow)
4. [How to Trigger a Deployment](#4-how-to-trigger-a-deployment)
5. [Key Things to Remember](#5-key-things-to-remember)
6. [Common Gotchas](#6-common-gotchas)

---

## 1. Prerequisites — One-Time Setup

Before any pipeline can run, Jenkins itself needs to be set up. Jenkins runs **inside a Docker container** on your local/friend's machine.

### Start the Jenkins Container

```bash
docker run -d \
  --name password-manager-jenkins \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

> ⚠️ The `-v /var/run/docker.sock` mount is **critical** — it lets Jenkins inside the container use Docker on the host machine to build images.

---

### Install Required Tools Inside the Container

Run these **once** after starting the Jenkins container:

```bash
# Update packages and install Git, Maven, Docker CLI
docker exec -u root password-manager-jenkins apt-get update
docker exec -u root password-manager-jenkins apt-get install -y git maven docker.io

# Fix Git ownership issue inside Jenkins workspace
docker exec password-manager-jenkins git config --global --add safe.directory "*"
```

### Install AWS CLI Inside the Container

```bash
docker exec -u root password-manager-jenkins curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
docker exec -u root password-manager-jenkins apt-get install -y unzip
docker exec -u root password-manager-jenkins unzip awscliv2.zip
docker exec -u root password-manager-jenkins ./aws/install
```

### Inject AWS Credentials Into the Container

```bash
docker exec -u jenkins password-manager-jenkins aws configure set aws_access_key_id     YOUR_ACCESS_KEY
docker exec -u jenkins password-manager-jenkins aws configure set aws_secret_access_key  YOUR_SECRET_KEY
docker exec -u jenkins password-manager-jenkins aws configure set region                 us-east-1
```

> 💡 Get your keys from the AWS IAM Console. Make sure the IAM user has ECR push and SSM send-command permissions.

---

## 2. Jenkins Configuration — One-Time

1. Open Jenkins in your browser: `http://localhost:8080`
2. Install these plugins: **Pipeline**, **Git**
3. Create a new job: **New Item → Pipeline**
4. Point it to your **GitHub repository** (it will auto-detect the `Jenkinsfile` at the root)
5. Set the following **environment variables** in the Jenkins job configuration:

| Variable | Value |
|---|---|
| `AWS_ACCOUNT_ID` | Your 12-digit AWS account number |
| `AWS_REGION` | `us-east-1` |
| `ECR_BACKEND_REPOSITORY` | `password-manager-phase1-backend` |
| `ECR_FRONTEND_REPOSITORY` | `password-manager-phase1-frontend` |

---

## 3. Full Pipeline Flow

Below is exactly what happens when you push to `main`:

```
Git Push to main
      │
      ▼
Stage 1: Checkout
  ├── Pulls latest code from GitHub
  └── Sets IMAGE_TAG = first 12 chars of the git commit hash

      │
      ▼
Stage 2: Backend Build
  ├── cd "Rev-PasswordManager (2)/Rev-PasswordManager"
  └── mvn -B -DskipTests clean package   ← produces the .jar file

      │
      ▼
Stage 3: Backend Tests
  ├── mvn -B test
  └── JUnit XML results are archived in Jenkins

      │
      ▼
Stage 4: Docker Build
  ├── docker build -t password-manager-backend:<TAG> .          ← Spring Boot app
  └── docker build -t password-manager-frontend:<TAG> ./frontend ← Angular app

      │
      ▼
Stage 5: Push Images to ECR  [main branch only]
  ├── aws ecr get-login-password | docker login ...
  ├── Tags both images with <commit-hash> AND latest
  └── Pushes backend + frontend images to AWS ECR

      │
      ▼
Stage 6: Deploy to EC2  [main branch only]
  └── Uses AWS SSM send-command (no SSH needed!) to run on EC2:
        1.  docker login to ECR
        2.  docker pull backend:latest
        3.  docker pull frontend:latest
        4.  docker stop + rm old backend & frontend containers
        5.  docker network create rev-net  (if not already there)
        6.  docker run db (MySQL 8.0, skipped if already running)
        7.  docker run backend (with all env vars injected)
        8.  docker run frontend

      │
      ▼
Post-Cleanup
  ├── Archives frontend dist artifacts in Jenkins
  └── Removes local backend & frontend images from the Jenkins machine
```

### What Runs on EC2 (Expanded)

Jenkins sends these commands remotely to the EC2 instance via **AWS SSM** — no SSH, no passwords needed:

```bash
# 1. Authenticate with ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ECR_URL>

# 2. Pull latest images
docker pull <ECR_URL>/password-manager-phase1-backend:latest
docker pull <ECR_URL>/password-manager-phase1-frontend:latest

# 3. Remove old containers
docker stop backend frontend
docker rm backend frontend

# 4. Ensure network exists
docker network create rev-net

# 5. Start MySQL (if not already running)
docker run -d --name db --network rev-net \
  -e MYSQL_DATABASE=rev_password_manager \
  -e MYSQL_USER=admin -e MYSQL_PASSWORD=admin \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3306:3306 --restart unless-stopped mysql:8.0 \
  --innodb-buffer-pool-size=64M || true

# 6. Start backend
docker run -d --name backend --network rev-net \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/rev_password_manager \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD=admin \
  -e CORS_ALLOWED_ORIGINS=https://revpasswordmanager.duckdns.org \
  -e SPRING_MAIL_HOST=smtp.gmail.com \
  -e SPRING_MAIL_PORT=587 \
  -e SPRING_MAIL_USERNAME=your@email.com \
  -e "SPRING_MAIL_PASSWORD=your app password" \
  -e "JWT_SECRET=your_jwt_secret" \
  -p 8080:8080 -p 8082:8082 --restart unless-stopped \
  <ECR_URL>/password-manager-phase1-backend:latest

# 7. Start frontend
docker run -d --name frontend --network rev-net \
  -p 8081:80 --restart unless-stopped \
  <ECR_URL>/password-manager-phase1-frontend:latest
```

---

## 4. How to Trigger a Deployment

### Automatic (via Git Push)

Just push to `main` — if a webhook is configured on GitHub pointing to your Jenkins URL, the pipeline fires automatically:

```bash
git add .
git commit -m "your commit message"
git push origin main
```

### Manual

1. Open Jenkins at `http://localhost:8080`
2. Go to your pipeline job
3. Click **"Build Now"**

---

## 5. Key Things to Remember

| Topic | Detail |
|---|---|
| **Where Jenkins runs** | Inside a Docker container on your local machine |
| **How it deploys to EC2** | Via **AWS SSM** `send-command` — no SSH or passwords |
| **What triggers a deploy** | Pushing to the `main` branch |
| **Where Docker images live** | AWS ECR (Elastic Container Registry) |
| **How containers communicate** | Via internal Docker network `rev-net` |
| **DB container persistence** | Data survives container restarts; use `docker rm -v db` to fully wipe |

---

## 6. Common Gotchas

These are real issues we hit and solved during setup:

### ❌ Git / Maven / Docker not found in container
The base `jenkins/jenkins:lts` image ships with none of these. Install them manually inside the container after starting it (see Section 1).

### ❌ `Cannot connect to the Docker daemon` (permission denied)
You must mount `/var/run/docker.sock` when starting the Jenkins container. Also ensure the `jenkins` user has r/w access to that socket file.

### ❌ `Unable to locate credentials` (AWS)
The Jenkins Docker container is a blank slate — it doesn't inherit your host's AWS config. Run `aws configure` **inside** the container as the `jenkins` user.

### ❌ Deployment fails with Exit Code 252 (SSM quoting issue)
If any environment variable contains **spaces** (like Gmail App Passwords: `ylst blrd kfea swtm`), wrap the entire assignment in **double quotes** in the Jenkinsfile:
```groovy
# ✅ Correct
-e "SPRING_MAIL_PASSWORD=ylst blrd kfea swtm"

# ❌ Wrong
-e SPRING_MAIL_PASSWORD='ylst blrd kfea swtm'
```

### ❌ Old DB data not wiping after `docker rm db`
Standard `docker rm` leaves the volume (and all data) behind. Use the `-v` flag to also delete the volume:
```bash
docker stop db && docker rm -v db
```

### ❌ Git `dubious ownership` error
Git inside the container sees a different file owner for the Jenkins workspace. Fix it once with:
```bash
docker exec password-manager-jenkins git config --global --add safe.directory "*"
```

---

*This guide covers the complete, battle-tested Jenkins setup for the Rev Password Manager project.*
