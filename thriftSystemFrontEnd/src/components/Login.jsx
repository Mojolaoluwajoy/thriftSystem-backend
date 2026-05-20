import { useState } from "react";

const API_BASE = "http://localhost:8080/api/v1";

export default function Login({ onLogin }) {
  const [form, setForm] = useState({ email: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [isRegister, setIsRegister] = useState(false);
  const [regForm, setRegForm] = useState({
    firstName: "", lastName: "", email: "",
    phoneNumber: "", password: ""
  });

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      const data = await res.json();
      if (data.success) {
        onLogin(data.data, data.data.token);
      } else {
        setError(data.message || "Login failed");
      }
    } catch {
      setError("Connection error. Make sure the server is running.");
    }
    setLoading(false);
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(regForm),
      });
      const data = await res.json();
      if (data.success) {
        onLogin(data.data, data.data.token);
      } else {
        setError(data.message || "Registration failed");
      }
    } catch {
      setError("Connection error. Make sure the server is running.");
    }
    setLoading(false);
  };

  return (
    <div style={{
      minHeight: "100vh",
      background: "linear-gradient(135deg, #1a0533 0%, #0d1b4b 50%, #0a2d1f 100%)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      fontFamily: "'Syne', sans-serif",
      position: "relative",
      overflow: "hidden"
    }}>
      <link href="https://fonts.googleapis.com/css2?family=Syne:wght@400;700;800&family=DM+Sans:wght@300;400;500&display=swap" rel="stylesheet" />

      {/* Decorative blobs */}
      <div style={{
        position: "absolute", top: "-100px", left: "-100px",
        width: "400px", height: "400px", borderRadius: "50%",
        background: "radial-gradient(circle, rgba(255,100,0,0.2), transparent)",
        pointerEvents: "none"
      }} />
      <div style={{
        position: "absolute", bottom: "-100px", right: "-100px",
        width: "500px", height: "500px", borderRadius: "50%",
        background: "radial-gradient(circle, rgba(0,200,150,0.15), transparent)",
        pointerEvents: "none"
      }} />
      <div style={{
        position: "absolute", top: "50%", left: "60%",
        width: "300px", height: "300px", borderRadius: "50%",
        background: "radial-gradient(circle, rgba(150,50,255,0.15), transparent)",
        pointerEvents: "none"
      }} />

      <div style={{
        width: "100%", maxWidth: "440px", padding: "0 1.5rem",
        position: "relative", zIndex: 1
      }}>
        {/* Logo */}
        <div style={{ textAlign: "center", marginBottom: "2.5rem" }}>
          <div style={{
            display: "inline-flex", alignItems: "center", justifyContent: "center",
            width: "64px", height: "64px", borderRadius: "20px",
            background: "linear-gradient(135deg, #ff6400, #ff9500)",
            marginBottom: "1rem", fontSize: "28px"
          }}>💰</div>
          <h1 style={{
            margin: 0, fontSize: "2rem", fontWeight: "800",
            color: "#fff", letterSpacing: "-0.5px"
          }}>ThriftSystem</h1>
          <p style={{
            margin: "0.4rem 0 0", color: "rgba(255,255,255,0.5)",
            fontSize: "0.9rem", fontFamily: "'DM Sans', sans-serif"
          }}>Cooperative savings, reimagined</p>
        </div>

        {/* Card */}
        <div style={{
          background: "rgba(255,255,255,0.05)",
          backdropFilter: "blur(20px)",
          border: "1px solid rgba(255,255,255,0.1)",
          borderRadius: "24px",
          padding: "2rem",
        }}>
          {/* Toggle */}
          <div style={{
            display: "flex", background: "rgba(0,0,0,0.3)",
            borderRadius: "12px", padding: "4px", marginBottom: "1.5rem"
          }}>
            {["Login", "Register"].map((tab) => (
              <button key={tab} onClick={() => setIsRegister(tab === "Register")}
                style={{
                  flex: 1, padding: "0.6rem",
                  borderRadius: "10px", border: "none", cursor: "pointer",
                  fontFamily: "'Syne', sans-serif", fontWeight: "700",
                  fontSize: "0.85rem", transition: "all 0.2s",
                  background: (tab === "Register") === isRegister
                    ? "linear-gradient(135deg, #ff6400, #ff9500)"
                    : "transparent",
                  color: (tab === "Register") === isRegister
                    ? "#fff" : "rgba(255,255,255,0.5)",
                }}>
                {tab}
              </button>
            ))}
          </div>

          {error && (
            <div style={{
              background: "rgba(255,50,50,0.15)", border: "1px solid rgba(255,50,50,0.3)",
              borderRadius: "10px", padding: "0.75rem 1rem",
              color: "#ff8080", fontSize: "0.85rem",
              fontFamily: "'DM Sans', sans-serif", marginBottom: "1rem"
            }}>{error}</div>
          )}

          {!isRegister ? (
            <form onSubmit={handleLogin}>
              {[
                { label: "Email", key: "email", type: "email", placeholder: "you@example.com" },
                { label: "Password", key: "password", type: "password", placeholder: "••••••••" }
              ].map(({ label, key, type, placeholder }) => (
                <div key={key} style={{ marginBottom: "1rem" }}>
                  <label style={{
                    display: "block", color: "rgba(255,255,255,0.6)",
                    fontSize: "0.8rem", fontWeight: "700",
                    letterSpacing: "0.5px", marginBottom: "0.4rem",
                    textTransform: "uppercase"
                  }}>{label}</label>
                  <input
                    type={type} placeholder={placeholder} required
                    value={form[key]}
                    onChange={e => setForm({ ...form, [key]: e.target.value })}
                    style={{
                      width: "100%", padding: "0.8rem 1rem",
                      background: "rgba(255,255,255,0.07)",
                      border: "1px solid rgba(255,255,255,0.12)",
                      borderRadius: "12px", color: "#fff",
                      fontFamily: "'DM Sans', sans-serif", fontSize: "0.95rem",
                      outline: "none", boxSizing: "border-box",
                      transition: "border-color 0.2s"
                    }}
                  />
                </div>
              ))}
              <button type="submit" disabled={loading} style={{
                width: "100%", padding: "0.9rem",
                background: loading ? "rgba(255,100,0,0.4)" : "linear-gradient(135deg, #ff6400, #ff9500)",
                border: "none", borderRadius: "12px", color: "#fff",
                fontFamily: "'Syne', sans-serif", fontWeight: "700",
                fontSize: "1rem", cursor: loading ? "not-allowed" : "pointer",
                marginTop: "0.5rem", transition: "opacity 0.2s"
              }}>
                {loading ? "Signing in..." : "Sign In →"}
              </button>
            </form>
          ) : (
            <form onSubmit={handleRegister}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1rem" }}>
                {[
                  { label: "First Name", key: "firstName", placeholder: "John" },
                  { label: "Last Name", key: "lastName", placeholder: "Doe" }
                ].map(({ label, key, placeholder }) => (
                  <div key={key}>
                    <label style={{
                      display: "block", color: "rgba(255,255,255,0.6)",
                      fontSize: "0.8rem", fontWeight: "700",
                      letterSpacing: "0.5px", marginBottom: "0.4rem",
                      textTransform: "uppercase"
                    }}>{label}</label>
                    <input
                      placeholder={placeholder} required
                      value={regForm[key]}
                      onChange={e => setRegForm({ ...regForm, [key]: e.target.value })}
                      style={{
                        width: "100%", padding: "0.8rem 1rem",
                        background: "rgba(255,255,255,0.07)",
                        border: "1px solid rgba(255,255,255,0.12)",
                        borderRadius: "12px", color: "#fff",
                        fontFamily: "'DM Sans', sans-serif", fontSize: "0.9rem",
                        outline: "none", boxSizing: "border-box"
                      }}
                    />
                  </div>
                ))}
              </div>
              {[
                { label: "Email", key: "email", type: "email", placeholder: "you@example.com" },
                { label: "Phone Number", key: "phoneNumber", type: "tel", placeholder: "08012345678" },
                { label: "Password", key: "password", type: "password", placeholder: "Min 8 characters" }
              ].map(({ label, key, type, placeholder }) => (
                <div key={key} style={{ marginBottom: "1rem" }}>
                  <label style={{
                    display: "block", color: "rgba(255,255,255,0.6)",
                    fontSize: "0.8rem", fontWeight: "700",
                    letterSpacing: "0.5px", marginBottom: "0.4rem",
                    textTransform: "uppercase"
                  }}>{label}</label>
                  <input
                    type={type || "text"} placeholder={placeholder} required
                    value={regForm[key]}
                    onChange={e => setRegForm({ ...regForm, [key]: e.target.value })}
                    style={{
                      width: "100%", padding: "0.8rem 1rem",
                      background: "rgba(255,255,255,0.07)",
                      border: "1px solid rgba(255,255,255,0.12)",
                      borderRadius: "12px", color: "#fff",
                      fontFamily: "'DM Sans', sans-serif", fontSize: "0.95rem",
                      outline: "none", boxSizing: "border-box"
                    }}
                  />
                </div>
              ))}
              <button type="submit" disabled={loading} style={{
                width: "100%", padding: "0.9rem",
                background: loading ? "rgba(255,100,0,0.4)" : "linear-gradient(135deg, #ff6400, #ff9500)",
                border: "none", borderRadius: "12px", color: "#fff",
                fontFamily: "'Syne', sans-serif", fontWeight: "700",
                fontSize: "1rem", cursor: loading ? "not-allowed" : "pointer",
                marginTop: "0.5rem"
              }}>
                {loading ? "Creating account..." : "Create Account →"}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}