import { CheckCircle2, CircleAlert, Info, X } from "lucide-react";
import { createContext, useCallback, useContext, useMemo, useState } from "react";

type ToastTone = "success" | "error" | "info";

type ToastItem = {
  id: number;
  tone: ToastTone;
  title: string;
  description?: string;
};

type ToastContextValue = {
  showToast: (toast: Omit<ToastItem, "id">) => void;
  success: (title: string, description?: string) => void;
  error: (title: string, description?: string) => void;
  info: (title: string, description?: string) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((items) => items.filter((item) => item.id !== id));
  }, []);

  const showToast = useCallback((toast: Omit<ToastItem, "id">) => {
    const id = Date.now() + Math.floor(Math.random() * 1000);
    setToasts((items) => [...items, { ...toast, id }]);
    window.setTimeout(() => dismiss(id), 3600);
  }, [dismiss]);

  const value = useMemo<ToastContextValue>(() => ({
    showToast,
    success: (title, description) => showToast({ tone: "success", title, description }),
    error: (title, description) => showToast({ tone: "error", title, description }),
    info: (title, description) => showToast({ tone: "info", title, description })
  }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite" aria-atomic="true">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast-card ${toast.tone}`}>
            <div className="toast-icon">
              {toast.tone === "success" ? <CheckCircle2 size={18} /> : toast.tone === "error" ? <CircleAlert size={18} /> : <Info size={18} />}
            </div>
            <div className="toast-copy">
              <strong>{toast.title}</strong>
              {toast.description ? <span>{toast.description}</span> : null}
            </div>
            <button className="toast-close" type="button" onClick={() => dismiss(toast.id)} aria-label="关闭提示">
              <X size={16} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within ToastProvider");
  }
  return context;
}
