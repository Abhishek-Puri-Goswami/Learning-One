// L1 UC3 - Single API client shared by all hooks/components.
//
// IMPORTANT (UI risk mitigation - "avoid hallucinated APIs"): every endpoint
// used here corresponds 1:1 to an operationId defined in the contract-first
// OpenAPI specs from L1/UC2 (product-service.yaml, cart-service.yaml) or the
// order-management-service api_boundaries defined in L1/UC1 architecture.json.
// No endpoint is invented; if a new endpoint is needed, it must be added to
// the relevant OpenAPI spec first (contract-first discipline).

const DEFAULT_TIMEOUT_MS = 8000;

class ApiError extends Error {
  constructor(message, status, details) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details ?? [];
  }
}

async function request(path, { method = "GET", body, signal } = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS);

  // Support an external AbortSignal (e.g. from a component unmount) as well
  // as our own timeout-based one, so callers can cancel in-flight requests -
  // this is the race-condition mitigation documented in ui-risk/ui-risk-report.md.
  if (signal) {
    signal.addEventListener("abort", () => controller.abort(), { once: true });
  }

  try {
    const response = await fetch(path, {
      method,
      headers: body ? { "Content-Type": "application/json" } : undefined,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal
    });

    if (!response.ok) {
      let errorBody = null;
      try {
        errorBody = await response.json();
      } catch {
        // response had no JSON body; fall through with generic message
      }
      throw new ApiError(
        errorBody?.message ?? `Request failed with status ${response.status}`,
        response.status,
        errorBody?.details
      );
    }

    if (response.status === 204) return null;
    return await response.json();
  } finally {
    clearTimeout(timeout);
  }
}

// --- Product Catalog Service (matches openapi/product-service.yaml) ---
export const ProductApi = {
  list: (params = {}, signal) => {
    const qs = new URLSearchParams(params).toString();
    return request(`/api/v1/products${qs ? `?${qs}` : ""}`, { signal });
  },
  getById: (id, signal) => request(`/api/v1/products/${encodeURIComponent(id)}`, { signal }),
  search: (q, params = {}, signal) => {
    const qs = new URLSearchParams({ q, ...params }).toString();
    return request(`/api/v1/products/search?${qs}`, { signal });
  }
};

// --- Cart Service (matches openapi/cart-service.yaml) ---
export const CartApi = {
  get: (userId, signal) => request(`/api/v1/cart/${encodeURIComponent(userId)}`, { signal }),
  addItem: (userId, item, signal) =>
    request(`/api/v1/cart/${encodeURIComponent(userId)}/items`, { method: "POST", body: item, signal }),
  updateItem: (userId, itemId, update, signal) =>
    request(`/api/v1/cart/${encodeURIComponent(userId)}/items/${encodeURIComponent(itemId)}`, {
      method: "PUT",
      body: update,
      signal
    }),
  removeItem: (userId, itemId, signal) =>
    request(`/api/v1/cart/${encodeURIComponent(userId)}/items/${encodeURIComponent(itemId)}`, {
      method: "DELETE",
      signal
    })
};

// --- Order Management Service (matches api_boundaries in L1/UC1 architecture.json) ---
export const OrderApi = {
  create: (order, signal) => request("/api/v1/orders", { method: "POST", body: order, signal })
};

export { ApiError };
