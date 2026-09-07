import { useCallback, useEffect, useState } from "react";
import { CartApi } from "../api/apiClient";

/**
 * A custom React hook that holds a shopping cart's data and the actions
 * that change it (add, update quantity, remove) for one user.
 * <p>
 * Two things worth understanding about how this hook stays safe from
 * bugs:
 * <ul>
 *   <li>Every change (add/update/remove) calls the backend first, and
 *       only THEN updates what's shown on screen — using the server's
 *       response as the new truth, rather than guessing what the cart
 *       looks like before the server confirms it. This matters because
 *       the backend, not this component, is the real source of truth for
 *       prices and stock — if we guessed locally and guessed wrong, the
 *       screen could show something that doesn't match reality.</li>
 *   <li>The {@code busy} flag disables the "Add to Cart" type buttons
 *       while a change is in flight, so a user double-clicking quickly
 *       can't accidentally submit the same action twice.</li>
 * </ul>
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
      setCart(updatedCart); // always replace the whole cart object, never edit the old one in place
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
