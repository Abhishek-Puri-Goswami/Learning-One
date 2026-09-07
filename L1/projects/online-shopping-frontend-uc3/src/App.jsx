import { BrowserRouter, Routes, Route, NavLink } from "react-router-dom";
import ProductListing from "./components/ProductListing";
import Cart from "./components/Cart";
import CheckoutForm from "./components/CheckoutForm";

const DEMO_USER_ID = "guest";

export default function App() {
  return (
    <BrowserRouter>
      {/* Skip link for keyboard users (WCAG 2.1 AA - 2.4.1 Bypass Blocks) */}
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>

      <header>
        <nav aria-label="Primary">
          <NavLink to="/" end>
            Shop
          </NavLink>
          <NavLink to="/cart">Cart</NavLink>
          <NavLink to="/checkout">Checkout</NavLink>
        </nav>
      </header>

      <main id="main-content">
        <Routes>
          <Route path="/" element={<ProductListing userId={DEMO_USER_ID} />} />
          <Route path="/cart" element={<Cart userId={DEMO_USER_ID} />} />
          <Route path="/checkout" element={<CheckoutForm userId={DEMO_USER_ID} />} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}
