MyBooks - Personal Library & Notes Management System

MyBooks is a backend application that allows users to manage their personal book library, track reading status, and write notes for each book.

The project is built with Spring Boot and uses SQLite as the database. It follows a layered architecture and includes JWT-based authentication, role-based authorization, file upload support, and Flyway database migrations.

This project is designed as a real-world backend system suitable for learning modern backend development practices.

FEATURES

User registration and login system (JWT Authentication)

Role-based authorization (USER / ADMIN)

Add, update, delete, and list books

Track reading status (READING, COMPLETED, WANT_TO_READ)

Add notes for each book

Upload book cover images

Global exception handling

Custom validation (username validation)

Automatic database migrations with Flyway

RESTful API architecture

Simple HTML frontend pages (login, register, dashboard)

TECHNOLOGIES USED

Java 21

Spring Boot

Spring Security + JWT

Spring Data JPA (Hibernate)

SQLite

Flyway Migration

Maven

REST API

HTML / CSS

PROJECT ARCHITECTURE

Package structure:

com.example.mybooks

config -> Security and filter configuration

controller -> REST API endpoints

dto -> Request and response models

exception -> Global exception handling

model -> Entity classes

repository -> JPA repository layer

security -> JWT services and filters

service -> Business logic layer

validation -> Custom validation

DATABASE

The application uses SQLite as the database.
The database file is created automatically when the application starts.

Database file:
mybooks.db

Flyway migration files:

V1__create_users_table.sql

V2__create_books_table.sql

V3__add_role_to_users.sql

V4__create_notes_table.sql

Main tables:

users

books

notes

AUTHENTICATION SYSTEM

The application uses JWT (JSON Web Token) authentication.

Flow:

User registers

User logs in

Server returns a JWT token

All protected requests must include the token in the Authorization header

Header format:

Authorization: Bearer JWT_TOKEN

API ENDPOINTS

AUTH

POST /api/auth/register -> Register new user
POST /api/auth/login -> Login and receive JWT

BOOKS

GET /api/books -> Get all books
POST /api/books -> Add new book
PUT /api/books/{id} -> Update book
DELETE /api/books/{id} -> Delete book

NOTES

GET /api/notes/book/{bookId} -> Get notes for a book
POST /api/notes -> Add new note

FILE UPLOAD

POST /api/upload/book-cover -> Upload book cover image

SETUP

Requirements:

Java 21

Maven

SQLite does not require any additional installation.

RUNNING THE APPLICATION

Clone the project

Build with Maven

mvn clean install

Run the application

mvn spring-boot:run

The SQLite database file will be created automatically.

APPLICATION URLS

Home Page:
http://localhost:8080

Login Page:
http://localhost:8080/login.html

Register Page:
http://localhost:8080/register.html

Dashboard:
http://localhost:8080/dashboard.html

FILE UPLOADS

Uploaded book cover images are stored in:

/uploads/book-covers

LEARNING OUTCOMES

This project demonstrates:

JWT authentication and authorization

Spring Security configuration

REST API design

SQLite integration with JPA

Flyway database migration management

Layered backend architecture

File upload handling

Global exception handling

Validation mechanisms

TESTING

The project includes automated tests to verify core business logic, security, and application behavior.

Unit Tests

Service layer tests using mocked repositories (Mockito)

UserService tests for:

Password hashing

Input validation

Entity validation tests

Integration Tests

Authentication flow tests (register, login, protected endpoints)

JWT-based access control verification

Ensuring users cannot access other users’ data

MVC tests for secured endpoints

CSRF token validation for form submissions

Tests are located under:

src/test/java

To run tests locally:

mvn test

CI / CD (GitHub Actions)

This project uses GitHub Actions for Continuous Integration.

Workflow Features

Automatically triggered on push and pull_request

Runs mvn test

Build fails if any test fails

Generates test and build logs for verification

Workflow Location

.github/workflows/ci.yml

Optional Quality Tools

JaCoCo code coverage reporting

Code style and static analysis tools (e.g. Checkstyle / SpotBugs)

The CI pipeline ensures code quality and prevents broken builds from being merged.
