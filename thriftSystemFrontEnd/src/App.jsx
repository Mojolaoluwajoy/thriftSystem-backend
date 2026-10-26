import { useState } from "react";
import Login from "./components/Login";
import Dashboard from "./components/Dashboard";

const SESSION_KEY = "thrift-session";

// Kept in sessionStorage so the member stays signed in through the Paystack checkout
// redirect; it's cleared when the tab closes
function loadSession() {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY)) || { user: null, token: null };
  } catch {
    return { user: null, token: null };
  }
}

function saveSession(session) {
  try {
    if (session.user) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    else sessionStorage.removeItem(SESSION_KEY);
  } catch {
    // Storage unavailable (e.g. private mode); the session just won't survive a reload
  }
}

export default function App() {
  const [session, setSession] = useState(loadSession);

  const handleLogin = (userData, authToken) => {
    const next = { user: userData, token: authToken };
    saveSession(next);
    setSession(next);
  };

  const handleLogout = () => {
    saveSession({ user: null, token: null });
    setSession({ user: null, token: null });
  };

  return (
    <div>
      {!session.user ? (
        <Login onLogin={handleLogin} />
      ) : (
        <Dashboard user={session.user} token={session.token} onLogout={handleLogout} />
      )}
    </div>
  );
}
