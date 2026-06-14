# 💊 PharmacyApp — Online Pharmacy Order Management System

A full-featured **desktop pharmacy application** built with **Java** and **JavaFX** that simulates a real-world online pharmacy experience. Users can browse medicines, add them to a cart, place orders with delivery or pickup options, and manage their complete order history — all through a modern, visually polished GUI.

---

## 📸 Features at a Glance

| Feature | Description |
|---------|-------------|
| 🔐 **User Authentication** | Sign up & login with email/password validation |
| 💊 **Medicine Inventory** | Browse 10 medicines with search functionality |
| 🛒 **Shopping Cart** | Add/remove medicines, adjust quantities, apply voucher codes |
| 📦 **Checkout System** | Choose between **Delivery** or **Pickup** fulfillment |
| 🚚 **Delivery Details** | Full address form, date/time scheduling, saved addresses |
| 💳 **Payment Options** | Cash on Delivery or Credit/Debit Card |
| 📋 **Order History** | View all past orders with filtering, search, cancellation & deletion |
| 💾 **Data Persistence** | All user data, orders, and addresses survive app restarts |

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Core programming language |
| **JavaFX** | 21-ea+24 | GUI framework for desktop UI |
| **Maven** | 3.x (via Maven Wrapper) | Build tool & dependency management |
| **FormsFX** | 11.6.0 | Form utility library |
| **JUnit 5** | 5.9.2 | Unit testing framework |
| **Flat-File Database** | — | Text-based persistence (`pharmacy_users.txt`) |

---

## 📁 Project Structure

```
PharmacyApp/
├── .mvn/                          # Maven Wrapper configuration
├── src/
│   └── main/
│       ├── java/com/example/pharmacyapp/
│       │   ├── PharmacyApp.java   # Main application class (UI + logic)
│       │   ├── User.java          # User model (auth, cart, order history)
│       │   ├── Medicine.java      # Medicine model with JavaFX properties
│       │   ├── Order.java         # Order model (medicines, status, date)
│       │   └── Pharmacy.java      # Pharmacy utility (CRUD for medicines)
│       └── resources/
│           ├── background.jpg     # App background image
│           ├── medicine_bottle.png # Splash screen image
│           ├── style.css          # Base stylesheet
│           └── META-INF/
│               └── module-info    # Java module descriptor
├── pom.xml                        # Maven build configuration
├── mvnw / mvnw.cmd                # Maven Wrapper (no Maven install needed)
├── .gitignore
└── README.md
```

---

## 🏗️ Architecture & Design

### Object-Oriented Design (OOP Principles Used)

| Principle | Implementation |
|-----------|---------------|
| **Encapsulation** | All model classes (`User`, `Medicine`, `Order`) use private fields with public getters/setters |
| **Abstraction** | `Pharmacy` class provides static utility methods for medicine CRUD operations |
| **Composition** | `Order` contains a `User` and a `List<Medicine>`; `User` contains an `ObservableList<Order>` |
| **Separation of Concerns** | Models handle data, `PharmacyApp` handles UI rendering and event logic |

### Data Persistence

The app uses a flat-file database stored at:
```
{USER_HOME}/pharmacy_users.txt
```

Records are stored in CSV format with tagged line prefixes:
```
USER,name,address,phone,email,password
ORDER,email,status,timestamp,totalPrice,med1:qty:price;med2:qty:price;
ADDRESS,email,name;;phone;;street;;area;;city
```

---

## 🚀 How to Run

### Prerequisites

- **Java Development Kit (JDK) 21** or later
  - Download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
  - Verify installation:
    ```bash
    java --version
    ```
  - Ensure `JAVA_HOME` is set to your JDK installation path.

> **Note:** You do **NOT** need to install Maven. The project includes a Maven Wrapper (`mvnw`) that automatically downloads the correct Maven version.

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/AroojFatima512/Pharmacy.git
   cd Pharmacy
   ```

2. **Run the application**

   **On Windows:**
   ```cmd
   .\mvnw clean javafx:run
   ```

   **On macOS / Linux:**
   ```bash
   chmod +x mvnw
   ./mvnw clean javafx:run
   ```

3. **Wait for the build** — Maven will automatically download all dependencies on the first run (this may take 1–2 minutes).

4. **Use the app** — A splash screen appears, followed by the Sign Up / Login screen.

### Demo Credentials (Pre-loaded)

If a `pharmacy_users.txt` file already exists in your home directory, you can log in with:

| Email | Password |
|-------|----------|
| `arooj@gmail.com` | `12345678` |

Otherwise, simply **Sign Up** with a new account to get started.

---

## 📖 Application Walkthrough

### 1. Splash Screen
A branded loading screen with the app logo and a progress animation.

### 2. Authentication
- **Sign Up**: Create an account with name, email, phone, and password (with confirmation).
- **Login**: Enter email and password to access your account.
- Duplicate email registration is prevented.

### 3. Medicine Inventory
- Browse a list of **10 medicines** (Panadol, Neurofen, Amoxilin, Ventolin, Zyrtec, Aspirin, Lipitor, Prozac, Nexium, Synthroid).
- **Search** by name or description.
- **Select quantities** using a spinner control.
- **Add to cart** individual medicines or proceed to checkout.

### 4. Cart Summary
- View all added medicines with quantity controls (+/−).
- See per-item subtotals and a running grand total.
- **Apply voucher code** `DISCOUNT10` for a 10% discount.
- Remove items with the trash icon.

### 5. Checkout
- Choose between **Delivery** 🚚 or **Pickup** 🏪 fulfillment.
- View a complete order summary before confirming.

### 6. Delivery Details (if Delivery is selected)
- Enter or select a **saved address**.
- Pick a **delivery date** (future dates only) and **time slot**.
- Choose **payment method**: Cash on Delivery or Credit/Debit Card.
- Addresses can be saved for future use.

### 7. Order Success Screen
- Confirmation with order details, user info, and total amount.
- **Four action buttons:**
  - 🔄 **Browse Again** — Return to the inventory
  - 📋 **View History** — See all past orders
  - ❌ **Cancel Order** — Immediately cancel the just-placed order
  - 🚪 **Logout** — Sign out

### 8. Order History
- **Full history** of all orders persisted across app restarts.
- **Search** by medicine name or order ID.
- **Filter by date range**: All Time, Last 2 Days, Last 7 Days, Last 30 Days, Previous Month, 2024, 2025.
- **Cancel orders** (Placed or Pickup status, within 48 hours of placement).
- **Permanently delete** any order with the trash icon.
- Status badges: ✅ Placed, 🏪 Pickup, 📦 Shipped, ✔️ Delivered, ❌ Cancelled.

---

## 🎨 UI / UX Highlights

- **Modern glassmorphism design** with semi-transparent cards and soft shadows.
- **Background-threaded icon preloading** for sub-second screen transitions.
- **Rich iconography** using [Icons8](https://icons8.com/) color icons.
- **Responsive layouts** that adapt to content dynamically.
- **Gradient buttons**, hover effects, and micro-animations for a premium feel.

---

## 🗄️ Database Schema

The flat-file database (`pharmacy_users.txt`) uses three record types:

### USER Record
```
USER,<name>,<address>,<phone>,<email>,<password>
```

### ORDER Record
```
ORDER,<email>,<status>,<unix_timestamp_ms>,<total_price>,<med1_name>:<qty>:<unit_price>;...
```
- **Status values**: `Placed`, `Pickup`, `Shipped`, `Delivered`, `Cancelled`

### ADDRESS Record
```
ADDRESS,<email>,<name>;;<phone>;;<street>;;<area>;;<city>
```

---

## 🧪 Running Tests

```bash
.\mvnw test
```

---

## 📝 Notes

- The application requires an **internet connection** on first launch to download dependency JARs and to load medicine/status icons from Icons8 CDN.
- The database file is created automatically in your **home directory** (`~/pharmacy_users.txt`) on first use.
- All icons are cached in memory after first load, so subsequent screen transitions are instant.

---

## 📄 License

This project was developed as part of the **Object-Oriented Programming (OOP)** course at **COMSATS University** — Fall 2023 semester.

---

## 👤 Author

**Arooj Fatima**  
COMSATS University Islamabad  
[GitHub: @AroojFatima512](https://github.com/AroojFatima512)
