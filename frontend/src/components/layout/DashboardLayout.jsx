import { useState } from 'react';
import Navbar from './Navbar';
import Sidebar from './Sidebar';
import './DashboardLayout.css';

export default function DashboardLayout({ children, onSearch, searchQuery, onOpenSell }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="cc-dashboard-layout">
      <Navbar
        onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}
        onSearch={onSearch}
        searchQuery={searchQuery}
      />
      <div className="cc-dashboard-layout__body">
        <Sidebar
          isOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          onOpenSell={onOpenSell}
        />
        <main className="cc-dashboard-layout__content">
          {children}
        </main>
      </div>
    </div>
  );
}
