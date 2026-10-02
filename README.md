# Cargo Automation Backend

Backend service for a cargo automation system that manages users, addresses, shipments, and shipment price calculations.

## Features

* User registration and authentication
* JWT-based authentication
* User and address management
* Shipment creation and management
* Shipment price calculation based on shipment details
* City and district management
* Soft delete support
* RESTful API architecture
* Swagger API documentation

## Technologies

* **Java 21**
* **Spring Boot**
* **Spring Security**
* **JWT**
* **Spring Data JPA**
* **PostgreSQL**
* **Maven**
* **Swagger / OpenAPI**

## Project Structure

```text
src/main/java/
└── ...
    ├── controller
    ├── service
    ├── repository
    ├── model
    ├── dto
    ├── security
    └── exception
```

## Getting Started

### Prerequisites

* Java 21
* PostgreSQL
* Git

### Clone the Repository

```bash
git clone https://github.com/MirayBozdogan/cargo-automation-backend.git
cd cargo-automation-backend
```

### Database Configuration

Create a PostgreSQL database and configure the database connection in:

```text
src/main/resources/application.properties
```

Set your database URL, username, and password accordingly.

### Run the Application

Using the Maven Wrapper:

**macOS / Linux**

```bash
./mvnw spring-boot:run
```

**Windows**

```bash
mvnw.cmd spring-boot:run
```

The application will run on:

```text
http://localhost:8080
```

## API Documentation

After starting the application, Swagger UI can be used to explore and test the available API endpoints.

```text
http://localhost:8080/swagger-ui/index.html
```

## Authentication

The application uses **JWT (JSON Web Token)** for authentication.

Users can register and log in through the authentication endpoints. After successful login, the returned JWT is used to access protected endpoints.

## Main API Endpoints

| Resource       | Endpoint                    |
| -------------- | --------------------------- |
| Authentication | `/auth/register`            |
| Authentication | `/auth/login`               |
| Users          | `/users`                    |
| Addresses      | `/users/{user_id}/address`  |
| Cities         | `/city`                     |
| Districts      | `/district`                 |
| Shipment Price | `/shipment/calculate-price` |
| Shipments      | `/shipment`                 |

## Related Repository

### Frontend

The frontend of the project is available here:

[Cargo Automation Frontend](https://github.com/MirayBozdogan/cargo-automation-frontend?utm_source=chatgpt.com)

## Author

**Miray Bozdoğan**

Computer Engineering Student

[GitHub](https://github.com/MirayBozdogan?utm_source=chatgpt.com) · [LinkedIn](https://www.linkedin.com/in/miraybozdoğan?utm_source=chatgpt.com)
