/**
 * A simple spinner shown while something is loading. The
 * {@code role="status"} and {@code aria-live="polite"} attributes let a
 * screen reader announce "Loading…" on its own, without the user having
 * to move their focus anywhere to notice it.
 */
export default function LoadingSpinner({ label = "Loading…" }) {
  return (
    <div className="loading-spinner" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
