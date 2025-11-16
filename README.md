# Brokerage Service

A production-grade Spring Boot application for managing stock orders and assets for a brokerage firm. This service allows employees and customers to create, list, and delete stock orders, as well as manage customer assets.

## Features

- **Order Management**: Create, list, and cancel stock orders
- **Asset Management**: List and track customer assets
- **Authentication**: JWT-based authentication with role-based access control (Admin and Customer)
- **Order Matching**: Admin endpoint to match pending orders
- **Transaction Safety**: Proper transaction management with pessimistic locking
- **API Documentation**: Swagger/OpenAPI integration
- **Global Exception Handling**: Comprehensive error handling
- **Validation**: Request validation using Jakarta Validation

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.7**
- **Spring Security** (JWT authentication)
- **Spring Data JPA**
- **H2 Database** (In-memory)
- **MapStruct** (DTO mapping)
- **Lombok** (Boilerplate reduction)
- **Swagger/OpenAPI** (API documentation)
- **Maven** (Build tool)

## Prerequisites

- Java 21 or higher
- Maven 3.6+ (or use the included Maven wrapper)

## Building the Project

### Using Maven Wrapper (Recommended)

```bash
./mvnw clean install
```

### Using Maven

```bash
mvn clean install
```

## Running the Application

### Using Maven Wrapper

```bash
./mvnw spring-boot:run
```

### Using Maven

```bash
mvn spring-boot:run
```

### Using Java

After building the project:

```bash
java -jar target/brokage-service-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Documentation

Once the application is running, you can access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## H2 Database Console

Access the H2 console at: http://localhost:8080/h2-console

- **JDBC URL**: `jdbc:h2:mem:brokage_db`
- **Username**: `sa`
- **Password**: (leave empty)

## Default Users

The application comes with pre-configured users (password: `customer123` for customers, `admin123` for admin):

- **Admin**: 
  - Username: `admin`
  - Password: `admin123`
  - Role: `ADMIN`

- **Customer 1**:
  - Username: `customer1`
  - Password: `customer123`
  - Role: `CUSTOMER`
  - Initial TRY: 100,000.00
  - Assets: AAPL (100 shares), GOOGL (50 shares)

- **Customer 2**:
  - Username: `customer2`
  - Password: `customer123`
  - Role: `CUSTOMER`
  - Initial TRY: 50,000.00
  - Assets: MSFT (75 shares)

## API Endpoints

### Authentication

#### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "customer1",
  "password": "customer123"
}
```

Response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "customer1",
    "role": "CUSTOMER"
  }
}
```

### Orders

All order endpoints require authentication. Include the JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

#### Create Order
```
POST /api/orders
Content-Type: application/json

{
  "customerId": 2,
  "assetName": "AAPL",
  "orderSide": "BUY",
  "size": 10.00,
  "price": 150.00
}
```

#### List Orders
```
GET /api/orders?customerId=2&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59&status=PENDING
```

Query Parameters:
- `customerId` (required): Customer ID
- `startDate` (optional): Start date (ISO 8601 format)
- `endDate` (optional): End date (ISO 8601 format)
- `status` (optional): Order status (PENDING, MATCHED, CANCELED)

#### Delete Order (Cancel)
```
DELETE /api/orders/{orderId}
```

#### Match Order (Admin Only)
```
POST /api/orders/match
Content-Type: application/json

{
  "orderId": 1
}
```

### Assets

#### List Assets
```
GET /api/assets?customerId=2&assetName=AAPL
```

Query Parameters:
- `customerId` (required): Customer ID
- `assetName` (optional): Filter by asset name

## Business Logic

### Order Creation

- Orders are created with `PENDING` status
- For `BUY` orders: Validates that customer has enough TRY (usableSize)
- For `SELL` orders: Validates that customer has enough of the asset being sold (usableSize)
- Reserves the required amount by reducing `usableSize`

### Order Cancellation

- Only `PENDING` orders can be canceled
- Releases the reserved assets by increasing `usableSize` back

### Order Matching

- Only `PENDING` orders can be matched
- For `BUY` orders:
  - Deducts TRY from `size` (already deducted from `usableSize` when created)
  - Adds the bought asset to customer's portfolio
- For `SELL` orders:
  - Deducts the asset from `size` (already deducted from `usableSize` when created)
  - Adds TRY to customer's portfolio

### Authorization

- **Customers**: Can only access and manipulate their own data
- **Admins**: Can access and manipulate all customers' data
- All endpoints (except login) require authentication

## Project Structure

```
src/main/java/com/inghubs/brokage_service/
├── config/              # Configuration classes (Security, Swagger, JWT)
├── controller/          # REST controllers
├── dto/                 # Data Transfer Objects (Request/Response)
│   ├── request/
│   └── response/
├── exception/           # Custom exceptions and global exception handler
├── mapper/              # MapStruct mappers
├── model/               # Entity models and enums
│   ├── entity/
│   └── enums/
├── repository/          # JPA repositories
├── service/             # Business logic services
└── util/                # Utility classes
```

## Testing

Run tests using:

```bash
./mvnw test
```

## Production Considerations

For production deployment, consider:

1. **Database**: Replace H2 with PostgreSQL, MySQL, or another production database
2. **JWT Secret**: Use a strong, randomly generated secret key (at least 256 bits)
3. **Password Hashing**: Already using BCrypt (recommended)
4. **Connection Pooling**: Configure appropriate connection pool settings
5. **Logging**: Configure proper logging (e.g., Logback with file rotation)
6. **Monitoring**: Add monitoring and health checks (Spring Actuator already included)
7. **Security**: Enable HTTPS, configure CORS properly
8. **Error Handling**: Consider masking sensitive error messages in production

## License

This project is a demo application for Ing Hubs.

