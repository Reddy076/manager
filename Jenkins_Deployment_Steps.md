# Jenkins CI/CD Deployment Process

This document outlines the complete CI/CD pipeline steps utilized by Jenkins to build, test, and deploy the **Rev Password Manager** application.

The deployment process assumes that Jenkins is already configured, running, and triggered appropriately (e.g., via a Git push or a webhook).

## Prerequisites (Automated by Jenkins Environment)
* **AWS Credentials**: `AWS_REGION` and `AWS_ACCOUNT_ID` setup in the environment.
* **ECR Repositories**: Backend and frontend ECR repositories specified (`ECR_BACKEND_REPOSITORY`, `ECR_FRONTEND_REPOSITORY`).
* **Source Control**: Application source code with `pom.xml` and `Dockerfile`s in their respective directories.

---

## Pipeline Stages

### 1. Checkout
Jenkins checks out the source code from the configured Git repository. 
* A shortened Git commit hash (first 12 characters) is extracted and set as the `IMAGE_TAG` for Docker images.
* The AWS Elastic Container Registry (ECR) URL is generated using the AWS Account ID and Region.

### 2. Backend Build
Jenkins navigates to the backend directory (`Rev-PasswordManager`) and compiles the Java Spring Boot code.
* **Command:** `mvn -B -DskipTests clean package`
* This stage builds the executable `.jar` file without running the tests yet.

### 3. Backend Tests
Unit and integration tests for the backend are executed.
* **Command:** `mvn -B test`
* Jenkins generates and archives JUnit XML test results for review.

### 4. Docker Build
Docker images for both the backend and frontend are built using their respective `Dockerfile`s.
* **Backend Image:** Built from the root of the backend folder and tagged with the `IMAGE_TAG`.
* **Frontend Image:** Built from the `./frontend` folder and tagged with the `IMAGE_TAG`.

### 5. Push Images to ECR (AWS Elastic Container Registry)
*This stage only runs if the current branch is `main`.*
* Jenkins logs into AWS ECR using AWS CLI (`aws ecr get-login-password`).
* The locally built Docker images are re-tagged with the remote ECR repository URIs and both the specific `IMAGE_TAG` and a `latest` tag.
* The images are pushed to AWS ECR safely.

### 6. Deploy to EC2
*This stage only runs for deployments from the `main` branch with the AWS Account correctly configured.*
Jenkins uses AWS Systems Manager (SSM) `send-command` to remotely execute a deployment shell script on the target EC2 instance.

**Remote Execution Steps on EC2:**
1. **Authenticate:** Logs into AWS ECR to pull the newly built images.
2. **Pull Images:** Pulls the `latest` frontend and backend images from ECR.
3. **Clean Up:** Stops and removes any previously running `backend` and `frontend` Docker containers.
4. **Networking:** Ensures the local Docker network (`rev-net`) is present.
5. **Database Setup:** Starts up a `mysql:8.0` database container named `db` (if it's not already running) on `rev-net` with the required user credentials, databases, and memory pool allocations.
6. **Backend Container Start:** Runs the Java Spring Boot backend container asynchronously on `rev-net`. It is injected with necessary environment variables, including:
   * Database credentials and JDBC URL pointing to the `db` container.
   * Remote CORS allowed origins.
   * Email server details (SMTP host, port, credentials).
   * JWT Secret Key.
   * Exposes application ports `8080` and `8082`.
7. **Frontend Container Start:** Runs the Angular/frontend application container, joining the `rev-net` network, exposing on HTTP port `80`.

### 7. Post-Build Actions & Cleanup
Regardless of success or failure, Jenkins performs a post-run cleanup.
* Any frontend dist artifacts are archived in Jenkins.
* Local Docker images generated during the pipeline run (`APP_NAME-backend:IMAGE_TAG` and `APP_NAME-frontend:IMAGE_TAG`) are removed from the Jenkins runner's local system to free up space.

---

### End of Deployment
At this stage, the deployment is fully complete. The EC2 instance now hosts the latest database, backend APIs, and frontend client containers running concurrently and communicating securely over an internal Docker network.
