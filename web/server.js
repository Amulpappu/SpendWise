const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.apk': 'application/vnd.android.package-archive'
};

function getApkSize() {
  const apkPath = path.join(PUBLIC_DIR, 'SpendWise.apk');
  try {
    if (fs.existsSync(apkPath)) {
      const stats = fs.statSync(apkPath);
      return (stats.size / (1024 * 1024)).toFixed(1) + ' MB';
    }
  } catch (e) {
    console.error('Error getting APK size:', e);
  }
  return '37.5 MB';
}

const server = http.createServer((req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;

  // 1. API Endpoint: App Info
  if (pathname === '/api/app-info') {
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({
      appName: 'SpendWise',
      tagline: 'Smart Bank & UPI Expense Tracker',
      version: '2.4.0',
      buildNumber: 24,
      releaseDate: 'August 2026',
      fileSize: getApkSize(),
      downloadUrl: '/download',
      directApkUrl: '/SpendWise.apk',
      compatibility: 'Android 8.0+ (Oreo to Android 15)',
      features: [
        '⚡ 100% On-Device Bank SMS Auto-Detection',
        '📊 Automated Multi-User Google Sheets Live Sync',
        '🔒 Password Protected Official Bank Statements (PDF & Excel)',
        '🏦 Smart Detection for 25+ Top Indian Banks (SBI, HDFC, ICICI, TMB, etc.)',
        '🔐 Offline Sandbox Privacy Guarantee (Zero Cloud Leaks)'
      ]
    }));
    return;
  }

  // 2. Direct Download Route
  if (pathname === '/download' || pathname === '/SpendWise.apk') {
    const apkPath = path.join(PUBLIC_DIR, 'SpendWise.apk');
    if (fs.existsSync(apkPath)) {
      const stat = fs.statSync(apkPath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': 'attachment; filename="SpendWise.apk"',
        'Content-Length': stat.size
      });
      if (req.method === 'HEAD') {
        res.end();
      } else {
        fs.createReadStream(apkPath).pipe(res);
      }
      return;
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('SpendWise.apk not found on server.');
      return;
    }
  }

  // 3. Static File Serving
  let filePath = path.join(PUBLIC_DIR, pathname === '/' ? 'index.html' : pathname);
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(PUBLIC_DIR, 'index.html');
  }

  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(500, { 'Content-Type': 'text/plain' });
      res.end('Server Error: ' + err.code);
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    }
  });
});

server.listen(PORT, () => {
  console.log(`=========================================`);
  console.log(`🚀 SpendWise Web Server running on port ${PORT}`);
  console.log(`🌐 Local URL: http://localhost:${PORT}`);
  console.log(`📥 Download APK: http://localhost:${PORT}/download`);
  console.log(`=========================================`);
});
