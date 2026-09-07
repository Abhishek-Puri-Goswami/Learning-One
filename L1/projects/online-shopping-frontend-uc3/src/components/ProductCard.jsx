/**
 * One product tile in the product grid: shows its name, description,
 * price, and stock level, with an "Add to cart" button. The button
 * automatically disables itself and shows "Unavailable" when the
 * product is out of stock, or "Adding…" while a click is being
 * processed, so a shopper can't add the same item twice by clicking
 * repeatedly.
 */
export default function ProductCard({ product, onAddToCart, busy }) {
  const outOfStock = product.stockQuantity <= 0;

  return (
    <li className="product-card">
      <article aria-labelledby={`product-${product.id}-name`}>
        <h3 id={`product-${product.id}-name`}>{product.name}</h3>
        <p className="product-description">{product.description}</p>
        <p className="product-price">
          <span className="visually-hidden">Price: </span>
          {new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(product.price)}
        </p>
        <p className="product-stock" aria-live="polite">
          {outOfStock ? "Out of stock" : `${product.stockQuantity} in stock`}
        </p>
        <button
          type="button"
          disabled={outOfStock || busy}
          aria-disabled={outOfStock || busy}
          aria-describedby={`product-${product.id}-name`}
          onClick={() => onAddToCart(product.id)}
        >
          {outOfStock ? "Unavailable" : busy ? "Adding…" : "Add to cart"}
        </button>
      </article>
    </li>
  );
}
