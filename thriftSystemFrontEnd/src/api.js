// In development Vite proxies /api to the backend (see vite.config.js). For a deployed
// build, set VITE_API_BASE_URL, e.g. https://api.example.com/api/v1
const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api/v1";

// Calls the backend and returns the `data` field of its ApiResponse, or throws an Error
// carrying the server's message
export async function api(path, { token, method = "GET", body } = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  let payload = null;
  try {
    payload = await res.json();
  } catch {
    // Some errors (e.g. 401/403 from the security filter) have no JSON body
  }

  if (!res.ok || !payload?.success) {
    throw new Error(payload?.message || `Request failed (${res.status})`);
  }
  return payload.data;
}
