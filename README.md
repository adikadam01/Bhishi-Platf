# 🪙 Digital Bhishi Platform
### Full-Stack Group Savings Management System (Simulation)
**Stack:** Spring Boot 3.2 + MongoDB + React 18 + JWT + Razorpay (Test Mode)

---

## 📁 Project Structure

```
bhishi-platform/
├── backend/          ← Spring Boot (Java 17)
│   ├── pom.xml
│   └── src/main/java/com/bhishi/
│       ├── Foundation.java         (main entry + exceptions + utilities)
│       ├── model/Models.java       (all 9 MongoDB documents)
│       ├── repository/             (all 9 repositories)
│       ├── security/               (JWT + Spring Security)
│       ├── auth/                   (Phase 2 - registration + login)
│       ├── group/                  (Phase 3 - group management)
│       ├── payment/                (Phase 4 - payments + Razorpay)
│       ├── payout/                 (Phase 5 - all 3 payout algorithms)
│       ├── urgency/                (Phase 6 - voting + early payout)
│       └── scheduler/              (Phase 7 - automated jobs)
├── frontend/         ← React 18
│   ├── package.json
│   └── src/
│       ├── App.js                  (router + role-based routing)
│       ├── context/AuthContext.js  (global JWT state)
│       ├── services/api.js         (all API calls to backend)
│       ├── components/shared/      (Navbar + Sidebar + Layout)
│       └── pages/
│           ├── auth/               (Login + 3-step Register)
│           ├── member/             (Dashboard, Groups, Payments, Payouts, Urgency)
│           ├── groupadmin/         (Dashboard, Create, Members, Payments, Payout)
│           └── superadmin/         (Dashboard, Approve Groups, Scheduler)
└── schema/
    └── 01_mongodb_schema.js        (MongoDB collections + indexes)
```

---

## ✅ Prerequisites — Install These First

| Tool | Version | Download |
|------|---------|---------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org/download.cgi |
| Node.js | 18+ | https://nodejs.org |
| MongoDB | 6+ | https://www.mongodb.com/try/download/community |
| VS Code | Latest | https://code.visualstudio.com |

**Recommended VS Code Extensions:**
- Extension Pack for Java (Microsoft)
- Spring Boot Extension Pack
- ES7+ React/Redux/React-Native snippets
- MongoDB for VS Code

---

## 🚀 Setup & Run — Step by Step

### STEP 1 — Open Project in VS Code

```bash
# Unzip the downloaded file, then open in VS Code
code bhishi-platform
```

---

### STEP 2 — Start MongoDB

**Option A — MongoDB installed locally:**
```bash
# Windows
mongod

# Mac
brew services start mongodb-community

# Linux
sudo systemctl start mongod
```

**Option B — MongoDB Atlas (cloud, free tier):**
1. Go to https://cloud.mongodb.com → create free cluster
2. Get connection string → paste in `backend/src/main/resources/application.properties`:
```properties
spring.data.mongodb.uri=mongodb+srv://<user>:<pass>@cluster.mongodb.net/bhishi_db
```

---

### STEP 3 — Setup MongoDB Schema (optional but recommended)

```bash
# Open MongoDB Shell and run the schema file
mongosh bhishi_db < schema/01_mongodb_schema.js
```

---

### STEP 4 — Configure Backend

Open `backend/src/main/resources/application.properties` and update:

```properties
# MongoDB (update if using Atlas)
spring.data.mongodb.uri=mongodb://localhost:27017/bhishi_db

# JWT secret (change in production)
app.jwt.secret=bhishi_super_secret_key_minimum_256_bits_change_in_production_env

# Razorpay TEST mode keys (get from https://dashboard.razorpay.com)
razorpay.key.id=rzp_test_XXXXXXXXXXXXXXXX
razorpay.key.secret=XXXXXXXXXXXXXXXXXXXXXXXX

# Gmail SMTP (for email notifications)
spring.mail.username=your-gmail@gmail.com
spring.mail.password=your-app-password   # Use Gmail App Password, not your real password
```

> 📝 To get Gmail App Password: Gmail → Settings → Security → 2-Step Verification → App Passwords

---

### STEP 5 — Run Backend

**Option A — VS Code terminal:**
```bash
cd backend
mvn spring-boot:run
```

**Option B — VS Code Spring Boot Dashboard:**
- Open Command Palette → `Spring Boot Dashboard: Run`

**Option C — Package and run:**
```bash
cd backend
mvn clean package -DskipTests
java -jar target/bhishi-platform-1.0.0.jar
```

✅ Backend runs at: **http://localhost:8080**
✅ Swagger UI at:   **http://localhost:8080/swagger-ui.html**

---

### STEP 6 — Run Frontend

Open a **new terminal** in VS Code:

```bash
cd frontend
npm install
npm start
```

✅ Frontend runs at: **http://localhost:3000**

> The `"proxy": "http://localhost:8080"` in `package.json` forwards all `/api` calls to the backend automatically — no CORS issues.

---

## 👤 Creating Your First Super Admin

The first user must be set up directly in MongoDB (no signup flow for Super Admin):

```bash
mongosh bhishi_db
```

```javascript
// Insert Super Admin user
db.users.insertOne({
  name: "Super Admin",
  email: "superadmin@bhishi.com",
  phone: "+919999999999",
  passwordHash: "$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TiGX9q1FRXP4K7d4wVqjWRZq8R5i",
  // ↑ This is bcrypt hash of "Admin@1234" — change after first login
  role: "SUPER_ADMIN",
  status: "ACTIVE",
  termsAccepted: true,
  createdAt: new Date(),
  updatedAt: new Date()
})
```

Then login at http://localhost:3000/login with:
- Phone: `+919999999999`
- Use Password login: `Admin@1234`

---

## 🔄 Typical User Flow

```
1. Member registers (3-step) at /register
2. Group Admin creates a group at /admin/groups/create
3. Super Admin approves group at /super/groups  ← generates group code
4. Members join using group code at /member/groups
5. Group Admin approves join requests at /admin/groups/:id/members
6. Members pay monthly at /member/payments/:id
7. Group Admin initiates payout at /admin/groups/:id/payout
8. Winner notified by email automatically
```

---

## 🧪 Testing Razorpay (Test Mode)

The platform is set to **TEST MODE** — no real money moves.

When paying, click **"[TEST] Simulate Payment"** — this bypasses the real Razorpay checkout and marks the payment as confirmed.

Test card (for when you integrate real Razorpay checkout):
- Card: `4111 1111 1111 1111`
- Expiry: Any future date
- CVV: Any 3 digits

---

## 📡 Key API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register/step1` | Step 1 registration |
| POST | `/api/auth/otp/verify` | Verify OTP |
| POST | `/api/auth/login/otp` | OTP login |
| POST | `/api/auth/login/password` | Password login |
| GET  | `/api/auth/me` | Get logged-in user |
| POST | `/api/groups` | Create group |
| POST | `/api/groups/join` | Join group by code |
| POST | `/api/super-admin/groups/action` | Approve/reject group |
| POST | `/api/payments/order` | Create payment order |
| POST | `/api/payments/verify` | Confirm payment |
| POST | `/api/payouts/initiate` | Start payout cycle |
| POST | `/api/payouts/bidding/bid` | Place bid |
| POST | `/api/urgency/raise` | Raise urgency request |
| POST | `/api/urgency/vote` | Cast vote |

Full docs: **http://localhost:8080/swagger-ui.html**

---

## ⚠️ Common Issues & Fixes

| Problem | Fix |
|---------|-----|
| `Port 8080 already in use` | `lsof -i :8080` then `kill -9 <PID>` |
| `MongoDB connection refused` | Make sure `mongod` is running |
| `npm install fails` | Delete `node_modules` + `package-lock.json`, run again |
| `CORS error` | Make sure backend is running on 8080 and proxy is set in `package.json` |
| `JWT invalid` | Clear localStorage in browser and login again |
| `OTP not received` | Check backend logs — OTP is printed to console in dev mode |
| `Email not sending` | Check Gmail app password in `application.properties` |

---

## 📦 Build for Production

**Backend:**
```bash
cd backend
mvn clean package -DskipTests
# Deploy target/bhishi-platform-1.0.0.jar to Render / AWS / Railway
```

**Frontend:**
```bash
cd frontend
npm run build
# Deploy build/ folder to Vercel / Netlify
```

---

*Digital Bhishi Platform — Simulation only. Not a regulated financial service.*
#   B h i s h i - P l a t f o r m  
 #   B h i s h i - P l a t f  
 