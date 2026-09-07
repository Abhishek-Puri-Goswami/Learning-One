// role="alert" ensures assistive tech announces errors immediately
// (WCAG 2.1 AA - 4.1.3 Status Messages / 3.3.1 Error Identification).
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
