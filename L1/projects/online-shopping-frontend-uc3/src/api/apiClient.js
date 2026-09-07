/**
 * This is the one shared place where every network call to our backend
 * services goes through. Every hook and component in this app uses these
 * functions instead of calling {@code fetch} directly, so there's only
 * one place to update if an API URL or error format ever changes.
 * <p>
 * Every endpoint called here matches a real endpoint that the backend
 * team has already defined and agreed on — nothing here is a made-up or
 * guessed URL. If a new endpoint is ever needed, it should be added to
 * the backend's API contract first, and only then called from here.
 */

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

  // A request can be cancelled two ways: our own timeout above, or an
  // AbortSignal a caller passes in (for example, when a component
  // unmounts before the request finishes). Either one aborts the same
  // underlying fetch, which stops an old, no-longer-needed response from
  // ever coming back and overwriting newer state.
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

// Talks to the Product Catalog service: listing, looking up, and searching products.
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

// Talks to the Cart service: reading a user's cart and adding, updating, or removing items in it.
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

// Talks to the Order Management service: placing a new order.
export const OrderApi = {
  create: (order, signal) => request("/api/v1/orders", { method: "POST", body: order, signal })
};

export { ApiError };
