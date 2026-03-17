# Jenkins Deployment Troubleshooting Guide

This document captures the entire end-to-end process of deploying the Rev Password Manager using Jenkins inside a Docker container. It heavily focuses on the errors encountered during the migration from a native Windows Jenkins installation to a Linux-based Docker Jenkins container, the commands used to diagnose them, and the ultimate fixes.

## 1. Initial Setup: Jenkins in Docker
The deployment started by spinning up Jenkins inside a Docker container (`password-manager-jenkins`).

**The Core Issue:** A Docker container is an isolated Linux environment. When Jenkins tried to run the `Jenkinsfile` pipeline, it failed immediately because it lacked the global tools that were present on the host Windows machine.

---

## 2. Pipeline Stage: Checkout & Build (The Missing Tools Errors)

### Error 1: Git Not Found
* **Symptoms:** The Jenkins pipeline failed at the very first step (`Checkout`) with an error indicating it could not fetch the remote repository from GitHub.
* **Cause:** The `jenkins/jenkins:lts` Docker image does not come with `git` installed by default.
* **The Fix:** We had to enter the Jenkins container as the root user and install Git globally.
  ```bash
  docker exec -u root password-manager-jenkins apt-get update
  docker exec -u root password-manager-jenkins apt-get install -y git
  ```

### Error 2: "Dubious Ownership" Git Error
* **Symptoms:** After installing Git, the checkout still failed with `fatal: detected dubious ownership in repository at '/var/jenkins_home/workspace'`.
* **Cause:** Docker volume mounting permissions caused Git to view the workspace folder as being owned by a different user than the Jenkins executor, triggering a security block.
* **The Fix:** We told Git to trust the entire Jenkins workspace directory globally.
  ```bash
  docker exec password-manager-jenkins git config --global --add safe.directory "*"
  ```

### Error 3: Maven Not Found (`mvn: command not found`)
* **Symptoms:** The checkout succeeded, but the `Backend Build` stage failed with an error stating the `mvn` command was not recognized.
* **Cause:** Just like Git, Maven was not installed inside the basic Jenkins container.
* **The Fix:** We installed Maven into the Jenkins container as the root user.
  ```bash
  docker exec -u root password-manager-jenkins apt-get install -y maven
  ```

---

## 3. Pipeline Stage: Docker Build (The Daemon Error)

### Error 4: Docker Command Not Found (`docker: command not found`)
* **Symptoms:** The Java app compiled successfully, but the `Docker Build` stage failed when Jenkins tried to use the `docker build` command to create the frontend and backend images.
* **Cause:** The Jenkins container did not have the Docker CLI installed.
* **The Fix:** Installed the Docker CLI inside the Jenkins container.
  ```bash
  docker exec -u root password-manager-jenkins apt-get update
  docker exec -u root password-manager-jenkins apt-get install -y docker.io
  ```

### Error 5: Permission Denied (`Cannot connect to the Docker daemon`)
* **Symptoms:** Even after installing the `docker` CLI, Jenkins still couldn't build images. Running a test command like `docker version` inside Jenkins resulted in a permission error accessing `/var/run/docker.sock`.
* **Cause:** The Jenkins user inside the container did not have Linux permissions to talk to the host machine's Docker engine (the `.sock` file).
* **The Fix:** We bind-mounted the host's `/var/run/docker.sock` when starting the Jenkins container, and then explicitly granted the `jenkins` user read/write access to that socket file so Jenkins could "borrow" the host's Docker engine to build images.

---

## 4. Pipeline Stage: Push to AWS ECR

### Error 6: AWS CLI Not Found
* **Symptoms:** The `Push Images To ECR` stage failed because the `aws` command was missing.
* **Cause:** The AWS CLI was not installed inside the Jenkins container.
* **The Fix:** Downloaded and installed the AWS CLI v2 inside the container.
  ```bash
  docker exec -u root password-manager-jenkins curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
  docker exec -u root password-manager-jenkins apt-get install unzip
  docker exec -u root password-manager-jenkins unzip awscliv2.zip
  docker exec -u root password-manager-jenkins ./aws/install
  ```

### Error 7: "Unable to locate credentials"
* **Symptoms:** The `aws ecr get-login-password` step failed because Jenkins was not authorized to push to the ECR repositories.
* **Cause:** While the host Windows machine had the AWS `AKIA...` keys configured the Jenkins container was a blank slate and had no AWS identity.
* **The Fix:** Extracted the AWS keys from the Windows host and injected them directly into the Jenkins container's environment so it could act on behalf of the developer.
  ```bash
  # Extracted from host:
  aws configure get aws_access_key_id
  
  # Injected to Jenkins container:
  docker exec -u jenkins password-manager-jenkins aws configure set aws_access_key_id [ACCESS_KEY]
  docker exec -u jenkins password-manager-jenkins aws configure set aws_secret_access_key [SECRET_KEY]
  docker exec -u jenkins password-manager-jenkins aws configure set region us-east-1
  ```

---

## 5. Pipeline Stage: Deploy to EC2 (via AWS SSM)

### Error 8: SSM Exit Code 252 (Unterminated quoted string)
* **Symptoms:** The ECR push worked, but the final stage (`Deploy To EC2`) failed with `Exit Code 252` when trying to run the `aws ssm send-command`.
* **Cause:** The `SPRING_MAIL_PASSWORD` environment variable in the Jenkinsfile contained spaces. 
  ```groovy
  -e SPRING_MAIL_PASSWORD='ylst blrd kfea swtm'
  ```
  Because the entire `docker run` command was being sent as an element in a JSON array to AWS SSM, the Linux bash parser on the EC2 instance stripped the single quotes, causing AWS CLI to misinterpret the spaces as separate, invalid command-line arguments.
* **The Fix:** Updated the `Jenkinsfile` to use double quotes wrapped around the entire variable assignment to ensure the shell parsed it as a single block safely.
  ```groovy
  # Updated Jenkinsfile:
  -e "SPRING_MAIL_PASSWORD=ylst blrd kfea swtm"
  ```
  Committed and pushed this fix directly to GitHub, triggering a successful pipeline run.

---

## 6. Post-Deployment Database Wipe

### Error 9: Local Database Retained Old Data
* **Symptoms:** The user requested to clear the `db` container on the EC2 instance. The initial AWS SSM command via Windows PowerShell (`docker stop db && docker rm db`) failed silently.
* **Cause:** Windows PowerShell struggles with nested quotes inside complex JSON array strings passed via the `commands=[...]` argument flag, causing the command to silently fail to reach the EC2 instance. Furthermore, even when deleted, standard `docker rm` leaves orphan volumes behind, preserving the data for the next container.
* **The Fix:** Created a physical `wipe.json` file locally to sidestep PowerShell escaping limitations. We included the critical `-v` flag to forcefully delete attached volumes, completely wiping all user data.
  ```json
  // wipe.json
  {
      "InstanceIds": ["i-08800fb73214ce6ce"],
      "DocumentName": "AWS-RunShellScript",
      "Parameters": {
          "commands": [
              "docker stop db",
              "docker rm -v db",
              "docker run -d --name db --network rev-net -e MYSQL_DATABASE=rev_password_manager ...",
              "docker restart backend"
          ]
      }
  }
  ```
  Executed this payload directly via the AWS CLI to reset the EC2 database successfully:
  ```bash
  aws ssm send-command --cli-input-json "file://e:\Deploy\manager\wipe.json" --region us-east-1
  ```

### Conclusion
By overcoming container isolation, credential mirroring, and shell escaping complexities, the Jenkins pipeline is now successfully architected to checkout, build, package, and deploy the entire application stack fully autonomously.
