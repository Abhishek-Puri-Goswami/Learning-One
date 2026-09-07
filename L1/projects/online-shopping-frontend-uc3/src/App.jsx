import { BrowserRouter, Routes, Route, NavLink } from "react-router-dom";
import ProductListing from "./components/ProductListing";
import Cart from "./components/Cart";
import CheckoutForm from "./components/CheckoutForm";

// A stand-in user id shared by every page, since this demo app has no real login.
const DEMO_USER_ID = "guest";

/**
 * The root component of this online shopping app. It sets up the page's
 * three routes — the product listing (home page), the cart, and
 * checkout — and shares one demo user id across all of them, since this
 * app doesn't have a real login system.
 */
export default function App() {
  return (
    <BrowserRouter>
      {/*
        This hidden-until-focused link lets someone navigating with a
        keyboard (instead of a mouse) jump straight to the main content,
        skipping past the repeated header/nav links on every page —
        an accessibility best practice for keyboard and screen-reader users.
      */}
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
