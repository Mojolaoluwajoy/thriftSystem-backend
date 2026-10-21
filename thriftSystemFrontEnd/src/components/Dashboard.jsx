import { useState, useEffect } from "react";

const API_BASE = "http://localhost:8080/api/v1";

const colors = {
  orange: "linear-gradient(135deg, #ff6400, #ff9500)",
  green: "linear-gradient(135deg, #00c896, #00e6a8)",
  purple: "linear-gradient(135deg, #7c3aed, #a855f7)",
  blue: "linear-gradient(135deg, #0ea5e9, #38bdf8)",
  pink: "linear-gradient(135deg, #ec4899, #f472b6)",
};

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

  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`
  };

  useEffect(() => {
    const hour = new Date().getHours();
    if (hour < 12) setGreeting("Good morning");
    else if (hour < 17) setGreeting("Good afternoon");
    else setGreeting("Good evening");
  }, []);

  useEffect(() => {
    fetchGroups();
    fetchContributions();
    fetchPayouts();
    fetchCurrencies();
  }, []);

  const fetchGroups = async () => {
    try {
      const res = await fetch(`${API_BASE}/group/my-groups`, { headers });
      const data = await res.json();
      if (data.success) setGroups(data.data || []);
    } catch {}
  };

  const fetchContributions = async () => {
    try {
      const res = await fetch(`${API_BASE}/contributions/my-contributions`, { headers });
      const data = await res.json();
      if (data.success) setContributions(data.data || []);
    } catch {}
  };

  const fetchPayouts = async () => {
    try {
      const res = await fetch(`${API_BASE}/payouts/my-payouts`, { headers });
      const data = await res.json();
      if (data.success) setPayouts(data.data || []);
    } catch {}
  };

  const fetchCurrencies = async () => {
    try {
      const res = await fetch(`${API_BASE}/currencies`, { headers });
      const data = await res.json();
      if (data.success) setCurrencies(data.data || []);
    } catch {}
  };

  const handleCreateGroup = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/group/create-group`, {
        method: "POST", headers,
        body: JSON.stringify({
          ...createGroupForm,
          contributionAmount: parseFloat(createGroupForm.contributionAmount),
          totalMembers: parseInt(createGroupForm.totalMembers),
          startDate: new Date(createGroupForm.startDate).toISOString()
        })
      });
      const data = await res.json();
      if (data.success) {
        setShowCreateGroup(false);
        fetchGroups();
        setCreateGroupForm({
          name: "", description: "", contributionAmount: "",
          currency: "NGN", frequency: "MONTHLY",
          startDate: "", totalMembers: ""
        });
      }
    } catch {}
    setLoading(false);
  };

  const statusColor = (status) => {
    const map = {
      ACTIVE: "#00c896", COMPLETED: "#0ea5e9",
      PENDING: "#ff9500", SUCCESSFUL: "#00c896",
      FAILED: "#ef4444", PROCESSING: "#a855f7",
      SUSPENDED: "#ef4444", INACTIVE: "#888"
    };
    return map[status] || "#888";
  };

  const totalContributed = contributions
    .filter(c => c.status === "SUCCESSFUL")
    .reduce((sum, c) => sum + (c.amount || 0), 0);

  const totalPayouts = payouts
    .filter(p => p.status === "SUCCESSFUL")
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
            <button key={item.id} onClick={() => setActiveNav(item.id)}
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
      <div style={{ flex: 1, padding: "2rem", overflowY: "auto" }}>

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
        {activeNav === "groups" && (
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
                        {["DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", "BIMONTHLY"].map(f => (
                          <option key={f} value={f} style={{ background: "#1a1a1a" }}>{f}</option>
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
                <div key={group.id} style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: "16px", padding: "1.25rem",
                  position: "relative", overflow: "hidden"
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
                      { label: "Cycle", value: `${group.currentCycle}/${group.totalCycles}` },
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
                        Cycle {c.cycleNumber} · {c.currency}
                      </p>
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <span style={{
                      padding: "0.25rem 0.75rem", borderRadius: "20px",
                      fontSize: "0.75rem", fontWeight: "600",
                      background: `${statusColor(c.status)}20`,
                      color: statusColor(c.status)
                    }}>{c.status}</span>
                    {c.dueDate && (
                      <p style={{ margin: "0.25rem 0 0", color: "rgba(255,255,255,0.3)", fontSize: "0.75rem" }}>
                        Due: {new Date(c.dueDate).toLocaleDateString()}
                      </p>
                    )}
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
                        Cycle {p.cycleNumber} · {p.currency}
                      </p>
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <span style={{
                      padding: "0.25rem 0.75rem", borderRadius: "20px",
                      fontSize: "0.75rem", fontWeight: "600",
                      background: `${statusColor(p.status)}20`,
                      color: statusColor(p.status)
                    }}>{p.status}</span>
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
          </div>
        )}
      </div>
    </div>
  );
}
