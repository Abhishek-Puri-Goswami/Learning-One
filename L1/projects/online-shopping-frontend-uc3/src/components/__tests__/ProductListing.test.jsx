import { render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { axe } from "jest-axe";
import ProductListing from "../ProductListing";

const samplePage = {
  content: [
    {
      id: "p1",
      name: "Wireless Mouse",
      description: "Ergonomic wireless mouse",
      price: 19.99,
      category: "Electronics",
      stockQuantity: 5
    }
  ],
  page: 0,
  size: 12,
  totalElements: 1,
  totalPages: 1
};

describe("ProductListing", () => {
  beforeEach(() => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => samplePage
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows a loading state, then renders fetched products", async () => {
    render(<ProductListing />);
    expect(screen.getByRole("status")).toBeInTheDocument(); // LoadingSpinner

    await waitFor(() => expect(screen.getByText("Wireless Mouse")).toBeInTheDocument());
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/products"),
      expect.any(Object)
    );
  });

  it("renders an accessible error state when the API call fails", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ message: "Internal Server Error" })
    });

    render(<ProductListing />);
    await waitFor(() => expect(screen.getByRole("alert")).toBeInTheDocument());
    expect(screen.getByText(/Internal Server Error/i)).toBeInTheDocument();
  });

  it("has no detectable accessibility violations (axe)", async () => {
    const { container } = render(<ProductListing />);
    await waitFor(() => expect(screen.getByText("Wireless Mouse")).toBeInTheDocument());

    const results = await axe(container);
    expect(results.violations).toHaveLength(0);
  });
});
