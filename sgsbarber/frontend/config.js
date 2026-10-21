window.SGS_CONFIG = {
  API_BASE_URL: window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://localhost:8080"
    : `http://${window.location.hostname}:8080`,
  USE_API: true,
  STORAGE_PREFIX: "sgs_v4_",
  DEFAULT_DATE: new Date().toISOString().slice(0, 10)
};
