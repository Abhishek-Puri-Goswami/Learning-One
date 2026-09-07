/**
 * Shows an error message (and, optionally, a "Retry" button) when
 * something goes wrong. The {@code role="alert"} attribute makes screen
 * readers announce the message right away, without the user needing to
 * navigate to it first — so a visually impaired user finds out about the
 * error at the same moment a sighted user would see it appear.
 */
export default function ErrorMessage({ error, onRetry }) {
  if (!error) return null;

  const message = error.message || "Something went wrong. Please try again.";

  return (
    <div className="error-message" role="alert">
      <p>{message}</p>
      {error.details && error.details.length > 0 && (
        <ul>
          {error.details.map((d, i) => (
            <li key={i}>{d}</li>
          ))}
        </ul>
      )}
      {onRetry && (
        <button type="button" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}
