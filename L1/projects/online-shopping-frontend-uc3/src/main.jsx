import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles/index.css";

// This is the entry point of the whole app: it finds the empty <div id="root">
// in index.html and tells React to render our <App /> component inside it.
// React.StrictMode is a development-only helper that runs extra checks to
// catch common mistakes early — it doesn't do anything in a production build.
ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
