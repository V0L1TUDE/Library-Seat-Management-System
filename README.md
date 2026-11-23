# Smart Library Seat Management System

An enterprise-grade, modern desktop application built for university library seat management. Designed as an academic capstone/mini-project demonstrating all concepts required by the university Java curriculum.

---

## 1. Project Overview

The **Smart Library Seat Management System** provides real-time seating visibility, self-service desk reservations, interactive check-in/out workflows, automated no-show auto-release, and administrative analytics for university libraries.

### Key Capabilities:
* **Interactive Floor Plan Grid:** Visual seating pods with real-time status indicators (🟢 Available, 🟡 Reserved, 🔴 Occupied) and facility badges (🔌 Power Socket, 🪟 Window View, 🖥️ PC Workstation).
* **Strict Overlap Prevention:** Mathematical time-slot validation preventing double bookings:
  $$\text{Overlap} \iff (\text{NewStart} < \text{ExistingEnd}) \land (\text{NewEnd} > \text{ExistingStart})$$
* **No-Show Detection & Grace Period:** 15-minute grace period after slot start; unattended reservations automatically transition to `NO_SHOW` and release seats.
* **Smart Rule-Based Recommendation:** Scores and ranks available seats based on student study preferences.
* **Live Analytics & Reports:** Occupancy metrics, JavaFX `PieChart` visual capacity breakdown, and CSV report export.
* **Dual Theme Engine:** Clean Academic Light theme (default) and Deep Forest Dark theme with session persistence via Java Object Serialization.

---

## 2. Technology Stack & Architecture

| Layer | Technologies Used |
| :--- | :--- |
| **Language** | Java 17+ (Compiled on Oracle JDK 26) |
| **User Interface** | JavaFX 21 (Controls, FXML, Graphics, Charts, CSS3) |
| **Database** | MySQL Server 8.0+ |
| **Database Access** | JDBC with Data Access Object (DAO) Pattern |
| **Build Tool** | Apache Maven 3.9+ (Wrapper included via `mvnw.cmd`) |
| **Testing** | JUnit 5 Jupiter |
| **Security** | Salted SHA-256 Cryptographic Password Hashing |

### Architecture: 3-Tier Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 PRESENTATION LAYER (JavaFX)                 │
│      FXML Views, Custom CSS Themes, UI Controllers          │
├─────────────────────────────────────────────────────────────┤
│                  BUSINESS / SERVICE LAYER                   │
│   BookingService, SeatService, AuthService, Recommender     │
├─────────────────────────────────────────────────────────────┤
│                 DATA ACCESS LAYER (DAO Pattern)             │
│   UserDAO, ZoneDAO, SeatDAO, BookingDAO, ActivityLogDAO     │
│       • PreparedStatements  • JDBC Transactions             │
├─────────────────────────────────────────────────────────────┤
│                    DATABASE LAYER (MySQL)                   │
│      Foreign Keys, Unique Constraints, Indexed Lookups      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Java Syllabus Concepts Mapping

Every syllabus concept is explicitly demonstrated in the codebase:

| Java Syllabus Requirement | Implementation in Project | File Reference |
| :--- | :--- | :--- |
| **Abstraction** | `abstract class User` with common identity and abstract polymorphic `getRoleDisplayName()` | [User.java](file:///c:/Users/Arjan%20Singh%20Sawhney/OneDrive/Desktop/LMS/src/main/java/com/library/seatmgmt/model/User.java) |
| **Encapsulation** | Private fields, getters, setters, invariant checks | Domain models in `model/` |
| **Inheritance** | `Student extends User` and `Admin extends User` | [Student.java](file:///c:/Users/Arjan%20Singh%20Sawhney/OneDrive/Desktop/LMS/src/main/java/com/library/seatmgmt/model/Student.java), [Admin.java](file:///c:/Users/Arjan%20Singh%20Sawhney/OneDrive/Desktop/LMS/src/main/java/com/library/seatmgmt/model/Admin.java) |
| **Polymorphism** | Overridden methods, dynamic method dispatch in collections | `getRoleDisplayName()`, `toString()` |
| **Interfaces** | Clean contracts separating business logic from MySQL implementations | `dao/` package interfaces |
| **Custom Exceptions** | Hierarchy with `LibraryException` as root, `BookingConflictException`, `SeatUnavailableException`, etc. | `exception/` package |
| **Collections & Generics** | Generic collections (`List<Seat>`, `Map<Integer, Booking>`), custom `Comparator` | `RecommendationService.java`, DAOs |
| **Date & Time API** | Modern `LocalDate`, `LocalTime`, `LocalDateTime`, `Duration` without legacy Date | `Booking.java`, `ValidationUtil.java` |
| **File I/O & CSV Export** | Writing formatted spreadsheet reports with `BufferedWriter` & `FileWriter` | [FileUtil.java](file:///c:/Users/Arjan%20Singh%20Sawhney/OneDrive/Desktop/LMS/src/main/java/com/library/seatmgmt/util/FileUtil.java) |
| **Object Serialization** | Persisting user theme preferences across sessions using `ObjectOutputStream` / `ObjectInputStream` | [SerializationUtil.java](file:///c:/Users/Arjan%20Singh%20Sawhney/OneDrive/Desktop/LMS/src/main/java/com/library/seatmgmt/util/SerializationUtil.java) |
| **Logging** | Rotating application audit log files using `java.util.logging.Logger` | [LoggerUtil.java](file:///c:/Users/Arjan%20Singh%20Sawhney/OneDrive/Desktop/LMS/src/main/java/com/library/seatmgmt/util/LoggerUtil.java) |
| **JDBC Transactions** | Atomic booking creation with `setAutoCommit(false)`, `commit()`, and `rollback()` | [BookingDAOImpl.java](file:///c:/Users/Arjan%20Singh%20Sawhney/OneDrive/Desktop/LMS/src/main/java/com/library/seatmgmt/dao/impl/BookingDAOImpl.java) |

---

## 4. Setup and Execution Instructions

### Step 1: Database Setup (MySQL)
1. Ensure your MySQL server service is running.
2. Open MySQL Workbench, MySQL CLI, or your preferred SQL tool.
3. Execute the provided SQL script:
   ```powershell
   mysql -u root -p < schema.sql
   ```
4. Verify or update your database credentials in:
   `src/main/resources/config/db.properties`
   ```properties
   db.url=jdbc:mysql://localhost:3306/library_seat_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   db.username=root
   db.password=YOUR_PASSWORD
   ```

### Step 2: Running Automated Unit Tests
To run the automated JUnit 5 test suite verifying conflict algorithms, serialization, file I/O, and scoring:
```powershell
.\mvnw.cmd test
```

### Step 3: Launching the Desktop JavaFX Application
To launch the native desktop GUI:
```powershell
.\mvnw.cmd javafx:run
```
*(Alternatively, open the project in **IntelliJ IDEA** and run `src/main/java/com/library/seatmgmt/Main.java`).*

### Step 4: Web Browser Preview (Optional)
A companion preview server is available for instant inspection in any web browser:
```powershell
java -cp target/classes com.library.seatmgmt.util.WebPreviewServer
```
Open: **http://localhost:8080**

---

## 5. Default Demonstration Accounts

| Role | Username | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Student** | `student1` | `password123` | Reserve seats, test check-in/out, view personal history |
| **Administrator** | `admin` | `password123` | View library capacity, inspect live PieChart, trigger no-show auto-release |

---

## 6. Viva Preparation: Top 25 Questions & Answers

### 1. What architecture does this project use?
> *"The project uses a standard 3-Tier Layered Architecture consisting of a Presentation Layer (JavaFX FXML and CSS), a Service Layer (business rules and validations), a Data Access Layer (DAO Pattern with JDBC), and a Relational Database Layer (MySQL). This enforces the Single Responsibility Principle and loose coupling."*

### 2. Why did you use the DAO (Data Access Object) design pattern?
> *"The DAO pattern decouples business logic from persistence logic. If the database schema changes or if we switch from MySQL to PostgreSQL, only the DAO implementation classes change; the Service and Controller layers remain untouched."*

### 3. How do you prevent overlapping bookings?
> *"We use both software logic and database locking. Mathematically, two time intervals $[S_1, E_1)$ and $[S_2, E_2)$ overlap if and only if $(S_2 < E_1) \land (E_2 > S_1)$. In our `BookingDAOImpl`, we execute a query within an atomic transaction with `conn.setAutoCommit(false)`. If any active booking exists for that seat during the requested window, we roll back and throw `BookingConflictException`."*

### 4. What is the check-in grace period and how does No-Show detection work?
> *"A student can check in starting 15 minutes before their slot up until 15 minutes after the start time. If 15 minutes have passed without check-in, the `NoShowScannerService` flags the reservation as `NO_SHOW` and immediately frees the seat for other students."*

### 5. Where is Object Serialization used in your project?
> *"We use Java Object Serialization in `SerializationUtil` to save and restore `UserPreferences` (such as Dark/Light mode theme state and remembered username) into a binary file `data/user_preferences.ser` using `ObjectOutputStream` and `ObjectInputStream`."*

### 6. Where is File Handling implemented?
> *"File handling is implemented in `FileUtil` using `BufferedWriter` and `FileWriter` wrapped in `try-with-resources` to export student bookings and audit records into structured `.csv` files."*

### 7. How are passwords stored securely?
> *"Passwords are never stored in plain text. We hash passwords using SHA-256 with a unique cryptographic salt in `PasswordUtil.hashPassword()`, generating a secure 64-character hexadecimal digest."*

### 8. How did you implement polymorphism in this project?
> *"Both `Student` and `Admin` inherit from the abstract class `User`. They override the abstract method `getRoleDisplayName()`. When a collection of `User` objects is iterated, the JVM dynamically invokes the subclass-specific method at runtime."*

### 9. Why use PreparedStatement instead of Statement in JDBC?
> *"PreparedStatement pre-compiles SQL queries on the database server, improving performance for repetitive executions. Crucially, it completely prevents SQL Injection attacks by safely parameterizing user inputs."*

### 10. Why is try-with-resources used in your DAO methods?
> *"Try-with-resources automatically closes `Connection`, `PreparedStatement`, and `ResultSet` objects when the block exits, even if an exception occurs, completely preventing memory leaks and database connection exhaustion."*
