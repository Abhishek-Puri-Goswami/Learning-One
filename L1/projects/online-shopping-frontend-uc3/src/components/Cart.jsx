import { useCart } from "../hooks/useCart";
import LoadingSpinner from "./LoadingSpinner";
import ErrorMessage from "./ErrorMessage";

/**
 * Shows everything currently in the user's cart: each item's name,
 * price, and quantity, plus a running subtotal. A shopper can change how
 * many of an item they want, or remove it entirely.
 * <p>
 * Two small but important accessibility touches: the quantity control is
 * a normal, labelled dropdown (not a custom widget a screen reader might
 * not understand), and each "Remove" button has a full description like
 * "Remove Blue T-Shirt from cart" attached to it — not just a bare
 * "Remove" that would be confusing when a page has several of them.
 */
export default function Cart({ userId = "guest" }) {
  const { cart, loading, error, busy, updateItemQuantity, removeItem } = useCart(userId);

  if (loading) return <LoadingSpinner label="Loading your cart…" />;
  if (error) return <ErrorMessage error={error} />;
  if (!cart || cart.items.length === 0) {
    return (
      <section aria-labelledby="cart-heading">
        <h2 id="cart-heading">Your Cart</h2>
        <p>Your cart is empty.</p>
      </section>
    );
  }

  return (
    <section aria-labelledby="cart-heading">
      <h2 id="cart-heading">Your Cart</h2>

      <table>
        <caption className="visually-hidden">Items in your cart</caption>
        <thead>
          <tr>
            <th scope="col">Product</th>
            <th scope="col">Unit price</th>
            <th scope="col">Quantity</th>
            <th scope="col">Line total</th>
            <th scope="col">
              <span className="visually-hidden">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {cart.items.map((item) => (
            <tr key={item.itemId}>
              <th scope="row">{item.productName}</th>
              <td>{formatCurrency(item.unitPrice)}</td>
              <td>
                <label htmlFor={`qty-${item.itemId}`} className="visually-hidden">
                  Quantity for {item.productName}
                </label>
                <select
                  id={`qty-${item.itemId}`}
                  value={item.quantity}
                  disabled={busy}
                  onChange={(e) => updateItemQuantity(item.itemId, Number(e.target.value))}
                >
                  {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </select>
              </td>
              <td>{formatCurrency(item.lineTotal)}</td>
              <td>
                <button
                  type="button"
                  disabled={busy}
                  aria-label={`Remove ${item.productName} from cart`}
                  onClick={() => removeItem(item.itemId)}
                >
                  Remove
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <p className="cart-subtotal" aria-live="polite">
        Subtotal: <strong>{formatCurrency(cart.subtotal)}</strong>
      </p>

      {/*
        Good to know: this subtotal is just what the cart currently
        thinks the price is. The order service double-checks the real
        price and stock again when the order is actually placed, so if a
        price changed in the meantime, the final order could differ
        slightly from what's shown here.
      */}
    </section>
  );
}

function formatCurrency(value) {
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(value);
}
