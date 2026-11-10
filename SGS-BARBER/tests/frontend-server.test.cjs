const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const http = require("node:http");
const { createServer } = require("../frontend/server.js");
let server, port;
before(async () => {
  server = createServer();
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  port = server.address().port;
});
after(() => new Promise(resolve => server.close(resolve)));
function request(path, method = "GET") {
  return new Promise((resolve, reject) => {
    const req = http.request({ host: "127.0.0.1", port, path, method }, res => {
      let body = "";
      res.on("data", chunk => body += chunk);
      res.on("end", () => resolve({ status: res.statusCode, headers: res.headers, body }));
    });
    req.on("error", reject); req.end();
  });
}
test("página inicial e arquivos usados pelas páginas", async () => {
  const root = await request("/");
  assert.equal(root.status, 200); assert.match(root.body, /SGS BARBER/);
  for (const name of ["config.js", "api.js", "app.js", "style.css", "dashboard-cliente.html", "dashboard-barbeiro.html", "login-cliente.html", "login-barbeiro.html"]) {
    assert.equal((await request("/" + name)).status, 200, name);
  }
});
test("não expõe servidor, arquivos privados ou caminhos externos", async () => {
  for (const path of ["/server.js", "/package.json", "/.env", "/../backend/pom.xml", "/%2e%2e/backend/pom.xml", "/inexistente"]) {
    assert.equal((await request(path)).status, 404, path);
  }
});
test("URL inválida retorna 400 e servidor continua atendendo", async () => {
  assert.equal((await request("/%ZZ")).status, 400);
  assert.equal((await request("/")).status, 200);
});
test("HEAD sem corpo e POST recusado", async () => {
  assert.equal((await request("/", "HEAD")).body, "");
  assert.equal((await request("/", "POST")).status, 405);
});
