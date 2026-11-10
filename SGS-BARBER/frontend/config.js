const today = new Date();
const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
window.SGS_CONFIG = {
  // Para hospedagem externa, informe a URL HTTPS real do backend.
  API_BASE_URL: `${window.location.protocol}//${window.location.hostname}:8080`,
  USE_API: true,
  STORAGE_PREFIX: "sgs_v4_",
  DEFAULT_DATE: localDate
};
