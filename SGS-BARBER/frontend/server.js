const http = require("node:http");
const fs = require("node:fs/promises");
const path = require("node:path");
const root = __dirname;
const mime = {
  ".html": "text/html; charset=utf-8", ".js": "application/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8", ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml", ".png": "image/png", ".jpg": "image/jpeg", ".ico": "image/x-icon"
};
// Sirva apenas arquivos públicos; código do servidor e configurações ficam privados.
const publicFiles = new Set([
  "index.html", "login-cliente.html", "login-barbeiro.html",
  "dashboard-cliente.html", "dashboard-barbeiro.html", "style.css", "config.js", "api.js", "app.js"
]);
function createServer() {
  return http.createServer(async (request, response) => {
    if (!["GET", "HEAD"].includes(request.method)) {
      response.writeHead(405, { Allow: "GET, HEAD" }); return response.end();
    }
    let pathname;
    try { pathname = decodeURIComponent(request.url.split("?")[0]); }
    catch { response.writeHead(400); return response.end("Bad request"); }
    if (pathname === "/") pathname = "/index.html";
    const name = pathname.slice(1);
    if (!publicFiles.has(name)) { response.writeHead(404); return response.end("Not found"); }
    try {
      const content = await fs.readFile(path.join(root, name));
      response.writeHead(200, { "Content-Type": mime[path.extname(name)] || "application/octet-stream", "X-Content-Type-Options": "nosniff" });
      response.end(request.method === "HEAD" ? undefined : content);
    } catch {
      response.writeHead(404); response.end("Not found");
    }
  });
}
if (require.main === module) {
  const port = Number(process.env.PORT || 5500);
  const host = process.env.HOST || "127.0.0.1";
  createServer().listen(port, host, () => console.log(`Frontend: http://${host}:${port}`));
}
module.exports = { createServer };
