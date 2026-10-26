import { useEffect, useState } from "react";
import { api } from "../api";
import {
  card, errorText, formatDate, heading, input, label, muted, naira,
  primaryButton, secondaryButton, StatusPill,
} from "../ui.jsx";

// One group: members, the current cycle's contributions and every payout.
// The group admin can add members and start the group while it is FORMING,
// and process payouts once a cycle is fully paid.
export default function GroupDetails({ group, user, token, onBack, onChanged }) {
  const [contributions, setContributions] = useState([]);
  const [payouts, setPayouts] = useState([]);
  const [names, setNames] = useState({});
  const [newMemberId, setNewMemberId] = useState("");
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");

  const isAdmin = group.adminId === user.id;
  // Groups created before cycles existed are ACTIVE but were never started
  const isForming = !group.startedAt && group.status !== "COMPLETED";
  const memberIds = group.memberIds || [];

  const load = async () => {
    try {
      const [c, p] = await Promise.all([
        api(`/contributions/group/${group.id}`, { token }),
        api(`/payouts/group/${group.id}`, { token }),
      ]);
      setContributions(c || []);
      setPayouts((p || []).sort((a, b) => a.cycleNumber - b.cycleNumber));
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    load();
  }, [group.id]);

  // Show names instead of raw user IDs
  useEffect(() => {
    let cancelled = false;
    Promise.all(
      memberIds.map((id) =>
        api(`/user/${id}`, { token })
          .then((u) => [id, `${u.firstName || ""} ${u.lastName || ""}`.trim() || u.email])
          .catch(() => [id, id])
      )
    ).then((entries) => {
      if (!cancelled) setNames(Object.fromEntries(entries));
    });
    return () => { cancelled = true; };
  }, [memberIds.join(",")]);

  const nameOf = (userId) => (userId === user.id ? "You" : names[userId] || userId);

  const run = async (key, action) => {
    setBusy(key);
    setError("");
    try {
      await action();
      await onChanged();
      await load();
    } catch (e) {
      setError(e.message);
    }
    setBusy("");
  };

  const addMember = (e) => {
    e.preventDefault();
    const id = newMemberId.trim();
    if (!id) return;
    run("add", async () => {
      await api(`/group/${group.id}/members/${id}`, { token, method: "POST" });
      setNewMemberId("");
    });
  };

  const startGroup = () => {
    if (!window.confirm(
      `Start "${group.name}" with ${memberIds.length} members? Members can't be added after this.`
    )) return;
    run("start", () => api(`/group/${group.id}/start`, { token, method: "POST" }));
  };

  const processPayout = (payout) => {
    if (!window.confirm(
      `Send ${naira(payout.amount)} to ${nameOf(payout.userId)} for cycle ${payout.cycleNumber}?`
    )) return;
    run(payout.id, () => api(`/payouts/${payout.id}/process`, { token, method: "POST" }));
  };

  const currentCycle = group.currentCycle || 0;
  const cycleContributions = contributions.filter((c) => c.cycleNumber === currentCycle);
  const paidCount = cycleContributions.filter((c) => c.status === "SUCCESSFUL").length;

  return (
    <div>
      <button onClick={onBack} style={{ ...secondaryButton, marginBottom: "1rem" }}>
        ← Back to groups
      </button>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem", flexWrap: "wrap", marginBottom: "1.5rem" }}>
        <div>
          <h2 style={heading}>{group.name}</h2>
          <p style={{ ...muted, marginTop: "0.25rem" }}>
            {naira(group.contributionAmount)} · {group.frequency?.replace("_", "-").toLowerCase()}
            {isAdmin && " · You're the admin"}
          </p>
        </div>
        <StatusPill status={group.status} />
      </div>

      {error && <p style={{ ...errorText, marginBottom: "1rem" }}>{error}</p>}

      {/* Members */}
      <div style={{ ...card, marginBottom: "1rem" }}>
        <h3 style={{ margin: "0 0 0.75rem", color: "#fff", fontFamily: "'Syne', sans-serif", fontSize: "1rem" }}>
          Members ({memberIds.length}/{group.totalMembers})
        </h3>
        {memberIds.map((id) => (
          <p key={id} style={{ margin: "0.35rem 0", color: "#fff", fontSize: "0.9rem" }}>
            {nameOf(id)}{id === group.adminId && <span style={muted}> · admin</span>}
          </p>
        ))}

        {isAdmin && isForming && (
          <>
            <form onSubmit={addMember} style={{ display: "flex", gap: "0.5rem", marginTop: "1rem", flexWrap: "wrap" }}>
              <div style={{ flex: "1 1 220px" }}>
                <label style={label}>Add member by User ID</label>
                <input
                  style={input} value={newMemberId}
                  placeholder="Ask them for the User ID on their Profile page"
                  onChange={(e) => setNewMemberId(e.target.value)}
                />
              </div>
              <button type="submit" disabled={busy === "add"} style={{ ...secondaryButton, alignSelf: "flex-end" }}>
                {busy === "add" ? "Adding..." : "Add"}
              </button>
            </form>
            <div style={{ marginTop: "1rem", display: "flex", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
              <button
                onClick={startGroup}
                disabled={busy === "start" || memberIds.length < 2}
                style={{ ...primaryButton, opacity: memberIds.length < 2 ? 0.5 : 1 }}
              >
                {busy === "start" ? "Starting..." : "Start group"}
              </button>
              <p style={muted}>
                {memberIds.length < 2
                  ? "Add at least one more member to start."
                  : "Starting locks membership and opens cycle 1."}
              </p>
            </div>
          </>
        )}
      </div>

      {/* Current cycle */}
      {group.startedAt && group.status !== "COMPLETED" && (
        <div style={{ ...card, marginBottom: "1rem" }}>
          <h3 style={{ margin: "0 0 0.25rem", color: "#fff", fontFamily: "'Syne', sans-serif", fontSize: "1rem" }}>
            Cycle {currentCycle} of {group.totalCycles}
          </h3>
          <p style={{ ...muted, marginBottom: "0.75rem" }}>
            {paidCount} of {cycleContributions.length} paid
          </p>
          {cycleContributions.map((c) => (
            <div key={c.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "0.5rem 0", borderBottom: "1px solid rgba(255,255,255,0.05)" }}>
              <span style={{ color: "#fff", fontSize: "0.9rem" }}>{nameOf(c.userId)}</span>
              <StatusPill status={c.status} />
            </div>
          ))}
        </div>
      )}

      {/* Payouts */}
      <div style={card}>
        <h3 style={{ margin: "0 0 0.75rem", color: "#fff", fontFamily: "'Syne', sans-serif", fontSize: "1rem" }}>
          Payouts
        </h3>
        {payouts.length === 0 && <p style={muted}>No payouts scheduled yet.</p>}
        {payouts.map((p) => {
          const cyclePaid =
            contributions.filter((c) => c.cycleNumber === p.cycleNumber && c.status === "SUCCESSFUL").length;
          const payable = ["PENDING", "FAILED", "REVERSED"].includes(p.payoutStatus);
          const fullyPaid = cyclePaid >= memberIds.length;
          return (
            <div key={p.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem", flexWrap: "wrap", padding: "0.75rem 0", borderBottom: "1px solid rgba(255,255,255,0.05)" }}>
              <div>
                <p style={{ margin: 0, color: "#fff", fontWeight: "500" }}>
                  Cycle {p.cycleNumber}: {naira(p.amount)} to {nameOf(p.userId)}
                </p>
                <p style={muted}>
                  Due {formatDate(p.scheduledDate)}
                  {p.failureReason && ` · ${p.failureReason}`}
                </p>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                <StatusPill status={p.payoutStatus} />
                {isAdmin && payable && (
                  <button
                    onClick={() => processPayout(p)}
                    disabled={busy === p.id || !fullyPaid}
                    title={fullyPaid ? "" : "Every member must pay this cycle first"}
                    style={{ ...primaryButton, opacity: fullyPaid ? 1 : 0.5 }}
                  >
                    {busy === p.id ? "Sending..." : p.payoutStatus === "PENDING" ? "Process payout" : "Retry payout"}
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
