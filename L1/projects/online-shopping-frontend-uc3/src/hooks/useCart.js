import { useCallback, useEffect, useState } from "react";
import { CartApi } from "../api/apiClient";

/**
 * Cart state + mutating actions for a given userId.
 *
 * UI risk mitigations (see ui-risk/ui-risk-report.md):
 * - Every mutation (add/update/remove) calls the backend first and then
 *   replaces state with the server's authoritative response, rather than
 *   optimistically mutating local state. This avoids client/server drift,
 *   since Cart is explicitly NOT the system of record for price/stock
 *   (see L1/UC1 architecture.json - cart-service risks).
 * - `busy` flag disables the UI during a mutation to prevent double-submit
 *   race conditions (e.g. rapid double-click on "Add to Cart").
 */
export function useCart(userId) {
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(
    (signal) => {
      setLoading(true);
      setError(null);
      return CartApi.get(userId, signal)
        .then((data) => setCart(data))
        .catch((err) => {
          if (err.name !== "AbortError") setError(err);
        })
        .finally(() => setLoading(false));
    },
    [userId]
  );

  useEffect(() => {
    const controller = new AbortController();
    reload(controller.signal);
    return () => controller.abort();
  }, [reload]);

  const guardedMutation = useCallback(async (mutationFn) => {
    setBusy(true);
    setError(null);
    try {
      const updatedCart = await mutationFn();
      setCart(updatedCart); // replace, never mutate in place
      return updatedCart;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setBusy(false);
    }
  }, []);

  const addItem = useCallback(
    (productId, quantity) => guardedMutation(() => CartApi.addItem(userId, { productId, quantity })),
    [userId, guardedMutation]
  );

  const updateItemQuantity = useCallback(
    (itemId, quantity) => guardedMutation(() => CartApi.updateItem(userId, itemId, { quantity })),
    [userId, guardedMutation]
  );

  const removeItem = useCallback(
    (itemId) => guardedMutation(() => CartApi.removeItem(userId, itemId)),
    [userId, guardedMutation]
  );

  return { cart, loading, error, busy, addItem, updateItemQuantity, removeItem };
}
