# SpendWise Web Portal & APK Download Service

A Google Antigravity-inspired web landing page and official APK distribution service for **SpendWise — Smart Bank & UPI Expense Tracker**.

---

## 🌟 Features
- **Antigravity Dark Aesthetics**: Deep obsidian backdrop, mesh cyber grid, and glowing emerald & cyan neon accents.
- **Direct APK Distribution**: Direct `/download` and `/SpendWise.apk` download routes serving the latest compiled Android package.
- **Live Metadata API**: `/api/app-info` endpoint providing version number, build number, features, and file size.
- **Interactive Feature Breakdown**: Highlights 100% on-device SMS parsing, password-protected statements, and multi-user Google Sheets sync.
- **Zero External Dependencies**: Pure Node.js native server running without heavy frameworks or setup overhead.

---

## 🚀 How to Deploy on Render.com (Free Web Service)

1. **Push your project to GitHub**:
   ```bash
   git add .
   git commit -m "Add SpendWise Web Portal with APK download"
   git push origin main
   ```

2. **Create New Web Service on Render**:
   - Go to [dashboard.render.com](https://dashboard.render.com/) → Click **New +** → **Web Service**.
   - Connect your GitHub repository.
   - Configure the following settings:
     * **Name**: `spendwise-app`
     * **Root Directory**: `web`
     * **Runtime**: `Node`
     * **Build Command**: `npm install` (or leave empty)
     * **Start Command**: `npm start` (or `node server.js`)
     * **Instance Type**: `Free`

3. **Automatic Updates**:
   - Whenever you compile a new APK (`SpendWise.apk`), replace `web/public/SpendWise.apk` and push to GitHub.
   - Render will automatically re-deploy the updated portal and serve the latest APK to all visitors!

---

## 💻 Local Development

Run locally:
```bash
node web/server.js
```
Open in browser: `http://localhost:3000`
Direct download link: `http://localhost:3000/download`
