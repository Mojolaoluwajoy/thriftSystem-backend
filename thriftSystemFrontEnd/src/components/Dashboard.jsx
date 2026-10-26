import { useState, useEffect } from "react";
import { api } from "../api";
import { colors, statusColor, StatusPill, primaryButton, errorText } from "../ui.jsx";
import GroupDetails from "./GroupDetails";
import BankAccount from "./BankAccount";

// Values must match the backend's ContributionFrequency enum
const frequencies = [
  { value: "DAILY", label: "Daily" },
  { value: "WEEKLY", label: "Weekly" },
  { value: "BI_WEEKLY", label: "Every 2 weeks" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "BI_MONTHLY", label: "Every 2 months" },
];

const navItems = [
  { id: "dashboard", label: "Dashboard", icon: "🏠" },
  { id: "groups", label: "My Groups", icon: "👥" },
  { id: "contributions", label: "Contributions", icon: "💳" },
  { id: "payouts", label: "Payouts", icon: "💸" },
  { id: "currencies", label: "Currencies", icon: "💱" },
  { id: "profile", label: "Profile", icon: "👤" },
];

export default function Dashboard({ user, token, onLogout }) {
  const [activeNav, setActiveNav] = useState("dashboard");
  const [selectedGroupId, setSelectedGroupId] = useState(null);
  const [payingId, setPayingId] = useState(null);
  const [error, setError] = useState("");
  // Set when Paystack sends the member back after checkout
  const [paymentNotice, setPaymentNotice] = useState(
    () => new URLSearchParams(window.location.search).get("payment") === "complete"
  );
  const [groups, setGroups] = useState([]);
  const [contributions, setContributions] = useState([]);
  const [payouts, setPayouts] = useState([]);
  const [currencies, setCurrencies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showCreateGroup, setShowCreateGroup] = useState(false);
  const [greeting, setGreeting] = useState("");
  const [createGroupForm, setCreateGroupForm] = useState({
    name: "", description: "", contributionAmount: "",
    currency: "NGN", frequency: "MONTHLY",
    startDate: "", totalMembers: ""
  });

  useEffect(() => {
    const hour = new Date().getHours();
    if (hour < 12) setGreeting("Good morning");
    else if (hour < 17) setGreeting("Good afternoon");
    else setGreeting("Good evening");
  }, []);

  useEffect(() => {
    if (paymentNotice) {
      // Drop Paystack's query parameters so a refresh doesn't show the notice again
      window.history.replaceState({}, "", window.location.pathname);
      setActiveNav("contributions");
    }
    refreshAll();
    fetchCurrencies();
  }, []);

  const load = async (path, setter) => {
    try {
      setter((await api(path, { token })) || []);
    } catch (e) {
      setError(e.message);
    }
  };

  const fetchGroups = () => load("/group/my-groups", setGroups);
  const fetchContributions = () => load("/contributions/my-contributions", setContributions);
  const fetchPayouts = () => load("/payouts/my-payouts", setPayouts);
  const fetchCurrencies = () => load("/currencies", setCurrencies);
  const refreshAll = () => Promise.all([fetchGroups(), fetchContributions(), fetchPayouts()]);

  const handleCreateGroup = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      await api("/group/create-group", {
        token, method: "POST",
        body: {
          ...createGroupForm,
          contributionAmount: parseFloat(createGroupForm.contributionAmount),
          totalMembers: parseInt(createGroupForm.totalMembers),
          startDate: new Date(createGroupForm.startDate).toISOString()
        }
      });
      setShowCreateGroup(false);
      fetchGroups();
      setCreateGroupForm({
        name: "", description: "", contributionAmount: "",
        currency: "NGN", frequency: "MONTHLY",
        startDate: "", totalMembers: ""
      });
    } catch (err) {
      setError(err.message);
    }
    setLoading(false);
  };

  // Opens Paystack checkout for a contribution; Paystack returns to /?payment=complete
  const payContribution = async (contribution) => {
    setPayingId(contribution.id);
    setError("");
    try {
      const paystack = await api(`/payments/initialize/${contribution.id}`, { token, method: "POST" });
      const url = paystack?.data?.authorization_url;
      if (!url) throw new Error("Paystack didn't return a checkout link");
      window.location.href = url;
    } catch (err) {
      setError(err.message);
      setPayingId(null);
      fetchContributions();
    }
  };

  const groupName = (groupId) => groups.find(g => g.id === groupId)?.name;
  const selectedGroup = groups.find(g => g.id === selectedGroupId);

  const totalContributed = contributions
    .filter(c => c.status === "SUCCESSFUL")
    .reduce((sum, c) => sum + (c.amount || 0), 0);

  const totalPayouts = payouts
    .filter(p => p.payoutStatus === "SUCCESSFUL")
    .reduce((sum, p) => sum + (p.amount || 0), 0);

  return (
    <div style={{
      minHeight: "100vh",
      background: "#0d0d0d",
      fontFamily: "'DM Sans', sans-serif",
      display: "flex"
    }}>
      <link href="https://fonts.googleapis.com/css2?family=Syne:wght@400;700;800&family=DM+Sans:wght@300;400;500&display=swap" rel="stylesheet" />

      {/* Sidebar */}
      <div style={{
        width: "260px", minHeight: "100vh",
        background: "rgba(255,255,255,0.03)",
        borderRight: "1px solid rgba(255,255,255,0.07)",
        display: "flex", flexDirection: "column",
        padding: "1.5rem 1rem", position: "sticky", top: 0,
        flexShrink: 0
      }}>
        {/* Logo */}
        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginBottom: "2rem", padding: "0 0.5rem" }}>
          <div style={{
            width: "40px", height: "40px", borderRadius: "12px",
            background: colors.orange, display: "flex",
            alignItems: "center", justifyContent: "center", fontSize: "20px"
          }}>💰</div>
          <span style={{
            fontFamily: "'Syne', sans-serif", fontWeight: "800",
            color: "#fff", fontSize: "1.1rem"
          }}>ThriftSystem</span>
        </div>

        {/* Nav */}
        <nav style={{ flex: 1 }}>
          {navItems.map(item => (
            <button key={item.id} onClick={() => { setActiveNav(item.id); setSelectedGroupId(null); setError(""); }}
              style={{
                width: "100%", display: "flex", alignItems: "center",
                gap: "0.75rem", padding: "0.75rem 1rem",
                borderRadius: "12px", border: "none", cursor: "pointer",
                background: activeNav === item.id
                  ? "rgba(255,100,0,0.15)" : "transparent",
                color: activeNav === item.id
                  ? "#ff6400" : "rgba(255,255,255,0.5)",
                fontFamily: "'DM Sans', sans-serif",
                fontWeight: activeNav === item.id ? "500" : "400",
                fontSize: "0.9rem", textAlign: "left",
                transition: "all 0.2s", marginBottom: "0.25rem"
              }}>
              <span style={{ fontSize: "1.1rem" }}>{item.icon}</span>
              {item.label}
              {activeNav === item.id && (
                <div style={{
                  marginLeft: "auto", width: "6px", height: "6px",
                  borderRadius: "50%", background: "#ff6400"
                }} />
              )}
            </button>
          ))}
        </nav>

        {/* User info */}
        <div style={{
          padding: "1rem",
          background: "rgba(255,255,255,0.05)",
          borderRadius: "14px", marginTop: "auto"
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
            <div style={{
              width: "38px", height: "38px", borderRadius: "50%",
              background: colors.purple,
              display: "flex", alignItems: "center", justifyContent: "center",
              fontFamily: "'Syne', sans-serif", fontWeight: "700",
              color: "#fff", fontSize: "0.9rem"
            }}>
              {user.firstName?.[0]}{user.lastName?.[0]}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <p style={{
                margin: 0, color: "#fff", fontWeight: "500",
                fontSize: "0.85rem",
                overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap"
              }}>{user.firstName} {user.lastName}</p>
              <p style={{
                margin: 0, color: "rgba(255,255,255,0.4)",
                fontSize: "0.75rem",
                overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap"
              }}>{user.email}</p>
            </div>
          </div>
          <button onClick={onLogout} style={{
            width: "100%", marginTop: "0.75rem",
            padding: "0.5rem", background: "rgba(255,50,50,0.1)",
            border: "1px solid rgba(255,50,50,0.2)",
            borderRadius: "8px", color: "#ff6060",
            fontSize: "0.8rem", cursor: "pointer",
            fontFamily: "'DM Sans', sans-serif"
          }}>Sign out</button>
        </div>
      </div>

      {/* Main content */}
      <div style={{ flex: 1, padding: "2rem", overflowY: "auto", minWidth: 0 }}>

        {paymentNotice && (
          <div style={{
            background: "rgba(0,200,150,0.1)", border: "1px solid rgba(0,200,150,0.3)",
            borderRadius: "12px", padding: "0.75rem 1rem", marginBottom: "1rem",
            display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem"
          }}>
            <span style={{ color: "#00c896", fontSize: "0.9rem" }}>
              Payment submitted. It shows as paid once Paystack confirms it, usually within a minute.
            </span>
            <button onClick={() => { setPaymentNotice(false); refreshAll(); }} style={{
              background: "transparent", border: "none", color: "#00c896", cursor: "pointer", fontSize: "0.85rem"
            }}>Refresh</button>
          </div>
        )}

        {error && (
          <p style={{ ...errorText, margin: "0 0 1rem" }}>{error}</p>
        )}

        {/* Dashboard */}
        {activeNav === "dashboard" && (
          <div>
            {/* Greeting */}
            <div style={{ marginBottom: "2rem" }}>
              <h1 style={{
                margin: 0, fontFamily: "'Syne', sans-serif",
                fontWeight: "800", fontSize: "2rem", color: "#fff"
              }}>
                {greeting}, <span style={{
                  background: colors.orange,
                  WebkitBackgroundClip: "text",
                  WebkitTextFillColor: "transparent"
                }}>{user.firstName}</span> 👋
              </h1>
              <p style={{ margin: "0.5rem 0 0", color: "rgba(255,255,255,0.4)", fontSize: "0.95rem" }}>
                Here's your financial overview
              </p>
            </div>

            {/* Stats */}
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
              gap: "1rem", marginBottom: "2rem"
            }}>
              {[
                { label: "Active Groups", value: groups.filter(g => g.status === "ACTIVE").length, gradient: colors.orange, icon: "👥" },
                { label: "Total Contributions", value: `₦${totalContributed.toLocaleString()}`, gradient: colors.green, icon: "💳" },
                { label: "Total Payouts", value: `₦${totalPayouts.toLocaleString()}`, gradient: colors.purple, icon: "💸" },
                { label: "Pending Contributions", value: contributions.filter(c => c.status === "PENDING").length, gradient: colors.blue, icon: "⏳" },
              ].map((stat) => (
                <div key={stat.label} style={{
                  background: "rgba(255,255,255,0.05)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: "16px", padding: "1.25rem",
                  position: "relative", overflow: "hidden"
                }}>
                  <div style={{
                    position: "absolute", top: "-20px", right: "-20px",
                    width: "80px", height: "80px", borderRadius: "50%",
                    background: stat.gradient, opacity: 0.1
                  }} />
                  <div style={{ fontSize: "1.5rem", marginBottom: "0.5rem" }}>{stat.icon}</div>
                  <p style={{ margin: 0, color: "rgba(255,255,255,0.5)", fontSize: "0.8rem", textTransform: "uppercase", letterSpacing: "0.5px" }}>{stat.label}</p>
                  <p style={{
                    margin: "0.25rem 0 0",
                    fontFamily: "'Syne', sans-serif", fontWeight: "800",
                    fontSize: "1.6rem", color: "#fff"
                  }}>{stat.value}</p>
                </div>
              ))}
            </div>

            {/* Recent activity */}
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))",
              gap: "1rem"
            }}>
              {/* Recent groups */}
              <div style={{
                background: "rgba(255,255,255,0.03)",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: "16px", padding: "1.25rem"
              }}>
                <h3 style={{
                  margin: "0 0 1rem",
                  fontFamily: "'Syne', sans-serif",
                  color: "#fff", fontSize: "1rem"
                }}>Recent Groups</h3>
                {groups.slice(0, 3).map(g => (
                  <div key={g.id} style={{
                    display: "flex", justifyContent: "space-between",
                    alignItems: "center", padding: "0.75rem 0",
                    borderBottom: "1px solid rgba(255,255,255,0.05)"
                  }}>
                    <div>
                      <p style={{ margin: 0, color: "#fff", fontSize: "0.9rem", fontWeight: "500" }}>{g.name}</p>
                      <p style={{ margin: 0, color: "rgba(255,255,255,0.4)", fontSize: "0.8rem" }}>
                        ₦{g.contributionAmount?.toLocaleString()} · {g.frequency}
                      </p>
                    </div>
                    <span style={{
                      padding: "0.25rem 0.75rem", borderRadius: "20px",
                      fontSize: "0.75rem", fontWeight: "500",
                      background: `${statusColor(g.status)}20`,
                      color: statusColor(g.status)
                    }}>{g.status}</span>
                  </div>
                ))}
                {groups.length === 0 && (
                  <p style={{ color: "rgba(255,255,255,0.3)", fontSize: "0.85rem", textAlign: "center", padding: "1rem 0" }}>
                    No groups yet
                  </p>
                )}
              </div>

              {/* Recent contributions */}
              <div style={{
                background: "rgba(255,255,255,0.03)",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: "16px", padding: "1.25rem"
              }}>
                <h3 style={{
                  margin: "0 0 1rem",
                  fontFamily: "'Syne', sans-serif",
                  color: "#fff", fontSize: "1rem"
                }}>Recent Contributions</h3>
                {contributions.slice(0, 3).map(c => (
                  <div key={c.id} style={{
                    display: "flex", justifyContent: "space-between",
                    alignItems: "center", padding: "0.75rem 0",
                    borderBottom: "1px solid rgba(255,255,255,0.05)"
                  }}>
                    <div>
                      <p style={{ margin: 0, color: "#fff", fontSize: "0.9rem", fontWeight: "500" }}>
                        ₦{c.amount?.toLocaleString()}
                      </p>
                      <p style={{ margin: 0, color: "rgba(255,255,255,0.4)", fontSize: "0.8rem" }}>
                        Cycle {c.cycleNumber}
                      </p>
                    </div>
                    <span style={{
                      padding: "0.25rem 0.75rem", borderRadius: "20px",
                      fontSize: "0.75rem", fontWeight: "500",
                      background: `${statusColor(c.status)}20`,
                      color: statusColor(c.status)
                    }}>{c.status}</span>
                  </div>
                ))}
                {contributions.length === 0 && (
                  <p style={{ color: "rgba(255,255,255,0.3)", fontSize: "0.85rem", textAlign: "center", padding: "1rem 0" }}>
                    No contributions yet
                  </p>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Groups */}
        {activeNav === "groups" && selectedGroup && (
          <GroupDetails
            group={selectedGroup} user={user} token={token}
            onBack={() => setSelectedGroupId(null)}
            onChanged={refreshAll}
          />
        )}

        {activeNav === "groups" && !selectedGroup && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
              <h2 style={{
                margin: 0, fontFamily: "'Syne', sans-serif",
                fontWeight: "800", color: "#fff", fontSize: "1.5rem"
              }}>My Groups</h2>
              <button onClick={() => setShowCreateGroup(true)} style={{
                padding: "0.7rem 1.25rem",
                background: colors.orange, border: "none",
                borderRadius: "12px", color: "#fff",
                fontFamily: "'Syne', sans-serif", fontWeight: "700",
                fontSize: "0.85rem", cursor: "pointer"
              }}>+ Create Group</button>
            </div>

            {showCreateGroup && (
              <div style={{
                background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.1)",
                borderRadius: "16px", padding: "1.5rem",
                marginBottom: "1.5rem"
              }}>
                <h3 style={{ margin: "0 0 1rem", color: "#fff", fontFamily: "'Syne', sans-serif" }}>
                  Create New Group
                </h3>
                <form onSubmit={handleCreateGroup}>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
                    {[
                      { label: "Group Name", key: "name", placeholder: "e.g. Office Thrift 2026" },
                      { label: "Description", key: "description", placeholder: "Optional description" },
                      { label: "Contribution Amount (₦)", key: "contributionAmount", placeholder: "e.g. 10000", type: "number" },
                      { label: "Total Members", key: "totalMembers", placeholder: "e.g. 10", type: "number" },
                      { label: "Start Date", key: "startDate", type: "date" },
                    ].map(({ label, key, placeholder, type }) => (
                      <div key={key}>
                        <label style={{ display: "block", color: "rgba(255,255,255,0.5)", fontSize: "0.8rem", marginBottom: "0.4rem", textTransform: "uppercase", letterSpacing: "0.5px" }}>{label}</label>
                        <input
                          type={type || "text"} placeholder={placeholder} required={key !== "description"}
                          value={createGroupForm[key]}
                          onChange={e => setCreateGroupForm({ ...createGroupForm, [key]: e.target.value })}
                          style={{
                            width: "100%", padding: "0.75rem",
                            background: "rgba(255,255,255,0.07)",
                            border: "1px solid rgba(255,255,255,0.12)",
                            borderRadius: "10px", color: "#fff",
                            fontFamily: "'DM Sans', sans-serif", fontSize: "0.9rem",
                            outline: "none", boxSizing: "border-box"
                          }}
                        />
                      </div>
                    ))}
                    <div>
                      <label style={{ display: "block", color: "rgba(255,255,255,0.5)", fontSize: "0.8rem", marginBottom: "0.4rem", textTransform: "uppercase", letterSpacing: "0.5px" }}>Frequency</label>
                      <select
                        value={createGroupForm.frequency}
                        onChange={e => setCreateGroupForm({ ...createGroupForm, frequency: e.target.value })}
                        style={{
                          width: "100%", padding: "0.75rem",
                          background: "rgba(255,255,255,0.07)",
                          border: "1px solid rgba(255,255,255,0.12)",
                          borderRadius: "10px", color: "#fff",
                          fontFamily: "'DM Sans', sans-serif", fontSize: "0.9rem",
                          outline: "none", boxSizing: "border-box"
                        }}>
                        {frequencies.map(f => (
                          <option key={f.value} value={f.value} style={{ background: "#1a1a1a" }}>{f.label}</option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label style={{ display: "block", color: "rgba(255,255,255,0.5)", fontSize: "0.8rem", marginBottom: "0.4rem", textTransform: "uppercase", letterSpacing: "0.5px" }}>Currency</label>
                      <select
                        value={createGroupForm.currency}
                        onChange={e => setCreateGroupForm({ ...createGroupForm, currency: e.target.value })}
                        style={{
                          width: "100%", padding: "0.75rem",
                          background: "rgba(255,255,255,0.07)",
                          border: "1px solid rgba(255,255,255,0.12)",
                          borderRadius: "10px", color: "#fff",
                          fontFamily: "'DM Sans', sans-serif", fontSize: "0.9rem",
                          outline: "none", boxSizing: "border-box"
                        }}>
                        {currencies.map(c => (
                          <option key={c.code} value={c.code} style={{ background: "#1a1a1a" }}>
                            {c.symbol} {c.code} - {c.name}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                  <div style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
                    <button type="submit" disabled={loading} style={{
                      padding: "0.75rem 1.5rem",
                      background: colors.orange, border: "none",
                      borderRadius: "10px", color: "#fff",
                      fontFamily: "'Syne', sans-serif", fontWeight: "700",
                      cursor: "pointer"
                    }}>{loading ? "Creating..." : "Create Group"}</button>
                    <button type="button" onClick={() => setShowCreateGroup(false)} style={{
                      padding: "0.75rem 1.5rem",
                      background: "transparent",
                      border: "1px solid rgba(255,255,255,0.2)",
                      borderRadius: "10px", color: "rgba(255,255,255,0.5)",
                      cursor: "pointer", fontFamily: "'DM Sans', sans-serif"
                    }}>Cancel</button>
                  </div>
                </form>
              </div>
            )}

            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "1rem" }}>
              {groups.map((group, i) => (
                <div key={group.id} onClick={() => setSelectedGroupId(group.id)} style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: "16px", padding: "1.25rem",
                  position: "relative", overflow: "hidden", cursor: "pointer"
                }}>
                  <div style={{
                    position: "absolute", top: 0, left: 0, right: 0,
                    height: "4px",
                    background: [colors.orange, colors.green, colors.purple, colors.blue, colors.pink][i % 5]
                  }} />
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "0.75rem" }}>
                    <h3 style={{ margin: 0, color: "#fff", fontFamily: "'Syne', sans-serif", fontWeight: "700", fontSize: "1rem" }}>
                      {group.name}
                    </h3>
                    <span style={{
                      padding: "0.2rem 0.6rem", borderRadius: "20px",
                      fontSize: "0.7rem", fontWeight: "600",
                      background: `${statusColor(group.status)}20`,
                      color: statusColor(group.status)
                    }}>{group.status}</span>
                  </div>
                  {group.description && (
                    <p style={{ margin: "0 0 0.75rem", color: "rgba(255,255,255,0.4)", fontSize: "0.85rem" }}>
                      {group.description}
                    </p>
                  )}
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.5rem" }}>
                    {[
                      { label: "Amount", value: `₦${group.contributionAmount?.toLocaleString()}` },
                      { label: "Members", value: `${group.memberIds?.length || 0}/${group.totalMembers}` },
                      { label: "Cycle", value: group.startedAt ? `${group.currentCycle}/${group.totalCycles}` : "Not started" },
                      { label: "Frequency", value: group.frequency },
                    ].map(({ label, value }) => (
                      <div key={label} style={{
                        background: "rgba(255,255,255,0.05)",
                        borderRadius: "8px", padding: "0.5rem 0.75rem"
                      }}>
                        <p style={{ margin: 0, color: "rgba(255,255,255,0.4)", fontSize: "0.7rem", textTransform: "uppercase", letterSpacing: "0.5px" }}>{label}</p>
                        <p style={{ margin: 0, color: "#fff", fontSize: "0.85rem", fontWeight: "500" }}>{value}</p>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
              {groups.length === 0 && (
                <div style={{
                  gridColumn: "1/-1", textAlign: "center",
                  padding: "3rem", color: "rgba(255,255,255,0.3)"
                }}>
                  <div style={{ fontSize: "3rem", marginBottom: "1rem" }}>👥</div>
                  <p>No groups yet. Create your first thrift group!</p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Contributions */}
        {activeNav === "contributions" && (
          <div>
            <h2 style={{ margin: "0 0 1.5rem", fontFamily: "'Syne', sans-serif", fontWeight: "800", color: "#fff", fontSize: "1.5rem" }}>
              My Contributions
            </h2>
            <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
              {contributions.map(c => (
                <div key={c.id} style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: "14px", padding: "1rem 1.25rem",
                  display: "flex", justifyContent: "space-between", alignItems: "center"
                }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                    <div style={{
                      width: "42px", height: "42px", borderRadius: "12px",
                      background: `${statusColor(c.status)}20`,
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontSize: "1.2rem"
                    }}>💳</div>
                    <div>
                      <p style={{ margin: 0, color: "#fff", fontWeight: "500" }}>
                        ₦{c.amount?.toLocaleString()}
                      </p>
                      <p style={{ margin: 0, color: "rgba(255,255,255,0.4)", fontSize: "0.8rem" }}>
                        {groupName(c.groupId) ? `${groupName(c.groupId)} · ` : ""}Cycle {c.cycleNumber} · {c.currency}
                      </p>
                    </div>
                  </div>
                  <div style={{ textAlign: "right", display: "flex", alignItems: "center", gap: "0.75rem" }}>
                    {["PENDING", "PROCESSING", "FAILED"].includes(c.status) && (
                      <button onClick={() => payContribution(c)} disabled={payingId === c.id} style={primaryButton}>
                        {payingId === c.id ? "Opening..." : c.status === "PENDING" ? "Pay now" : "Pay again"}
                      </button>
                    )}
                    <div>
                    <StatusPill status={c.status} />
                    {c.dueDate && (
                      <p style={{ margin: "0.25rem 0 0", color: "rgba(255,255,255,0.3)", fontSize: "0.75rem" }}>
                        Due: {new Date(c.dueDate).toLocaleDateString()}
                      </p>
                    )}
                    </div>
                  </div>
                </div>
              ))}
              {contributions.length === 0 && (
                <div style={{ textAlign: "center", padding: "3rem", color: "rgba(255,255,255,0.3)" }}>
                  <div style={{ fontSize: "3rem", marginBottom: "1rem" }}>💳</div>
                  <p>No contributions yet</p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Payouts */}
        {activeNav === "payouts" && (
          <div>
            <h2 style={{ margin: "0 0 1.5rem", fontFamily: "'Syne', sans-serif", fontWeight: "800", color: "#fff", fontSize: "1.5rem" }}>
              My Payouts
            </h2>
            <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
              {payouts.map(p => (
                <div key={p.id} style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: "14px", padding: "1rem 1.25rem",
                  display: "flex", justifyContent: "space-between", alignItems: "center"
                }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                    <div style={{
                      width: "42px", height: "42px", borderRadius: "12px",
                      background: "rgba(0,200,150,0.15)",
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontSize: "1.2rem"
                    }}>💸</div>
                    <div>
                      <p style={{ margin: 0, color: "#fff", fontWeight: "500" }}>
                        ₦{p.amount?.toLocaleString()}
                      </p>
                      <p style={{ margin: 0, color: "rgba(255,255,255,0.4)", fontSize: "0.8rem" }}>
                        {groupName(p.groupId) ? `${groupName(p.groupId)} · ` : ""}Cycle {p.cycleNumber} · {p.currency}
                      </p>
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <StatusPill status={p.payoutStatus} />
                    {p.scheduledDate && (
                      <p style={{ margin: "0.25rem 0 0", color: "rgba(255,255,255,0.3)", fontSize: "0.75rem" }}>
                        Scheduled: {new Date(p.scheduledDate).toLocaleDateString()}
                      </p>
                    )}
                  </div>
                </div>
              ))}
              {payouts.length === 0 && (
                <div style={{ textAlign: "center", padding: "3rem", color: "rgba(255,255,255,0.3)" }}>
                  <div style={{ fontSize: "3rem", marginBottom: "1rem" }}>💸</div>
                  <p>No payouts yet</p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Currencies */}
        {activeNav === "currencies" && (
          <div>
            <h2 style={{ margin: "0 0 1.5rem", fontFamily: "'Syne', sans-serif", fontWeight: "800", color: "#fff", fontSize: "1.5rem" }}>
              Currencies
            </h2>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "1rem" }}>
              {currencies.map((c, i) => (
                <div key={c.code} style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: "14px", padding: "1.25rem",
                  position: "relative", overflow: "hidden"
                }}>
                  <div style={{
                    position: "absolute", top: "-15px", right: "-15px",
                    width: "60px", height: "60px", borderRadius: "50%",
                    background: [colors.orange, colors.green, colors.purple, colors.blue, colors.pink][i % 5],
                    opacity: 0.15
                  }} />
                  <p style={{
                    margin: "0 0 0.25rem",
                    fontFamily: "'Syne', sans-serif", fontWeight: "800",
                    fontSize: "1.5rem", color: "#fff"
                  }}>{c.symbol}</p>
                  <p style={{ margin: 0, color: "#fff", fontWeight: "500" }}>{c.code}</p>
                  <p style={{ margin: "0.25rem 0 0", color: "rgba(255,255,255,0.4)", fontSize: "0.8rem" }}>{c.name}</p>
                  <p style={{
                    margin: "0.75rem 0 0",
                    fontFamily: "'Syne', sans-serif",
                    color: "#00c896", fontSize: "0.9rem", fontWeight: "700"
                  }}>₦{c.exchangeRate?.toLocaleString()}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Profile */}
        {activeNav === "profile" && (
          <div>
            <h2 style={{ margin: "0 0 1.5rem", fontFamily: "'Syne', sans-serif", fontWeight: "800", color: "#fff", fontSize: "1.5rem" }}>
              My Profile
            </h2>
            <div style={{
              background: "rgba(255,255,255,0.04)",
              border: "1px solid rgba(255,255,255,0.08)",
              borderRadius: "20px", padding: "2rem",
              maxWidth: "500px"
            }}>
              <div style={{ display: "flex", alignItems: "center", gap: "1.25rem", marginBottom: "2rem" }}>
                <div style={{
                  width: "72px", height: "72px", borderRadius: "50%",
                  background: colors.purple,
                  display: "flex", alignItems: "center", justifyContent: "center",
                  fontFamily: "'Syne', sans-serif", fontWeight: "800",
                  color: "#fff", fontSize: "1.5rem"
                }}>
                  {user.firstName?.[0]}{user.lastName?.[0]}
                </div>
                <div>
                  <h3 style={{ margin: 0, color: "#fff", fontFamily: "'Syne', sans-serif", fontWeight: "700" }}>
                    {user.firstName} {user.lastName}
                  </h3>
                  <p style={{ margin: "0.25rem 0 0", color: "rgba(255,255,255,0.4)", fontSize: "0.9rem" }}>{user.email}</p>
                </div>
              </div>

              {[
                { label: "Email", value: user.email, icon: "📧" },
                { label: "User ID", value: user.id, icon: "🔑" },
              ].map(({ label, value, icon }) => (
                <div key={label} style={{
                  display: "flex", justifyContent: "space-between",
                  alignItems: "center", padding: "0.75rem 0",
                  borderBottom: "1px solid rgba(255,255,255,0.05)"
                }}>
                  <span style={{ color: "rgba(255,255,255,0.5)", fontSize: "0.85rem" }}>
                    {icon} {label}
                  </span>
                  <span style={{ color: "#fff", fontSize: "0.85rem" }}>{value}</span>
                </div>
              ))}

              {/* WhatsApp support link */}
              <div style={{ marginTop: "1.5rem" }}>
                <a
                  href="https://wa.me/2349137205789?text=Hello%2C%20I%20need%20support%20with%20the%20Thrift%20System%20app"
                  target="_blank"
                  rel="noreferrer"
                  style={{
                    display: "flex", alignItems: "center", justifyContent: "center",
                    gap: "0.5rem", padding: "0.75rem",
                    background: "rgba(37,211,102,0.15)",
                    border: "1px solid rgba(37,211,102,0.3)",
                    borderRadius: "12px", color: "#25d366",
                    textDecoration: "none", fontWeight: "500",
                    fontSize: "0.9rem"
                  }}>
                  💬 Contact Support on WhatsApp
                </a>
              </div>
            </div>
            <BankAccount token={token} />
          </div>
        )}
      </div>
    </div>
  );
}
