import { useMemo, useState } from "react";
import { useCart } from "../hooks/useCart";
import { OrderApi, ApiError } from "../api/apiClient";
import LoadingSpinner from "./LoadingSpinner";
import ErrorMessage from "./ErrorMessage";

const initialFormState = {
  fullName: "",
  addressLine1: "",
  city: "",
  postalCode: "",
  paymentMethod: "card"
};

/**
 * Checkout form.
 *
 * UI risk mitigations (see ui-risk/ui-risk-report.md):
 * - Client-generated idempotency key (crypto.randomUUID()) sent with the
 *   order creation request, so a network retry or accidental double-click
 *   cannot create two orders (mirrors the idempotency-key requirement
 *   documented for order-management-service in L1/UC1 TR-04).
 * - Submit button is disabled while a request is in flight ("submitting")
 *   AND after a successful submission, closing the double-submit race window.
 * - Validation errors are field-scoped and linked via aria-describedby +
 *   aria-invalid so screen reader users get the same signal sighted users
 *   get from red border/text (WCAG 2.1 AA 3.3.1 Error Identification, 4.1.2).
 */
export default function CheckoutForm({ userId = "guest" }) {
  const { cart, loading: cartLoading } = useCart(userId);
  const [form, setForm] = useState(initialFormState);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [orderConfirmation, setOrderConfirmation] = useState(null);

  // Generated once per checkout attempt; regenerated only after a
  // successful order so a resubmit of the SAME failed attempt reuses the
  // same key (idempotent retry), while a fresh checkout gets a fresh key.
  const idempotencyKey = useMemo(() => crypto.randomUUID(), [orderConfirmation]);

  function handleChange(field) {
    return (event) => {
      const value = event.target.value;
      setForm((prev) => ({ ...prev, [field]: value })); // never mutate prev directly
    };
  }

  function validate(values) {
    const errors = {};
    if (!values.fullName.trim()) errors.fullName = "Full name is required.";
    if (!values.addressLine1.trim()) errors.addressLine1 = "Address is required.";
    if (!values.city.trim()) errors.city = "City is required.";
    if (!/^[0-9]{4,10}$/.test(values.postalCode.trim())) {
      errors.postalCode = "Enter a valid postal code (numbers only).";
    }
    return errors;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return; // guards a race from rapid double Enter/click

    const errors = validate(form);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    if (!cart || cart.items.length === 0) {
      setSubmitError(new Error("Your cart is empty."));
      return;
    }

    setSubmitting(true);
    setSubmitError(null);
    try {
      const order = await OrderApi.create({
        userId,
        idempotencyKey,
        shippingAddress: {
          fullName: form.fullName,
          addressLine1: form.addressLine1,
          city: form.city,
          postalCode: form.postalCode
        },
        paymentMethod: form.paymentMethod
        // NOTE: cart items are resolved server-side from the user's cart,
        // and price/stock are revalidated there -- the client never sends
        // prices, per ADR-003 (no client-trusted financial data) in L1/UC1.
      });
      setOrderConfirmation(order);
      setForm(initialFormState);
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err : new Error("Could not place order. Please try again."));
    } finally {
      setSubmitting(false);
    }
  }

  if (cartLoading) return <LoadingSpinner label="Loading checkout…" />;

  if (orderConfirmation) {
    return (
      <section aria-labelledby="checkout-confirmation-heading">
        <h2 id="checkout-confirmation-heading">Order placed!</h2>
        <p role="status">
          Thank you. Your order <strong>{orderConfirmation.id}</strong> has been received.
        </p>
      </section>
    );
  }

  return (
    <section aria-labelledby="checkout-heading">
      <h2 id="checkout-heading">Checkout</h2>

      <ErrorMessage error={submitError} />

      <form onSubmit={handleSubmit} noValidate>
        <FormField
          id="fullName"
          label="Full name"
          value={form.fullName}
          error={fieldErrors.fullName}
          onChange={handleChange("fullName")}
          autoComplete="name"
        />
        <FormField
          id="addressLine1"
          label="Address"
          value={form.addressLine1}
          error={fieldErrors.addressLine1}
          onChange={handleChange("addressLine1")}
          autoComplete="address-line1"
        />
        <FormField
          id="city"
          label="City"
          value={form.city}
          error={fieldErrors.city}
          onChange={handleChange("city")}
          autoComplete="address-level2"
        />
        <FormField
          id="postalCode"
          label="Postal code"
          value={form.postalCode}
          error={fieldErrors.postalCode}
          onChange={handleChange("postalCode")}
          autoComplete="postal-code"
          inputMode="numeric"
        />

        <fieldset>
          <legend>Payment method</legend>
          <label>
            <input
              type="radio"
              name="paymentMethod"
              value="card"
              checked={form.paymentMethod === "card"}
              onChange={handleChange("paymentMethod")}
            />
            Card
          </label>
          <label>
            <input
              type="radio"
              name="paymentMethod"
              value="upi"
              checked={form.paymentMethod === "upi"}
              onChange={handleChange("paymentMethod")}
            />
            UPI
          </label>
        </fieldset>

        <button type="submit" disabled={submitting} aria-busy={submitting}>
          {submitting ? "Placing order…" : "Place order"}
        </button>
      </form>
    </section>
  );
}

function FormField({ id, label, value, error, onChange, ...inputProps }) {
  const errorId = `${id}-error`;
  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        name={id}
        value={value}
        onChange={onChange}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
        {...inputProps}
      />
      {error && (
        <p id={errorId} className="field-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
