import { Outlet } from 'react-router-dom';
import ChatSidebar from '../components/ChatSidebar';

export default function ChatLayout() {
  return (
    <div className="h-screen flex bg-gray-50">
      {/* Sidebar */}
      <div className="w-80 shrink-0 border-r border-gray-200 bg-white">
        <ChatSidebar />
      </div>

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        <Outlet />
      </div>
    </div>
  );
}
