import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./fonts.css";
import "./index.css";
import "./styles/globals.css";

// React 앱 렌더링
createRoot(document.getElementById("root")!).render(<App />);

// App Shell Overlay 제거 (React 렌더링 후 즉시 실행)
const removeOverlay = () => {
  const shell = document.getElementById("app-shell-overlay");
  if (shell) {
    shell.style.opacity = "0";
    setTimeout(() => shell.remove(), 400);
  }
};

// DOM이 준비되면 오버레이 제거
if (document.readyState === "complete") {
  removeOverlay();
} else {
  window.addEventListener("load", removeOverlay);
}
  