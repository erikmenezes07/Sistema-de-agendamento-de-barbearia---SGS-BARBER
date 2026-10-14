const http = require("http");
const fs = require("fs");
const path = require("path");

const root = __dirname;
const mime = {
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml"
};

http.createServer((request, response) => {
  let pathname = decodeURIComponent(request.url.split("?")[0]);
  if (pathname === "/") pathname = "/index.html";
  const file = path.resolve(root, `.${pathname}`);
  if (!file.startsWith(root)) {
    response.writeHead(403);
    return response.end("Forbidden");
  }
  fs.readFile(file, (error, content) => {
    if (error) {
      response.writeHead(404);
      return response.end("Not found");
    }
    response.writeHead(200, { "Content-Type": mime[path.extname(file)] || "application/octet-stream" });
    response.end(content);
  });
}).listen(5500, "0.0.0.0", () => {
  console.log("Frontend disponível na porta 5500 em todas as interfaces de rede.");
});
