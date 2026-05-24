import { Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { HistoryPage } from "./pages/HistoryPage";
import { HotwordsPage } from "./pages/HotwordsPage";
import { ModelConfigPage } from "./pages/ModelConfigPage";
import { WorkbenchPage } from "./pages/WorkbenchPage";
import { ExportCenterPage } from "./pages/ExportCenterPage";

export default function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<WorkbenchPage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/exports" element={<ExportCenterPage />} />
        <Route path="/hotwords" element={<HotwordsPage />} />
        <Route path="/config" element={<ModelConfigPage />} />
      </Routes>
    </AppShell>
  );
}
