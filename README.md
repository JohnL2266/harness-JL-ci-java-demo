# harness-ci-java-demo

A minimal Java Maven application used to demonstrate an end-to-end Harness CI/CD pipeline, including containerization, Kubernetes deployment, canary releases, and pipeline templating.

This project prioritizes **pipeline design and delivery workflows** over application complexity.

---

## Project Overview

This repository contains a simple Java application built with Maven and deployed using Harness CI and CD.  
It demonstrates how Harness can be used to:

- Build and test source code  
- Package a JAR artifact  
- Build and push a Docker image  
- Deploy the application to Kubernetes  
- Perform a canary deployment with verification  
- Promote traffic via a rolling deployment  
- Reuse pipeline logic through step templating  

---

## Project Structure

harness-ci-java-demo/
├── src/
│ ├── main/java/com/jl/App.java
│ └── test/java/com/jl/AppTest.java
├── k8s/
│ ├── deployment.yaml
│ └── service.yaml
├── Dockerfile
├── pom.xml
└── README.md

yaml
Copy code

---

## Prerequisites

- Java 17 (Eclipse Temurin)
- Maven 3.6+
- Git
- Docker (optional for local testing)
- Access to:
  - Harness CI/CD
  - Kubernetes cluster (Rancher-managed in this lab)
  - Container registry (Docker Hub)

---

## Local Development

### Run Unit Tests

```bash
mvn test

---

Build the Application
bash
Copy code
mvn package
A successful build produces a JAR file in the target/ directory.

Docker
Build Image Locally (Optional)
docker build -t harness-ci-java-demo:latest .

The image uses a lightweight Eclipse Temurin Java 17 runtime and mirrors the image built in the CI pipeline.

Harness CI Pipeline
The CI stage performs the following steps:
Clone the GitHub repository
Run unit tests (mvn test)
Package the application (mvn package)
Build a Docker image
Push the image to Docker Hub
The CI pipeline runs on Kubernetes-based build infrastructure using a Harness Delegate.

Harness CD Pipeline
The CD stage deploys the application to Kubernetes using manifests stored in this repository.
Deployment Strategy
Canary Deployment
Deploys a subset of replicas
Executes a verification step
Automatically deletes canary resources after validation
Primary Deployment
Promotes the release using a rolling deployment strategy
Kubernetes manifests are sourced from the k8s/ directory in GitHub.

Canary Verification Template (Bonus)
As part of the bonus objective, the Verify Canary step was templatized and reused within the pipeline.
This demonstrates one of Harness’s key value propositions:
Reusable, versioned pipeline components
Consistent deployment verification logic
Reduced duplication across services and pipelines

Accessing the Application
The Kubernetes Service is deployed as a ClusterIP service.
To access the application locally:
kubectl port-forward svc/harness-ci-lab 8080:80
Then visit:
http://localhost:8080


Technologies Used
Java 17 (Eclipse Temurin)
Apache Maven
JUnit
Docker
Kubernetes
Harness CI
Harness CD

Purpose
This project was created as a Harness CI/CD lab exercise to demonstrate:
CI pipelines running on Kubernetes infrastructure
Container image creation and publishing
Kubernetes-based deployments with canary strategies
Pipeline reusability through templating

Author
John Louro
