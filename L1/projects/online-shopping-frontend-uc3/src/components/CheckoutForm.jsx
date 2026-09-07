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
 * The checkout page: collects a shipping address and payment method,
 * validates them, and places the order.
 * <p>
 * A few things worth understanding here:
 * <ul>
 *   <li>Before sending the order, we generate a random
 *       "idempotency key" — a one-time id attached to this specific
 *       checkout attempt. If the network hiccups and the same request
 *       gets sent twice, or the user accidentally double-clicks
 *       "Place order," the backend can recognize it's the same attempt
 *       and avoid creating two separate orders.</li>
 *   <li>The submit button disables itself both WHILE the order is being
 *       placed and again AFTER it succeeds, closing the same
 *       double-submit gap from the other direction.</li>
 *   <li>Validation errors are attached to their specific field using
 *       {@code aria-describedby} and {@code aria-invalid}, so a screen
 *       reader user gets the same "this field has a problem" signal that
 *       a sighted user gets from red text and a red border.</li>
 * </ul>
 */
export default function CheckoutForm({ userId = "guest" }) {
  const { cart, loading: cartLoading } = useCart(userId);
  const [form, setForm] = useState(initialFormState);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [orderConfirmation, setOrderConfirmation] = useState(null);

  // This key is created once per checkout attempt. If the order fails and the
  // user tries again with the SAME attempt, this stays the same key (so the
  // backend still sees it as a retry, not a new order) — it only changes to a
  // fresh value once an order has actually gone through successfully.
  const idempotencyKey = useMemo(() => crypto.randomUUID(), [orderConfirmation]);

  function handleChange(field) {
    return (event) => {
      const value = event.target.value;
      setForm((prev) => ({ ...prev, [field]: value })); // build a new object instead of editing prev in place
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
    if (submitting) return; // already submitting — ignore a second Enter/click before the first finishes

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
        // Notice we never send prices or item details here — the backend looks up
        // the user's cart itself and re-checks prices and stock there. The
        // frontend is never trusted to say how much something costs.
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
