\# harness-ci-java-demo



A minimal Java Maven application configured to demonstrate a Harness CI pipeline. This project focuses on automated testing, packaging, and Docker containerization rather than application complexity.



---



\## Project Overview



This is a simple Java application built with Maven and intended for use in a Harness CI learning lab. The project demonstrates how Harness CI can:



\- Build source code

\- Run unit tests

\- Package a JAR artifact

\- Build and push a Docker image using Kubernetes-based infrastructure



---



\## Prerequisites



\- Java 17 (Eclipse Temurin)

\- Maven 3.6+

\- Docker Desktop (optional for local testing)

\- Git



---



\## Project Structure



```



harness-ci-java-demo/

├── src/

│   ├── main/java/com/jl/App.java

│   └── test/java/com/jl/AppTest.java

├── Dockerfile

├── pom.xml

└── README.md



````



---



\## Local Development



\### Run Unit Tests

```bash

mvn test

````



\### Build the Application



```bash

mvn package

```



A successful build generates a JAR file in the `target/` directory.



---



\## Docker



\### Build Docker Image (optional local step)



```bash

docker build -t harness-ci-java-demo:latest .

```



The Docker image uses a lightweight Eclipse Temurin Java 17 runtime.



---



\## Harness CI Pipeline



This repository is designed to be consumed by a Harness CI pipeline that performs the following steps:



1\. Clone the GitHub repository

2\. Run `mvn test`

3\. Run `mvn package`

4\. Build a Docker image using the provided Dockerfile

5\. Push the image to a container registry



The pipeline executes using \*\*Kubernetes (self-managed)\*\* build infrastructure via a Harness Delegate.



---



\## Technologies Used



\* \*\*Java 17 (Eclipse Temurin)\*\*

\* \*\*Apache Maven\*\*

\* \*\*JUnit\*\*

\* \*\*Docker\*\*

\* \*\*Harness CI\*\*

\* \*\*Kubernetes\*\*



---



\## Purpose



This project was created as part of a Harness CI learning lab to demonstrate CI pipeline configuration, tool integration, and Kubernetes-based execution.



---



\## Author



John Louro

