# ChemApp - SAST Demo Application

## 1. Overview

**ChemApp** is a simple Java web application built for SAST demonstration purposes.

It uses the following technologies:
* **Language:** Java 8
* **Build System:** Maven
* **Web Framework:** SparkJava
* **Database:** H2 (In-Memory)

---

## 2. Build & Execution

### Prerequisites

* Java Development Kit (JDK) 8 or higher
* Apache Maven

### Steps

1.  **Compile and Package:**
    ```bash
    mvn clean package
    ```

2.  **Start the Application:**
    ```bash
    java -jar target/ChemCompoundDB-1.0-SNAPSHOT.jar
    ```

3.  **Access the Application:**
    Open a web browser and navigate to `http://localhost:8080`.
