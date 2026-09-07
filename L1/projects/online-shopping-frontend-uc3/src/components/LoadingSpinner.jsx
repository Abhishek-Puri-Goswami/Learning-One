// Accessible loading indicator: role="status" + aria-live so screen readers
// announce the loading state without needing focus to move (WCAG 2.1 AA -
// 4.1.3 Status Messages).
export default function LoadingSpinner({ label = "Loading…" }) {
  return (
    <div className="loading-spinner" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
