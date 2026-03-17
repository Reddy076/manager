# Complete AWS Deployment Guide: Rev Password Manager

This guide walks through step-by-step instructions on how to take a fresh clone of the Rev Password Manager repository and fully deploy it to a brand new AWS Account using Jenkins, Docker, ECR, and EC2.

---

## 🏗️ Phase 1: AWS Setup & Infrastructure

Before running any code, you need to prepare your AWS environment to host and store the application.

### Step 1: Create an EC2 Instance (The Server)
1. Log into your AWS Console and go to **EC2**.
2. Click **Launch Instance**.
3. **Name:** `PasswordManager-Server`
4. **OS:** Select `Amazon Linux 2023 AMI` (Free Tier Eligible).
5. **Instance Type:** `t2.micro` (Free Tier).
6. **Key Pair:** Create a new key pair (e.g., `rev-key.pem`) and download it. You will need this to SSH into the server later.
7. **Network Settings (Security Group):**
   - Allow **SSH traffic from Anywhere** (Port 22)
   - Allow **HTTP traffic from the internet** (Port 80)
   - Allow **HTTPS traffic from the internet** (Port 443)
   - *Optional:* Allow **Custom TCP Port 8080** (if you want to bypass Nginx and hit the backend API directly during testing).
8. Click **Launch**.

### Step 2: Create AWS IAM User (The Credentials)
Jenkins needs permission to talk to your AWS account to upload Docker images and restart your EC2 server.
1. Go to **IAM** in the AWS Console.
2. Click **Users** -> **Create User**. Name it `Jenkins-Deploy-User`.
3. Select **Attach policies directly**.
4. Attach the following policies:
   - `AmazonEC2ContainerRegistryFullAccess` (Allows pushing Docker images)
   - `AmazonSSMFullAccess` (Allows sending remote restart commands to EC2)
5. Create the user.
6. Click on the new user, go to the **Security credentials** tab, and click **Create access key**.
7. Select **Command Line Interface (CLI)**.
8. **CRITICAL:** Copy and save the **Access Key ID** and **Secret Access Key**. You will need these for your Jenkins server.

### Step 3: Create Elastic Container Registry (ECR) Repositories
You need a place to securely store your Docker images in the cloud.
*(Note: You only need to create repositories for the Frontend and Backend. The MySQL Database is pulled automatically from the public Docker Hub during deployment, so you don't need to host it yourself!)*
1. Go to **Elastic Container Registry (ECR)** in the AWS Console.
2. Click **Create repository**.
3. **Name:** `password-manager-phase1-backend` -> Click Create.
4. Click **Create repository** again.
5. **Name:** `password-manager-phase1-frontend` -> Click Create.
6. **Important:** Note your AWS Account ID (the 12-digit number at the beginning of the ECR URI, e.g., `123456789012`).

### Step 4: Install Docker & AWS SSM on your EC2 Instance
1. Connect to your EC2 instance via SSH using your `.pem` key, or use EC2 Instance Connect in the browser.
2. Run the following commands to install Docker and start it:
   ```bash
   sudo dnf update -y
   sudo dnf install docker -y
   sudo systemctl start docker
   sudo systemctl enable docker
   sudo usermod -aG docker ec2-user
   ```
3. *Note: AWS Amazon Linux 2023 pre-installs the AWS Systems Manager Agent (SSM), which we use for remote deployments.*

---

## ⚙️ Phase 2: Updating the Repository Configuration

Now that your AWS environment exists, you must update the repository code to point to **your** AWS account, not the original developer's account.

1. Clone the repository to your local machine:
   ```bash
   git clone https://github.com/YOUR_GITHUB_USERNAME/manager.git
   cd "manager/Rev-PasswordManager (2)/Rev-PasswordManager"
   ```

2. Open the `Jenkinsfile` in a code editor.

3. Complete the following crucial variable replacements at the top of the `Jenkinsfile`:
   - Replace the `AWS_ACCOUNT_ID` value (`678882708871`) with your actual **12-digit AWS Account ID**.
   - Ensure the `AWS_REGION` matches where you created your ECR and EC2 (e.g., `us-east-1`).
   - Find the AWS SSM command block under the "Deploy To EC2" stage:
     ```groovy
     sh "aws ssm send-command --region ${AWS_REGION} --instance-ids \"i-08800fb73214ce6ce\" ...
     ```
   - **CRITICAL:** Replace `i-08800fb73214ce6ce` with the actual **Instance ID** of your newly created EC2 server.

4. Replace Environment Variables: 
   Inside that same AWS SSM `send-command` block, look for the `docker run` command for the backend. Update the following environment variables to match your personal setup:
   - `CORS_ALLOWED_ORIGINS`: Update this to your new EC2 Public IP (e.g., `http://YOUR_NEW_EC2_IP`) or your custom domain name.
   - `SPRING_MAIL_USERNAME`: Change to your own Gmail address for sending OTPs.
   - `SPRING_MAIL_PASSWORD`: Change to your own 16-character Google App Password.
   - `JWT_SECRET`: Generate a new secure Base64 encoded string for JWT signing.

5. Save the `Jenkinsfile`, commit your changes, and push them back to your GitHub repository.

---

## 🚀 Phase 3: Setting Up Jenkins & Deploying

You need a CI/CD server to run the pipeline. You can run Jenkins locally via Docker on your computer, or spin up a separate EC2 instance strictly to act as your Jenkins server. Assuming you have a standard Jenkins installation running:

### Step 1: Install Required Jenkins Plugins
Ensure your Jenkins instance has the **Pipeline** and **Git** plugins installed.

### Step 2: Inject AWS Credentials into the Jenkins Environment
If your Jenkins is running directly on Windows/Linux, configure the AWS CLI:
```bash
aws configure
# Enter the Access Key and Secret Key you created in Phase 1, Step 2.
```
*If Jenkins is running inside a Docker Container, you must manually install the `awscli`, `docker`, and `maven` inside that container environment, and run `aws configure` inside it as the `jenkins` user.*

### Step 3: Create the Jenkins Pipeline
1. Go to your Jenkins Dashboard -> **New Item**.
2. Name it `Rev-Password-Manager-Deploy`.
3. Select **Pipeline** and click OK.
4. Scroll down to the **Pipeline** section.
5. Definition: **Pipeline script from SCM**.
6. SCM: **Git**.
7. Repository URL: Enter your GitHub repository URL.
8. Branch: `*/main` (or whichever branch you pushed your changes to).
9. Script Path: `Rev-PasswordManager (2)/Rev-PasswordManager/Jenkinsfile` (Make sure this exactly matches the folder path in your repo).
10. Click **Save**.

### Step 4: Build and Deploy!
1. Click **Build Now** on the left menu of your new Pipeline.
2. Jenkins will automatically run the following exact commands in the background for you:
   - **Build backend:** `mvn clean package -DskipTests`
   - **Build frontend images:** `docker build -t frontend:latest .`
   - **Log into AWS:** `aws ecr get-login-password ... | docker login ...`
   - **Push to AWS ECR:** `docker push ...`
   - **Execute on EC2:** `aws ssm send-command ...`
3. The final `aws ssm` script that Jenkins runs on your EC2 instance will explicitly download the public MySQL image and start your database, backend, and frontend containers automatically:
   ```bash
   # Jenkins executes this final block on the EC2 server:
   docker network create rev-net
   
   # Creates the automated database
   docker run -d --name db --network rev-net -e MYSQL_DATABASE=rev_password_manager -e MYSQL_USER=admin -e MYSQL_PASSWORD=admin -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 --restart unless-stopped mysql:8.0
   
   # Starts the backend
   docker run -d --name backend --network rev-net ... password-manager-phase1-backend:latest
   
   # Starts the frontend
   docker run -d --name frontend --network rev-net -p 8081:80 ... password-manager-phase1-frontend:latest
   ```

---

## 🔒 Phase 4: Production Polish (HTTPS & Custom Domain)

Out of the box, the `Jenkinsfile` configures the frontend container to run on Port `8081`. This is designed so Nginx can sit in front of it on Port `80` to secure the site.

1. Point a custom domain (like DuckDNS) to your EC2 Public IP address.
2. SSH into your EC2 server.
3. Install Nginx and Let's Encrypt Certbot:
   ```bash
   sudo dnf install -y nginx certbot python3-certbot-nginx
   ```
4. Create an Nginx Reverse Proxy config (`sudo nano /etc/nginx/conf.d/rev.conf`):
   ```nginx
   server {
       listen 80;
       server_name your_custom_domain.com;
       location /api/ {
           proxy_pass http://localhost:8080;
           proxy_set_header Host $host;
       }
       location / {
           proxy_pass http://localhost:8081; # Points to the frontend container
           proxy_set_header Host $host;
       }
   }
   ```
5. Enable and start Nginx:
   ```bash
   sudo systemctl enable nginx --now
   sudo systemctl restart nginx
   ```
6. Generate the SSL Encryption Certificate:
   ```bash
   sudo certbot --nginx -d your_custom_domain.com --redirect
   ```

**Congratulations! Your friend has successfully deployed the Rev Password Manager from scratch.**
