import { ChartColumn, ClipboardList, History, Home, Settings } from "lucide-react";
import { NavLink } from "react-router-dom";

const navItems = [
  { to: "/", label: "工作台", icon: Home },
  { to: "/history", label: "历史记录", icon: History },
  { to: "/hotwords", label: "热词管理", icon: ClipboardList },
  { to: "/config", label: "模型配置", icon: Settings }
];

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-badge">∿</div>
          <div>
            <div className="brand-title">VoiceInput Pro</div>
            <div className="brand-subtitle">Web 智能语音输入工作台</div>
          </div>
        </div>
        <nav className="topnav">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
              <Icon size={18} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="topbar-actions">
          <button className="ghost-pill" type="button">
            <ChartColumn size={16} />
            <span>帮助</span>
          </button>
          <div className="user-pill">演示账号</div>
        </div>
      </header>
      <main className="page-shell">{children}</main>
    </div>
  );
}

