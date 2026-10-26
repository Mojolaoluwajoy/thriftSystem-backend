import { useEffect, useState } from "react";
import { api } from "../api";
import { card, errorText, input, label, muted, primaryButton } from "../ui.jsx";

// Where the member's payouts are sent. Saving registers the account with Paystack.
export default function BankAccount({ token }) {
  const [profile, setProfile] = useState(null);
  const [banks, setBanks] = useState([]);
  const [form, setForm] = useState({ bankCode: "", accountNumber: "", accountName: "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    api("/user/me", { token }).then(setProfile).catch((e) => setError(e.message));
    api("/payments/banks", { token })
      .then((list) => setBanks((list || []).sort((a, b) => a.name.localeCompare(b.name))))
      .catch(() => setError("Couldn't load the bank list. Check the Paystack key on the server."));
  }, []);

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    setSaved(false);
    try {
      setProfile(await api("/user/me/bank-account", { token, method: "PUT", body: form }));
      setSaved(true);
      setForm({ bankCode: "", accountNumber: "", accountName: "" });
    } catch (err) {
      setError(err.message);
    }
    setSaving(false);
  };

  return (
    <div style={{ ...card, maxWidth: "500px", marginTop: "1rem" }}>
      <h3 style={{ margin: "0 0 0.25rem", color: "#fff", fontFamily: "'Syne', sans-serif", fontSize: "1rem" }}>
        Payout bank account
      </h3>
      <p style={{ ...muted, marginBottom: "1rem" }}>
        {profile?.bankAccountLinked
          ? `Linked: ${profile.bankAccountName}. Saving a new account replaces it.`
          : "Add the account your payouts should go to."}
      </p>

      <form onSubmit={save} style={{ display: "grid", gap: "0.75rem" }}>
        <div>
          <label style={label}>Bank</label>
          <select
            required style={input} value={form.bankCode}
            onChange={(e) => setForm({ ...form, bankCode: e.target.value })}
          >
            <option value="" style={{ background: "#1a1a1a" }}>Select your bank</option>
            {banks.map((b) => (
              <option key={`${b.code}-${b.name}`} value={b.code} style={{ background: "#1a1a1a" }}>
                {b.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label style={label}>Account number</label>
          <input
            required style={input} inputMode="numeric" pattern="\d{10}" maxLength={10}
            placeholder="10-digit NUBAN" value={form.accountNumber}
            onChange={(e) => setForm({ ...form, accountNumber: e.target.value.replace(/\D/g, "") })}
          />
        </div>
        <div>
          <label style={label}>Account name</label>
          <input
            required style={input} placeholder="As it appears on the account"
            value={form.accountName}
            onChange={(e) => setForm({ ...form, accountName: e.target.value })}
          />
        </div>
        <button type="submit" disabled={saving} style={{ ...primaryButton, justifySelf: "start" }}>
          {saving ? "Saving..." : "Save bank account"}
        </button>
      </form>

      {saved && <p style={{ color: "#00c896", fontSize: "0.85rem", margin: "0.75rem 0 0" }}>Bank account saved.</p>}
      {error && <p style={errorText}>{error}</p>}
    </div>
  );
}
