# Complete Manual Deployment Guide: Rev Password Manager

This guide covers everything from start to finish. It is divided into two parts:
1. **Setting up the Jenkins Container manually** (installing tools like AWS CLI, Docker, Maven, Git, and adding AWS credentials).
2. **Executing the Deployment manually** (how to run the exact commands locally without relying on Jenkins).

---

## Part 1: Setting up the Jenkins Environment Manually

When you run Jenkins inside a Docker container (`jenkins/jenkins:lts`), it is a completely isolated Linux environment. It doesn't have Git, Maven, Docker, or AWS configurations out of the box. Here are the exact commands to set it up:

### 1. Enter the Jenkins Container as Root
To install software, you need `root` privileges inside the container.
```bash
docker exec -it -u root password-manager-jenkins /bin/bash
```

### 2. Install Git and Maven
Update the package manager and install the necessary build tools.
```bash
apt-get update
apt-get install -y git maven
```

### 3. Fix Git Workspace Permissions
Because Docker mounts volumes with different permissions, Git will throw a "dubious ownership" error. Fix it by trusting the workspace directory globally:
```bash
git config --global --add safe.directory "*"
```

### 4. Install Docker CLI inside Jenkins
Jenkins needs the `docker` command to build and push images.
```bash
apt-get install -y docker.io
```
*(Note: For this to work, you must also bind mount `/var/run/docker.sock` from the host to the container and give the `jenkins` user permissions to it, which was done during your initial Jenkins container startup).*

### 5. Install the AWS CLI v2
The AWS CLI is required to log into AWS ECR and trigger the SSM deployment on the EC2 instance.
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
apt-get install -y unzip
unzip awscliv2.zip
./aws/install
rm awscliv2.zip
```

### 6. Configure AWS Credentials inside Jenkins
Jenkins needs your Windows host's AWS permissions. Exit the root shell (`exit`) and run these from your host machine to inject the credentials into the Jenkins user's environment:
```bash
# Get your keys from your Windows host (if you don't remember them)
aws configure get aws_access_key_id
aws configure get aws_secret_access_key

# Inject them into the Jenkins container
docker exec -u jenkins password-manager-jenkins aws configure set aws_access_key_id AKIAZ4EEQBWDSCWGTAPY
docker exec -u jenkins password-manager-jenkins aws configure set aws_secret_access_key <YOUR_SECRET_KEY>
docker exec -u jenkins password-manager-jenkins aws configure set region us-east-1
```

---

## Part 2: Manually Executing the Deployment Pipeline (Without Jenkins)

If you wanted to do exactly what the `Jenkinsfile` does directly from your Windows terminal (or any local machine), these are the steps you would take. 

*Prerequisites: Ensure you have Git, Maven, Docker, and AWS CLI installed on your local machine, and that `aws configure` is set up.*

### 1. Checkout the Code
Clone the repository and go into the project folder.
```bash
git clone https://github.com/Reddy076/manager.git
cd "manager/Rev-PasswordManager (2)/Rev-PasswordManager"
```

### 2. Build and Test the Backend (Maven)
Compile the Java code and run the tests to ensure it works.
```bash
mvn clean package
mvn test
```

### 3. Build the Docker Images
Package both the backend and frontend into Docker container images.
```bash
# Build Backend
docker build -t password-manager-backend:latest .

# Build Frontend (navigate into the frontend folder or point to it)
docker build -t password-manager-frontend:latest ./frontend
```

### 4. Tag the Docker Images for AWS ECR
Before you can push images to AWS, they must be tagged with the destination registry URL.
```bash
# Tag Backend
docker tag password-manager-backend:latest 678882708871.dkr.ecr.us-east-1.amazonaws.com/password-manager-phase1-backend:latest

# Tag Frontend
docker tag password-manager-frontend:latest 678882708871.dkr.ecr.us-east-1.amazonaws.com/password-manager-phase1-frontend:latest
```

### 5. Push the Images to AWS ECR
Log in to AWS Elastic Container Registry and push your newly tagged images to the cloud repository.
```bash
# Authenticate Docker with AWS ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 678882708871.dkr.ecr.us-east-1.amazonaws.com

# Push Backend
docker push 678882708871.dkr.ecr.us-east-1.amazonaws.com/password-manager-phase1-backend:latest

# Push Frontend
docker push 678882708871.dkr.ecr.us-east-1.amazonaws.com/password-manager-phase1-frontend:latest
```

### 6. Clean Up Local Images (Optional)
Remove the locally built images to save disk space.
```bash
docker image rm password-manager-backend:latest
docker image rm password-manager-frontend:latest
```

### 7. Deploy to EC2 (via AWS Systems Manager)
This command reaches out to your EC2 instance (ID: `i-08800fb73214ce6ce`), logs it into AWS ECR, downloads your new images, stops the old containers, and runs the fresh ones.

*Note: Because passing complex JSON arrays with quotes in Windows PowerShell can cause syntax errors, the safest way to execute this manually is to create a JSON file `deploy.json` with your commands and send it via the AWS CLI.*

**Step A:** Create `deploy.json`
```json
{
    "InstanceIds": ["i-08800fb73214ce6ce"],
    "DocumentName": "AWS-RunShellScript",
    "Parameters": {
        "commands": [
            "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 678882708871.dkr.ecr.us-east-1.amazonaws.com",
            "docker pull 678882708871.dkr.ecr.us-east-1.amazonaws.com/password-manager-phase1-backend:latest",
            "docker pull 678882708871.dkr.ecr.us-east-1.amazonaws.com/password-manager-phase1-frontend:latest",
            "docker stop backend frontend 2>/dev/null || true",
            "docker rm backend frontend 2>/dev/null || true",
            "docker network create rev-net 2>/dev/null || true",
            "docker run -d --name db --network rev-net -e MYSQL_DATABASE=rev_password_manager -e MYSQL_USER=admin -e MYSQL_PASSWORD=admin -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 --restart unless-stopped mysql:8.0 --innodb-buffer-pool-size=64M || true",
            "docker run -d --name backend --network rev-net -e SPRING_PROFILES_ACTIVE=docker -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/rev_password_manager?useSSL=false\\&serverTimezone=UTC\\&allowPublicKeyRetrieval=true -e SPRING_DATASOURCE_USERNAME=admin -e SPRING_DATASOURCE_PASSWORD=admin -e CORS_ALLOWED_ORIGINS=http://54.157.109.237 -e SPRING_MAIL_HOST=smtp.gmail.com -e SPRING_MAIL_PORT=587 -e SPRING_MAIL_USERNAME=mulachinna102@gmail.com -e \"SPRING_MAIL_PASSWORD=ylst blrd kfea swtm\" -e \"JWT_SECRET=UmV2UGFzc3dvcmRNYW5hZ2VyU2VjcmV0S2V5U2lnbmluZzIwMjRSZXZhdHVyZVByb2plY3Q=\" -p 8080:8080 -p 8082:8082 --restart unless-stopped 678882708871.dkr.ecr.us-east-1.amazonaws.com/password-manager-phase1-backend:latest",
            "docker run -d --name frontend --network rev-net -p 80:80 --restart unless-stopped 678882708871.dkr.ecr.us-east-1.amazonaws.com/password-manager-phase1-frontend:latest"
        ]
    }
}
```

**Step B:** Run the Deployment Command in Windows PowerShell
```bash
aws ssm send-command --cli-input-json "file://deploy.json" --region us-east-1
```

Once that command executes, your application is updated and live!
