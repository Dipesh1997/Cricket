const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8080;
const APK_FILE = path.join(__dirname, 'cricket-auction-debug.apk');

const server = http.createServer((req, res) => {
    if (req.url === '/download' || req.url === '/cricket-auction-debug.apk') {
        if (!fs.existsSync(APK_FILE)) {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            return res.end('APK file not found.');
        }
        const stat = fs.statSync(APK_FILE);
        res.writeHead(200, {
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Disposition': 'attachment; filename="cricket-auction-debug.apk"',
            'Content-Length': stat.size
        });
        return fs.createReadStream(APK_FILE).pipe(res);
    }

    // Landing Page with Download Button
    const fileSizeMB = fs.existsSync(APK_FILE) 
        ? (fs.statSync(APK_FILE).size / (1024 * 1024)).toFixed(1) + ' MB'
        : 'Unknown';

    const html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Download Cricket Auction APK</title>
    <style>
        * { box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        body { background: #121816; color: #ECEFF1; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 20px; text-align: center; }
        .card { background: #1B2320; border: 1px solid #2E7D32; border-radius: 20px; padding: 32px 24px; max-width: 420px; width: 100%; box-shadow: 0 8px 32px rgba(0,0,0,0.5); }
        .icon { font-size: 56px; margin-bottom: 16px; }
        h1 { margin: 0 0 8px; font-size: 24px; color: #FFFFFF; }
        p { color: #CFD8DC; font-size: 14px; line-height: 1.5; margin: 0 0 24px; }
        .btn { display: inline-block; background: #2E7D32; color: #FFFFFF; font-size: 17px; font-weight: bold; text-decoration: none; padding: 16px 28px; border-radius: 12px; transition: background 0.2s; box-shadow: 0 4px 14px rgba(46,125,50,0.4); }
        .btn:hover { background: #1B5E20; }
        .meta { margin-top: 20px; font-size: 12px; color: #90A4AE; }
        .steps { text-align: left; background: #25312C; border-radius: 12px; padding: 16px; margin-top: 24px; font-size: 13px; color: #B0BEC5; }
        .steps ol { margin: 8px 0 0; padding-left: 20px; }
        .steps li { margin-bottom: 6px; }
    </style>
</head>
<body>
    <div class="card">
        <div class="icon">🏏</div>
        <h1>Cricket Auction App</h1>
        <p>Your custom auction APK is ready for testing on Android!</p>
        <a href="/download" class="btn">📥 Download APK (${fileSizeMB})</a>
        <div class="meta">Package: com.cricket.auction (v1.0 Debug)</div>
        <div class="steps">
            <strong>📲 Installation Instructions:</strong>
            <ol>
                <li>Tap <b>Download APK</b> above.</li>
                <li>When prompted in Chrome or browser, tap <i>Download anyway</i>.</li>
                <li>Tap the downloaded file and select <b>Install</b> (enable <i>Install unknown apps</i> if prompted).</li>
            </ol>
        </div>
    </div>
</body>
</html>`;

    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(html);
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Server listening on http://0.0.0.0:${PORT}`);
});
