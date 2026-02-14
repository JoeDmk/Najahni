# NAJAHNI - Business & Entrepreneurship Platform

## PIDEV JavaFX Desktop Application

A comprehensive business and entrepreneurship management platform built with JavaFX and JDBC for academic validation.

---

## 📋 Project Overview

**NAJAHNI** is a desktop application designed to manage:
- **Users** (Entrepreneurs and Investors)
- **Projects** (Business ventures created by entrepreneurs)
- **Investment Opportunities** (Investment proposals for projects)

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming Language |
| JavaFX | 17.0.2 | Desktop UI Framework |
| JDBC | - | Database Connectivity |
| MySQL | 8.0+ | Database |
| Maven | 3.8+ | Build Tool |

---

## 📁 Project Architecture

```
src/main/java/com/najahni/
├── MainApp.java              # Application entry point
├── controllers/              # JavaFX Controllers
│   ├── MainController.java
│   ├── DashboardController.java
│   ├── UserController.java
│   ├── ProjectController.java
│   └── InvestmentController.java
├── dao/                      # Data Access Objects (JDBC)
│   ├── GenericDAO.java
│   ├── UserDAO.java
│   ├── ProjectDAO.java
│   └── InvestmentOpportunityDAO.java
├── models/                   # Entity Classes
│   ├── User.java
│   ├── Project.java
│   ├── InvestmentOpportunity.java
│   ├── Role.java
│   ├── ProjectStatus.java
│   └── InvestmentStatus.java
├── services/                 # Business Logic Layer
│   ├── UserService.java
│   ├── ProjectService.java
│   └── InvestmentOpportunityService.java
└── utils/                    # Utility Classes
    ├── DBConnection.java     # Singleton Pattern
    └── AlertUtils.java

src/main/resources/
├── fxml/                     # FXML Views
│   ├── MainView.fxml
│   ├── DashboardView.fxml
│   ├── UserView.fxml
│   ├── ProjectView.fxml
│   └── InvestmentView.fxml
└── css/
    └── styles.css            # Application Styling

sql/
└── schema.sql                # Database Schema
```

---

## 🗄️ Database Setup

### 1. Install MySQL
Ensure MySQL is installed and running on your system.

### 2. Create Database
Run the SQL schema file:

```bash
mysql -u root -p < sql/schema.sql
```

Or manually execute the contents of `sql/schema.sql` in MySQL Workbench.

### 3. Configure Connection
Update database credentials in `src/main/java/com/najahni/utils/DBConnection.java`:

```java
private static final String URL = "jdbc:mysql://localhost:3306/najahni_db";
private static final String USER = "root";
private static final String PASSWORD = "";  // Update with your password
```

---

## 🚀 Running the Application

### Using Maven

```bash
# Navigate to project directory
cd pidev_java

# Compile the project
mvn clean compile

# Run the application
mvn javafx:run
```

### Using IDE (IntelliJ IDEA / Eclipse)

1. Import as Maven project
2. Wait for dependencies to download
3. Run `MainApp.java`

---

## ✨ Features

### Dashboard
- Platform statistics overview
- Quick actions for common tasks
- Recent projects table

### User Management
- Create, Read, Update, Delete users
- Filter by role (Entrepreneur/Investor)
- Search by name or email
- Input validation with error messages

### Project Management
- Full CRUD operations
- Status management (Draft, Pending, Approved, Rejected, Funded)
- Sector categorization
- Link to entrepreneur

### Investment Management
- Track investment opportunities
- Status workflow (Pending → Accepted → Completed)
- Filter by status and project
- Total investment calculations

---

## 📊 Entities

### User
| Field | Type | Description |
|-------|------|-------------|
| id | INT | Primary Key |
| name | VARCHAR(100) | User's full name |
| email | VARCHAR(150) | Unique email address |
| password | VARCHAR(255) | User password |
| role | ENUM | ENTREPRENEUR or INVESTOR |

### Project
| Field | Type | Description |
|-------|------|-------------|
| id | INT | Primary Key |
| title | VARCHAR(200) | Project title |
| description | TEXT | Project description |
| sector | VARCHAR(100) | Business sector |
| status | ENUM | DRAFT, PENDING, APPROVED, REJECTED, FUNDED |
| entrepreneur_id | INT | Foreign Key to User |

### InvestmentOpportunity
| Field | Type | Description |
|-------|------|-------------|
| id | INT | Primary Key |
| amount | DECIMAL(15,2) | Investment amount |
| status | ENUM | PENDING, ACCEPTED, REJECTED, COMPLETED |
| project_id | INT | Foreign Key to Project |

---

## 🎨 Design Patterns Used

1. **Singleton Pattern** - DBConnection class ensures single database connection instance
2. **MVC Pattern** - Clear separation between Models, Views (FXML), and Controllers
3. **DAO Pattern** - Data access layer abstraction for database operations
4. **Service Layer** - Business logic separation from controllers

---

## ✅ Validation Rules

### User Validation
- Name: Required, 2-100 characters
- Email: Required, valid format, unique
- Password: Required, minimum 6 characters
- Role: Required

### Project Validation
- Title: Required, 3-200 characters
- Sector: Required
- Status: Required
- Entrepreneur: Required, must have ENTREPRENEUR role

### Investment Validation
- Amount: Required, must be positive number
- Status: Required
- Project: Required

---

## 🔧 Troubleshooting

### Database Connection Error
1. Verify MySQL is running
2. Check credentials in `DBConnection.java`
3. Ensure `najahni_db` database exists

### JavaFX Not Found
1. Ensure JavaFX dependencies are in `pom.xml`
2. Run `mvn clean install`

### Module Error
1. Check `module-info.java` exports
2. Ensure all packages are properly opened

---

## 👥 Academic Information

- **Project**: PIDEV - NAJAHNI
- **Sprint**: Java Desktop (JDBC Validation)
- **Type**: JavaFX Desktop Application

---

## 📝 License

This project is for academic purposes only.

---

**NAJAHNI** - Empowering Entrepreneurs, Connecting Investors 🚀
