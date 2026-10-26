// Shared styles and helpers, matching the look of the existing dashboard

export const colors = {
  orange: "linear-gradient(135deg, #ff6400, #ff9500)",
  green: "linear-gradient(135deg, #00c896, #00e6a8)",
  purple: "linear-gradient(135deg, #7c3aed, #a855f7)",
  blue: "linear-gradient(135deg, #0ea5e9, #38bdf8)",
  pink: "linear-gradient(135deg, #ec4899, #f472b6)",
};

export const statusColor = (status) => {
  const map = {
    FORMING: "#ff9500", ACTIVE: "#00c896", COMPLETED: "#0ea5e9",
    PENDING: "#ff9500", SUCCESSFUL: "#00c896",
    FAILED: "#ef4444", PROCESSING: "#a855f7",
    REVERSED: "#ef4444", REFUNDED: "#0ea5e9",
    SUSPENDED: "#ef4444", INACTIVE: "#888",
  };
  return map[status] || "#888";
};

export const card = {
  background: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.08)",
  borderRadius: "16px", padding: "1.25rem",
};

export const label = {
  display: "block", color: "rgba(255,255,255,0.5)", fontSize: "0.8rem",
  marginBottom: "0.4rem", textTransform: "uppercase", letterSpacing: "0.5px",
};

export const input = {
  width: "100%", padding: "0.75rem",
  background: "rgba(255,255,255,0.07)",
  border: "1px solid rgba(255,255,255,0.12)",
  borderRadius: "10px", color: "#fff",
  fontFamily: "'DM Sans', sans-serif", fontSize: "0.9rem",
  outline: "none", boxSizing: "border-box",
};

export const primaryButton = {
  padding: "0.6rem 1.1rem",
  background: colors.orange, border: "none",
  borderRadius: "10px", color: "#fff",
  fontFamily: "'Syne', sans-serif", fontWeight: "700",
  fontSize: "0.85rem", cursor: "pointer",
};

export const secondaryButton = {
  padding: "0.6rem 1.1rem",
  background: "transparent",
  border: "1px solid rgba(255,255,255,0.2)",
  borderRadius: "10px", color: "rgba(255,255,255,0.7)",
  fontFamily: "'DM Sans', sans-serif", fontSize: "0.85rem", cursor: "pointer",
};

export const heading = {
  margin: 0, fontFamily: "'Syne', sans-serif",
  fontWeight: "800", color: "#fff", fontSize: "1.5rem",
};

export const muted = { color: "rgba(255,255,255,0.4)", fontSize: "0.8rem", margin: 0 };

export const errorText = { color: "#ff6060", fontSize: "0.85rem", margin: "0.5rem 0 0" };

export const naira = (amount) => `₦${Number(amount || 0).toLocaleString()}`;

export const formatDate = (value) => (value ? new Date(value).toLocaleDateString() : "—");

export function StatusPill({ status }) {
  return (
    <span style={{
      padding: "0.25rem 0.75rem", borderRadius: "20px",
      fontSize: "0.75rem", fontWeight: "600",
      background: `${statusColor(status)}20`,
      color: statusColor(status), whiteSpace: "nowrap",
    }}>{status || "—"}</span>
  );
}
