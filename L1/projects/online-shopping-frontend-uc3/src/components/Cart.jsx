import { useCart } from "../hooks/useCart";
import LoadingSpinner from "./LoadingSpinner";
import ErrorMessage from "./ErrorMessage";

/**
 * Cart page/component.
 * WCAG 2.1 AA: quantity changes use a labelled <select>, removal buttons have
 * a descriptive accessible name via aria-label rather than an icon alone.
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

      {/* RISK NOTE: subtotal shown here is CART-computed (see cart-service),
          not authoritative — order-management-service revalidates price/stock
          again before creating the order (see CheckoutForm.jsx and
          ui-risk/ui-risk-report.md, "Stale price/stock" risk). */}
    </section>
  );
}

function formatCurrency(value) {
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(value);
}
