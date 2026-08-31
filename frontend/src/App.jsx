import React, { useState } from 'react';
import { 
  LayoutDashboard, Store, Wallet, Users, Smartphone, FileText, 
  ShieldCheck, Settings, History, Lock, Unlock, AlertTriangle, 
  QrCode, Search, Filter, Plus, Bell, ChevronRight, CheckCircle2, 
  Clock, XCircle, ArrowUpRight, ArrowDownRight, RefreshCw, Eye,
  CreditCard, PhoneCall, ChevronDown, Check, Menu, X, LogOut
} from 'lucide-react';

// Brand Logo Component
const AbizLogo = ({ className = "w-8 h-8", textClass = "text-xl" }) => (
  <div className="flex items-center gap-2.5">
    <svg className={className} viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M50 10 L90 85 L10 85 Z" stroke="#0EA5E9" strokeWidth="12" strokeLinejoin="round" fill="none" />
      <path d="M50 28 L78 78 L22 78 Z" stroke="#2563EB" strokeWidth="8" strokeLinejoin="round" fill="none" opacity="0.6" />
      <circle cx="50" cy="58" r="8" fill="#0EA5E9" />
    </svg>
    <div className="flex flex-col">
      <span className={`font-black tracking-tight text-white ${textClass}`}>ABIZ LOCKER</span>
      <span className="text-[9px] font-semibold text-slate-400 -mt-1 tracking-widest uppercase">Device Financing</span>
    </div>
  </div>
);

export default function App() {
  const [activeScreen, setActiveScreen] = useState('superadmin-dash');
  const [phoneState, setPhoneState] = useState('LOCKED'); // 'LOCKED' | 'WARNING' | 'UNLOCKED'
  const [isAddShopOpen, setIsAddShopOpen] = useState(false);
  const [isAddCreditsOpen, setIsAddCreditsOpen] = useState(false);
  const [isRecordPaymentOpen, setIsRecordPaymentOpen] = useState(false);
  const [isAddCustomerOpen, setIsAddCustomerOpen] = useState(false);

  // Navigation Items
  const navItems = [
    { id: 'superadmin-dash', label: 'Dashboard', icon: LayoutDashboard, badge: 'SuperAdmin' },
    { id: 'shop-mgmt', label: 'Shops', icon: Store },
    { id: 'credit-mgmt', label: 'Credit Driver', icon: Wallet },
    { id: 'shop-owner-dash', label: 'Shop Dashboard', icon: LayoutDashboard, badge: 'Shop' },
    { id: 'customer-mgmt', label: 'Customers', icon: Users },
    { id: 'device-mgmt', label: 'Devices', icon: Smartphone },
    { id: 'device-detail', label: 'Device Detail', icon: Smartphone },
    { id: 'qr-enroll', label: 'Enrollment', icon: QrCode },
    { id: 'installment-contract', label: 'Installment', icon: FileText },
    { id: 'mobile-warning', label: 'Mobile: Warning', icon: AlertTriangle, badge: 'App' },
    { id: 'mobile-locked', label: 'Mobile: Locked', icon: Lock, badge: 'App' },
    { id: 'audit-logs', label: 'Audits', icon: History },
    { id: 'settings', label: 'Settings', icon: Settings },
    { id: 'login', label: 'Login Screen', icon: LogOut, badge: 'Auth' },
  ];

  return (
    <div className="min-h-screen bg-[#F8F6F0] text-slate-800 flex flex-col font-sans">
      
      {/* Top Global Quick Screen Switcher Banner (For Demo Navigation) */}
      <div className="bg-slate-900 text-white px-4 py-2 text-xs flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 sticky top-0 z-50">
        <div className="flex items-center gap-2">
          <span className="bg-sky-500 text-slate-950 font-extrabold px-1.5 py-0.5 rounded text-[10px] uppercase">14-Screen Live Suite</span>
          <span className="text-slate-300 font-medium hidden sm:inline">Click any tab to switch view instantly:</span>
        </div>
        <div className="flex flex-wrap gap-1">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => setActiveScreen(item.id)}
              className={`px-2.5 py-1 rounded text-[11px] font-medium transition-all ${
                activeScreen === item.id 
                  ? 'bg-sky-500 text-slate-950 font-bold shadow' 
                  : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      <div className="flex-1 flex overflow-hidden">
        
        {/* SIDEBAR (Exact Match to Design: Dark Navy #0F172A) */}
        {activeScreen !== 'login' && (
          <aside className="w-64 bg-[#0F172A] text-slate-300 flex flex-col justify-between shrink-0 shadow-xl">
            <div>
              {/* Logo Area */}
              <div className="p-6 border-b border-slate-800/80">
                <AbizLogo />
              </div>

              {/* Navigation Links */}
              <nav className="p-4 space-y-1">
                {navItems.filter(n => n.id !== 'login').map((item) => {
                  const Icon = item.icon;
                  const isActive = activeScreen === item.id;
                  return (
                    <button
                      key={item.id}
                      onClick={() => setActiveScreen(item.id)}
                      className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all ${
                        isActive
                          ? 'bg-sky-500 text-slate-950 shadow-md font-bold'
                          : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <Icon className={`w-4 h-4 ${isActive ? 'text-slate-950' : 'text-slate-400'}`} />
                        <span>{item.label}</span>
                      </div>
                      {item.badge && (
                        <span className={`text-[9px] px-1.5 py-0.5 rounded font-bold uppercase ${
                          isActive ? 'bg-slate-950/20 text-slate-950' : 'bg-slate-800 text-slate-400'
                        }`}>
                          {item.badge}
                        </span>
                      )}
                    </button>
                  );
                })}
              </nav>
            </div>

            {/* Bottom Profile Section */}
            <div className="p-4 border-t border-slate-800/80">
              <div className="flex items-center gap-3 p-2 rounded-xl bg-slate-900/60 border border-slate-800">
                <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-sky-400 to-blue-600 flex items-center justify-center font-bold text-white text-xs">
                  AU
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-bold text-white truncate">Admin User</p>
                  <p className="text-[10px] text-slate-400 truncate">superadmin@abiz.com</p>
                </div>
              </div>
            </div>
          </aside>
        )}

        {/* MAIN CONTENT CANVAS (Creamy Off-White Background: #F8F6F0) */}
        <div className="flex-1 flex flex-col overflow-y-auto bg-[#F8F6F0]">
          
          {/* Top Header Bar */}
          {activeScreen !== 'login' && (
            <header className="h-16 bg-[#F8F6F0] border-b border-[#E8E4D8] px-8 flex items-center justify-between shrink-0">
              <div className="flex items-center gap-4">
                <h2 className="text-xl font-extrabold text-slate-900 tracking-tight">
                  {navItems.find(n => n.id === activeScreen)?.label || 'Dashboard'}
                </h2>
              </div>

              <div className="flex items-center gap-4">
                {/* Search Bar */}
                <div className="relative">
                  <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
                  <input 
                    type="text" 
                    placeholder="Search anything..." 
                    className="pl-9 pr-4 py-1.5 bg-white border border-[#E2DEC9] rounded-lg text-xs w-64 focus:outline-none focus:ring-2 focus:ring-sky-500/20 text-slate-700"
                  />
                </div>

                {/* Notifications */}
                <button className="w-9 h-9 bg-white border border-[#E2DEC9] rounded-lg flex items-center justify-center text-slate-600 hover:text-slate-900 shadow-sm relative">
                  <Bell className="w-4 h-4" />
                  <span className="w-2 h-2 rounded-full bg-rose-500 absolute top-2 right-2 ring-2 ring-white"></span>
                </button>

                {/* User Avatar */}
                <div className="flex items-center gap-2 pl-2 border-l border-[#E2DEC9]">
                  <div className="w-8 h-8 rounded-full bg-slate-900 text-white font-bold text-xs flex items-center justify-center">
                    A
                  </div>
                </div>
              </div>
            </header>
          )}

          {/* DYNAMIC SCREEN CONTENT INJECTOR */}
          <div className="p-8">
            
            {/* SCREEN 1: LOGIN PAGE */}
            {activeScreen === 'login' && (
              <div className="min-h-[85vh] flex items-center justify-center">
                <div className="w-full max-w-md bg-white border border-[#E2DEC9] rounded-2xl p-8 shadow-xl text-center space-y-6">
                  <div className="flex justify-center">
                    <svg className="w-16 h-16" viewBox="0 0 100 100" fill="none">
                      <path d="M50 10 L90 85 L10 85 Z" stroke="#0EA5E9" strokeWidth="10" strokeLinejoin="round" fill="none" />
                      <path d="M50 28 L78 78 L22 78 Z" stroke="#2563EB" strokeWidth="7" strokeLinejoin="round" fill="none" opacity="0.6" />
                      <circle cx="50" cy="58" r="7" fill="#0EA5E9" />
                    </svg>
                  </div>
                  <div>
                    <h3 className="text-2xl font-black text-slate-900">Welcome</h3>
                    <p className="text-xs text-slate-500 font-medium mt-1">Android Device Financing</p>
                  </div>
                  <div className="space-y-4 text-left">
                    <div>
                      <label className="text-[11px] font-bold text-slate-600 uppercase">Email/Username</label>
                      <input 
                        type="text" 
                        defaultValue="admin@abizlocker.com" 
                        className="w-full mt-1.5 px-3.5 py-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl text-xs focus:ring-2 focus:ring-sky-500/30 outline-none font-medium"
                      />
                    </div>
                    <div>
                      <label className="text-[11px] font-bold text-slate-600 uppercase">Password</label>
                      <input 
                        type="password" 
                        defaultValue="••••••••••••" 
                        className="w-full mt-1.5 px-3.5 py-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl text-xs focus:ring-2 focus:ring-sky-500/30 outline-none font-medium"
                      />
                    </div>
                    <div className="flex items-center justify-between text-xs">
                      <label className="flex items-center gap-2 text-slate-600 cursor-pointer">
                        <input type="checkbox" defaultChecked className="rounded border-slate-300 text-sky-500" />
                        Remember Me
                      </label>
                      <a href="#" className="text-sky-600 font-semibold hover:underline">Forgot Password?</a>
                    </div>
                    <button 
                      onClick={() => setActiveScreen('superadmin-dash')}
                      className="w-full py-3 bg-[#0F172A] hover:bg-slate-800 text-white font-bold text-xs rounded-xl shadow-lg transition-all uppercase tracking-wider"
                    >
                      Sign In
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* SCREEN 2: SUPER ADMIN DASHBOARD */}
            {activeScreen === 'superadmin-dash' && (
              <div className="space-y-6">
                
                {/* 6 Top Metric Tiles */}
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
                  {[
                    { label: 'Total Shops', val: '3,088', badge: 'Active Shops', color: 'text-slate-900', subColor: 'text-emerald-600 bg-emerald-50' },
                    { label: 'Active Devices', val: '2,664', badge: 'Locked Devices', color: 'text-emerald-600', subColor: 'text-emerald-600 bg-emerald-50' },
                    { label: 'Locked Devices', val: '136', badge: 'Locked Devices', color: 'text-rose-500', subColor: 'text-rose-600 bg-rose-50' },
                    { label: 'Overdue Devices', val: '05', badge: 'Overdue Devices', color: 'text-amber-500', subColor: 'text-amber-600 bg-amber-50' },
                    { label: 'Credits Issued', val: '275', badge: 'Credits Issued', color: 'text-indigo-600', subColor: 'text-indigo-600 bg-indigo-50' },
                    { label: 'Credits Consumed', val: '360', badge: 'Credits Consumed', color: 'text-blue-600', subColor: 'text-blue-600 bg-blue-50' },
                  ].map((tile, i) => (
                    <div key={i} className="bg-white border border-[#E2DEC9] p-4 rounded-2xl shadow-sm space-y-1">
                      <span className="text-[11px] font-bold text-slate-500 uppercase">{tile.label}</span>
                      <p className={`text-2xl font-black ${tile.color}`}>{tile.val}</p>
                      <span className={`inline-block text-[9px] font-bold px-2 py-0.5 rounded-full ${tile.subColor}`}>
                        {tile.badge}
                      </span>
                    </div>
                  ))}
                </div>

                {/* Middle Row: Device Status Overview (Donut) & Payment Analytics (Wave Area Chart) */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                  
                  {/* Left: Device Status Overview */}
                  <div className="lg:col-span-5 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-4">
                    <h3 className="font-bold text-slate-900 text-sm">Device Status Overview</h3>
                    <div className="flex items-center justify-around py-2">
                      <div className="relative w-36 h-36 flex items-center justify-center">
                        <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
                          <circle cx="50" cy="50" r="40" stroke="#E2DEC9" strokeWidth="12" fill="none" />
                          <circle cx="50" cy="50" r="40" stroke="#0EA5E9" strokeWidth="12" strokeDasharray="251.2" strokeDashoffset="50" fill="none" />
                          <circle cx="50" cy="50" r="40" stroke="#F59E0B" strokeWidth="12" strokeDasharray="251.2" strokeDashoffset="210" fill="none" />
                          <circle cx="50" cy="50" r="40" stroke="#EF4444" strokeWidth="12" strokeDasharray="251.2" strokeDashoffset="235" fill="none" />
                        </svg>
                        <div className="absolute text-center">
                          <span className="text-xl font-black text-slate-900">80%</span>
                          <span className="text-[9px] text-slate-400 block font-semibold">Overview</span>
                        </div>
                      </div>

                      <div className="space-y-2 text-xs">
                        <div className="flex items-center justify-between gap-6"><span className="text-slate-500">Total Shops:</span><strong className="text-slate-900">100</strong></div>
                        <div className="flex items-center justify-between gap-6"><span className="text-slate-500">Active Devices:</span><strong className="text-emerald-600">70</strong></div>
                        <div className="flex items-center justify-between gap-6"><span className="text-slate-500">Locked Devices:</span><strong className="text-rose-600">10</strong></div>
                        <div className="flex items-center justify-between gap-6"><span className="text-slate-500">Overdue Devices:</span><strong className="text-amber-500">0</strong></div>
                        <div className="flex items-center justify-between gap-6"><span className="text-slate-500">Offline:</span><strong className="text-slate-400">0</strong></div>
                      </div>
                    </div>
                  </div>

                  {/* Right: Payment Collection Analytics */}
                  <div className="lg:col-span-7 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="font-bold text-slate-900 text-sm">Payment Collection Analytics</h3>
                      <span className="text-xs text-slate-400">Jan - Oct</span>
                    </div>
                    <div className="h-44 flex items-end justify-between gap-2 pt-4 px-2">
                      {[30, 45, 60, 40, 75, 55, 90, 65, 80, 95].map((h, i) => (
                        <div key={i} className="flex-1 flex flex-col items-center gap-1.5">
                          <div className="w-full bg-sky-100 rounded-t-lg relative group overflow-hidden" style={{ height: `${h}%` }}>
                            <div className="w-full h-full bg-sky-500 opacity-80 group-hover:opacity-100 transition-all"></div>
                          </div>
                          <span className="text-[10px] text-slate-400 font-semibold">{['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct'][i]}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                </div>

                {/* Bottom Row: Analytics & Recent Activity */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                  
                  {/* Left: Device Registration Analytics */}
                  <div className="lg:col-span-4 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-3">
                    <h3 className="font-bold text-slate-900 text-sm">Device Registration Analytics</h3>
                    <div className="h-32 flex items-center justify-center text-slate-400 border border-dashed border-[#E2DEC9] rounded-xl bg-[#FAF9F5]">
                      <span className="text-xs">📈 Dynamic Growth: +14% WoW</span>
                    </div>
                  </div>

                  {/* Middle: Payment Collection Bar */}
                  <div className="lg:col-span-4 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-3">
                    <h3 className="font-bold text-slate-900 text-sm">Payment Collection Analytics</h3>
                    <div className="h-32 flex items-end justify-around gap-2 px-2 pb-2">
                      {[40, 65, 85, 55, 75].map((v, i) => (
                        <div key={i} className="w-8 bg-[#0F172A] rounded-t-md" style={{ height: `${v}%` }}></div>
                      ))}
                    </div>
                  </div>

                  {/* Right: Recent Activity Table */}
                  <div className="lg:col-span-4 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-3">
                    <h3 className="font-bold text-slate-900 text-sm">Recent Activity</h3>
                    <div className="space-y-2 text-xs">
                      {[
                        { device: 'Device 1', date: '03 Nov 2021', time: 'Today' },
                        { device: '2024-03-27', date: '03 Nov 2021', time: '1 day ago' },
                        { device: '2024-03-27', date: '03 Nov 2021', time: 'Today' },
                      ].map((act, i) => (
                        <div key={i} className="flex items-center justify-between p-2 rounded-lg bg-[#FAF9F5] border border-[#E2DEC9]">
                          <span className="font-semibold text-slate-800">{act.device}</span>
                          <span className="text-slate-500">{act.date}</span>
                          <span className="text-[10px] text-sky-600 font-bold">{act.time}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                </div>

              </div>
            )}

            {/* SCREEN 3: SHOP MANAGEMENT */}
            {activeScreen === 'shop-mgmt' && (
              <div className="space-y-6">
                
                {/* Search & Actions Bar */}
                <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
                  <div className="flex items-center gap-3 w-full sm:w-auto">
                    <div className="relative flex-1 sm:w-64">
                      <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
                      <input type="text" placeholder="Search Shop..." className="w-full pl-9 pr-4 py-2 bg-white border border-[#E2DEC9] rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-sky-500" />
                    </div>
                    <button className="px-3.5 py-2 bg-white border border-[#E2DEC9] rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 shadow-sm">
                      <Filter className="w-3.5 h-3.5" /> Filter
                    </button>
                  </div>
                  <button 
                    onClick={() => setIsAddShopOpen(true)}
                    className="px-4 py-2 bg-[#0F172A] hover:bg-slate-800 text-white font-bold text-xs rounded-xl shadow transition-all flex items-center gap-2"
                  >
                    <Plus className="w-4 h-4" /> Add Shop
                  </button>
                </div>

                {/* Shop Data Table */}
                <div className="bg-white border border-[#E2DEC9] rounded-2xl overflow-hidden shadow-sm">
                  <table className="w-full text-left text-xs text-slate-600">
                    <thead className="bg-[#FAF9F5] text-slate-500 uppercase text-[10px] font-bold border-b border-[#E2DEC9]">
                      <tr>
                        <th className="p-4">Shop Name</th>
                        <th className="p-4">Status</th>
                        <th className="p-4">Devices</th>
                        <th className="p-4">Credits</th>
                        <th className="p-4">Created Date</th>
                        <th className="p-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#E2DEC9]">
                      {[
                        { name: 'Shop 1 Management', status: 'Active', devices: '128', credits: '45,000 PKR', date: '08-14-2024' },
                        { name: 'Arsalan Devices', status: 'Active', devices: '94', credits: '30,000 PKR', date: '08-14-2024' },
                        { name: 'Anas Electronics', status: 'Active', devices: '310', credits: '120,000 PKR', date: '08-14-2024' },
                        { name: 'Arsalan Devices', status: 'Active', devices: '45', credits: '15,000 PKR', date: '08-14-2024' },
                        { name: 'Anas Electronics', status: 'Inactive', devices: '0', credits: '0 PKR', date: '08-14-2024' },
                        { name: 'Ali Telecom', status: 'Active', devices: '82', credits: '28,000 PKR', date: '08-14-2024' },
                      ].map((shop, i) => (
                        <tr key={i} className="hover:bg-[#FAF9F5] transition-all">
                          <td className="p-4 font-bold text-slate-900">{shop.name}</td>
                          <td className="p-4">
                            <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                              shop.status === 'Active' ? 'bg-emerald-50 text-emerald-600 border border-emerald-200' : 'bg-rose-50 text-rose-600 border border-rose-200'
                            }`}>
                              {shop.status}
                            </span>
                          </td>
                          <td className="p-4 font-mono font-semibold">{shop.devices}</td>
                          <td className="p-4 font-mono font-bold text-emerald-600">{shop.credits}</td>
                          <td className="p-4 text-slate-500">{shop.date}</td>
                          <td className="p-4 text-right space-x-2">
                            <button onClick={() => setActiveScreen('shop-owner-dash')} className="px-3 py-1 bg-sky-50 text-sky-600 hover:bg-sky-100 rounded-lg text-xs font-bold transition-all">
                              Manage
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Add Shop Modal Drawer */}
                {isAddShopOpen && (
                  <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
                    <div className="w-full max-w-md bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-2xl space-y-4">
                      <div className="flex items-center justify-between border-b border-[#E2DEC9] pb-3">
                        <h3 className="font-bold text-slate-900 text-base">Add New Shop</h3>
                        <button onClick={() => setIsAddShopOpen(false)} className="text-slate-400 hover:text-slate-600">
                          <X className="w-5 h-5" />
                        </button>
                      </div>
                      <div className="space-y-3 text-xs">
                        <div>
                          <label className="font-bold text-slate-700">Shop Name</label>
                          <input type="text" placeholder="Mobile Shop" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                        </div>
                        <div>
                          <label className="font-bold text-slate-700">City / Province</label>
                          <input type="text" placeholder="Lahore, Punjab" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                        </div>
                        <div>
                          <label className="font-bold text-slate-700">Owner Email</label>
                          <input type="email" placeholder="owner@shop.com" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                        </div>
                        <div>
                          <label className="font-bold text-slate-700">Owner Contact Phone</label>
                          <input type="text" placeholder="0300-1234567" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                        </div>
                      </div>
                      <div className="flex justify-end gap-2 pt-2">
                        <button onClick={() => setIsAddShopOpen(false)} className="px-4 py-2 bg-slate-100 text-slate-700 font-bold rounded-xl text-xs">Cancel</button>
                        <button onClick={() => { setIsAddShopOpen(false); alert('Shop added successfully!'); }} className="px-4 py-2 bg-[#0F172A] text-white font-bold rounded-xl text-xs">Add Shop</button>
                      </div>
                    </div>
                  </div>
                )}

              </div>
            )}

            {/* SCREEN 4: SHOP WALLET / CREDIT MANAGEMENT */}
            {activeScreen === 'credit-mgmt' && (
              <div className="space-y-6">
                
                {/* Credit Balance Card & Usage Analytics */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                  
                  <div className="lg:col-span-4 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-4">
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Credit Balance</span>
                    <p className="text-3xl font-black text-slate-900 font-mono">$66,000</p>
                    <span className="inline-block text-[10px] font-bold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-600 border border-emerald-200">
                      Available
                    </span>
                    <div className="pt-2 border-t border-[#E2DEC9]">
                      <span className="text-[10px] text-slate-400 uppercase font-semibold">Usage Analytics</span>
                      <p className="text-lg font-bold text-slate-700 font-mono mt-0.5">$13.80</p>
                    </div>
                  </div>

                  <div className="lg:col-span-8 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="font-bold text-slate-900 text-sm">Credit Usage Analytics</h3>
                      <button onClick={() => setIsAddCreditsOpen(true)} className="px-3.5 py-1.5 bg-[#0F172A] text-white font-bold text-xs rounded-xl shadow">
                        + Add / Remove Credits
                      </button>
                    </div>
                    <div className="h-28 flex items-end justify-between gap-3 px-4 pt-2">
                      {[20, 35, 50, 80, 45, 90, 60, 40].map((v, i) => (
                        <div key={i} className="flex-1 bg-[#0F172A] rounded-t-md" style={{ height: `${v}%` }}></div>
                      ))}
                    </div>
                  </div>

                </div>

                {/* Transaction Ledger Table */}
                <div className="bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-sm space-y-4">
                  <h3 className="font-bold text-slate-900 text-base">Transaction Ledger</h3>
                  <table className="w-full text-left text-xs text-slate-600">
                    <thead className="bg-[#FAF9F5] text-slate-500 uppercase text-[10px] font-bold border-b border-[#E2DEC9]">
                      <tr>
                        <th className="p-3">Date & Time</th>
                        <th className="p-3">Credits</th>
                        <th className="p-3">Debit / Deduction</th>
                        <th className="p-3">Remaining Balance</th>
                        <th className="p-3 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#E2DEC9]">
                      {[
                        { date: 'Tue Jan 28, 2021', cr: '$66.00', dr: '$254.80', bal: '$66,000' },
                        { date: 'Tue Jan 28, 2021', cr: '$64.85', dr: '$120.00', bal: '$64,850' },
                        { date: 'Tue Jan 28, 2021', cr: '$34.90', dr: '$144.90', bal: '$34,900' },
                        { date: 'Tue Jan 28, 2021', cr: '$12.00', dr: '$88.50', bal: '$12,000' },
                      ].map((t, i) => (
                        <tr key={i} className="hover:bg-[#FAF9F5]">
                          <td className="p-3 font-medium text-slate-800">{t.date}</td>
                          <td className="p-3 font-mono font-bold text-emerald-600">+{t.cr}</td>
                          <td className="p-3 font-mono font-bold text-rose-500">-{t.dr}</td>
                          <td className="p-3 font-mono font-bold text-slate-900">{t.bal}</td>
                          <td className="p-3 text-right">
                            <button className="text-sky-600 hover:underline font-semibold">View Receipt</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Add Credits Modal */}
                {isAddCreditsOpen && (
                  <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
                    <div className="w-full max-w-sm bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-2xl space-y-4 text-center">
                      <h3 className="font-bold text-slate-900 text-base">Add Credits to Wallet</h3>
                      <input type="number" placeholder="Enter Credit Amount ($)" className="w-full p-3 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl text-center font-bold text-slate-800 outline-none text-sm" />
                      <div className="flex justify-center gap-2 pt-2">
                        <button onClick={() => setIsAddCreditsOpen(false)} className="px-4 py-2 bg-slate-100 text-slate-700 font-bold rounded-xl text-xs">Cancel</button>
                        <button onClick={() => { setIsAddCreditsOpen(false); alert('Credits added!'); }} className="px-4 py-2 bg-[#0F172A] text-white font-bold rounded-xl text-xs">Add Credits</button>
                      </div>
                    </div>
                  </div>
                )}

              </div>
            )}

            {/* SCREEN 5: SHOP OWNER DASHBOARD */}
            {activeScreen === 'shop-owner-dash' && (
              <div className="space-y-6">
                
                {/* 4 KPI Metric Cards */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm">
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Credit Balance</span>
                    <p className="text-2xl font-black text-slate-900 mt-1 font-mono">$15,091</p>
                    <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full mt-2 inline-block">Active Balance</span>
                  </div>
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm">
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Active Devices</span>
                    <p className="text-2xl font-black text-emerald-600 mt-1 font-mono">15</p>
                    <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full mt-2 inline-block">Running / Normal</span>
                  </div>
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm">
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Locked Devices</span>
                    <p className="text-2xl font-black text-rose-500 mt-1 font-mono">31</p>
                    <span className="text-[10px] font-bold text-rose-600 bg-rose-50 px-2 py-0.5 rounded-full mt-2 inline-block">Warning / Overdue</span>
                  </div>
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm">
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Overdue Installments</span>
                    <p className="text-2xl font-black text-amber-500 mt-1 font-mono">05</p>
                    <span className="text-[10px] font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full mt-2 inline-block">Action Required</span>
                  </div>
                </div>

                {/* Second Row: Collections & Summary */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm flex items-center justify-between">
                    <div>
                      <span className="text-[11px] font-bold text-slate-500 uppercase">Today's Collections</span>
                      <p className="text-2xl font-black text-slate-900 font-mono mt-1">$1,288</p>
                    </div>
                    <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-3 py-1 rounded-xl">7 Payments</span>
                  </div>
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm flex items-center justify-between">
                    <div>
                      <span className="text-[11px] font-bold text-slate-500 uppercase">Installments Requiring Attention</span>
                      <p className="text-2xl font-black text-rose-500 font-mono mt-1">10</p>
                    </div>
                    <span className="text-xs font-bold text-rose-600 bg-rose-50 px-3 py-1 rounded-xl">5 Locked / 5 Offline</span>
                  </div>
                </div>

                {/* Device Overview Chart & Recent Payments */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                  
                  <div className="lg:col-span-8 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-4">
                    <h3 className="font-bold text-slate-900 text-sm">Device Overview</h3>
                    <div className="h-44 flex items-end justify-around gap-4 px-4 pb-2">
                      {[40, 75, 95, 60, 85].map((v, i) => (
                        <div key={i} className="flex-1 flex flex-col items-center gap-2">
                          <div className="w-12 bg-[#0F172A] rounded-t-lg" style={{ height: `${v}%` }}></div>
                          <span className="text-xs font-semibold text-slate-400">{['Jan','Feb','Mar','Apr','May'][i]}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="lg:col-span-4 bg-white border border-[#E2DEC9] p-6 rounded-2xl shadow-sm space-y-3">
                    <h3 className="font-bold text-slate-900 text-sm">Recent Payments</h3>
                    <div className="space-y-2 text-xs">
                      {[
                        { label: 'Pending', val: '$4,800.00' },
                        { label: 'Paid', val: '$650.00' },
                        { label: 'Unverified', val: '$320.00' },
                        { label: 'Overdue', val: '$1,500.00' },
                        { label: 'Offline', val: '$1,540.00' },
                      ].map((p, i) => (
                        <div key={i} className="flex items-center justify-between p-2 rounded-xl bg-[#FAF9F5] border border-[#E2DEC9]">
                          <span className="text-slate-600 font-medium">{p.label}</span>
                          <strong className="text-slate-900 font-mono">{p.val}</strong>
                        </div>
                      ))}
                    </div>
                  </div>

                </div>

              </div>
            )}

            {/* SCREEN 6: CUSTOMER MANAGEMENT */}
            {activeScreen === 'customer-mgmt' && (
              <div className="space-y-6">
                
                <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
                  <div className="flex items-center gap-3 w-full sm:w-auto">
                    <div className="relative flex-1 sm:w-64">
                      <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
                      <input type="text" placeholder="Search Customer..." className="w-full pl-9 pr-4 py-2 bg-white border border-[#E2DEC9] rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-sky-500" />
                    </div>
                    <button className="px-3.5 py-2 bg-white border border-[#E2DEC9] rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 shadow-sm">
                      <Filter className="w-3.5 h-3.5" /> Filter
                    </button>
                  </div>
                  <button 
                    onClick={() => setIsAddCustomerOpen(true)}
                    className="px-4 py-2 bg-[#0F172A] text-white font-bold text-xs rounded-xl shadow flex items-center gap-2"
                  >
                    <Plus className="w-4 h-4" /> Add Customer
                  </button>
                </div>

                <div className="bg-white border border-[#E2DEC9] rounded-2xl overflow-hidden shadow-sm">
                  <table className="w-full text-left text-xs text-slate-600">
                    <thead className="bg-[#FAF9F5] text-slate-500 uppercase text-[10px] font-bold border-b border-[#E2DEC9]">
                      <tr>
                        <th className="p-4">Customer Name</th>
                        <th className="p-4">Status</th>
                        <th className="p-4">Assigned Shop</th>
                        <th className="p-4">Registration Date</th>
                        <th className="p-4">Next Due Date</th>
                        <th className="p-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#E2DEC9]">
                      {[
                        { name: 'Muhammad Tariq', status: 'Active', shop: 'Lahore Central Mall', reg: '08-10-2023', due: '09-10-2023' },
                        { name: 'Ali Raza', status: 'Active', shop: 'Karachi Saddar', reg: '08-10-2023', due: '09-10-2023' },
                        { name: 'Kamran Khan', status: 'Active', shop: 'Rawalpindi Plaza', reg: '08-10-2023', due: '09-10-2023' },
                        { name: 'Zeeshan Ali', status: 'Inactive', shop: 'Lahore Central Mall', reg: '08-10-2023', due: '09-10-2023' },
                        { name: 'Usman Sheikh', status: 'Active', shop: 'Karachi Saddar', reg: '08-10-2023', due: '09-10-2023' },
                      ].map((c, i) => (
                        <tr key={i} className="hover:bg-[#FAF9F5]">
                          <td className="p-4 font-bold text-slate-900">{c.name}</td>
                          <td className="p-4">
                            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-200">
                              {c.status}
                            </span>
                          </td>
                          <td className="p-4">{c.shop}</td>
                          <td className="p-4 text-slate-500">{c.reg}</td>
                          <td className="p-4 font-semibold text-slate-800">{c.due}</td>
                          <td className="p-4 text-right space-x-2">
                            <button onClick={() => setActiveScreen('installment-contract')} className="px-3 py-1 bg-sky-50 text-sky-600 hover:bg-sky-100 rounded-lg text-xs font-bold">
                              View Contract
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Add Customer Modal */}
                {isAddCustomerOpen && (
                  <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
                    <div className="w-full max-w-md bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-2xl space-y-4">
                      <div className="flex items-center justify-between border-b border-[#E2DEC9] pb-3">
                        <h3 className="font-bold text-slate-900 text-base">Register New Customer</h3>
                        <button onClick={() => setIsAddCustomerOpen(false)} className="text-slate-400 hover:text-slate-600"><X className="w-5 h-5" /></button>
                      </div>
                      <div className="space-y-3 text-xs">
                        <div>
                          <label className="font-bold text-slate-700">Full Name</label>
                          <input type="text" placeholder="Customer Name" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                        </div>
                        <div>
                          <label className="font-bold text-slate-700">National ID / CNIC</label>
                          <input type="text" placeholder="35202-1234567-1" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                        </div>
                        <div>
                          <label className="font-bold text-slate-700">Phone Number</label>
                          <input type="text" placeholder="0300-1234567" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                        </div>
                      </div>
                      <div className="flex justify-end gap-2 pt-2">
                        <button onClick={() => setIsAddCustomerOpen(false)} className="px-4 py-2 bg-slate-100 text-slate-700 font-bold rounded-xl text-xs">Cancel</button>
                        <button onClick={() => { setIsAddCustomerOpen(false); alert('Customer created!'); }} className="px-4 py-2 bg-[#0F172A] text-white font-bold rounded-xl text-xs">Save Customer</button>
                      </div>
                    </div>
                  </div>
                )}

              </div>
            )}

            {/* SCREEN 7: INSTALLMENT CONTRACT & RECORD PAYMENT */}
            {activeScreen === 'installment-contract' && (
              <div className="space-y-6">
                
                {/* Contract Summary Banner */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm">
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Contract Balance</span>
                    <p className="text-2xl font-black text-slate-900 mt-1 font-mono">$228.00</p>
                    <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full mt-2 inline-block">Active Schedule</span>
                  </div>
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm">
                    <span className="text-[11px] font-bold text-slate-500 uppercase">Monthly Installment</span>
                    <p className="text-2xl font-black text-sky-600 mt-1 font-mono">$27.00</p>
                    <span className="text-[10px] font-bold text-slate-600 bg-slate-100 px-2 py-0.5 rounded-full mt-2 inline-block">10 Terms</span>
                  </div>
                  <div className="bg-white border border-[#E2DEC9] p-5 rounded-2xl shadow-sm flex items-center justify-between">
                    <div>
                      <span className="text-[11px] font-bold text-slate-500 uppercase">Quick Actions</span>
                      <p className="text-sm font-bold text-slate-800 mt-1">Receive In-Person Cash</p>
                    </div>
                    <button 
                      onClick={() => setIsRecordPaymentOpen(true)}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-xl shadow-lg transition-all"
                    >
                      Record Payment
                    </button>
                  </div>
                </div>

                {/* Installment Schedule Table */}
                <div className="bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-sm space-y-4">
                  <h3 className="font-bold text-slate-900 text-base">Installment Schedule Matrix</h3>
                  <table className="w-full text-left text-xs text-slate-600">
                    <thead className="bg-[#FAF9F5] text-slate-500 uppercase text-[10px] font-bold border-b border-[#E2DEC9]">
                      <tr>
                        <th className="p-3">Term #</th>
                        <th className="p-3">Amount Due</th>
                        <th className="p-3">Status</th>
                        <th className="p-3">Due Date</th>
                        <th className="p-3">Paid Date</th>
                        <th className="p-3 text-right">Receipt</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#E2DEC9]">
                      {[
                        { term: 'Installment #01', amt: '$27.00', status: 'PAID', statusClass: 'bg-emerald-50 text-emerald-600 border-emerald-200', due: '05-10-2023', paid: '05-10-2023' },
                        { term: 'Installment #02', amt: '$27.00', status: 'PAID', statusClass: 'bg-emerald-50 text-emerald-600 border-emerald-200', due: '06-10-2023', paid: '06-10-2023' },
                        { term: 'Installment #03', amt: '$27.00', status: 'UPCOMING', statusClass: 'bg-slate-100 text-slate-600 border-slate-300', due: '07-10-2023', paid: '-' },
                        { term: 'Installment #04', amt: '$27.00', status: 'DUE TODAY', statusClass: 'bg-amber-50 text-amber-600 border-amber-300', due: '08-10-2023', paid: '-' },
                        { term: 'Installment #05', amt: '$27.00', status: 'OVERDUE', statusClass: 'bg-rose-50 text-rose-600 border-rose-300', due: '09-10-2023', paid: '-' },
                        { term: 'Installment #06', amt: '$27.00', status: 'GRACE PERIOD', statusClass: 'bg-indigo-50 text-indigo-600 border-indigo-300', due: '10-10-2023', paid: '-' },
                      ].map((ins, i) => (
                        <tr key={i} className="hover:bg-[#FAF9F5]">
                          <td className="p-3 font-bold text-slate-800">{ins.term}</td>
                          <td className="p-3 font-mono font-bold text-slate-900">{ins.amt}</td>
                          <td className="p-3">
                            <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${ins.statusClass}`}>
                              {ins.status}
                            </span>
                          </td>
                          <td className="p-3">{ins.due}</td>
                          <td className="p-3 text-slate-400">{ins.paid}</td>
                          <td className="p-3 text-right">
                            <button className="text-sky-600 hover:underline font-semibold">Download PDF</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Record Payment Modal */}
                {isRecordPaymentOpen && (
                  <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
                    <div className="w-full max-w-sm bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-2xl space-y-4">
                      <div className="flex items-center justify-between border-b border-[#E2DEC9] pb-3">
                        <h3 className="font-bold text-slate-900 text-base">Record Payment</h3>
                        <button onClick={() => setIsRecordPaymentOpen(false)} className="text-slate-400 hover:text-slate-600"><X className="w-5 h-5" /></button>
                      </div>
                      <div className="space-y-3 text-xs">
                        <div>
                          <label className="font-bold text-slate-700">Payment Amount ($)</label>
                          <input type="number" defaultValue="27.00" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none font-bold text-slate-800 text-sm" />
                        </div>
                        <div>
                          <label className="font-bold text-slate-700">Payment Method</label>
                          <select className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none">
                            <option>Cash (Shop Counter)</option>
                            <option>EasyPaisa / JazzCash</option>
                            <option>Bank Transfer</option>
                          </select>
                        </div>
                      </div>
                      <div className="flex justify-end gap-2 pt-2">
                        <button onClick={() => setIsRecordPaymentOpen(false)} className="px-4 py-2 bg-slate-100 text-slate-700 font-bold rounded-xl text-xs">Cancel</button>
                        <button onClick={() => { setIsRecordPaymentOpen(false); alert('Payment recorded and device unlocked!'); }} className="px-4 py-2 bg-emerald-600 text-white font-bold rounded-xl text-xs">Confirm & Unlock</button>
                      </div>
                    </div>
                  </div>
                )}

              </div>
            )}

            {/* SCREEN 8 & 9: MOBILE APP SCREENS (WARNING & LOCKED) */}
            {(activeScreen === 'mobile-warning' || activeScreen === 'mobile-locked') && (
              <div className="flex flex-col items-center justify-center py-6">
                
                <div className="mb-4 flex gap-2">
                  <button onClick={() => setActiveScreen('mobile-warning')} className={`px-4 py-1.5 rounded-xl text-xs font-bold ${activeScreen === 'mobile-warning' ? 'bg-amber-500 text-white shadow' : 'bg-white border border-[#E2DEC9] text-slate-700'}`}>
                    📱 Show Payment Warning
                  </button>
                  <button onClick={() => setActiveScreen('mobile-locked')} className={`px-4 py-1.5 rounded-xl text-xs font-bold ${activeScreen === 'mobile-locked' ? 'bg-rose-600 text-white shadow' : 'bg-white border border-[#E2DEC9] text-slate-700'}`}>
                    📱 Show Device Locked State
                  </button>
                </div>

                {/* Realistic Phone Bezel */}
                <div className="w-[330px] h-[670px] bg-slate-900 border-[8px] border-slate-800 rounded-[48px] shadow-2xl p-3 relative flex flex-col justify-between overflow-hidden">
                  
                  {/* Phone Screen Canvas */}
                  <div className="w-full h-full bg-slate-950 rounded-[38px] overflow-hidden flex flex-col justify-between p-5 text-white relative">
                    
                    {/* Top Notch & Status */}
                    <div>
                      <div className="w-32 h-4 bg-black rounded-b-xl mx-auto"></div>
                      <div className="flex justify-between items-center text-[10px] text-slate-400 mt-1 font-mono">
                        <span>12:00 PM</span>
                        <div className="flex items-center gap-1">
                          <span>📶 4G</span>
                          <span className="text-sky-400 font-bold">🛡️ ABIZ DO</span>
                          <span>🔋 85%</span>
                        </div>
                      </div>
                    </div>

                    {/* Content Body */}
                    {activeScreen === 'mobile-warning' ? (
                      <div className="text-center space-y-4 my-auto">
                        <div className="flex justify-center">
                          <svg className="w-16 h-16" viewBox="0 0 100 100" fill="none">
                            <path d="M50 10 L90 85 L10 85 Z" stroke="#0EA5E9" strokeWidth="10" strokeLinejoin="round" fill="none" />
                            <path d="M50 28 L78 78 L22 78 Z" stroke="#2563EB" strokeWidth="7" strokeLinejoin="round" fill="none" opacity="0.6" />
                            <circle cx="50" cy="58" r="7" fill="#0EA5E9" />
                          </svg>
                        </div>
                        <h3 className="text-lg font-extrabold text-white">Android Payment Warning</h3>
                        <p className="text-xs text-slate-400 px-2 leading-relaxed">
                          Your next installment payment is due in <strong>2 days</strong>. Please clear your balance to prevent device restrictions.
                        </p>
                        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-3.5 text-left text-xs space-y-1.5">
                          <div className="flex justify-between text-slate-400"><span>Payment Amount:</span><strong className="text-white font-mono">$27.00</strong></div>
                          <div className="flex justify-between text-slate-400"><span>Due Date:</span><strong className="text-amber-400 font-mono">10-10-2023</strong></div>
                        </div>
                        <button className="w-full py-3 bg-sky-500 hover:bg-sky-400 text-slate-950 font-extrabold text-xs rounded-xl shadow-lg transition-all uppercase tracking-wider">
                          Pay with EasyPaisa
                        </button>
                      </div>
                    ) : (
                      <div className="text-center space-y-4 my-auto">
                        <div className="w-16 h-16 mx-auto rounded-full bg-rose-600/20 border-2 border-rose-500 flex items-center justify-center shadow-lg shadow-rose-600/40">
                          <Lock className="w-8 h-8 text-rose-500" />
                        </div>
                        <div>
                          <h3 className="text-xl font-black text-rose-500">Android Locked State</h3>
                          <p className="text-xs text-slate-300 mt-1 px-1">
                            This device has been locked via <strong>ABIZ LOCKER</strong> due to overdue installment balance.
                          </p>
                        </div>
                        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-3.5 text-left text-xs space-y-1">
                          <span className="text-[10px] text-sky-400 uppercase font-bold">Authorized Retail Partner</span>
                          <p className="font-bold text-white text-sm">Lahore Central Mobile Mall</p>
                          <p className="text-slate-400 text-[11px]">Helpline: <span className="text-emerald-400 font-mono">0300-1234567</span></p>
                        </div>
                        <div className="space-y-2">
                          <button className="w-full py-2.5 bg-emerald-600 text-white font-bold text-xs rounded-xl shadow flex items-center justify-center gap-1.5">
                            <CreditCard className="w-4 h-4" /> Pay Online Now
                          </button>
                          <button className="w-full py-2.5 bg-slate-800 text-slate-300 font-bold text-xs rounded-xl flex items-center justify-center gap-1.5">
                            <PhoneCall className="w-4 h-4" /> Call Emergency / Shop
                          </button>
                        </div>
                      </div>
                    )}

                    {/* Bottom Indicator */}
                    <div className="w-28 h-1 bg-slate-700 rounded-full mx-auto mt-2"></div>
                  </div>

                </div>

              </div>
            )}

            {/* SCREEN 10: DEVICE MANAGEMENT */}
            {activeScreen === 'device-mgmt' && (
              <div className="space-y-6">
                
                <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
                  <div className="flex items-center gap-3 w-full sm:w-auto">
                    <div className="relative flex-1 sm:w-64">
                      <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
                      <input type="text" placeholder="Search IMEI or Model..." className="w-full pl-9 pr-4 py-2 bg-white border border-[#E2DEC9] rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-sky-500" />
                    </div>
                    <button className="px-3.5 py-2 bg-white border border-[#E2DEC9] rounded-xl text-xs font-semibold text-slate-700 flex items-center gap-1.5 shadow-sm">
                      <Filter className="w-3.5 h-3.5" /> Filter
                    </button>
                  </div>
                  <button onClick={() => setActiveScreen('qr-enroll')} className="px-4 py-2 bg-[#0F172A] text-white font-bold text-xs rounded-xl shadow flex items-center gap-2">
                    <QrCode className="w-4 h-4" /> QR Enrollment
                  </button>
                </div>

                <div className="bg-white border border-[#E2DEC9] rounded-2xl overflow-hidden shadow-sm">
                  <table className="w-full text-left text-xs text-slate-600">
                    <thead className="bg-[#FAF9F5] text-slate-500 uppercase text-[10px] font-bold border-b border-[#E2DEC9]">
                      <tr>
                        <th className="p-4 w-8"><input type="checkbox" className="rounded text-sky-500" /></th>
                        <th className="p-4">Device Model & IMEI</th>
                        <th className="p-4">Lock Status</th>
                        <th className="p-4">Connectivity</th>
                        <th className="p-4">Enrollment Date</th>
                        <th className="p-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#E2DEC9]">
                      {[
                        { model: 'Samsung Galaxy S23', imei: '861794058291048', status: 'Active', conn: 'Connected', date: 'Apr 10, 2023' },
                        { model: 'Redmi Note 13', imei: '864920481029482', status: 'Active', conn: 'Connected', date: 'Apr 15, 2023' },
                        { model: 'Infinix Note 40', imei: '869381048201948', status: 'Active', conn: 'Connected', date: 'Apr 20, 2023' },
                        { model: 'Tecno Spark 20', imei: '862910481920381', status: 'Locked', conn: 'Connected', date: 'Mar 23, 2023' },
                        { model: 'Vivo Y17s', imei: '868192048192048', status: 'Active', conn: 'Connected', date: 'Nov 23, 2024' },
                      ].map((d, i) => (
                        <tr key={i} className="hover:bg-[#FAF9F5]">
                          <td className="p-4"><input type="checkbox" className="rounded text-sky-500" /></td>
                          <td className="p-4">
                            <span className="font-bold text-slate-900">{d.model}</span>
                            <span className="text-[10px] text-slate-400 font-mono block">IMEI: {d.imei}</span>
                          </td>
                          <td className="p-4">
                            <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                              d.status === 'Active' ? 'bg-emerald-50 text-emerald-600 border border-emerald-200' : 'bg-rose-50 text-rose-600 border border-rose-200'
                            }`}>
                              {d.status}
                            </span>
                          </td>
                          <td className="p-4 font-semibold text-emerald-600 flex items-center gap-1.5 mt-2">
                            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span> {d.conn}
                          </td>
                          <td className="p-4 text-slate-500">{d.date}</td>
                          <td className="p-4 text-right space-x-2">
                            <button onClick={() => setActiveScreen('device-detail')} className="px-3 py-1 bg-sky-50 text-sky-600 hover:bg-sky-100 rounded-lg text-xs font-bold">
                              Inspect
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

              </div>
            )}

            {/* SCREEN 11: DEVICE DETAIL & ACTIONS */}
            {activeScreen === 'device-detail' && (
              <div className="space-y-6">
                
                <div className="bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-sm">
                  <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
                    
                    {/* Device Visual Specs */}
                    <div className="lg:col-span-4 flex items-center gap-4 border-b lg:border-b-0 lg:border-r border-[#E2DEC9] pb-6 lg:pb-0 lg:pr-6">
                      <div className="w-24 h-36 bg-slate-900 border-2 border-slate-700 rounded-2xl shadow flex items-center justify-center text-white">
                        <Smartphone className="w-10 h-10 text-sky-400" />
                      </div>
                      <div className="space-y-1">
                        <span className="text-[10px] bg-sky-50 text-sky-600 font-bold px-2 py-0.5 rounded uppercase">Financed Phone</span>
                        <h3 className="font-extrabold text-slate-900 text-base">Samsung Galaxy S23</h3>
                        <p className="text-xs text-slate-500 font-mono">IMEI: 861794058291048</p>
                        <p className="text-[11px] text-slate-600 font-medium">Serial: R58M129X01</p>
                      </div>
                    </div>

                    {/* License & Health Badges */}
                    <div className="lg:col-span-4 space-y-3">
                      <div className="flex items-center justify-between p-3 rounded-xl bg-[#FAF9F5] border border-[#E2DEC9] text-xs">
                        <span className="text-slate-600 font-semibold">License Credit Status:</span>
                        <strong className="text-emerald-600">Active (5/10)</strong>
                      </div>
                      <div className="flex items-center justify-between p-3 rounded-xl bg-[#FAF9F5] border border-[#E2DEC9] text-xs">
                        <span className="text-slate-600 font-semibold">Device Owner Status:</span>
                        <strong className="text-sky-600 font-bold">Knox DO Active</strong>
                      </div>
                    </div>

                    {/* Hardware Lock / Unlock Trigger Controls */}
                    <div className="lg:col-span-4 space-y-2">
                      <button 
                        onClick={() => alert('Remote LOCK command dispatched to Samsung Galaxy S23!')}
                        className="w-full py-2.5 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs rounded-xl shadow-lg transition-all flex items-center justify-center gap-2 uppercase tracking-wider"
                      >
                        <Lock className="w-4 h-4" /> LOCK DEVICE NOW
                      </button>
                      <button 
                        onClick={() => alert('Remote UNLOCK command dispatched to Samsung Galaxy S23!')}
                        className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-xl shadow-lg transition-all flex items-center justify-center gap-2 uppercase tracking-wider"
                      >
                        <Unlock className="w-4 h-4" /> UNLOCK DEVICE NOW
                      </button>
                    </div>

                  </div>
                </div>

                {/* Event Timeline */}
                <div className="bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-sm space-y-4">
                  <h3 className="font-bold text-slate-900 text-base">Security & Audit Event Timeline</h3>
                  <div className="space-y-3 text-xs">
                    {[
                      { event: 'Agent Heartbeat Acknowledged', time: 'Today, 11:45 AM', user: 'System Worker #02' },
                      { event: 'Payment $27.00 Received (Auto-Unlocked)', time: 'Yesterday, 04:30 PM', user: 'Haji Usman (Shop Owner)' },
                      { event: 'Device Owner Provisioned via QR Scan', time: '08-10-2023, 10:00 AM', user: 'Shop Staff' },
                    ].map((ev, i) => (
                      <div key={i} className="flex items-center justify-between p-3 rounded-xl bg-[#FAF9F5] border border-[#E2DEC9]">
                        <div className="flex items-center gap-2">
                          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                          <span className="font-bold text-slate-800">{ev.event}</span>
                        </div>
                        <div className="text-right">
                          <span className="text-slate-500 text-[11px] block">{ev.time}</span>
                          <span className="text-[10px] text-slate-400">{ev.user}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

              </div>
            )}

            {/* SCREEN 12: QR ENROLLMENT */}
            {activeScreen === 'qr-enroll' && (
              <div className="flex flex-col items-center justify-center py-6">
                <div className="w-full max-w-md bg-white border border-[#E2DEC9] rounded-2xl p-8 shadow-xl text-center space-y-6">
                  <div>
                    <h3 className="text-xl font-black text-slate-900">Enroll Android Device</h3>
                    <p className="text-xs text-slate-500 mt-1">Scan this QR code during Android Setup Wizard (Tap 6 times on welcome screen)</p>
                  </div>

                  {/* Crisp QR Code Graphic */}
                  <div className="p-4 bg-white border-2 border-slate-900 rounded-2xl inline-block shadow-inner">
                    <svg className="w-48 h-48 mx-auto" viewBox="0 0 100 100" fill="currentColor">
                      <rect x="10" y="10" width="30" height="30" rx="4" fill="#0F172A" />
                      <rect x="15" y="15" width="20" height="20" fill="white" />
                      <rect x="20" y="20" width="10" height="10" fill="#0F172A" />

                      <rect x="60" y="10" width="30" height="30" rx="4" fill="#0F172A" />
                      <rect x="65" y="15" width="20" height="20" fill="white" />
                      <rect x="70" y="20" width="10" height="10" fill="#0F172A" />

                      <rect x="10" y="60" width="30" height="30" rx="4" fill="#0F172A" />
                      <rect x="15" y="65" width="20" height="20" fill="white" />
                      <rect x="20" y="70" width="10" height="10" fill="#0F172A" />

                      <rect x="45" y="45" width="10" height="10" fill="#0EA5E9" />
                      <rect x="60" y="60" width="15" height="15" fill="#0F172A" />
                      <rect x="75" y="75" width="15" height="15" fill="#0F172A" />
                      <rect x="45" y="20" width="10" height="15" fill="#0F172A" />
                      <rect x="20" y="45" width="15" height="10" fill="#0F172A" />
                    </svg>
                  </div>

                  <div className="bg-[#FAF9F5] border border-[#E2DEC9] p-4 rounded-xl text-left text-xs space-y-2">
                    <p className="font-bold text-slate-800">Enrollment Instructions:</p>
                    <p className="text-slate-600">1. Turn on new or factory reset phone.</p>
                    <p className="text-slate-600">2. Tap 6 times anywhere on the Welcome screen.</p>
                    <p className="text-slate-600">3. Connect to Wi-Fi and scan this QR code.</p>
                  </div>

                  <button onClick={() => alert('Provisioning token refreshed!')} className="w-full py-2.5 bg-[#0F172A] text-white font-bold text-xs rounded-xl shadow">
                    Generate New QR Token
                  </button>
                </div>
              </div>
            )}

            {/* SCREEN 13: AUDIT LOGS */}
            {activeScreen === 'audit-logs' && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <h3 className="font-bold text-slate-900 text-lg">Immutable Security Audit Logs</h3>
                  <button className="px-3.5 py-1.5 bg-white border border-[#E2DEC9] rounded-xl text-xs font-semibold text-slate-700 shadow-sm">
                    Export CSV / PDF
                  </button>
                </div>

                <div className="bg-white border border-[#E2DEC9] rounded-2xl overflow-hidden shadow-sm">
                  <table className="w-full text-left text-xs text-slate-600">
                    <thead className="bg-[#FAF9F5] text-slate-500 uppercase text-[10px] font-bold border-b border-[#E2DEC9]">
                      <tr>
                        <th className="p-4">Actor</th>
                        <th className="p-4">Security Action</th>
                        <th className="p-4">Target Device</th>
                        <th className="p-4">Status</th>
                        <th className="p-4">Timestamp</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#E2DEC9]">
                      {[
                        { actor: 'Super Admin', act: 'RELEASE_APPROVAL', tgt: 'Release 22.0.0', status: 'SUCCESS', time: '08-14-2024 11:30' },
                        { actor: 'Haji Usman', act: 'RECORD_PAYMENT', tgt: 'IMEI: 861794058291048', status: 'SUCCESS', time: '08-14-2024 10:15' },
                        { actor: 'System Worker', act: 'AUTO_LOCK_TRIGGER', tgt: 'IMEI: 862910481920381', status: 'SUCCESS', time: '08-14-2024 09:00' },
                        { actor: 'Shop Staff', act: 'DEVICE_ENROLLMENT', tgt: 'IMEI: 869381048201948', status: 'SUCCESS', time: '08-13-2024 16:40' },
                        { actor: 'System Defense', act: 'BLOCKED_TAMPER_ATTEMPT', tgt: 'IMEI: 868192048192048', status: 'WARNING', time: '08-13-2024 14:20' },
                      ].map((log, i) => (
                        <tr key={i} className="hover:bg-[#FAF9F5]">
                          <td className="p-4 font-bold text-slate-900">{log.actor}</td>
                          <td className="p-4 font-mono font-bold text-sky-600">{log.act}</td>
                          <td className="p-4 font-mono text-slate-600">{log.tgt}</td>
                          <td className="p-4">
                            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-200">
                              {log.status}
                            </span>
                          </td>
                          <td className="p-4 text-slate-500 font-mono">{log.time}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* SCREEN 14: SETTINGS */}
            {activeScreen === 'settings' && (
              <div className="max-w-3xl space-y-6">
                <div className="bg-white border border-[#E2DEC9] rounded-2xl p-6 shadow-sm space-y-4">
                  <h3 className="font-bold text-slate-900 text-base">Enterprise Profile & Branding Configuration</h3>
                  <div className="space-y-3 text-xs">
                    <div>
                      <label className="font-bold text-slate-700">Enterprise Name</label>
                      <input type="text" defaultValue="ABIZ LOCKER Enterprise" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                    </div>
                    <div>
                      <label className="font-bold text-slate-700">Support Contact Email</label>
                      <input type="email" defaultValue="support@abizlocker.com" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none" />
                    </div>
                    <div>
                      <label className="font-bold text-slate-700">Default Grace Period (Days)</label>
                      <input type="number" defaultValue="3" className="w-full mt-1 p-2.5 bg-[#FAF9F5] border border-[#E2DEC9] rounded-xl outline-none font-bold" />
                    </div>
                  </div>
                  <button onClick={() => alert('Settings saved!')} className="px-4 py-2 bg-[#0F172A] text-white font-bold text-xs rounded-xl shadow">
                    Save Profile
                  </button>
                </div>
              </div>
            )}

          </div>

        </div>

      </div>

    </div>
  );
}
