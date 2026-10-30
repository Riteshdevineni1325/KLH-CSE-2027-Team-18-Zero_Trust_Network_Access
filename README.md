# CAR-ZTNA: Context-Aware Role-Based Zero Trust Network Architecture

## Project Information

- **Project Name:** CAR-ZTNA
- **Project Title:** Context-Aware Role-Based Zero Trust Network Architecture
- **Domain:** Cybersecurity / Network Security
- **Project Type:** Final Year Academic Project
- **Team ID:** Team 18
- **Academic Year:** 2026–2027
- **Institution:** KLH University
- **Branch:** CSE

## Team Members

| S.No. | Name | Student ID |
|---|---|---|
| 1 | Devineni Ritesh | 2320030067 |
| 2 | Y. Hemanth Reddy | 2320030468 |
| 3 | K. Uday | 2320030429 |

**Project Supervisor:** Dr. K. Swanthana

---

## 1. Abstract

Traditional access-control systems primarily depend on user authentication and predefined permissions. However, successful authentication alone does not guarantee that a user, device, or network environment is trustworthy.

CAR-ZTNA (Context-Aware Role-Based Zero Trust Network Architecture) is a cybersecurity project designed to provide secure and context-aware access control based on the Zero Trust principle, **"Never Trust, Always Verify."**

The proposed system evaluates multiple contextual attributes, including user roles, network zones, device registration status, and resource sensitivity before granting access to protected resources.

The system incorporates role-based access control, device verification, office and home network identification, account lockout after repeated failed login attempts, dual-admin approval for administrative role promotion, periodic session reassessment, and security audit logging.

The application is developed using Java and Spring Boot, with PostgreSQL for database management and HTML, CSS, and JavaScript for the web interface.

## 2. Objectives

- Implement secure user authentication using BCrypt password hashing.
- Provide role-based access control for USER and ADMIN roles.
- Identify access requests originating from office and home networks.
- Verify device registration and trust status.
- Apply resource-sensitivity-based access policies.
- Implement default-deny authorization.
- Prevent brute-force login attempts through account lockout.
- Introduce dual-admin approval for USER-to-ADMIN role promotion.
- Monitor active sessions and reassess access conditions.
- Maintain security logs for monitoring and auditing.

## 3. Key Features

### Authentication and Account Security
- Secure user registration and login.
- BCrypt password hashing.
- Account lockout after five consecutive failed login attempts.
- Controlled account unlocking through an administrator-generated recovery mechanism.

### Role-Based Access Control
- Two roles: USER and ADMIN.
- Different access permissions based on assigned roles.
- Dual-admin approval for USER-to-ADMIN promotion.

### Context-Aware Access Control
- Office and home network identification.
- Device registration and trust-status verification.
- Resource sensitivity classification.
- Context-based authorization decisions.

### Zero Trust Policy Enforcement
- Default-deny access policy.
- Explicit allow and deny decisions.
- Periodic session reassessment.
- Access restriction based on policy conditions.

### Security Monitoring and Auditing
- Login activity tracking.
- Access request monitoring.
- Security event logging.
- Active session monitoring.
- Administrative activity tracking.

## 4. Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Java |
| Backend Framework | Spring Boot |
| Security Framework | Spring Security |
| Database | PostgreSQL |
| Frontend | HTML, CSS, JavaScript |
| Password Security | BCrypt |
| Build Tool | Maven |
| API Testing | Postman |
| Testing Framework | JUnit 5 |
| Version Control | Git and GitHub |

## 5. System Architecture

The CAR-ZTNA system follows a context-aware Zero Trust access-control architecture.

**Main components:**

1. **Authentication Module:** Verifies user credentials.
2. **Context Collector:** Collects network, device, and user context.
3. **Policy Evaluation Module:** Evaluates roles, contextual attributes, and resource sensitivity.
4. **Policy Enforcement Module:** Allows or denies access based on policy decisions.
5. **Session Monitoring Module:** Periodically reassesses active sessions.
6. **Security Logging Module:** Records authentication, access, and administrative events.
7. **Database Module:** Stores user information, device details, access policies, sessions, and security logs.

### Working Methodology

1. The user registers or logs into the application.
2. The system verifies the user's credentials.
3. Contextual information, including network zone and device status, is collected.
4. The policy evaluation module checks the user's role and requested resource.
5. The system generates an access decision based on predefined Zero Trust policies.
6. The request is allowed or denied through policy enforcement.
7. Active sessions are periodically reassessed, and security events are recorded.

## 6. Project Structure

```text
ztna-system/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── ztna/
│   │   │           ├── controller/
│   │   │           ├── service/
│   │   │           ├── model/
│   │   │           ├── repository/
│   │   │           ├── config/
│   │   │           └── ...
│   │   │
│   │   └── resources/
│   │       ├── static/
│   │       ├── templates/
│   │       └── application.properties
│   │
│   └── test/
│
├── docs/
├── data/
├── results/
├── reports/
├── pom.xml
├── .gitignore
└── README.md
```

*The folder structure above is a suggested organization; update it to match the actual project files.*

## 7. Installation and Setup

### Prerequisites

Install the following software:

- Java Development Kit (JDK 17 or the version required by the project).
- PostgreSQL.
- Apache Maven.
- Git.
- Postman (for API testing).

### Step 1: Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/KLH-CSE-2027-Team-18-Adaptive_Zero_Trust_Network_Access.git
```

### Step 2: Navigate to the Project

```bash
cd KLH-CSE-2027-Team-18-Adaptive_Zero_Trust_Network_Access
```

If the Spring Boot application is inside the `ztna-system` folder:

```bash
cd ztna-system
```

### Step 3: Configure PostgreSQL

Create the project database:

```sql
CREATE DATABASE ztna_database;
```

Configure the database connection in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ztna_database
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
```

Replace the database credentials with your local PostgreSQL credentials. Do not commit real passwords to GitHub.

### Step 4: Build the Project

```bash
mvn clean install
```

### Step 5: Run the Application

```bash
mvn spring-boot:run
```

Alternatively:

```bash
mvn clean package
java -jar target/ztna-system-*.jar
```

### Step 6: Access the Application

Open your browser and navigate to:

```text
http://localhost:8080
```

## 8. API Testing

The following endpoints can be used for testing the application:

| HTTP Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/users/register` | Register a new user |
| POST | `/api/access/request` | Submit an access request |

Additional endpoints can be included as the application modules are completed.

Use Postman to test authentication, access requests, authorization decisions, and security responses.

## 9. Testing and Evaluation

The application can be evaluated using the following parameters:

- Authentication correctness.
- Authorization decision correctness.
- Access request response time.
- Failed-login lockout functionality.
- Network and device context verification.
- Role-based permission enforcement.
- Session reassessment.
- Security audit-log completeness.

Testing is performed using JUnit 5 and Postman.

## 10. Expected Outcomes

- Improved access security through context-aware authorization.
- Reduced unauthorized access through default-deny policies.
- Better protection against repeated failed login attempts.
- Controlled administrative privilege assignment.
- Improved visibility into user and system activities.
- Centralized security monitoring and auditing.

## 11. Future Enhancements

- Integration with additional device posture verification techniques.
- Support for more network environments.
- Advanced resource-level access policies.
- Improved scalability for enterprise environments.
- Integration with centralized security monitoring platforms.
- Extended testing under large-scale access-request workloads.

## 12. Current Project Status

**Current Phase:** Update this section with your actual implementation and evaluation phase.

The CAR-ZTNA application is being developed using Spring Boot and PostgreSQL. Authentication, role-based authorization, contextual access evaluation, and security monitoring are the primary implementation areas.

## 13. References

1. S. Rose et al., "Zero Trust Architecture," NIST SP 800-207, 2020.
2. C. Buck et al., "Never Trust, Always Verify: A Multivocal Literature Review on Current Knowledge and Research Gaps of Zero-Trust," *Computers & Security*, 2021.
3. N. F. Syed et al., "Zero Trust Architecture (ZTA): A Comprehensive Survey," *IEEE Access*, 2022.
4. P. García-Teodoro et al., "A Novel Zero-Trust Network Access Control Scheme Based on the Security Profile of Devices and Users," *Computer Networks*, 2022.
5. S. Xiao et al., "SoK: Context and Risk Aware Access Control for Zero Trust Systems," *Security and Communication Networks*, 2022.

---

**Repository:** KLH-CSE-2027-Team-18-Adaptive_Zero_Trust_Network_Access

**Project Domain:** Cybersecurity – Zero Trust Network Access Control
