import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { axe } from "jest-axe";
import CheckoutForm from "../CheckoutForm";

const sampleCart = {
  userId: "guest",
  items: [
    { itemId: "i1", productId: "p1", productName: "Wireless Mouse", unitPrice: 19.99, quantity: 1, lineTotal: 19.99 }
  ],
  itemCount: 1,
  subtotal: 19.99,
  updatedAt: new Date().toISOString()
};

describe("CheckoutForm", () => {
  beforeEach(() => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => sampleCart
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows field-level, screen-reader-linked validation errors on empty submit", async () => {
    const user = userEvent.setup();
    render(<CheckoutForm />);

    await waitFor(() => screen.getByRole("button", { name: /place order/i }));
    await user.click(screen.getByRole("button", { name: /place order/i }));

    const nameInput = screen.getByLabelText("Full name");
    expect(nameInput).toHaveAttribute("aria-invalid", "true");
    expect(nameInput).toHaveAttribute("aria-describedby", "fullName-error");
    expect(screen.getAllByRole("alert").length).toBeGreaterThan(0);
  });

  it("disables the submit button while a request is in flight (prevents double-submit)", async () => {
    let resolveOrder;
    global.fetch = vi
      .fn()
      .mockResolvedValueOnce({ ok: true, status: 200, json: async () => sampleCart }) // cart load
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveOrder = () =>
              resolve({ ok: true, status: 201, json: async () => ({ id: "ORD-1" }) });
          })
      );

    const user = userEvent.setup();
    render(<CheckoutForm />);
    await waitFor(() => screen.getByLabelText("Full name"));

    await user.type(screen.getByLabelText("Full name"), "Anaya Sharma");
    await user.type(screen.getByLabelText("Address"), "12 MG Road");
    await user.type(screen.getByLabelText("City"), "Bengaluru");
    await user.type(screen.getByLabelText("Postal code"), "560001");

    const submitButton = screen.getByRole("button", { name: /place order/i });
    await user.click(submitButton);

    expect(submitButton).toBeDisabled();
    resolveOrder();
    await waitFor(() => expect(screen.getByText(/Order placed/i)).toBeInTheDocument());
  });

  it("has no detectable accessibility violations (axe)", async () => {
    const { container } = render(<CheckoutForm />);
    await waitFor(() => screen.getByLabelText("Full name"));
    const results = await axe(container);
    expect(results.violations).toHaveLength(0);
  });
});
