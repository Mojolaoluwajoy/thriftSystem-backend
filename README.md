
# Thrift Cooperative Management System

A production-grade REST API for managing thrift cooperative groups built with Java Spring Boot and MongoDB.

## Features

- User authentication with JWT
- Thrift group management
- Contribution tracking
- Automated payout scheduling
- Paystack payment integration
- Real-time currency conversion
- Nigerian public holiday awareness
- Email notifications
- WhatsApp contact integration
- Role-based access control (Super Admin, Admin, Member)

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.5
- **Database:** MongoDB
- **Authentication:** JWT
- **Payment:** Paystack
- **Currency:** ExchangeRate-API
- **Public Holidays:** Nager.Date API
- **HTTP Client:** OpenFeign
- **Containerization:** Docker
- **Documentation:** Swagger UI

## Prerequisites

- Java 17
- Docker Desktop
- Maven

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/Mojolaoluwajoy/thriftSystem-backend.git
cd thriftSystem-backend
```

### 2. Configure environment variables
```bash
cp .env.example .env
```
Fill in your credentials in `.env`

### 3. Start MongoDB
```bash
docker-compose up -d
```

### 4. Run the application
```bash
mvn spring-boot:run
```

### 5. Access Swagger UI

http://localhost:8080/swagger-ui/index.html

## Environment Variables

| Variable | Description |
|---|---|
| `MONGO_USERNAME` | MongoDB username |
| `MONGO_PASSWORD` | MongoDB password |
| `MONGO_DATABASE` | MongoDB database name |
| `JWT_SECRET` | JWT secret key (min 64 chars) |
| `JWT_EXPIRATION` | JWT expiration in milliseconds |
| `MAIL_USERNAME` | Gmail address |
| `MAIL_PASSWORD` | Gmail app password |
| `PAYSTACK_SECRET_KEY` | Paystack secret key |
| `PAYSTACK_BASE_URL` | Paystack base URL |
| `EXCHANGE_RATE_API_KEY` | ExchangeRate-API key |
| `EXCHANGE_RATE_BASE_URL` | ExchangeRate-API base URL |
| `SUPPORT_WHATSAPP` | Support WhatsApp number |
| `SUPER_ADMIN_EMAIL` | Super admin email |
| `SUPER_ADMIN_PASSWORD` | Super admin password |

## API Endpoints

### Authentication
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Register new user | Public |
| POST | `/api/v1/auth/login` | Login | Public |

### Users
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/v1/users/me` | Get current user | Required |
| PATCH | `/api/v1/users/me/whatsapp` | Update WhatsApp | Required |
| PATCH | `/api/v1/users/me/profile-picture` | Update profile picture | Required |
| GET | `/api/v1/users/{id}` | Get user by ID | Required |
| GET | `/api/v1/users` | Get all users | Super Admin |

### Thrift Groups
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/groups` | Create group | Required |
| GET | `/api/v1/groups/my-groups` | Get my groups | Required |
| GET | `/api/v1/groups/{groupId}` | Get group by ID | Required |
| POST | `/api/v1/groups/{groupId}/members/{userId}` | Add member | Admin |
| GET | `/api/v1/groups` | Get all groups | Super Admin |

### Contributions
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/contributions` | Initiate contribution | Required |
| GET | `/api/v1/contributions/my-contributions` | Get my contributions | Required |
| GET | `/api/v1/contributions/group/{groupId}` | Get group contributions | Required |

### Payments
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/payments/initialize/{contributionId}` | Initialize payment | Required |

### Payouts
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/payouts/schedule` | Schedule payout | Admin |
| POST | `/api/v1/payouts/{payoutId}/process` | Process payout | Admin |
| GET | `/api/v1/payouts/my-payouts` | Get my payouts | Required |
| GET | `/api/v1/payouts/group/{groupId}` | Get group payouts | Required |

### Currencies
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/v1/currencies` | Get active currencies | Required |
| GET | `/api/v1/currencies/convert` | Convert currency | Required |

### Webhook
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/webhook/paystack` | Paystack webhook | Public |

## Running Tests
```bash
mvn test
```

## License
MIT
