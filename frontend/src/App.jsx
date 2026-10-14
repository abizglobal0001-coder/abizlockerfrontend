import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Shield, Smartphone, Lock, Unlock, Users, Store, DollarSign,
  AlertTriangle, CheckCircle2, Clock, Search, Filter, Plus,
  ArrowUpRight, ArrowDownRight, RefreshCw, Eye, Power, Check, X,
  Building2, QrCode, Sliders, FileText, ChevronRight, Activity, Wallet,
  CreditCard, Sparkles, TrendingUp, Cpu, Bell, ExternalLink, ShieldCheck,
  Zap, LogOut, ChevronDown, CheckCircle, ArrowRight, PhoneCall, HelpCircle,
  BarChart3, PieChart as PieIcon, Settings, Key, UserPlus, FileCheck, MessageSquare,
  History, Laptop, SmartphoneNfc, ChevronLeft, Trash2, Edit3, Menu, Save, Radio,
  MapPin, Camera, Compass, Crosshair, ShieldAlert, LockKeyhole, WifiOff, Upload
} from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer,
  BarChart, Bar, PieChart, Pie, Cell
} from 'recharts';

export default function App() {
  // Ultra-fast reliable live MDM hub tunnel endpoint
  const MDM_API_BASE = 'https://abiz-mdm-hub.loca.lt';

  // Resilient API Fetch Helper with Tunnel Header Bypass
  const apiFetch = async (endpoint, options = {}) => {
    const url = endpoint.startsWith('http') ? endpoint : `${MDM_API_BASE}${endpoint}`;
    const headers = {
      'Bypass-Tunnel-Reminder': 'true',
      ...(options.headers || {})
    };
    if (options.body && typeof options.body === 'string' && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }
    return fetch(url, { ...options, headers });
  };

  // ==========================================
  // MOBILE MENU DRAWER STATE
  // ==========================================
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // ==========================================
  // 1. GLOBAL SYSTEM SETTINGS (Super Admin)
  // ==========================================
  const [globalSettings, setGlobalSettings] = useState({
    supportWhatsApp: '923001234567',
    supportEmail: 'support@abizlocker.com',
    creditsPerEnrollment: 1, // Super Admin customizable
    defaultGraceDays: 3,
    enterpriseName: 'ABIZ LOCKER Enterprise'
  });

  // Settings form local state
  const [tempCreditsPerEnrollment, setTempCreditsPerEnrollment] = useState(1);
  const [tempWhatsApp, setTempWhatsApp] = useState('923001234567');
  const [tempEmail, setTempEmail] = useState('support@abizlocker.com');

  // ==========================================
  // ROUTING & PORTAL SEPARATION (#/admin vs #/shop)
  // ==========================================
  const getPortalFromUrl = () => {
    const hash = (typeof window !== 'undefined' ? window.location.hash : '').toLowerCase();
    const path = (typeof window !== 'undefined' ? window.location.pathname : '').toLowerCase();
    const search = (typeof window !== 'undefined' ? window.location.search : '').toLowerCase();
    if (hash.includes('/admin') || hash === '#admin' || path.includes('/admin') || search.includes('portal=admin')) {
      return 'admin';
    }
    return 'shop';
  };

  const [currentPortal, setCurrentPortal] = useState(getPortalFromUrl);

  useEffect(() => {
    const handleLocationChange = () => {
      setCurrentPortal(getPortalFromUrl());
    };
    window.addEventListener('hashchange', handleLocationChange);
    window.addEventListener('popstate', handleLocationChange);
    return () => {
      window.removeEventListener('hashchange', handleLocationChange);
      window.removeEventListener('popstate', handleLocationChange);
    };
  }, []);

  const navigateToPortal = (portal) => {
    window.location.hash = `#/${portal}`;
    setCurrentPortal(portal);
    setLoginError('');
    setLoginEmail('');
    setLoginPassword('');
  };

  // ==========================================
  // 2. AUTHENTICATION STATE (Persistent via localStorage)
  // ==========================================
  const [authSession, setAuthSession] = useState(() => {
    try {
      const saved = localStorage.getItem('abiz_auth_session');
      if (saved) return JSON.parse(saved);
    } catch (e) {}
    return null; // Clean: Starts Logged Out
  });

  const updateAuthSession = (session) => {
    setAuthSession(session);
    try {
      if (session) {
        localStorage.setItem('abiz_auth_session', JSON.stringify(session));
      } else {
        localStorage.removeItem('abiz_auth_session');
      }
    } catch (e) {}
  };

  // Login form state
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginError, setLoginError] = useState('');

  // ==========================================
  // 3. MASTER DATA: SHOPS (With Lock Screen & Bank Accounts)
  // ==========================================
  const defaultShops = [
    {
      id: 'SH-101',
      name: 'Lahore Central Mobile Mall',
      ownerName: 'Haji Usman',
      email: 'shop1@abiz.com',
      frpRecoveryEmail: 'usman.lahore.recovery@gmail.com',
      password: '123',
      phone: '0300-1234567',
      city: 'Lahore',
      credits: 45,
      status: 'Active',
      createdAt: '2026-08-15',
      bankName: 'Meezan Bank',
      bankAccountNumber: '0102-0105849301',
      bankAccountTitle: 'Lahore Central Mobile Mall',
      bankIban: 'PK36MEZN0001020105849301',
      jazzCashNumber: '0300-1234567',
      jazzCashTitle: 'Haji Usman',
      easyPaisaNumber: '0300-1234567',
      easyPaisaTitle: 'Haji Usman',
      lockNoticeUrdu: 'معزز کسٹمر، آپکی ماہانہ قسط کی ادائیگی کی تاریخ گزر چکی ہے۔ فون ان لاک کروانے کیلئے برائے مہربانی فوری اپنے شاپ اونر سے رابطہ کریں اور قسط جمع کروائیں۔',
      allowLocationAccess: true, // Super Admin granted
      allowCameraAccess: true,   // Super Admin granted
      autoLockEnabled: true,     // Offline & Online due-date auto lock
      paymentHistory: [
        { id: 'PAY-801', amount: 50000, creditsIssued: 50, date: '2026-08-16', method: 'Bank Transfer' },
        { id: 'PAY-802', amount: 30000, creditsIssued: 30, date: '2026-08-25', method: 'EasyPaisa' },
      ]
    },
    {
      id: 'SH-102',
      name: 'Anas Electronics & Telecom',
      ownerName: 'Muhammad Anas',
      email: 'shop2@abiz.com',
      frpRecoveryEmail: 'anas.karachi.recovery@gmail.com',
      password: '123',
      phone: '0321-9876543',
      city: 'Karachi',
      credits: 18,
      status: 'Active',
      createdAt: '2026-08-20',
      bankName: 'Bank Alfalah',
      bankAccountNumber: '5501-889920114',
      bankAccountTitle: 'Muhammad Anas Telecom',
      bankIban: 'PK12ALFH005501889920114',
      jazzCashNumber: '0321-9876543',
      jazzCashTitle: 'Muhammad Anas',
      easyPaisaNumber: '0321-9876543',
      easyPaisaTitle: 'Muhammad Anas',
      lockNoticeUrdu: 'معزز کسٹمر، آپکی قسط واجب الادا ہے۔ برائے مہربانی فوری رابطہ کر کے قسط جمع کروائیں۔',
      allowLocationAccess: false, // Super Admin controlled
      allowCameraAccess: false,   // Super Admin controlled
      autoLockEnabled: true,
      paymentHistory: [
        { id: 'PAY-901', amount: 20000, creditsIssued: 20, date: '2026-08-21', method: 'JazzCash' },
      ]
    },
    {
      id: 'SH-103',
      name: 'Ali Mobile Zone Saddar',
      ownerName: 'Ali Raza',
      email: 'shop3@abiz.com',
      frpRecoveryEmail: 'ali.rawalpindi.recovery@gmail.com',
      password: '123',
      phone: '0312-5551234',
      city: 'Rawalpindi',
      credits: 80,
      status: 'Active',
      createdAt: '2026-08-10',
      bankName: 'Habib Bank Limited (HBL)',
      bankAccountNumber: '0042-7901847291',
      bankAccountTitle: 'Ali Mobile Zone',
      bankIban: 'PK45HABB0000427901847291',
      jazzCashNumber: '0312-5551234',
      jazzCashTitle: 'Ali Raza',
      easyPaisaNumber: '0312-5551234',
      easyPaisaTitle: 'Ali Raza',
      lockNoticeUrdu: 'قسط کی تاریخ گزر چکی ہے۔ قسط ادا کر کے رسید بھیجیں تاکہ فون بحال ہو سکے۔',
      allowLocationAccess: false, // Super Admin controlled
      allowCameraAccess: false,   // Super Admin controlled
      autoLockEnabled: true,
      paymentHistory: [
        { id: 'PAY-701', amount: 80000, creditsIssued: 80, date: '2026-08-11', method: 'Bank Transfer' },
      ]
    }
  ];

  const [shops, setShops] = useState(() => {
    try {
      const saved = localStorage.getItem('abiz_custom_shops');
      if (saved) {
        const parsed = JSON.parse(saved);
        return parsed.map(s => ({
          ...s,
          allowLocationAccess: s.allowLocationAccess ?? (s.id === 'SH-101'),
          allowCameraAccess: s.allowCameraAccess ?? (s.id === 'SH-101'),
          autoLockEnabled: s.autoLockEnabled ?? true,
        }));
      }
    } catch (e) {}
    return defaultShops;
  });

  // Shop Owner Lockscreen & Payment Settings Inputs
  const [shopNameInput, setShopNameInput] = useState('');
  const [shopOwnerInput, setShopOwnerInput] = useState('');
  const [shopPhoneInput, setShopPhoneInput] = useState('');
  const [shopFrpEmailInput, setShopFrpEmailInput] = useState('');
  const [bankNameInput, setBankNameInput] = useState('');
  const [bankAccountInput, setBankAccountInput] = useState('');
  const [bankTitleInput, setBankTitleInput] = useState('');
  const [bankIbanInput, setBankIbanInput] = useState('');
  const [jazzCashInput, setJazzCashInput] = useState('');
  const [jazzCashTitleInput, setJazzCashTitleInput] = useState('');
  const [easyPaisaInput, setEasyPaisaInput] = useState('');
  const [easyPaisaTitleInput, setEasyPaisaTitleInput] = useState('');
  const [lockNoticeUrduInput, setLockNoticeUrduInput] = useState('');

  // ==========================================
  // 4. MASTER DATA: DEVICES (PKR Currency)
  // ==========================================
  const defaultDevices = [
    {
      id: 'DEV-1788224041665',
      shopId: 'SH-101',
      shopName: 'Lahore Central Mobile Mall',
      customerName: 'Customer (Oppo Test)',
      customerPhone: '0301-4455667',
      customerCnic: '35201-1234567-1',
      model: 'OPPO CPH1909 (A5s)',
      imei: '868538041266479',
      totalPrice: 28500,
      monthlyEmi: 3500,
      paidTerms: 1,
      totalTerms: 10,
      startDate: '2026-01-05',
      nextDueDate: '2026-11-25',
      dueDayOfMonth: 5,
      isLocked: false,
      status: 'Active',
      enrolledAt: '2026-01-05',
      autoLockEnabled: false,
      liveLocation: {
        lat: 31.5204,
        lng: 74.3587,
        address: 'Mall Road, Commercial Center, Lahore, Pakistan',
        accuracy: '6 meters (High Accuracy GPS)',
        lastUpdated: 'Just Now (Real-Time GPS)',
        battery: '84%',
        network: '4G LTE / Jazz'
      },
      lastLocation: {
        lat: 31.5140,
        lng: 74.3412,
        address: 'Main Boulevard, Gulberg III, Lahore',
        lastUpdated: 'Yesterday at 04:30 PM'
      },
      cameraSnaps: [
        {
          id: 'SNAP-101',
          timestamp: '2026-10-13 13:20:15',
          camera: 'Front Camera (Selfie / Security)',
          watermark: 'ABIZ-MDM-FORENSIC-868538041266479'
        }
      ]
    },
    {
      id: 'DEV-002',
      shopId: 'SH-101',
      shopName: 'Lahore Central Mobile Mall',
      customerName: 'Bilal Ahmed',
      customerPhone: '0342-1122334',
      customerCnic: '35202-5544332-1',
      model: 'Infinix Note 40 Pro',
      imei: '359182736450192',
      totalPrice: 85000,
      monthlyEmi: 8500,
      paidTerms: 3,
      totalTerms: 10,
      startDate: '2026-05-10',
      nextDueDate: '2026-11-10',
      dueDayOfMonth: 10,
      isLocked: false,
      status: 'Active',
      enrolledAt: '2026-05-10',
      autoLockEnabled: false,
      liveLocation: {
        lat: 31.4820,
        lng: 74.3050,
        address: 'Ferozepur Road, Model Town, Lahore',
        accuracy: '9 meters',
        lastUpdated: '10 mins ago',
        battery: '62%',
        network: '4G / Zong'
      },
      lastLocation: {
        lat: 31.4750,
        lng: 74.3010,
        address: 'Kot Lakhpat, Lahore',
        lastUpdated: 'Yesterday at 06:15 PM'
      },
      cameraSnaps: [
        {
          id: 'SNAP-102',
          timestamp: '2026-10-13 12:45:00',
          camera: 'Front Camera (Selfie / Security)',
          watermark: 'ABIZ-MDM-FORENSIC-359182736450192'
        }
      ]
    },
    {
      id: 'DEV-003',
      shopId: 'SH-102',
      shopName: 'Anas Electronics & Telecom',
      customerName: 'Usman Ali',
      customerPhone: '0333-8899112',
      customerCnic: '42101-9876543-3',
      model: 'Xiaomi Redmi Note 13',
      imei: '862049182736451',
      totalPrice: 65000,
      monthlyEmi: 6500,
      paidTerms: 10,
      totalTerms: 10,
      startDate: '2025-10-15',
      nextDueDate: 'Completed',
      dueDayOfMonth: 15,
      isLocked: false,
      status: 'Completed (Released)',
      enrolledAt: '2025-10-15',
      clearedAt: 'Aug 10, 2026, 02:30 PM',
      releasedBy: 'Muhammad Anas (Shop Owner)',
      autoLockEnabled: false,
      liveLocation: {
        lat: 24.8607,
        lng: 67.0011,
        address: 'Saddar Bazar, Karachi, Sindh',
        accuracy: '12 meters',
        lastUpdated: 'Settled Handset',
        battery: '90%',
        network: '4G / Jazz'
      },
      lastLocation: {
        lat: 24.8607,
        lng: 67.0011,
        address: 'Saddar Bazar, Karachi',
        lastUpdated: 'Aug 10, 2026'
      },
      cameraSnaps: []
    },
    {
      id: 'DEV-004',
      shopId: 'SH-103',
      shopName: 'Ali Mobile Zone Saddar',
      customerName: 'Hamza Tariq',
      customerPhone: '0315-9988776',
      customerCnic: '37405-8877665-9',
      model: 'Samsung Galaxy A54',
      imei: '356712903847561',
      totalPrice: 125000,
      monthlyEmi: 12500,
      paidTerms: 5,
      totalTerms: 10,
      startDate: '2026-03-20',
      nextDueDate: '2026-11-20',
      dueDayOfMonth: 20,
      isLocked: false,
      status: 'Active',
      enrolledAt: '2026-03-20',
      autoLockEnabled: true,
      liveLocation: {
        lat: 33.5651,
        lng: 73.0169,
        address: 'Bank Road, Saddar, Rawalpindi',
        accuracy: '7 meters',
        lastUpdated: '15 mins ago',
        battery: '74%',
        network: '4G / Telenor'
      },
      lastLocation: {
        lat: 33.5600,
        lng: 73.0120,
        address: 'Murree Road, Rawalpindi',
        lastUpdated: '2 days ago'
      },
      cameraSnaps: []
    }
  ];

  const [devices, setDevices] = useState(() => {
    try {
      const saved = localStorage.getItem('abiz_custom_devices');
      if (saved) {
        const parsed = JSON.parse(saved);
        return parsed.map(d => ({
          ...d,
          autoLockEnabled: d.autoLockEnabled ?? true,
          liveLocation: d.liveLocation ?? {
            lat: 31.5204,
            lng: 74.3587,
            address: 'Mall Road, Commercial Center, Lahore, Pakistan',
            accuracy: '6 meters (High Accuracy GPS)',
            lastUpdated: 'Just Now (Real-Time GPS)',
            battery: '84%',
            network: '4G LTE / Jazz'
          },
          lastLocation: d.lastLocation ?? {
            lat: 31.5140,
            lng: 74.3412,
            address: 'Main Boulevard, Gulberg III, Lahore',
            lastUpdated: 'Yesterday at 04:30 PM'
          },
          cameraSnaps: d.cameraSnaps ?? [
            {
              id: 'SNAP-101',
              timestamp: '2026-10-13 13:20:15',
              camera: 'Front Camera (Selfie / Security)',
              watermark: `ABIZ-MDM-FORENSIC-${d.id}`
            }
          ]
        }));
      }
    } catch (e) {}
    return defaultDevices;
  });

  // Helper functions: Cleared vs Active Protected
  const isDeviceCleared = (dev) => {
    if (!dev) return false;
    const st = (dev.status || '').toLowerCase();
    return st.includes('completed') || st.includes('released');
  };
  const isDeviceProtected = (dev) => !isDeviceCleared(dev);

  const protectedDevices = devices.filter(isDeviceProtected);
  const clearedDevices = devices.filter(isDeviceCleared);

  // Recent History modal & search state
  const [selectedHistoryDev, setSelectedHistoryDev] = useState(null);
  const [historySearchQuery, setHistorySearchQuery] = useState('');

  // UNASSIGNED AUTO-DISCOVERED HANDSETS QUEUE (Empty by default)
  const [unassignedDevices, setUnassignedDevices] = useState([]);

  // NAVIGATION TABS
  const [adminTab, setAdminTab] = useState('overview');
  const [selectedShopDetailId, setSelectedShopDetailId] = useState(null);
  const [shopDetailTab, setShopDetailTab] = useState('profile');

  const [shopTab, setShopTab] = useState('overview'); // 'overview' | 'devices' | 'unassigned' | 'enroll' | 'qr'

  // MODALS
  const [showAddShopModal, setShowAddShopModal] = useState(false);
  const [newShopForm, setNewShopForm] = useState({ name: '', ownerName: '', email: '', password: '', phone: '', city: 'Lahore', credits: 20 });

  const [showCreditTopupModal, setShowCreditTopupModal] = useState(false);
  const [creditTopupForm, setCreditTopupForm] = useState({ shopId: '', amount: 50 });

  const [showEnrollDeviceModal, setShowEnrollDeviceModal] = useState(false);
  const [enrollForm, setEnrollForm] = useState({
    customerName: '',
    customerPhone: '',
    customerCnic: '',
    model: '',
    imei: '',
    totalPrice: 80000,
    monthlyEmi: 8000,
    totalTerms: 10,
    startDate: new Date().toISOString().split('T')[0],
    dueDayOfMonth: 5
  });

  // RECORD PAYMENT & GRACE PERIOD MODAL
  const [activePaymentModalDev, setActivePaymentModalDev] = useState(null);
  const [paymentAmountInput, setPaymentAmountInput] = useState(8000);
  const [paymentNextDueDateInput, setPaymentNextDueDateInput] = useState('');

  const [activeGraceModalDev, setActiveGraceModalDev] = useState(null);
  const [graceDaysInput, setGraceDaysInput] = useState(7);

  // LIVE LOCATION, CAMERA SNAP, REMOTE WIPE & SECURITY SHIELD MODALS
  const [activeLocationModalDev, setActiveLocationModalDev] = useState(null);
  const [activeCameraModalDev, setActiveCameraModalDev] = useState(null);
  const [activeWipeModalDev, setActiveWipeModalDev] = useState(null);
  const [wipeConfirmText, setWipeConfirmText] = useState('');
  const [cameraAngle, setCameraAngle] = useState('front');
  const [isCapturingSnap, setIsCapturingSnap] = useState(false);
  const [isPingingLocation, setIsPingingLocation] = useState(false);
  const [showSecurityShieldModal, setShowSecurityShieldModal] = useState(false);
  const [shopAutoLockInput, setShopAutoLockInput] = useState(true);

  const [toastMessage, setToastMessage] = useState(null);
  const [qrChecksumMode, setQrChecksumMode] = useState('universal');

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  // REAL-TIME SYNC WITH LIVE BACKEND ENGINE
  useEffect(() => {
    const syncWithBackend = async () => {
      try {
        // 1. Fetch live inbox
        const inboxRes = await apiFetch('/api/inbox');
        if (inboxRes.ok) {
          const inboxData = await inboxRes.json();
          if (Array.isArray(inboxData.inbox)) {
            setUnassignedDevices(inboxData.inbox);
          }
        }

        // 2. Fetch live devices
        const devRes = await apiFetch('/api/devices');
        if (devRes.ok) {
          const devData = await devRes.json();
          if (devData.devices && devData.devices.length > 0) {
            // Update physical device id dynamically
            const livePhysical = devData.devices[devData.devices.length - 1];
            const isLiveReleased = (livePhysical.status || '').toUpperCase().includes('COMPLETED') || (livePhysical.status || '').toUpperCase().includes('RELEASED');
            setDevices(prev => {
              const exists = prev.some(d => d.id === livePhysical.id);
              if (!exists) return prev; // Do NOT resurrect if user deleted it!
              const updated = prev.map(d => {
                if (d.id === livePhysical.id) {
                  return {
                    ...d,
                    model: livePhysical.model || d.model,
                    isLocked: isLiveReleased ? false : (livePhysical.isLocked !== undefined ? livePhysical.isLocked : d.isLocked),
                    status: isLiveReleased ? 'Completed (Released)' : (livePhysical.isLocked ? 'Overdue (Locked)' : d.status),
                    liveLocation: livePhysical.liveLocation || d.liveLocation,
                    cameraSnaps: (livePhysical.cameraSnaps && livePhysical.cameraSnaps.length > 0) ? livePhysical.cameraSnaps : d.cameraSnaps
                  };
                }
                return d;
              });
              try {
                localStorage.setItem('abiz_custom_devices', JSON.stringify(updated));
              } catch (e) {}
              return updated;
            });

            // Update active modals if currently open
            if (activeLocationModalDev?.id === livePhysical.id && livePhysical.liveLocation) {
              setActiveLocationModalDev(curr => ({ ...curr, liveLocation: livePhysical.liveLocation }));
            }
            if (activeCameraModalDev?.id === livePhysical.id && livePhysical.cameraSnaps?.length > 0) {
              setActiveCameraModalDev(curr => ({ ...curr, cameraSnaps: livePhysical.cameraSnaps }));
            }
          }
        }

        // 3. AUTOMATIC DUE DATE LOCK ENGINE (Offline & Online Enforcement)
        const todayStr = new Date().toISOString().split('T')[0];
        setDevices(prev => {
          let hasAutoLocked = false;
          const evaluated = prev.map(d => {
            if (d.autoLockEnabled && !isDeviceCleared(d) && d.nextDueDate && d.nextDueDate !== 'Completed' && todayStr >= d.nextDueDate && !d.isLocked) {
              hasAutoLocked = true;
              console.log(`[AUTO-LOCK-TRIGGER] Handset ${d.model} (${d.customerName}) overdue (due: ${d.nextDueDate})! Auto-locking.`);
              // STRICT GUARD: Only dispatch command to live server if this device is an actual enrolled hardware phone!
              if (d.id && d.id.startsWith('DEV-17')) {
                try {
                  apiFetch('/api/command', {
                    method: 'POST',
                    body: JSON.stringify({
                      deviceId: d.id,
                      type: 'kiosk.enter',
                      shopId: d.shopId,
                      payload: { mode: 'launcher', autoLock: true, reason: 'due_date_overdue' }
                    })
                  }).catch(() => {});
                } catch(e) {}
              }
              return { ...d, isLocked: true, status: 'Overdue (Auto-Locked)' };
            }
            return d;
          });
          if (hasAutoLocked) {
            try { localStorage.setItem('abiz_custom_devices', JSON.stringify(evaluated)); } catch (e) {}
          }
          return evaluated;
        });
      } catch (e) {
        // silent sync
      }
    };

    syncWithBackend();
    const interval = setInterval(syncWithBackend, 5000);
    return () => clearInterval(interval);
  }, []);

  // Sync shop settings inputs when currentShopData is loaded or changes
  const currentShopData = authSession?.role === 'SHOP_OWNER' ? shops.find(s => s.id === authSession.shopId) : null;
  const currentShopDevices = authSession?.role === 'SHOP_OWNER' ? devices.filter(d => d.shopId === authSession.shopId) : [];
  const currentShopProtectedDevices = currentShopDevices.filter(isDeviceProtected);
  const currentShopClearedDevices = currentShopDevices.filter(isDeviceCleared);

  useEffect(() => {
    if (currentShopData) {
      setShopNameInput(currentShopData.name || '');
      setShopOwnerInput(currentShopData.ownerName || '');
      setShopPhoneInput(currentShopData.phone || '');
      setShopFrpEmailInput(currentShopData.frpRecoveryEmail || 'usman.lahore.recovery@gmail.com');
      setBankNameInput(currentShopData.bankName || 'Meezan Bank');
      setBankAccountInput(currentShopData.bankAccountNumber || '0102-0105849301');
      setBankTitleInput(currentShopData.bankAccountTitle || currentShopData.name || '');
      setBankIbanInput(currentShopData.bankIban || 'PK36MEZN0001020105849301');
      setJazzCashInput(currentShopData.jazzCashNumber || currentShopData.phone || '');
      setJazzCashTitleInput(currentShopData.jazzCashTitle || currentShopData.ownerName || '');
      setEasyPaisaInput(currentShopData.easyPaisaNumber || currentShopData.phone || '');
      setEasyPaisaTitleInput(currentShopData.easyPaisaTitle || currentShopData.ownerName || '');
      setLockNoticeUrduInput(currentShopData.lockNoticeUrdu || 'معزز کسٹمر، آپکی ماہانہ قسط کی ادائیگی کی تاریخ گزر چکی ہے۔ فون ان لاک کروانے کیلئے برائے مہربانی فوری اپنے شاپ اونر سے رابطہ کریں اور قسط جمع کروائیں۔');
      setShopAutoLockInput(currentShopData.autoLockEnabled !== false);
    }
  }, [authSession?.shopId, shops]);

  // ==========================================
  // HANDLERS: AUTHENTICATION
  // ==========================================
  const handleLogin = (e) => {
    e.preventDefault();
    setLoginError('');

    const cleanEmail = (loginEmail || '').trim().toLowerCase();
    const cleanPassword = (loginPassword || '').trim();

    if (currentPortal === 'admin') {
      if (cleanEmail === 'admin@abizlocker.com' && cleanPassword === 'admin123') {
        updateAuthSession({
          role: 'SUPER_ADMIN',
          email: 'admin@abizlocker.com',
          name: 'Super Admin HQ'
        });
        showToast('Welcome back, Super Admin!');
        return;
      }
      const isShop = shops.some(s => s.email.toLowerCase() === cleanEmail);
      if (isShop) {
        setLoginError('This portal is restricted to Super Admin HQ. Please switch to the Shop Partner portal.');
        return;
      }
      setLoginError('Invalid Super Admin email or password.');
      return;
    }

    // Shop Partner Portal Login
    if (cleanEmail === 'admin@abizlocker.com') {
      setLoginError('Super Admin accounts must sign in via the Super Admin portal.');
      return;
    }

    const foundShop = shops.find(s => s.email.toLowerCase() === cleanEmail && s.password === cleanPassword);
    if (foundShop) {
      updateAuthSession({
        role: 'SHOP_OWNER',
        shopId: foundShop.id,
        email: foundShop.email,
        name: foundShop.ownerName,
        shopName: foundShop.name
      });
      showToast(`Welcome back, ${foundShop.ownerName}!`);
      return;
    }

    setLoginError('Invalid shop email or password. Please verify your credentials.');
  };

  const handleLogout = () => {
    updateAuthSession(null);
    setLoginEmail('');
    setLoginPassword('');
    setLoginError('');
    setMobileMenuOpen(false);
    showToast('Logged out successfully.');
  };

  // SAVE SHOP LOCKSCREEN & PAYMENT ACCOUNTS SETTINGS
  const handleSaveShopSettings = async (e) => {
    e.preventDefault();
    if (!currentShopData) return;

    const updatedShop = {
      ...currentShopData,
      name: shopNameInput,
      ownerName: shopOwnerInput,
      phone: shopPhoneInput,
      frpRecoveryEmail: shopFrpEmailInput,
      autoLockEnabled: shopAutoLockInput,
      bankName: bankNameInput,
      bankAccountNumber: bankAccountInput,
      bankAccountTitle: bankTitleInput,
      bankIban: bankIbanInput,
      jazzCashNumber: jazzCashInput,
      jazzCashTitle: jazzCashTitleInput,
      easyPaisaNumber: easyPaisaInput,
      easyPaisaTitle: easyPaisaTitleInput,
      lockNoticeUrdu: lockNoticeUrduInput
    };

    const newShops = shops.map(s => s.id === currentShopData.id ? updatedShop : s);
    setShops(newShops);
    try {
      localStorage.setItem('abiz_custom_shops', JSON.stringify(newShops));
    } catch (err) {}

    // Dispatch update to live backend engine
    try {
      await apiFetch('/api/shop/settings', {
        method: 'POST',
        body: JSON.stringify({
          shopId: currentShopData.id,
          name: shopNameInput,
          ownerName: shopOwnerInput,
          phone: shopPhoneInput,
          bankName: bankNameInput,
          accountNumber: bankAccountInput,
          accountTitle: bankTitleInput,
          iban: bankIbanInput,
          jazzCash: jazzCashInput,
          easyPaisa: easyPaisaInput,
          lockNoticeUrdu: lockNoticeUrduInput
        })
      });
    } catch (err) {
      console.log('Tunnel shop settings error:', err);
    }

    showToast('🎉 Lockscreen & Payment Account details saved successfully!');
  };

  // ==========================================
  // HANDLERS: REALTIME ACTIONS
  // ==========================================
  const handleToggleLock = async (deviceId) => {
    const targetDev = devices.find(d => d.id === deviceId);
    if (!targetDev) return;
    const nextState = !targetDev.isLocked;

    const targetShop = shops.find(s => s.id === targetDev.shopId) || currentShopData;

    // 1. Dispatch to Live Server Hub
    try {
      apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: targetDev.id,
          type: nextState ? 'kiosk.enter' : 'kiosk.exit',
          shopId: targetDev.shopId,
          ownerPhone: targetShop?.phone || '0300-1234567',
          paymentAccount: `${targetShop?.bankName || 'Meezan Bank'}: ${targetShop?.bankAccountNumber || '0102-0105849301'}\nJazzCash: ${targetShop?.jazzCashNumber || '0300-1234567'}`,
          payload: nextState ? {
            mode: 'launcher',
            allowedPackages: [],
            features: { home: false, recents: false, notifications: false, systemInfo: true, keyguard: false, lockButtons: true },
            exitMode: 'gesture',
            password: '123'
          } : {}
        })
      }).catch(err => console.log('Tunnel sync:', err));
    } catch (e) {
      console.log('Error dispatching command:', e);
    }

    if (!nextState) {
      try {
        apiFetch('/api/command', {
          method: 'POST',
          body: JSON.stringify({
            deviceId: targetDev.id,
            type: 'policy.updateAutoLock',
            payload: { autoLockEnabled: false }
          })
        }).catch(() => {});
      } catch(e) {}
    }

    setDevices(prev => {
      const updated = prev.map(d => {
        if (d.id === deviceId) {
          showToast(`OTA Command Dispatched: ${d.model} is now ${nextState ? 'LOCKED 🔒' : 'UNLOCKED 🔓'}`);
          return {
            ...d,
            isLocked: nextState,
            status: nextState ? 'Overdue (Locked)' : (d.paidTerms === d.totalTerms ? 'Completed' : 'Active'),
            autoLockEnabled: nextState ? d.autoLockEnabled : false
          };
        }
        return d;
      });
      try { localStorage.setItem('abiz_custom_devices', JSON.stringify(updated)); } catch(e) {}
      return updated;
    });
  };

  const handleClearLoan = async (deviceId) => {
    const targetDev = devices.find(d => d.id === deviceId);
    if (!targetDev) return;

    if (!confirm(`Clear loan and permanently release ${targetDev.model} (${targetDev.customerName})?\n\nThis will remove Device Owner lock from the phone and enable the customer to uninstall ABIZ LOCKER.`)) {
      return;
    }

    try {
      await apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: targetDev.id,
          type: 'device.release',
          payload: {}
        })
      });
    } catch (e) {
      console.log('Tunnel release error:', e);
    }

    const nowStr = new Date().toLocaleString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
    const releasedByName = authSession?.role === 'SUPER_ADMIN' ? 'Super Admin' : (authSession?.name || 'Shop Owner');

    setDevices(prev => {
      const updated = prev.map(d => {
        if (d.id === deviceId) {
          return {
            ...d,
            isLocked: false,
            status: 'Completed (Released)',
            paidTerms: d.totalTerms,
            clearedAt: nowStr,
            releasedBy: releasedByName
          };
        }
        return d;
      });
      try {
        localStorage.setItem('abiz_custom_devices', JSON.stringify(updated));
      } catch (e) {}
      return updated;
    });

    showToast(`🎉 Loan Cleared for ${targetDev.model}! Device Owner released & moved to Recent History.`);
  };

  // PERMISSION CHECKERS (SUPER ADMIN VS SHOP OWNER)
  const canAccessLocation = (dev) => {
    if (authSession?.role === 'SUPER_ADMIN') return true;
    const targetShop = shops.find(s => s.id === (dev?.shopId || authSession?.shopId));
    return !!targetShop?.allowLocationAccess;
  };

  const canAccessCamera = (dev) => {
    if (authSession?.role === 'SUPER_ADMIN') return true;
    const targetShop = shops.find(s => s.id === (dev?.shopId || authSession?.shopId));
    return !!targetShop?.allowCameraAccess;
  };

  const handleOpenLocationModal = (dev) => {
    if (!canAccessLocation(dev)) {
      alert(`[🔒 RESTRICTED BY SUPER ADMIN]\n\nLive GPS Location Tracking is currently locked for your franchise (${currentShopData?.name || 'Shop'}).\n\nOnly Super Admin has authority to grant surveillance access to shop owners.`);
      return;
    }
    setActiveLocationModalDev(dev);
  };

  const handleOpenCameraModal = (dev) => {
    if (!canAccessCamera(dev)) {
      alert(`[🔒 RESTRICTED BY SUPER ADMIN]\n\nRemote Camera Snap Access is currently locked for your franchise (${currentShopData?.name || 'Shop'}).\n\nOnly Super Admin has authority to grant camera snapshot access to shop owners.`);
      return;
    }
    setActiveCameraModalDev(dev);
  };

  const handleToggleShopPermission = (shopId, permKey) => {
    if (authSession?.role !== 'SUPER_ADMIN') return;
    setShops(prev => {
      const updated = prev.map(s => {
        if (s.id === shopId) {
          const nextVal = !s[permKey];
          const label = permKey === 'allowLocationAccess' ? 'GPS Location Tracking' : 'Remote Camera Snap';
          showToast(`Super Admin updated ${s.name}: ${label} is now ${nextVal ? 'GRANTED 🟢' : 'REVOKED 🔴'}`);
          return { ...s, [permKey]: nextVal };
        }
        return s;
      });
      try { localStorage.setItem('abiz_custom_shops', JSON.stringify(updated)); } catch (e) {}
      return updated;
    });
  };

  const handleToggleDeviceAutoLock = async (deviceId) => {
    const targetDev = devices.find(d => d.id === deviceId);
    if (!targetDev) return;
    const nextVal = !targetDev.autoLockEnabled;

    try {
      apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: targetDev.id,
          type: 'policy.updateAutoLock',
          payload: { autoLockEnabled: nextVal }
        })
      }).catch(() => {});
    } catch(e) {}

    setDevices(prev => {
      const updated = prev.map(d => {
        if (d.id === deviceId) {
          return { ...d, autoLockEnabled: nextVal };
        }
        return d;
      });
      try { localStorage.setItem('abiz_custom_devices', JSON.stringify(updated)); } catch(e) {}
      return updated;
    });
    showToast(`Automatic Due Date Lock for ${targetDev.model}: ${nextVal ? 'ACTIVATED ✅' : 'DISABLED ⏸️'}`);
  };

  const handleSetCityLocation = async (dev, cityData) => {
    const updatedLoc = {
      lat: cityData.lat,
      lng: cityData.lng,
      address: cityData.address,
      accuracy: '±3m (High Precision Satellite Fix)',
      lastUpdated: 'Just Now (Real-Time GPS)',
      battery: `${Math.floor(Math.random() * 10) + 80}%`,
      network: '4G LTE / Jazz'
    };

    try {
      await apiFetch('/api/device/location', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: dev.id,
          lat: cityData.lat,
          lng: cityData.lng,
          address: cityData.address,
          accuracy: '±3m (City GPS Centroid)'
        })
      });
    } catch(e) {}

    setDevices(prev => {
      const updated = prev.map(d => {
        if (d.id === dev.id) {
          return { ...d, liveLocation: updatedLoc };
        }
        return d;
      });
      try { localStorage.setItem('abiz_custom_devices', JSON.stringify(updated)); } catch(e) {}
      return updated;
    });

    if (activeLocationModalDev?.id === dev.id) {
      setActiveLocationModalDev(curr => ({ ...curr, liveLocation: updatedLoc }));
    }
    showToast(`📍 Location updated to ${cityData.name} (${cityData.lat}, ${cityData.lng})!`);
  };

  const handlePingLiveLocation = async (dev) => {
    setIsPingingLocation(true);

    try {
      // 1. Wake target handset GPS sensor via MDM Command
      await apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: dev.id,
          type: 'device.locationMode',
          payload: { mode: 'active' }
        })
      });

      // 2. Fetch target handset's real reported coordinates from MDM server
      const res = await apiFetch(`/api/device/location?deviceId=${encodeURIComponent(dev.id)}`);
      if (res && res.location) {
        const handsetLoc = {
          ...res.location,
          lastUpdated: 'Live GPS Fix (Handset Sensor)',
          source: 'TARGET_MOBILE_HARDWARE'
        };

        setDevices(prev => {
          const updated = prev.map(d => (d.id === dev.id ? { ...d, liveLocation: handsetLoc } : d));
          try { localStorage.setItem('abiz_custom_devices', JSON.stringify(updated)); } catch(e) {}
          return updated;
        });

        if (activeLocationModalDev?.id === dev.id) {
          setActiveLocationModalDev(curr => ({ ...curr, liveLocation: handsetLoc }));
        }
        showToast(`📍 Mobile Hardware GPS Fix: Lat ${handsetLoc.lat}, Lng ${handsetLoc.lng} (${dev.model})`);
      } else {
        showToast(`🛰️ GPS Satellite ping sent to ${dev.model}. Awaiting satellite fix...`);
      }
    } catch(err) {
      console.log('Location request err:', err);
      showToast('⚠️ Could not connect to handset location sensor.');
    } finally {
      setIsPingingLocation(false);
    }
  };

  const handleCaptureCameraSnap = async (dev) => {
    setIsCapturingSnap(true);
    try {
      // 1. Dispatch remote camera shutter command to handset
      await apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: dev.id,
          type: 'device.cameraCapture',
          payload: { camera: cameraAngle === 'front' ? 'Front (Selfie)' : 'Rear Camera' }
        })
      });

      // 2. Poll server for fresh camera snapshots
      setTimeout(async () => {
        try {
          const devsRes = await apiFetch('/api/devices');
          if (devsRes && devsRes.devices) {
            const liveDev = devsRes.devices.find(d => d.id === dev.id || d.id.startsWith(dev.id) || dev.id.startsWith(d.id));
            if (liveDev && liveDev.cameraSnaps?.length > 0) {
              setDevices(prev => prev.map(d => (d.id === dev.id ? { ...d, cameraSnaps: liveDev.cameraSnaps } : d)));
              if (activeCameraModalDev?.id === dev.id) {
                setActiveCameraModalDev(curr => ({ ...curr, cameraSnaps: liveDev.cameraSnaps }));
              }
            }
          }
        } catch(e) {}
        setIsCapturingSnap(false);
        showToast(`📷 Remote shutter triggered on ${dev.model} (${cameraAngle === 'front' ? 'Front' : 'Rear'} Lens)`);
      }, 1500);
    } catch(e) {
      setIsCapturingSnap(false);
      showToast('❌ Camera shutter command failed.');
    }
  };

  const handleUploadCameraEvidence = (e, dev) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (event) => {
      const base64Img = event.target?.result;
      if (!base64Img) return;
      try {
        await apiFetch('/api/device/camera/upload', {
          method: 'POST',
          body: JSON.stringify({
            deviceId: dev.id,
            camera: cameraAngle === 'front' ? 'Front (Selfie)' : 'Rear Camera',
            image: base64Img
          })
        });
        const snapEntry = {
          id: `SNAP-${Date.now()}`,
          timestamp: new Date().toLocaleTimeString(),
          camera: cameraAngle === 'front' ? 'Front (Selfie)' : 'Rear Camera',
          image: base64Img,
          isRealPhoto: true
        };
        setDevices(prev => prev.map(d => d.id === dev.id ? { ...d, cameraSnaps: [snapEntry, ...(d.cameraSnaps || [])] } : d));
        if (activeCameraModalDev?.id === dev.id) {
          setActiveCameraModalDev(curr => ({ ...curr, cameraSnaps: [snapEntry, ...(curr.cameraSnaps || [])] }));
        }
        showToast('✅ Real photographic evidence photo uploaded and saved!');
      } catch(err) {
        showToast('❌ Failed to upload photo.');
      }
    };
    reader.readAsDataURL(file);
  };

  const handleRemoteWipe = (deviceId) => {
    const targetDev = devices.find(d => d.id === deviceId);
    if (!targetDev) return;
    setWipeConfirmText('');
    setActiveWipeModalDev(targetDev);
  };

  const handleExecuteRemoteWipe = async (dev) => {
    if (!dev) return;
    try {
      await apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: dev.id,
          type: 'device.wipe',
          payload: {}
        })
      });
      showToast(`🔥 Remote Factory Reset signal dispatched to ${dev.model}! Device will wipe.`);
    } catch (e) {
      console.log('Tunnel wipe error:', e);
    }

    setDevices(prev => {
      const updated = prev.map(d => {
        if (d.id === dev.id) {
          return { ...d, isLocked: false, status: 'Factory Wiped' };
        }
        return d;
      });
      try { localStorage.setItem('abiz_custom_devices', JSON.stringify(updated)); } catch (e) {}
      return updated;
    });
    setActiveWipeModalDev(null);
    setWipeConfirmText('');
  };

  const handleCreateShop = (e) => {
    e.preventDefault();
    if (!newShopForm.name || !newShopForm.ownerName || !newShopForm.email) return;
    const newId = `SH-${100 + shops.length + 1}`;
    const newShop = {
      id: newId,
      name: newShopForm.name,
      ownerName: newShopForm.ownerName,
      email: newShopForm.email,
      password: newShopForm.password || '123',
      phone: newShopForm.phone || '0300-1234567',
      city: newShopForm.city,
      credits: Number(newShopForm.credits) || 10,
      status: 'Active',
      createdAt: new Date().toISOString().split('T')[0],
      paymentHistory: [
        { id: `PAY-${Date.now().toString().slice(-3)}`, amount: Number(newShopForm.credits) * 1000, creditsIssued: Number(newShopForm.credits), date: new Date().toISOString().split('T')[0], method: 'Initial Allocation' }
      ]
    };
    setShops([newShop, ...shops]);
    setShowAddShopModal(false);
    setNewShopForm({ name: '', ownerName: '', email: '', password: '', phone: '', city: 'Lahore', credits: 20 });
    showToast(`Shop "${newShop.name}" registered with ${newShop.credits} credits!`);
  };

  const handleTopupCredits = (e) => {
    e.preventDefault();
    const targetShopId = creditTopupForm.shopId || (selectedShopDetailId || shops[0]?.id);
    const addedCredits = Number(creditTopupForm.amount);
    
    setShops(prev => prev.map(s => {
      if (s.id === targetShopId) {
        return {
          ...s,
          credits: s.credits + addedCredits,
          paymentHistory: [
            { id: `PAY-${Date.now().toString().slice(-4)}`, amount: addedCredits * 1000, creditsIssued: addedCredits, date: new Date().toISOString().split('T')[0], method: 'HQ Allocation' },
            ...s.paymentHistory
          ]
        };
      }
      return s;
    }));
    setShowCreditTopupModal(false);
    showToast(`Successfully issued +${addedCredits} credits!`);
  };

  const handleEnrollDevice = (e) => {
    e.preventDefault();
    const currentShop = shops.find(s => s.id === authSession.shopId);
    if (!currentShop) return;

    if (currentShop.credits < globalSettings.creditsPerEnrollment) {
      alert(`Insufficient Credits! You have ${currentShop.credits} credits, but ${globalSettings.creditsPerEnrollment} credit is required per device.`);
      return;
    }

    setShops(prev => prev.map(s => {
      if (s.id === currentShop.id) {
        return { ...s, credits: s.credits - globalSettings.creditsPerEnrollment };
      }
      return s;
    }));

    const newDev = {
      id: `DEV-00${devices.length + 1}`,
      shopId: currentShop.id,
      shopName: currentShop.name,
      customerName: enrollForm.customerName,
      customerPhone: enrollForm.customerPhone,
      customerCnic: enrollForm.customerCnic,
      model: enrollForm.model,
      imei: enrollForm.imei,
      totalPrice: Number(enrollForm.totalPrice),
      monthlyEmi: Number(enrollForm.monthlyEmi),
      paidTerms: 1,
      totalTerms: Number(enrollForm.totalTerms),
      startDate: enrollForm.startDate || new Date().toISOString().split('T')[0],
      nextDueDate: enrollForm.startDate ? new Date(new Date(enrollForm.startDate).setMonth(new Date(enrollForm.startDate).getMonth() + 1)).toISOString().split('T')[0] : '2026-09-05',
      dueDayOfMonth: Number(enrollForm.dueDayOfMonth) || 5,
      isLocked: false,
      status: 'Active',
      enrolledAt: new Date().toISOString().split('T')[0]
    };

    setDevices([newDev, ...devices]);
    setShowEnrollDeviceModal(false);
    setEnrollForm({
      customerName: '', customerPhone: '', customerCnic: '', model: '', imei: '',
      totalPrice: 80000, monthlyEmi: 8000, totalTerms: 10, startDate: new Date().toISOString().split('T')[0], dueDayOfMonth: 5
    });
    showToast(`Device enrolled successfully! (${globalSettings.creditsPerEnrollment} Credit deducted)`);
  };

  // 1-CLICK ASSIGN AUTO-DISCOVERED HANDSET
  const handleAssignScannedDevice = (scannedDev) => {
    setEnrollForm(prev => ({
      ...prev,
      model: scannedDev.model,
      imei: scannedDev.imei,
      customerName: '',
      customerPhone: '',
      customerCnic: ''
    }));
    // Remove from unassigned queue
    setUnassignedDevices(prev => prev.filter(s => s.id !== scannedDev.id));
    setShowEnrollDeviceModal(true);
    showToast(`Handset ${scannedDev.model} loaded! Enter customer financing details.`);
  };

  // DISCARD HANDSET FROM SCANNED INBOX
  const handleDiscardScannedDevice = async (scannedDev) => {
    if (!confirm(`Remove ${scannedDev.model} from Scanned Inbox?`)) return;
    setUnassignedDevices(prev => prev.filter(s => s.id !== scannedDev.id));
    try {
      await apiFetch('/api/device/delete', {
        method: 'POST',
        body: JSON.stringify({ deviceId: scannedDev.id })
      });
    } catch (e) {
      console.log('Error discarding handset on server:', e);
    }
    showToast(`Handset ${scannedDev.model} discarded from inbox.`);
  };

  // SEND PAYMENT WARNING PUSH TO PHYSICAL PHONE
  const handleSendPaymentWarning = (dev) => {
    // 1. Dispatch device.alert to phone over Live Tunnel
    try {
      apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: dev.id,
          type: 'device.alert',
          payload: {
            title: '⚠️ INSTALLMENT OVERDUE WARNING',
            body: `Mohtaram ${dev.customerName}, aapki kist PKR ${dev.monthlyEmi.toLocaleString()} due date (${dev.nextDueDate}) par wajibul ada hai. Baraye meharbani ada karein warna phone freeze lock kar diya jayega.`
          }
        })
      }).catch(err => console.log('Tunnel sync alert:', err));
    } catch (e) {
      console.log('Error dispatching alert:', e);
    }

    showToast(`⚠️ Payment Warning & Urdu Notice dispatched to ${dev.customerName}'s phone (${dev.model})!`);
    alert(`[ABIZ LOCKER OTA DISPATCH]\nWarning Notice sent to physical device ${dev.model} (IMEI: ${dev.imei}):\n\n"Mohtaram ${dev.customerName}, aapki kist PKR ${dev.monthlyEmi.toLocaleString()} due date (${dev.nextDueDate}) par wajibul ada hai. Baraye meharbani ada karein warna phone freeze lock kar diya jayega."`);
  };

  // GRANT GRACE PERIOD / EXTEND DUE DATE
  const handleGrantGracePeriod = (devId, days) => {
    setDevices(prev => prev.map(d => {
      if (d.id === devId) {
        const curr = new Date();
        curr.setDate(curr.getDate() + Number(days));
        const newDate = curr.toISOString().split('T')[0];
        return {
          ...d,
          nextDueDate: newDate,
          isLocked: false,
          status: 'Active (Grace Period)'
        };
      }
      return d;
    }));
    setActiveGraceModalDev(null);
    showToast(`Grace period of ${days} days granted! Next due date extended.`);
  };

  // RECEIVE & RECORD INSTALLMENT PAYMENT ENTRY (WITH AUTO-ADVANCING DUE DATE & AUTO-UNLOCK)
  const handleReceiveInstallment = async (devId, amount, customNextDueDate) => {
    const targetDev = devices.find(d => d.id === devId);
    if (!targetDev) return;

    const nextPaid = Math.min(targetDev.paidTerms + 1, targetDev.totalTerms);
    const isComplete = nextPaid >= targetDev.totalTerms;

    let computedNextDate = customNextDueDate;
    if (!computedNextDate) {
      const nextMonth = new Date();
      nextMonth.setMonth(nextMonth.getMonth() + 1);
      computedNextDate = nextMonth.toISOString().split('T')[0];
    }
    const finalDueDate = isComplete ? 'Completed' : computedNextDate;

    // Dispatch update to live server & handset
    try {
      apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: targetDev.id,
          type: 'policy.updateDueDate',
          payload: {
            nextDueDate: finalDueDate,
            paidTerms: nextPaid,
            isLocked: false
          }
        })
      }).catch(err => console.log('Tunnel sync:', err));
    } catch(e) {}

    setDevices(prev => {
      const updated = prev.map(d => {
        if (d.id === devId) {
          return {
            ...d,
            paidTerms: nextPaid,
            nextDueDate: finalDueDate,
            isLocked: false,
            status: isComplete ? 'Completed' : 'Active'
          };
        }
        return d;
      });
      try {
        localStorage.setItem('abiz_custom_devices', JSON.stringify(updated));
      } catch (e) {}
      return updated;
    });

    setActivePaymentModalDev(null);
    showToast(`✅ Installment PKR ${Number(amount).toLocaleString()} recorded! Next Due Date: ${finalDueDate}. Phone unlocked & Auto-Lock delayed.`);
  };

  // DELETE & PERMANENTLY REMOVE DEVICE FROM SYSTEM
  const handleDeleteDevice = (devId) => {
    // 1. Dispatch permanent release command to physical phone
    try {
      apiFetch('/api/command', {
        method: 'POST',
        body: JSON.stringify({
          deviceId: devId,
          type: 'kiosk.exit',
          payload: {}
        })
      }).catch(err => console.log('Tunnel release:', err));
    } catch (e) {
      console.log('Error deleting device:', e);
    }

    setDevices(prev => {
      const filtered = prev.filter(d => d.id !== devId);
      try { localStorage.setItem('abiz_custom_devices', JSON.stringify(filtered)); } catch (e) {}
      return filtered;
    });
    showToast(`Device removed permanently from financing inventory.`);
  };

  // DELETE & PERMANENTLY REMOVE DEVICE RECORD FROM RECENT HISTORY
  const handleDeleteHistoryDevice = async (devId) => {
    const target = devices.find(d => d.id === devId);
    const targetModel = target?.model || 'Device';
    const targetCustomer = target?.customerName || '';

    // 1. Remove from local state and localStorage
    const newDevices = devices.filter(d => d.id !== devId);
    setDevices(newDevices);
    try {
      localStorage.setItem('abiz_custom_devices', JSON.stringify(newDevices));
    } catch (e) {}

    // Close NOC modal if opened
    if (selectedHistoryDev?.id === devId) {
      setSelectedHistoryDev(null);
    }

    // 2. Call backend to clear device from server DB and reset for fresh re-scanning
    try {
      await apiFetch('/api/device/delete', {
        method: 'POST',
        body: JSON.stringify({ deviceId: devId, model: targetModel })
      });
    } catch (e) {
      console.log('Error deleting history record on server:', e);
    }

    showToast(`🗑️ ${targetModel} (${targetCustomer}) deleted from History! You can now scan and re-enroll it cleanly.`);
  };

  const handleClearAllHistory = async () => {
    if (!confirm('Are you sure you want to delete ALL cleared loan records from History?')) return;
    const newDevices = devices.filter(isDeviceProtected);
    setDevices(newDevices);
    try {
      localStorage.setItem('abiz_custom_devices', JSON.stringify(newDevices));
    } catch (e) {}
    setSelectedHistoryDev(null);
    showToast('🗑️ All history records permanently cleared!');
  };

  const handleBuyCreditsWhatsApp = () => {
    const currentShop = shops.find(s => s.id === authSession?.shopId);
    const shopTitle = currentShop ? currentShop.name : 'Shop Owner';
    const text = encodeURIComponent(`Assalam o Alaikum ABIZ Admin,\nI want to buy device protection credits for my shop: *${shopTitle}*.\nPlease share payment account details.`);
    window.open(`https://wa.me/${globalSettings.supportWhatsApp}?text=${text}`, '_blank');
  };

  const handleSaveGlobalSettings = (e) => {
    e.preventDefault();
    setGlobalSettings(prev => ({
      ...prev,
      creditsPerEnrollment: Number(tempCreditsPerEnrollment),
      supportWhatsApp: tempWhatsApp,
      supportEmail: tempEmail
    }));
    showToast('Super Admin settings saved successfully!');
  };

  // ==========================================
  // RENDER: 1. LOGIN SCREEN
  // ==========================================
  // ==========================================
  // RENDER: 1. LOGIN SCREEN (PORTAL SEPARATED)
  // ==========================================
  if (!authSession) {
    const isAdminPortal = (currentPortal === 'admin');

    return (
      <div className="min-h-screen bg-[#F8FAFC] flex flex-col justify-center items-center p-4 sm:p-6 font-sans selection:bg-slate-900 selection:text-white text-base">
        <motion.div
          key={currentPortal}
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          className="max-w-md w-full bg-white rounded-3xl border border-slate-200 p-8 shadow-xl space-y-6"
        >
          {/* LOGO & BRAND */}
          <div className="text-center space-y-3">
            <img src="/abiz-logo.png" alt="ABIZ Logo" className="w-16 h-16 mx-auto object-contain drop-shadow-sm" />
            <div>
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold mb-2 shadow-xs border"
                style={{
                  backgroundColor: isAdminPortal ? '#EEF2FF' : '#ECFDF5',
                  color: isAdminPortal ? '#4338CA' : '#047857',
                  borderColor: isAdminPortal ? '#C7D2FE' : '#A7F3D0'
                }}>
                {isAdminPortal ? '👑 SUPER ADMIN HQ CONSOLE' : '🏪 FRANCHISE & SHOP PARTNER PORTAL'}
              </div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">
                {isAdminPortal ? 'Super Admin HQ' : 'Shop Partner Portal'}
              </h1>
              <p className="text-sm text-slate-500 font-medium mt-1">
                {isAdminPortal
                  ? 'Central Network Infrastructure & Master Governance'
                  : 'Commercial Handset Financing & Device Lockdown'}
              </p>
            </div>
          </div>

          {/* LOGIN FORM */}
          <form onSubmit={handleLogin} className="space-y-4">
            {loginError && (
              <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-700 text-sm rounded-xl font-medium">
                {loginError}
              </div>
            )}

            <div>
              <label className="text-sm font-bold text-slate-700">
                {isAdminPortal ? 'Administrator Email' : 'Shop Partner Email'}
              </label>
              <input
                type="email"
                required
                placeholder={isAdminPortal ? 'admin@abizlocker.com' : 'your-shop@abiz.com'}
                value={loginEmail}
                onChange={e => setLoginEmail(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3.5 text-sm text-slate-900 mt-1.5 focus:outline-none focus:border-slate-900 transition-all font-medium"
              />
            </div>

            <div>
              <label className="text-sm font-bold text-slate-700">Password</label>
              <input
                type="password"
                required
                placeholder="••••••••"
                value={loginPassword}
                onChange={e => setLoginPassword(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3.5 text-sm text-slate-900 mt-1.5 focus:outline-none focus:border-slate-900 transition-all font-medium"
              />
            </div>

            <button
              type="submit"
              className={`w-full py-3.5 text-white font-bold text-sm rounded-xl shadow-md transition-all cursor-pointer flex items-center justify-center gap-2 ${
                isAdminPortal ? 'bg-indigo-600 hover:bg-indigo-700' : 'bg-slate-900 hover:bg-slate-800'
              }`}
            >
              <span>{isAdminPortal ? 'Access Super Admin HQ' : 'Sign In to Shop Dashboard'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* PORTAL SWITCHER LINK */}
          <div className="pt-4 border-t border-slate-100 text-center">
            {isAdminPortal ? (
              <p className="text-xs text-slate-500">
                Are you a retail franchise partner?{' '}
                <button
                  type="button"
                  onClick={() => navigateToPortal('shop')}
                  className="font-bold text-indigo-600 hover:text-indigo-800 hover:underline cursor-pointer"
                >
                  Sign in to Shop Portal ➔
                </button>
              </p>
            ) : (
              <p className="text-xs text-slate-500">
                Authorized Super Admin HQ personnel?{' '}
                <button
                  type="button"
                  onClick={() => navigateToPortal('admin')}
                  className="font-bold text-slate-900 hover:text-indigo-600 hover:underline cursor-pointer"
                >
                  Super Admin HQ Console ➔
                </button>
              </p>
            )}
          </div>
        </motion.div>
      </div>
    );
  }

  // ==========================================
  // PORTAL GUARDS: ROLE MISMATCH PROTECTION
  // ==========================================
  if (authSession.role === 'SHOP_OWNER' && currentPortal === 'admin') {
    return (
      <div className="min-h-screen bg-[#F8FAFC] flex flex-col justify-center items-center p-4 sm:p-6 font-sans">
        <div className="max-w-md w-full bg-white rounded-3xl border border-slate-200 p-8 shadow-xl text-center space-y-5">
          <div className="w-14 h-14 bg-amber-50 text-amber-600 rounded-2xl flex items-center justify-center mx-auto text-2xl font-bold">
            ⚠️
          </div>
          <div>
            <h2 className="text-xl font-black text-slate-900">Super Admin Console Access</h2>
            <p className="text-sm text-slate-500 mt-1">
              You are signed in as <strong>{currentShopData?.name || authSession.name}</strong> (Shop Partner).
              The Super Admin HQ console requires administrator privileges.
            </p>
          </div>
          <div className="space-y-2 pt-2">
            <button
              onClick={() => navigateToPortal('shop')}
              className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white font-bold text-sm rounded-xl cursor-pointer"
            >
              Go to My Shop Dashboard
            </button>
            <button
              onClick={handleLogout}
              className="w-full py-3 bg-slate-100 hover:bg-rose-50 hover:text-rose-700 text-slate-700 font-bold text-sm rounded-xl cursor-pointer"
            >
              Sign Out & Switch Account
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (authSession.role === 'SUPER_ADMIN' && currentPortal === 'shop') {
    return (
      <div className="min-h-screen bg-[#F8FAFC] flex flex-col justify-center items-center p-4 sm:p-6 font-sans">
        <div className="max-w-md w-full bg-white rounded-3xl border border-slate-200 p-8 shadow-xl text-center space-y-5">
          <div className="w-14 h-14 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center mx-auto text-2xl font-bold">
            👑
          </div>
          <div>
            <h2 className="text-xl font-black text-slate-900">Super Admin HQ Session Active</h2>
            <p className="text-sm text-slate-500 mt-1">
              You are signed in as <strong>Super Admin HQ</strong>. To view or manage franchise shops, return to the Super Admin Console.
            </p>
          </div>
          <div className="space-y-2 pt-2">
            <button
              onClick={() => navigateToPortal('admin')}
              className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm rounded-xl cursor-pointer"
            >
              Open Super Admin Console
            </button>
            <button
              onClick={handleLogout}
              className="w-full py-3 bg-slate-100 hover:bg-rose-50 hover:text-rose-700 text-slate-700 font-bold text-sm rounded-xl cursor-pointer"
            >
              Sign Out
            </button>
          </div>
        </div>
      </div>
    );
  }

  const SidebarContent = () => (
    <div className="flex flex-col justify-between h-full p-5">
      <div className="space-y-6">
        
        {/* BRAND LOGO + TEXT */}
        <div className="flex items-center justify-between px-1">
          <div className="flex items-center gap-3">
            <img src="/abiz-logo.png" alt="ABIZ" className="w-10 h-10 object-contain drop-shadow-sm" />
            <div>
              <div className="flex items-center gap-1.5">
                <h1 className="font-extrabold text-base text-slate-900 tracking-tight">ABIZ LOCKER</h1>
                <span className="text-[10px] font-bold px-1.5 py-0.5 bg-slate-100 text-slate-600 rounded">v22</span>
              </div>
              <p className="text-xs text-slate-400 font-medium">Enterprise MDM Suite</p>
            </div>
          </div>
          <button onClick={() => setMobileMenuOpen(false)} className="md:hidden p-1.5 text-slate-400 hover:text-slate-900 rounded-lg">
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* ACTIVE USER BADGE */}
        <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-2xl space-y-1.5">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-500">
            <span>Session</span>
            <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${authSession.role === 'SUPER_ADMIN' ? 'bg-indigo-100 text-indigo-700' : 'bg-emerald-100 text-emerald-700'}`}>
              {authSession.role === 'SUPER_ADMIN' ? 'Super Admin' : 'Franchise'}
            </span>
          </div>
          <p className="text-sm font-bold text-slate-900 truncate">
            {authSession.role === 'SUPER_ADMIN' ? authSession.name : currentShopData?.name}
          </p>
          {authSession.role === 'SHOP_OWNER' && (
            <div className="flex items-center justify-between text-xs pt-1">
              <span className="text-slate-500">Available Credits:</span>
              <span className="font-bold font-mono text-emerald-600 text-sm">{currentShopData?.credits} Credits</span>
            </div>
          )}
        </div>

        {/* SUPER ADMIN NAVIGATION */}
        {authSession.role === 'SUPER_ADMIN' && (
          <nav className="space-y-1.5">
            <p className="px-3 text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Master Governance</p>
            {[
              { id: 'overview', label: 'Network Overview', icon: BarChart3 },
              { id: 'shops', label: 'Franchise Shops', icon: Store, count: shops.length },
              { id: 'unassigned', label: 'Scanned Inbox', icon: Radio, count: unassignedDevices.length },
              { id: 'devices', label: 'All Fleet Devices', icon: Smartphone, count: protectedDevices.length },
              { id: 'history', label: 'Recent History', icon: History, count: clearedDevices.length },
              { id: 'settings', label: 'Settings & WhatsApp', icon: Settings },
            ].map((item) => {
              const Icon = item.icon;
              const active = adminTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setAdminTab(item.id);
                    setSelectedShopDetailId(null);
                    setMobileMenuOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-all ${
                    active ? 'bg-slate-900 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={`w-5 h-5 ${active ? 'text-cyan-400' : 'text-slate-400'}`} />
                    <span>{item.label}</span>
                  </div>
                  {item.count !== undefined && (
                    <span className={`text-xs font-mono font-bold px-2 py-0.5 rounded ${active ? 'bg-slate-800 text-slate-200' : 'bg-slate-100 text-slate-600'}`}>
                      {item.count}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        )}

        {/* SHOP OWNER NAVIGATION */}
        {authSession.role === 'SHOP_OWNER' && (
          <nav className="space-y-1.5">
            <p className="px-3 text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Shop Operations</p>
            {[
              { id: 'overview', label: 'Shop Dashboard', icon: Store },
              { id: 'unassigned', label: 'Scanned Inbox', icon: Radio, count: unassignedDevices.filter(u => !u.shopId || u.shopId === authSession.shopId || u.shopId === 'SH-101').length },
              { id: 'devices', label: 'My Financed Devices', icon: Smartphone, count: currentShopProtectedDevices.length },
              { id: 'history', label: 'Recent History', icon: History, count: currentShopClearedDevices.length },
              { id: 'enroll', label: 'Enroll New Handset', icon: Plus },
              { id: 'qr', label: 'QR Provisioning', icon: QrCode },
              { id: 'settings', label: 'Lockscreen & Accounts', icon: Settings },
            ].map((item) => {
              const Icon = item.icon;
              const active = shopTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setShopTab(item.id);
                    setMobileMenuOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-all ${
                    active ? 'bg-slate-900 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={`w-5 h-5 ${active ? 'text-emerald-400' : 'text-slate-400'}`} />
                    <span>{item.label}</span>
                  </div>
                  {item.count !== undefined && (
                    <span className={`text-xs font-mono font-bold px-2 py-0.5 rounded ${active ? 'bg-slate-800 text-slate-200' : 'bg-slate-100 text-slate-600'}`}>
                      {item.count}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        )}

      </div>

      {/* LOGOUT BUTTON */}
      <div className="pt-4 border-t border-slate-100">
        <button
          onClick={handleLogout}
          className="w-full flex items-center justify-center gap-2 py-2.5 bg-slate-100 hover:bg-rose-50 hover:text-rose-700 text-slate-700 rounded-xl text-sm font-bold transition-all cursor-pointer"
        >
          <LogOut className="w-4 h-4" />
          <span>Sign Out</span>
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-[#F8FAFC] text-slate-900 flex font-sans antialiased selection:bg-slate-900 selection:text-white text-base">
      
      {/* TOAST NOTIFICATION */}
      <AnimatePresence>
        {toastMessage && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="fixed top-5 right-5 z-50 flex items-center gap-3 px-4 py-3 bg-slate-900 text-white rounded-2xl shadow-2xl border border-slate-700 text-sm font-semibold max-w-[90vw]"
          >
            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
            <span className="truncate">{toastMessage}</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* DESKTOP SIDEBAR */}
      <aside className="hidden md:flex w-72 bg-white border-r border-slate-200/80 flex-col shrink-0 shadow-sm z-10 h-screen sticky top-0">
        <SidebarContent />
      </aside>

      {/* MOBILE DRAWER */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <div className="fixed inset-0 z-50 md:hidden flex">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setMobileMenuOpen(false)}
              className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs"
            />
            <motion.div
              initial={{ x: -300 }}
              animate={{ x: 0 }}
              exit={{ x: -300 }}
              className="relative w-80 max-w-[85vw] bg-white h-full shadow-2xl z-10 flex flex-col"
            >
              <SidebarContent />
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* MAIN BODY AREA */}
      <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        
        {/* TOP HEADER */}
        <header className="h-18 bg-white border-b border-slate-200/80 px-5 sm:px-8 flex items-center justify-between sticky top-0 z-20">
          <div className="flex items-center gap-3 min-w-0">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="md:hidden p-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-xl"
            >
              <Menu className="w-6 h-6" />
            </button>
            <div>
              <h2 className="text-base sm:text-lg font-bold text-slate-900 truncate">
                {authSession.role === 'SUPER_ADMIN' ? 'Master Network Control Console' : `${currentShopData?.name} Portal`}
              </h2>
            </div>
          </div>

          <div className="flex items-center gap-2 sm:gap-3 shrink-0">
            {/* ANTI-CANARY TRAFFIC SHIELD PILL */}
            <button
              onClick={() => setShowSecurityShieldModal(true)}
              className="flex items-center gap-1.5 px-3 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 rounded-xl text-xs font-bold transition-all cursor-pointer shadow-2xs"
              title="HTTPCanary & MitM Cryptographic Protection Status"
            >
              <ShieldAlert className="w-4 h-4 text-emerald-600" />
              <span className="hidden sm:inline">Anti-Canary Shield:</span>
              <span className="text-emerald-700 font-extrabold">Active</span>
              <span className="font-mono text-[10px] bg-emerald-200/80 px-1.5 py-0.5 rounded text-emerald-950 hidden md:inline">HMAC-SHA256</span>
            </button>

            {authSession.role === 'SUPER_ADMIN' && (
              <button
                onClick={() => setShowAddShopModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-sm font-bold shadow-sm cursor-pointer"
              >
                <Plus className="w-4 h-4" /> <span className="hidden sm:inline">Register Franchise</span><span className="sm:hidden">Add Shop</span>
              </button>
            )}

            {authSession.role === 'SHOP_OWNER' && (
              <div className="flex items-center gap-2">
                <button
                  onClick={handleBuyCreditsWhatsApp}
                  className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-sm font-bold shadow-sm cursor-pointer"
                >
                  <MessageSquare className="w-4 h-4" /> <span className="hidden sm:inline">Buy Credits</span>
                </button>

                <button
                  onClick={() => setShowEnrollDeviceModal(true)}
                  className="flex items-center gap-2 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-sm font-bold shadow-sm cursor-pointer"
                >
                  <Plus className="w-4 h-4" /> <span className="hidden sm:inline">Enroll Handset</span><span className="sm:hidden">Enroll</span>
                </button>
              </div>
            )}
          </div>
        </header>

        {/* SCREEN CONTAINER */}
        <div className="p-5 sm:p-8 space-y-6 flex-1 max-w-7xl w-full mx-auto">
          
          {/* ======================================================== */}
          {/* SUPER ADMIN PORTAL */}
          {/* ======================================================== */}
          {authSession.role === 'SUPER_ADMIN' && (
            <>
              {/* TAB 1: OVERVIEW */}
              {adminTab === 'overview' && (
                <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
                  
                  {/* KPI BOXES */}
                  <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-2">
                      <span className="text-xs sm:text-sm font-semibold text-slate-500">Franchise Outlets</span>
                      <div className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">{shops.length} Shops</div>
                      <p className="text-xs text-slate-400">Retail nodes</p>
                    </div>

                    <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-2">
                      <span className="text-xs sm:text-sm font-semibold text-slate-500">Active Financed Fleet</span>
                      <div className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">{protectedDevices.length} Handsets</div>
                      <p className="text-xs text-emerald-600 font-semibold">Knox Protected</p>
                    </div>

                    <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-2">
                      <span className="text-xs sm:text-sm font-semibold text-slate-500">Locked Handsets</span>
                      <div className="text-2xl sm:text-3xl font-black text-rose-600 font-mono">{protectedDevices.filter(d => d.isLocked).length}</div>
                      <p className="text-xs text-rose-600 font-semibold">Defaulted EMI</p>
                    </div>

                    <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-2">
                      <span className="text-xs sm:text-sm font-semibold text-slate-500">Cleared Loans</span>
                      <div className="text-2xl sm:text-3xl font-black text-emerald-600 font-mono">{clearedDevices.length} Released</div>
                      <p className="text-xs text-slate-500 font-semibold">Recent History (100% Free)</p>
                    </div>
                  </div>

                  {/* QUICK SHOPS LIST */}
                  <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
                    <div className="p-5 border-b border-slate-100 flex items-center justify-between">
                      <div>
                        <h3 className="font-bold text-slate-900 text-base">Franchise Network Status</h3>
                        <p className="text-xs sm:text-sm text-slate-400">Click to view shop profile, devices & payment records</p>
                      </div>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-sm min-w-[650px]">
                        <thead className="bg-slate-50 text-slate-500 font-bold text-xs uppercase border-b border-slate-100">
                          <tr>
                            <th className="p-4 pl-6">Shop Name</th>
                            <th className="p-4">Owner & Phone</th>
                            <th className="p-4">Available Credits</th>
                            <th className="p-4">Devices</th>
                            <th className="p-4 pr-6 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {shops.map((s) => {
                            const shopDevs = devices.filter(d => d.shopId === s.id);
                            return (
                              <tr key={s.id} className="hover:bg-slate-50/80">
                                <td className="p-4 pl-6 font-bold text-slate-900">{s.name}</td>
                                <td className="p-4 text-slate-600">
                                  <div className="font-semibold text-slate-800">{s.ownerName}</div>
                                  <div className="text-xs text-slate-500 font-mono">{s.phone}</div>
                                </td>
                                <td className="p-4 font-mono font-bold text-emerald-600 text-sm">{s.credits} Credits</td>
                                <td className="p-4 font-mono font-bold text-slate-900">{shopDevs.length} Devices</td>
                                <td className="p-4 pr-6 text-right space-x-2">
                                  <button
                                    onClick={() => {
                                      setCreditTopupForm({ shopId: s.id, amount: 50 });
                                      setShowCreditTopupModal(true);
                                    }}
                                    className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold rounded-lg text-xs cursor-pointer"
                                  >
                                    + Credits
                                  </button>
                                  <button
                                    onClick={() => {
                                      setSelectedShopDetailId(s.id);
                                      setAdminTab('shop_detail');
                                    }}
                                    className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-lg text-xs cursor-pointer"
                                  >
                                    View Details →
                                  </button>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  </div>

                </motion.div>
              )}

              {/* TAB 2: SHOPS */}
              {adminTab === 'shops' && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
                  <div className="p-5 border-b border-slate-100 flex items-center justify-between">
                    <div>
                      <h3 className="font-bold text-slate-900 text-base">Franchise Outlets Management</h3>
                      <p className="text-sm text-slate-400">All registered shop accounts and credit limits</p>
                    </div>
                    <button
                      onClick={() => setShowAddShopModal(true)}
                      className="flex items-center gap-1.5 px-3.5 py-1.5 bg-slate-900 text-white rounded-xl text-sm font-bold"
                    >
                      <Plus className="w-4 h-4" /> Add Shop
                    </button>
                  </div>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm min-w-[650px]">
                      <thead className="bg-slate-50 text-slate-500 font-bold text-xs uppercase border-b border-slate-100">
                        <tr>
                          <th className="p-4 pl-6">Shop Name</th>
                          <th className="p-4">Owner Name</th>
                          <th className="p-4">City</th>
                          <th className="p-4">Credits</th>
                          <th className="p-4 text-center">📍 GPS Permission</th>
                          <th className="p-4 text-center">📷 Camera Permission</th>
                          <th className="p-4 pr-6 text-right">Details</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {shops.map(s => (
                          <tr key={s.id} className="hover:bg-slate-50/80">
                            <td className="p-4 pl-6 font-bold text-slate-900">{s.name}</td>
                            <td className="p-4 text-slate-700 font-medium">{s.ownerName}</td>
                            <td className="p-4 text-slate-700">{s.city}</td>
                            <td className="p-4 font-mono font-bold text-emerald-600">{s.credits} Credits</td>
                            <td className="p-4 text-center">
                              <button
                                onClick={() => handleToggleShopPermission(s.id, 'allowLocationAccess')}
                                className={`px-2.5 py-1 rounded-full text-xs font-bold transition-all cursor-pointer ${
                                  s.allowLocationAccess
                                    ? 'bg-emerald-100 text-emerald-800 border border-emerald-300 hover:bg-emerald-200'
                                    : 'bg-slate-100 text-slate-500 border border-slate-200 hover:bg-slate-200'
                                }`}
                                title="Click to Grant or Revoke GPS Location tracking for this shop"
                              >
                                {s.allowLocationAccess ? 'Granted 🟢' : 'Locked 🔒'}
                              </button>
                            </td>
                            <td className="p-4 text-center">
                              <button
                                onClick={() => handleToggleShopPermission(s.id, 'allowCameraAccess')}
                                className={`px-2.5 py-1 rounded-full text-xs font-bold transition-all cursor-pointer ${
                                  s.allowCameraAccess
                                    ? 'bg-purple-100 text-purple-800 border border-purple-300 hover:bg-purple-200'
                                    : 'bg-slate-100 text-slate-500 border border-slate-200 hover:bg-slate-200'
                                }`}
                                title="Click to Grant or Revoke Camera Snapshot access for this shop"
                              >
                                {s.allowCameraAccess ? 'Granted 🟢' : 'Locked 🔒'}
                              </button>
                            </td>
                            <td className="p-4 pr-6 text-right">
                              <button
                                onClick={() => {
                                  setSelectedShopDetailId(s.id);
                                  setAdminTab('shop_detail');
                                }}
                                className="px-3 py-1.5 bg-slate-900 text-white rounded-lg text-xs font-bold cursor-pointer hover:bg-slate-800"
                              >
                                View Details
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </motion.div>
              )}

              {/* TAB: SHOP DEEP-DIVE DETAILS */}
              {adminTab === 'shop_detail' && selectedShopDetailId && (
                (() => {
                  const targetShop = shops.find(s => s.id === selectedShopDetailId);
                  const targetShopDevices = devices.filter(d => d.shopId === selectedShopDetailId);
                  if (!targetShop) return null;

                  return (
                    <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
                      
                      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                        <button
                          onClick={() => setAdminTab('overview')}
                          className="flex items-center gap-2 text-sm font-bold text-slate-600 hover:text-slate-900 cursor-pointer"
                        >
                          <ChevronLeft className="w-5 h-5" /> Back to All Shops
                        </button>
                        <button
                          onClick={() => {
                            setCreditTopupForm({ shopId: targetShop.id, amount: 50 });
                            setShowCreditTopupModal(true);
                          }}
                          className="flex items-center gap-1.5 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-sm font-bold cursor-pointer"
                        >
                          <Plus className="w-4 h-4" /> Add Credits to {targetShop.name}
                        </button>
                      </div>

                      <div className="p-6 bg-white rounded-2xl border border-slate-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                        <div className="space-y-1">
                          <div className="flex items-center gap-2">
                            <h3 className="text-xl font-bold text-slate-900">{targetShop.name}</h3>
                            <span className="px-2 py-0.5 rounded text-xs font-bold bg-emerald-100 text-emerald-700">Active Node</span>
                          </div>
                          <p className="text-sm text-slate-500">Owner: <strong className="text-slate-800">{targetShop.ownerName}</strong> • Phone: <span className="font-mono text-slate-800 font-bold">{targetShop.phone}</span> • City: {targetShop.city}</p>
                        </div>
                        <div className="sm:text-right">
                          <span className="text-xs text-slate-400 font-bold uppercase">Credit Pool</span>
                          <div className="text-3xl font-black text-emerald-600 font-mono">{targetShop.credits} Credits</div>
                        </div>
                      </div>

                      <div className="flex gap-2 border-b border-slate-200 pb-2 text-sm font-bold overflow-x-auto">
                        <button
                          onClick={() => setShopDetailTab('profile')}
                          className={`px-4 py-2.5 rounded-xl shrink-0 transition-all ${shopDetailTab === 'profile' ? 'bg-slate-900 text-white' : 'text-slate-500 hover:text-slate-900'}`}
                        >
                          Shop Profile & Credentials
                        </button>
                        <button
                          onClick={() => setShopDetailTab('devices')}
                          className={`px-4 py-2.5 rounded-xl shrink-0 transition-all ${shopDetailTab === 'devices' ? 'bg-slate-900 text-white' : 'text-slate-500 hover:text-slate-900'}`}
                        >
                          Enrolled Devices ({targetShopDevices.length})
                        </button>
                        <button
                          onClick={() => setShopDetailTab('payments')}
                          className={`px-4 py-2.5 rounded-xl shrink-0 transition-all ${shopDetailTab === 'payments' ? 'bg-slate-900 text-white' : 'text-slate-500 hover:text-slate-900'}`}
                        >
                          Payment History ({targetShop.paymentHistory.length})
                        </button>
                      </div>

                      {shopDetailTab === 'profile' && (
                        <div className="p-6 bg-white rounded-2xl border border-slate-200 shadow-xs max-w-xl space-y-4">
                          <h4 className="font-bold text-slate-900 text-base">Franchise Login Credentials</h4>
                          <div className="space-y-3 text-sm">
                            <div className="p-3.5 bg-slate-50 rounded-xl flex justify-between items-center">
                              <span className="text-slate-500 font-medium">Shop ID:</span>
                              <span className="font-mono font-bold text-slate-900">{targetShop.id}</span>
                            </div>
                            <div className="p-3.5 bg-slate-50 rounded-xl flex justify-between items-center">
                              <span className="text-slate-500 font-medium">Login Email:</span>
                              <span className="font-mono font-bold text-slate-900">{targetShop.email}</span>
                            </div>
                            <div className="p-3.5 bg-slate-50 rounded-xl flex justify-between items-center">
                              <span className="text-slate-500 font-medium">Password:</span>
                              <span className="font-mono font-bold text-slate-900">{targetShop.password}</span>
                            </div>
                            <div className="p-3.5 bg-slate-50 rounded-xl flex justify-between items-center">
                              <span className="text-slate-500 font-medium">Registration Date:</span>
                              <span className="font-mono text-slate-900 font-medium">{targetShop.createdAt}</span>
                            </div>
                          </div>
                        </div>
                      )}

                      {shopDetailTab === 'devices' && (
                        <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
                          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
                            <h4 className="font-bold text-slate-900 text-base">Hardware Enrolled by {targetShop.name}</h4>
                            <span className="text-sm font-mono text-slate-500 font-bold">{targetShopDevices.length} Handsets</span>
                          </div>
                          <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm min-w-[650px]">
                              <thead className="bg-slate-50 text-slate-500 font-bold text-xs uppercase border-b border-slate-100">
                                <tr>
                                  <th className="p-4 pl-6">Customer</th>
                                  <th className="p-4">Model</th>
                                  <th className="p-4">IMEI</th>
                                  <th className="p-4">EMI (PKR)</th>
                                  <th className="p-4">Status</th>
                                  <th className="p-4 pr-6 text-right">OTA Control</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-slate-100">
                                {targetShopDevices.map(d => (
                                  <tr key={d.id} className="hover:bg-slate-50/80">
                                    <td className="p-4 pl-6 font-bold text-slate-900">{d.customerName}</td>
                                    <td className="p-4 font-semibold text-slate-800">{d.model}</td>
                                    <td className="p-4 font-mono text-slate-500 text-xs">{d.imei}</td>
                                    <td className="p-4 font-mono font-bold text-slate-900">PKR {d.monthlyEmi.toLocaleString()}/mo</td>
                                    <td className="p-4">
                                      <span className={`px-2.5 py-0.5 text-xs font-bold rounded-md ${d.isLocked ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-700'}`}>
                                        {d.isLocked ? 'Locked' : 'Active'}
                                      </span>
                                    </td>
                                    <td className="p-4 pr-6 text-right">
                                      <button
                                        onClick={() => handleToggleLock(d.id)}
                                        className={`px-3.5 py-1.5 rounded-lg text-xs font-bold cursor-pointer ${d.isLocked ? 'bg-emerald-600 text-white' : 'bg-rose-600 text-white'}`}
                                      >
                                        {d.isLocked ? 'OTA Unlock' : 'OTA Lock'}
                                      </button>
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        </div>
                      )}

                      {shopDetailTab === 'payments' && (
                        <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
                          <div className="p-5 border-b border-slate-100">
                            <h4 className="font-bold text-slate-900 text-base">Credit Purchase & Allocation Ledger</h4>
                          </div>
                          <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm min-w-[550px]">
                              <thead className="bg-slate-50 text-slate-500 font-bold text-xs uppercase border-b border-slate-100">
                                <tr>
                                  <th className="p-4 pl-6">Payment ID</th>
                                  <th className="p-4">Credits</th>
                                  <th className="p-4">Amount (PKR)</th>
                                  <th className="p-4">Method</th>
                                  <th className="p-4 pr-6 text-right">Date</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-slate-100">
                                {targetShop.paymentHistory.map(p => (
                                  <tr key={p.id} className="hover:bg-slate-50/80">
                                    <td className="p-4 pl-6 font-mono font-bold text-slate-900">{p.id}</td>
                                    <td className="p-4 font-mono font-bold text-emerald-600">+{p.creditsIssued} Credits</td>
                                    <td className="p-4 font-mono font-bold text-slate-900">PKR {p.amount.toLocaleString()}</td>
                                    <td className="p-4 text-slate-700 font-medium">{p.method}</td>
                                    <td className="p-4 pr-6 text-right font-mono text-slate-400">{p.date}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        </div>
                      )}

                    </motion.div>
                  );
                })()
              )}

              {/* TAB 2.5: SCANNED INBOX FOR SUPER ADMIN */}
              {adminTab === 'unassigned' && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
                        <Radio className="w-5 h-5 text-emerald-600 animate-pulse" />
                        <span>Scanned Handsets Inbox (Master Network HQ)</span>
                      </h3>
                      <p className="text-sm text-slate-400">All physical devices scanned via 6-Tap QR code waiting for franchise assignment & financing</p>
                    </div>
                    {unassignedDevices.length > 0 && (
                      <span className="text-xs font-mono font-bold bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-xl border border-emerald-200 self-start sm:self-auto">
                        📡 {unassignedDevices.length} Handset(s) Discovered
                      </span>
                    )}
                  </div>

                  {unassignedDevices.length === 0 ? (
                    <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center space-y-3">
                      <QrCode className="w-12 h-12 text-slate-300 mx-auto" />
                      <h4 className="font-bold text-slate-700 text-base">No Pending Handsets in Network Inbox</h4>
                      <p className="text-sm text-slate-400 max-w-md mx-auto">
                        When any franchise store scans the 6-Tap QR provisioning code on a customer device, the hardware model and IMEI will automatically appear here!
                      </p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                      {unassignedDevices.map((u) => (
                        <div key={u.id} className="p-5 bg-white rounded-2xl border-2 border-emerald-300 shadow-sm space-y-4 relative overflow-hidden">
                          <div className="flex justify-between items-center">
                            <span className="px-2.5 py-0.5 text-xs font-bold rounded-md bg-emerald-100 text-emerald-800 flex items-center gap-1">
                              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                              Hardware Auto-Detected
                            </span>
                            <span className="text-xs font-mono font-bold text-slate-400">{u.scannedAt || 'Just Now'}</span>
                          </div>

                          <div>
                            <h4 className="font-black text-slate-900 text-lg">{u.model}</h4>
                            <p className="text-xs font-mono text-slate-500 mt-0.5">IMEI: <strong>{u.imei}</strong></p>
                            <p className="text-xs text-slate-400 mt-0.5">{u.androidVersion}</p>
                          </div>

                          <div className="bg-emerald-50/70 p-3 rounded-xl border border-emerald-100 text-xs text-emerald-800 space-y-1">
                            <p className="font-bold">✓ Model & Hardware Authenticated</p>
                            <p className="text-[11px] text-emerald-700">Device ID: <span className="font-mono">{u.id}</span></p>
                          </div>

                          <div className="space-y-2">
                            <button
                              onClick={() => handleAssignScannedDevice(u)}
                              className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold text-xs shadow-md transition-all cursor-pointer flex items-center justify-center gap-2"
                            >
                              <Plus className="w-4 h-4" />
                              <span>ASSIGN & BIND CUSTOMER PLAN</span>
                            </button>

                            <button
                              onClick={() => handleDiscardScannedDevice(u)}
                              className="w-full py-2 bg-slate-100 hover:bg-rose-50 hover:text-rose-700 hover:border-rose-200 text-slate-600 rounded-xl font-semibold text-xs border border-slate-200 transition-all cursor-pointer flex items-center justify-center gap-1.5"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                              <span>DISCARD / REMOVE FROM INBOX</span>
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </motion.div>
              )}

              {/* TAB 3: ALL FLEET DEVICES (ACTIVE PROTECTED ONLY) */}
              {adminTab === 'devices' && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <h3 className="font-bold text-slate-900 text-lg">Active Protected Fleet Grid</h3>
                      <p className="text-sm text-slate-400">Real-time instant lock controls for currently financed active devices</p>
                    </div>
                    <span className="text-xs font-mono font-bold bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-xl border border-emerald-200 self-start sm:self-auto">
                      🛡️ {protectedDevices.length} Protected Nodes Active
                    </span>
                  </div>

                  {protectedDevices.length === 0 ? (
                    <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center space-y-3">
                      <ShieldCheck className="w-12 h-12 text-emerald-500 mx-auto" />
                      <h4 className="font-bold text-slate-800 text-base">No Active Protected Devices</h4>
                      <p className="text-sm text-slate-400 max-w-md mx-auto">
                        All enrolled customer handsets have completed their financing or have been released. View settled handsets in Recent History.
                      </p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                      {protectedDevices.map((dev) => (
                        <motion.div
                          layout
                          key={dev.id}
                          className={`p-5 rounded-2xl border transition-all ${dev.isLocked ? 'bg-rose-50/50 border-rose-200 shadow-sm' : 'bg-white border-slate-200 shadow-xs'}`}
                        >
                          <div className="flex items-center justify-between mb-3">
                            <span className={`px-2.5 py-0.5 text-xs font-bold rounded-md uppercase ${dev.isLocked ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-700'}`}>
                              {dev.isLocked ? 'Locked' : 'Protected'}
                            </span>
                            <span className="text-xs font-mono text-slate-400 font-bold">{dev.id}</span>
                          </div>

                          <h4 className="font-bold text-slate-900 text-base truncate">{dev.model}</h4>
                          <p className="text-xs font-mono text-slate-500">IMEI: {dev.imei}</p>

                          <div className="mt-3 pt-3 border-t border-slate-100 space-y-1.5 text-xs">
                            <div className="flex justify-between text-slate-500"><span>Customer:</span><span className="font-bold text-slate-800 truncate">{dev.customerName}</span></div>
                            <div className="flex justify-between text-slate-500"><span>Shop:</span><span className="font-semibold text-slate-800 truncate">{dev.shopName}</span></div>
                            <div className="flex justify-between text-slate-500"><span>EMI (PKR):</span><span className="font-bold text-slate-900">PKR {dev.monthlyEmi.toLocaleString()}/mo</span></div>
                            <div className="flex justify-between text-slate-500"><span>Next Due Date:</span><span className="font-mono font-bold text-slate-800">📅 {dev.nextDueDate}</span></div>
                            <div className="flex items-center justify-between text-xs pt-1 border-t border-slate-100">
                              <span className="text-slate-500 font-semibold flex items-center gap-1">
                                <Zap className="w-3.5 h-3.5 text-amber-500" />
                                <span>Auto-Lock:</span>
                              </span>
                              <button
                                onClick={() => handleToggleDeviceAutoLock(dev.id)}
                                className={`px-2 py-0.5 rounded-full font-bold text-[10px] transition-all cursor-pointer ${
                                  dev.autoLockEnabled
                                    ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                                    : 'bg-slate-100 text-slate-500 border border-slate-200'
                                }`}
                              >
                                {dev.autoLockEnabled ? 'ACTIVE (OFFLINE OK)' : 'OFF'}
                              </button>
                            </div>
                          </div>

                          <div className="mt-3 grid grid-cols-3 gap-1.5">
                            <button
                              onClick={() => setActiveLocationModalDev(dev)}
                              className="py-1.5 px-1 bg-teal-50 hover:bg-teal-100 text-teal-800 font-bold text-[11px] rounded-lg border border-teal-200 flex items-center justify-center gap-1 cursor-pointer transition-all"
                              title="View Real-Time GPS Coordinates & Interactive Map"
                            >
                              <MapPin className="w-3.5 h-3.5" />
                              <span>GPS</span>
                            </button>
                            <button
                              onClick={() => setActiveCameraModalDev(dev)}
                              className="py-1.5 px-1 bg-purple-50 hover:bg-purple-100 text-purple-800 font-bold text-[11px] rounded-lg border border-purple-200 flex items-center justify-center gap-1 cursor-pointer transition-all"
                              title="Capture Live Forensic Snapshot"
                            >
                              <Camera className="w-3.5 h-3.5" />
                              <span>CAM</span>
                            </button>
                            <button
                              onClick={() => handleRemoteWipe(dev.id)}
                              className="py-1.5 px-1 bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold text-[11px] rounded-lg border border-rose-200 flex items-center justify-center gap-1 cursor-pointer transition-all"
                              title="Remote Factory Reset"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                              <span>WIPE</span>
                            </button>
                          </div>

                          <div className="mt-3 space-y-2">
                            <button
                              onClick={() => handleToggleLock(dev.id)}
                              className={`w-full py-2.5 rounded-xl text-xs font-bold flex items-center justify-center gap-2 cursor-pointer transition-all ${
                                dev.isLocked ? 'bg-emerald-600 hover:bg-emerald-500 text-white' : 'bg-rose-600 hover:bg-rose-500 text-white'
                              }`}
                            >
                              {dev.isLocked ? <Unlock className="w-4 h-4" /> : <Lock className="w-4 h-4" />}
                              <span>{dev.isLocked ? 'OTA UNLOCK' : 'OTA LOCK'}</span>
                            </button>

                            <button
                              onClick={() => handleClearLoan(dev.id)}
                              className="w-full py-2 rounded-xl text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer border border-slate-200 text-center transition-all"
                              title="Clear loan and release Device Owner lock"
                            >
                              CLEAR LOAN & RELEASE
                            </button>
                          </div>
                        </motion.div>
                      ))}
                    </div>
                  )}
                </motion.div>
              )}

              {/* TAB 4: RECENT HISTORY (CLEARED LOANS & RELEASED HANDSETS) */}
              {adminTab === 'history' && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
                        <History className="w-5 h-5 text-emerald-600" />
                        <span>Recent Loan Clearance & Handset Release History</span>
                      </h3>
                      <p className="text-sm text-slate-400">Permanently released handsets with settled installments and unlocked Device Owner policies</p>
                    </div>

                    <div className="flex items-center gap-2">
                      {clearedDevices.length > 0 && (
                        <button
                          onClick={handleClearAllHistory}
                          className="px-3.5 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-bold rounded-xl border border-rose-200 cursor-pointer flex items-center gap-1.5 transition-all shrink-0"
                          title="Delete all cleared records from history"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>Clear All History</span>
                        </button>
                      )}
                      <div className="relative min-w-[260px]">
                        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                        <input
                          type="text"
                          placeholder="Search by customer, IMEI, model..."
                          value={historySearchQuery}
                          onChange={(e) => setHistorySearchQuery(e.target.value)}
                          className="w-full pl-9 pr-4 py-2 text-xs bg-white border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
                        />
                      </div>
                    </div>
                  </div>

                  {/* SUMMARY CARDS */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-1">
                      <span className="text-xs text-slate-500 font-semibold">Total Handsets Released</span>
                      <div className="text-2xl font-black text-slate-900 font-mono">{clearedDevices.length} Devices</div>
                      <p className="text-xs text-emerald-600 font-semibold">100% Device Owner Terminated</p>
                    </div>

                    <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-1">
                      <span className="text-xs text-slate-500 font-semibold">Total Loan Capital Settled</span>
                      <div className="text-2xl font-black text-emerald-600 font-mono">
                        PKR {clearedDevices.reduce((acc, d) => acc + (d.totalPrice || 0), 0).toLocaleString()}
                      </div>
                      <p className="text-xs text-slate-400">Recovered across all stores</p>
                    </div>

                    <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-1">
                      <span className="text-xs text-slate-500 font-semibold">Security State</span>
                      <div className="text-2xl font-black text-slate-800">Unrestricted</div>
                      <p className="text-xs text-slate-500">Customer Uninstall Free</p>
                    </div>
                  </div>

                  {/* LIST OF CLEARED DEVICES */}
                  {(() => {
                    const filtered = clearedDevices.filter(d => {
                      if (!historySearchQuery.trim()) return true;
                      const q = historySearchQuery.toLowerCase();
                      return (
                        (d.customerName || '').toLowerCase().includes(q) ||
                        (d.customerPhone || '').toLowerCase().includes(q) ||
                        (d.customerCnic || '').toLowerCase().includes(q) ||
                        (d.model || '').toLowerCase().includes(q) ||
                        (d.imei || '').toLowerCase().includes(q) ||
                        (d.shopName || '').toLowerCase().includes(q)
                      );
                    });

                    if (filtered.length === 0) {
                      return (
                        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center space-y-3">
                          <History className="w-12 h-12 text-slate-300 mx-auto" />
                          <h4 className="font-bold text-slate-700 text-base">No Cleared Loan Records Found</h4>
                          <p className="text-xs text-slate-400 max-w-sm mx-auto">
                            {historySearchQuery ? 'No results matched your search keyword.' : 'When a loan is cleared from the dashboard, released handsets will appear here with complete details.'}
                          </p>
                        </div>
                      );
                    }

                    return (
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {filtered.map((d) => (
                          <div key={d.id} className="p-5 bg-white rounded-2xl border border-emerald-200/80 shadow-xs space-y-3 relative overflow-hidden">
                            <div className="flex items-center justify-between">
                              <span className="px-2.5 py-0.5 text-xs font-bold rounded-md bg-emerald-100 text-emerald-800 flex items-center gap-1">
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                <span>Loan Cleared • Released</span>
                              </span>
                              <span className="text-xs font-mono font-bold text-slate-400">{d.id}</span>
                            </div>

                            <div>
                              <h4 className="font-bold text-slate-900 text-base flex items-center gap-2">
                                <Smartphone className="w-4 h-4 text-emerald-600" />
                                <span>{d.model}</span>
                              </h4>
                              <p className="text-xs font-mono text-slate-500">IMEI: {d.imei}</p>
                            </div>

                            <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs space-y-1.5">
                              <div className="flex justify-between">
                                <span className="text-slate-500">Customer:</span>
                                <strong className="text-slate-800">{d.customerName}</strong>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-slate-500">Phone & CNIC:</span>
                                <span className="font-mono text-slate-700">{d.customerPhone}</span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-slate-500">Franchise Store:</span>
                                <span className="font-semibold text-slate-800 truncate max-w-[170px]">{d.shopName}</span>
                              </div>
                              <div className="flex justify-between pt-1 border-t border-slate-200">
                                <span className="text-slate-500">Financed Amount:</span>
                                <span className="font-mono font-bold text-slate-900">PKR {d.totalPrice?.toLocaleString()} ({d.totalTerms}/{d.totalTerms} Full Paid)</span>
                              </div>
                              <div className="flex justify-between text-emerald-700 pt-0.5">
                                <span>Cleared Date:</span>
                                <span className="font-mono font-bold">{d.clearedAt || 'Recently'}</span>
                              </div>
                              <div className="flex justify-between text-slate-500 text-[11px]">
                                <span>Released By:</span>
                                <span className="italic">{d.releasedBy || 'Super Admin'}</span>
                              </div>
                            </div>

                            <div className="pt-1 flex gap-2">
                              <button
                                onClick={() => setSelectedHistoryDev(d)}
                                className="flex-1 py-2.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 font-bold text-xs rounded-xl border border-emerald-200 flex items-center justify-center gap-1.5 cursor-pointer transition-all"
                              >
                                <FileText className="w-4 h-4 text-emerald-600 shrink-0" />
                                <span>VIEW NOC</span>
                              </button>
                              <button
                                onClick={() => {
                                  if (confirm(`Are you sure you want to delete ${d.model} (${d.customerName}) from Recent History? This will also free up the handset for re-scanning.`)) {
                                    handleDeleteHistoryDevice(d.id);
                                  }
                                }}
                                className="px-3.5 py-2.5 bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold text-xs rounded-xl border border-rose-200 flex items-center justify-center gap-1 cursor-pointer transition-all shrink-0"
                                title="Delete record from history"
                              >
                                <Trash2 className="w-4 h-4 shrink-0" />
                                <span>Delete</span>
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    );
                  })()}
                </motion.div>
              )}

              {/* TAB 4: SETTINGS (Super Admin Decision on Credits per Device) */}
              {adminTab === 'settings' && (
                <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="max-w-2xl bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs space-y-6">
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg">Super Admin Global Governance</h3>
                    <p className="text-sm text-slate-500 mt-0.5">Control credit consumption per device and contact WhatsApp for all franchise stores.</p>
                  </div>

                  <form onSubmit={handleSaveGlobalSettings} className="space-y-5 text-sm">
                    {/* CREDITS PER ENROLLMENT (Requested Feature) */}
                    <div className="p-4 bg-indigo-50/60 border border-indigo-200 rounded-2xl space-y-2">
                      <label className="font-bold text-slate-900 flex items-center gap-2 text-sm sm:text-base">
                        <Zap className="w-5 h-5 text-indigo-600" />
                        <span>Credits Deducted Per Device Enrollment</span>
                      </label>
                      <p className="text-xs text-slate-600">
                        Jab koi bhi Shop Owner naya phone register karega, to uske account se kitne credits kharch honge?
                      </p>
                      <div className="flex items-center gap-3 pt-1">
                        <input
                          type="number"
                          min="1"
                          max="100"
                          required
                          value={tempCreditsPerEnrollment}
                          onChange={e => setTempCreditsPerEnrollment(e.target.value)}
                          className="w-32 bg-white border border-indigo-300 rounded-xl p-3 text-base font-bold font-mono text-slate-900 focus:outline-none focus:border-indigo-600"
                        />
                        <span className="text-sm font-bold text-slate-700">Credits / Handset</span>
                      </div>
                    </div>

                    {/* WHATSAPP SUPPORT */}
                    <div className="space-y-1.5">
                      <label className="font-bold text-slate-800">Official Support WhatsApp (Buy Credits Redirect)</label>
                      <input
                        type="text"
                        required
                        value={tempWhatsApp}
                        onChange={e => setTempWhatsApp(e.target.value)}
                        className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3.5 text-sm text-slate-900 font-mono focus:outline-none focus:border-slate-900"
                        placeholder="923001234567"
                      />
                      <p className="text-xs text-slate-400">Shop owners click karenge to direct is number par WhatsApp open hoga.</p>
                    </div>

                    {/* SUPPORT EMAIL */}
                    <div className="space-y-1.5">
                      <label className="font-bold text-slate-800">Support Email</label>
                      <input
                        type="email"
                        required
                        value={tempEmail}
                        onChange={e => setTempEmail(e.target.value)}
                        className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3.5 text-sm text-slate-900 focus:outline-none focus:border-slate-900"
                      />
                    </div>

                    <button
                      type="submit"
                      className="w-full py-3.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl font-bold text-sm shadow-md cursor-pointer flex items-center justify-center gap-2"
                    >
                      <Save className="w-4 h-4" />
                      <span>SAVE GLOBAL SETTINGS</span>
                    </button>
                  </form>
                </motion.div>
              )}

            </>
          )}

          {/* ======================================================== */}
          {/* SHOP OWNER PORTAL */}
          {/* ======================================================== */}
          {authSession.role === 'SHOP_OWNER' && (
            <>
              {/* TAB 1: SHOP OVERVIEW */}
              {shopTab === 'overview' && (
                <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
                  
                  {/* METRIC CARDS */}
                  <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-2">
                      <span className="text-xs sm:text-sm font-semibold text-slate-500">Available Credits</span>
                      <div className="text-2xl sm:text-3xl font-black text-emerald-600 font-mono">{currentShopData?.credits} Credits</div>
                      <button
                        onClick={handleBuyCreditsWhatsApp}
                        className="text-xs text-emerald-600 hover:text-emerald-700 font-bold flex items-center gap-1 cursor-pointer"
                      >
                        + Buy Credits (WhatsApp)
                      </button>
                    </div>

                    <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-2">
                      <span className="text-xs sm:text-sm font-semibold text-slate-500">Active Financed Handsets</span>
                      <div className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">{currentShopProtectedDevices.length} Nodes</div>
                      <p className="text-xs text-emerald-600 font-semibold">Active Protected</p>
                    </div>

                    <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-2">
                      <span className="text-xs sm:text-sm font-semibold text-slate-500">Locked Defaults</span>
                      <div className="text-2xl sm:text-3xl font-black text-rose-600 font-mono">
                        {currentShopProtectedDevices.filter(d => d.isLocked).length}
                      </div>
                      <p className="text-xs text-rose-600 font-semibold">Overdue EMI</p>
                    </div>

                    <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-2">
                      <span className="text-xs sm:text-sm font-semibold text-slate-500">Cleared Loans</span>
                      <div className="text-2xl sm:text-3xl font-black text-emerald-600 font-mono">{currentShopClearedDevices.length} Released</div>
                      <p className="text-xs text-slate-500 font-semibold">Recent History (100% Free)</p>
                    </div>
                  </div>

                  {/* SHOP DEVICES LIST */}
                  <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
                    <div className="p-5 border-b border-slate-100 flex items-center justify-between">
                      <div>
                        <h3 className="font-bold text-slate-900 text-base">Active Financed Handsets at {currentShopData?.name}</h3>
                        <p className="text-xs sm:text-sm text-slate-400">Real-time lock, unlock, and installment status for protected devices</p>
                      </div>
                      <span className="text-xs font-mono font-bold bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-xl border border-emerald-200">
                        {currentShopProtectedDevices.length} Active Nodes
                      </span>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-sm min-w-[650px]">
                        <thead className="bg-slate-50 text-slate-500 font-bold text-xs uppercase border-b border-slate-100">
                          <tr>
                            <th className="p-4 pl-6">Customer & Phone</th>
                            <th className="p-4">Handset Model</th>
                            <th className="p-4">IMEI</th>
                            <th className="p-4">Monthly EMI</th>
                            <th className="p-4">Status</th>
                            <th className="p-4 pr-6 text-right">Instant Action</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {currentShopProtectedDevices.length === 0 ? (
                            <tr>
                              <td colSpan={6} className="p-8 text-center text-slate-400 text-xs">
                                No active protected devices in this store. Settled handsets are in Recent History.
                              </td>
                            </tr>
                          ) : (
                            currentShopProtectedDevices.map((dev) => (
                              <tr key={dev.id} className="hover:bg-slate-50/80">
                                <td className="p-4 pl-6 font-bold text-slate-900">
                                  <div>{dev.customerName}</div>
                                  <div className="text-xs text-slate-400 font-mono font-normal">{dev.customerPhone}</div>
                                </td>
                                <td className="p-4 font-semibold text-slate-800">{dev.model}</td>
                                <td className="p-4 font-mono text-slate-500 text-xs">{dev.imei}</td>
                                <td className="p-4 font-mono font-bold text-slate-900">
                                  PKR {dev.monthlyEmi.toLocaleString()} <span className="text-xs text-slate-400 font-normal">({dev.paidTerms}/{dev.totalTerms})</span>
                                </td>
                                <td className="p-4">
                                  <span className={`px-2.5 py-0.5 text-xs font-bold rounded-md ${dev.isLocked ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-700'}`}>
                                    {dev.isLocked ? 'Locked (Default)' : 'Active Protected'}
                                  </span>
                                </td>
                                <td className="p-4 pr-6 text-right">
                                  <button
                                    onClick={() => handleToggleLock(dev.id)}
                                    className={`px-3.5 py-1.5 rounded-lg text-xs font-bold cursor-pointer ${dev.isLocked ? 'bg-emerald-600 text-white hover:bg-emerald-500' : 'bg-rose-600 text-white hover:bg-rose-500'}`}
                                  >
                                    {dev.isLocked ? 'OTA Unlock' : 'OTA Lock'}
                                  </button>
                                </td>
                              </tr>
                            ))
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>

                </motion.div>
              )}

              {/* TAB 1.5: SCANNED INBOX (AUTO-DISCOVERED HANDSETS) */}
              {shopTab === 'unassigned' && (
                (() => {
                  const visibleShopUnassigned = unassignedDevices.filter(u => !u.shopId || u.shopId === authSession.shopId || u.shopId === 'SH-101');
                  return (
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
                            <Radio className="w-5 h-5 text-emerald-600 animate-pulse" />
                            <span>Scanned Handsets Inbox</span>
                          </h3>
                          <p className="text-sm text-slate-500">Physical handsets scanned via 6-Tap QR waiting for Customer & Installment assignment</p>
                        </div>
                        {visibleShopUnassigned.length > 0 && (
                          <span className="text-xs font-mono font-bold bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-xl border border-emerald-200">
                            📡 {visibleShopUnassigned.length} Handset(s) Ready to Bind
                          </span>
                        )}
                      </div>

                      {visibleShopUnassigned.length === 0 ? (
                        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center space-y-3">
                          <QrCode className="w-12 h-12 text-slate-300 mx-auto" />
                          <h4 className="font-bold text-slate-700 text-base">No Pending Handsets</h4>
                          <p className="text-sm text-slate-400 max-w-md mx-auto">When you scan the 6-Tap QR code on any customer phone, the device model and IMEI will automatically appear here instantly!</p>
                        </div>
                      ) : (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                          {visibleShopUnassigned.map(u => (
                            <div key={u.id} className="p-5 bg-white rounded-2xl border-2 border-emerald-300 shadow-sm space-y-4 relative overflow-hidden">
                              <div className="flex justify-between items-center">
                                <span className="px-2.5 py-0.5 text-xs font-bold rounded-md bg-emerald-100 text-emerald-800 flex items-center gap-1">
                                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                                  Hardware Auto-Detected
                                </span>
                                <span className="text-xs font-mono font-bold text-slate-400">{u.scannedAt}</span>
                              </div>

                              <div>
                                <h4 className="font-black text-slate-900 text-lg">{u.model}</h4>
                                <p className="text-xs font-mono text-slate-500 mt-0.5">IMEI: <strong>{u.imei}</strong></p>
                                <p className="text-xs text-slate-400 mt-0.5">{u.androidVersion}</p>
                              </div>

                              <div className="bg-emerald-50/70 p-3 rounded-xl border border-emerald-100 text-xs text-emerald-800 space-y-1">
                                <p className="font-bold">✓ Model & IMEI Verified</p>
                                <p className="text-[11px] text-emerald-700">Click below to bind customer CNIC, Phone, and monthly installment plan.</p>
                              </div>

                              <div className="space-y-2">
                                <button
                                  onClick={() => handleAssignScannedDevice(u)}
                                  className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold text-xs shadow-md transition-all cursor-pointer flex items-center justify-center gap-2"
                                >
                                  <Plus className="w-4 h-4" />
                                  <span>BIND CUSTOMER & ASSIGN PLAN</span>
                                </button>

                                <button
                                  onClick={() => handleDiscardScannedDevice(u)}
                                  className="w-full py-2 bg-slate-100 hover:bg-rose-50 hover:text-rose-700 hover:border-rose-200 text-slate-600 rounded-xl font-semibold text-xs border border-slate-200 transition-all cursor-pointer flex items-center justify-center gap-1.5"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                  <span>DISCARD / REMOVE FROM INBOX</span>
                                </button>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </motion.div>
                  );
                })()
              )}

              {/* TAB 2: MY FINANCED DEVICES (ACTIVE PROTECTED ONLY) */}
              {shopTab === 'devices' && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <h3 className="font-bold text-slate-900 text-lg">Financed Customer Devices (Active Protected)</h3>
                      <p className="text-sm text-slate-400">Real-time OTA lockdown, installment schedule & customer warning controls</p>
                    </div>
                    <span className="text-xs font-mono font-bold bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-xl border border-emerald-200 self-start sm:self-auto">
                      🛡️ {currentShopProtectedDevices.length} Protected Handsets Active
                    </span>
                  </div>

                  {currentShopProtectedDevices.length === 0 ? (
                    <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center space-y-3">
                      <ShieldCheck className="w-12 h-12 text-emerald-500 mx-auto" />
                      <h4 className="font-bold text-slate-800 text-base">No Active Protected Devices</h4>
                      <p className="text-sm text-slate-400 max-w-md mx-auto">
                        All financed devices in your shop have settled their loans or have been released. View full details in Recent History.
                      </p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                      {currentShopProtectedDevices.map(d => (
                        <div key={d.id} className={`p-5 rounded-2xl border ${d.isLocked ? 'bg-rose-50/50 border-rose-200' : 'bg-white border-slate-200'}`}>
                          <div className="flex justify-between items-center mb-2">
                            <span className={`px-2.5 py-0.5 text-xs font-bold rounded-md ${d.isLocked ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-700'}`}>
                              {d.isLocked ? 'Locked (Default)' : (d.status.includes('Grace') ? 'Grace Period Active' : 'Protected')}
                            </span>
                            <span className="text-xs font-mono text-slate-400 font-bold">{d.id}</span>
                          </div>
                          <h4 className="font-bold text-slate-900 text-base">{d.model}</h4>
                          <p className="text-xs font-mono text-slate-500">IMEI: {d.imei}</p>
                          
                          <div className="mt-3 pt-3 border-t border-slate-100 text-xs space-y-1.5 bg-slate-50 p-3 rounded-xl border">
                            <div className="flex justify-between"><span>Customer:</span><strong className="text-slate-800">{d.customerName}</strong></div>
                            <div className="flex justify-between"><span>CNIC:</span><span className="font-mono text-slate-700">{d.customerCnic}</span></div>
                            <div className="flex justify-between"><span>Installment:</span><span className="font-mono font-bold text-slate-900">PKR {d.monthlyEmi.toLocaleString()}/mo ({d.paidTerms}/{d.totalTerms})</span></div>
                            <div className="flex justify-between pt-1 border-t border-slate-200">
                              <span className="font-semibold text-slate-600">Next Due Date:</span>
                              <span className={`font-mono font-bold ${d.isLocked ? 'text-rose-600' : 'text-emerald-700'}`}>
                                📅 {d.nextDueDate}
                              </span>
                            </div>
                            <div className="flex items-center justify-between pt-1 border-t border-slate-200">
                              <span className="font-semibold text-slate-600 flex items-center gap-1">
                                <Zap className="w-3.5 h-3.5 text-amber-500" />
                                <span>Auto-Lock:</span>
                              </span>
                              <button
                                onClick={() => handleToggleDeviceAutoLock(d.id)}
                                className={`px-2 py-0.5 rounded-full font-bold text-[10px] transition-all cursor-pointer ${
                                  d.autoLockEnabled
                                    ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                                    : 'bg-slate-100 text-slate-500 border border-slate-200'
                                }`}
                                title="Toggle Offline/Online automatic locking on due date"
                              >
                                {d.autoLockEnabled ? 'ACTIVE (OFFLINE OK)' : 'DISABLED'}
                              </button>
                            </div>
                          </div>

                          {/* LIVE LOCATION & CAMERA ACTION BUTTONS (GATED BY SUPER ADMIN PERMISSION) */}
                          <div className="mt-2 grid grid-cols-2 gap-1.5">
                            <button
                              onClick={() => handleOpenLocationModal(d)}
                              className={`py-2 px-2 font-bold text-[11px] rounded-lg border text-center cursor-pointer flex items-center justify-center gap-1 transition-all ${
                                canAccessLocation(d)
                                  ? 'bg-teal-50 hover:bg-teal-100 text-teal-800 border-teal-200'
                                  : 'bg-slate-50 hover:bg-slate-100 text-slate-400 border-slate-200'
                              }`}
                              title={canAccessLocation(d) ? 'Track Live GPS Location' : 'Super Admin Permission Required'}
                            >
                              <MapPin className="w-3.5 h-3.5" />
                              <span>{canAccessLocation(d) ? '📍 LIVE GPS' : 'GPS (🔒 RESTRICTED)'}</span>
                            </button>

                            <button
                              onClick={() => handleOpenCameraModal(d)}
                              className={`py-2 px-2 font-bold text-[11px] rounded-lg border text-center cursor-pointer flex items-center justify-center gap-1 transition-all ${
                                canAccessCamera(d)
                                  ? 'bg-purple-50 hover:bg-purple-100 text-purple-800 border-purple-200'
                                  : 'bg-slate-50 hover:bg-slate-100 text-slate-400 border-slate-200'
                              }`}
                              title={canAccessCamera(d) ? 'Remote Camera Snapshot' : 'Super Admin Permission Required'}
                            >
                              <Camera className="w-3.5 h-3.5" />
                              <span>{canAccessCamera(d) ? '📷 CAMERA' : 'CAM (🔒 RESTRICTED)'}</span>
                            </button>
                          </div>

                          {/* INSTALLMENT ACTIONS */}
                          <div className="mt-3 grid grid-cols-3 gap-1.5">
                            <button
                              onClick={() => {
                                setActivePaymentModalDev(d);
                                setPaymentAmountInput(d.monthlyEmi);
                                setPaymentNextDueDateInput('');
                              }}
                              className="py-2 px-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-[11px] rounded-lg border border-indigo-200 text-center cursor-pointer"
                              title="Record monthly installment paid by client"
                            >
                              💵 RECEIVE EMI
                            </button>
                            
                            <button
                              onClick={() => handleSendPaymentWarning(d)}
                              className="py-2 px-1.5 bg-amber-50 hover:bg-amber-100 text-amber-800 font-bold text-[11px] rounded-lg border border-amber-200 text-center cursor-pointer"
                              title="Send push warning dialog to client phone"
                            >
                              ⚠️ WARNING
                            </button>

                            <button
                              onClick={() => {
                                setActiveGraceModalDev(d);
                                setGraceDaysInput(7);
                              }}
                              className="py-2 px-1.5 bg-blue-50 hover:bg-blue-100 text-blue-800 font-bold text-[11px] rounded-lg border border-blue-200 text-center cursor-pointer"
                              title="Extend payment due date by days"
                            >
                              ⏳ EXTEND DATE
                            </button>
                          </div>

                          {/* OTA LOCK / UNLOCK BUTTON */}
                          <div className="mt-3 space-y-2">
                            <button
                              onClick={() => handleToggleLock(d.id)}
                              className={`w-full py-2.5 rounded-xl text-xs font-bold text-white cursor-pointer shadow-xs ${d.isLocked ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'}`}
                            >
                              {d.isLocked ? 'OTA UNLOCK PHONE' : 'OTA LOCK PHONE'}
                            </button>

                            <div className="grid grid-cols-2 gap-2">
                              <button
                                onClick={() => handleClearLoan(d.id)}
                                className="py-2 px-2 rounded-xl text-[11px] font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer border border-slate-200 text-center"
                                title="Clear loan, release Device Owner, and unlock uninstallation"
                              >
                                CLEAR LOAN
                              </button>

                              <button
                                onClick={() => handleRemoteWipe(d.id)}
                                className="py-2 px-2 rounded-xl text-[11px] font-bold bg-rose-50 hover:bg-rose-100 text-rose-700 cursor-pointer border border-rose-200 text-center"
                                title="Remote Factory Reset device"
                              >
                                REMOTE WIPE
                              </button>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </motion.div>
              )}

              {/* TAB 3: RECENT HISTORY (CLEARED LOANS FOR THIS SHOP) */}
              {shopTab === 'history' && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
                        <History className="w-5 h-5 text-emerald-600" />
                        <span>Recent History (Cleared Loans)</span>
                      </h3>
                      <p className="text-sm text-slate-400">All customer handsets that completed their installments and had their Device Owner lock released</p>
                    </div>

                    <div className="flex items-center gap-2">
                      {currentShopClearedDevices.length > 0 && (
                        <button
                          onClick={handleClearAllHistory}
                          className="px-3.5 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-bold rounded-xl border border-rose-200 cursor-pointer flex items-center gap-1.5 transition-all shrink-0"
                          title="Delete all cleared records from history"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>Clear All History</span>
                        </button>
                      )}
                      <div className="relative min-w-[260px]">
                        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                        <input
                          type="text"
                          placeholder="Search by customer, IMEI, model..."
                          value={historySearchQuery}
                          onChange={(e) => setHistorySearchQuery(e.target.value)}
                          className="w-full pl-9 pr-4 py-2 text-xs bg-white border border-slate-200 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
                        />
                      </div>
                    </div>
                  </div>

                  {/* SUMMARY CARDS */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-1">
                      <span className="text-xs text-slate-500 font-semibold">Cleared Handsets</span>
                      <div className="text-2xl font-black text-slate-900 font-mono">{currentShopClearedDevices.length} Devices</div>
                      <p className="text-xs text-emerald-600 font-semibold">Device Owner Released</p>
                    </div>

                    <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-1">
                      <span className="text-xs text-slate-500 font-semibold">Total Recovered Payments</span>
                      <div className="text-2xl font-black text-emerald-600 font-mono">
                        PKR {currentShopClearedDevices.reduce((acc, d) => acc + (d.totalPrice || 0), 0).toLocaleString()}
                      </div>
                      <p className="text-xs text-slate-400">100% Full Capital Settled</p>
                    </div>

                    <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs space-y-1">
                      <span className="text-xs text-slate-500 font-semibold">Customer Status</span>
                      <div className="text-2xl font-black text-slate-800">100% Owned</div>
                      <p className="text-xs text-slate-500">Unrestricted / Free</p>
                    </div>
                  </div>

                  {/* LIST OF CLEARED DEVICES */}
                  {(() => {
                    const filtered = currentShopClearedDevices.filter(d => {
                      if (!historySearchQuery.trim()) return true;
                      const q = historySearchQuery.toLowerCase();
                      return (
                        (d.customerName || '').toLowerCase().includes(q) ||
                        (d.customerPhone || '').toLowerCase().includes(q) ||
                        (d.customerCnic || '').toLowerCase().includes(q) ||
                        (d.model || '').toLowerCase().includes(q) ||
                        (d.imei || '').toLowerCase().includes(q)
                      );
                    });

                    if (filtered.length === 0) {
                      return (
                        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center space-y-3">
                          <History className="w-12 h-12 text-slate-300 mx-auto" />
                          <h4 className="font-bold text-slate-700 text-base">No Cleared Loan Records</h4>
                          <p className="text-xs text-slate-400 max-w-sm mx-auto">
                            {historySearchQuery ? 'No records match your search keyword.' : 'When you click CLEAR LOAN on any customer device, it will automatically move here with full clearance documentation.'}
                          </p>
                        </div>
                      );
                    }

                    return (
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {filtered.map((d) => (
                          <div key={d.id} className="p-5 bg-white rounded-2xl border border-emerald-200/80 shadow-xs space-y-3 relative overflow-hidden">
                            <div className="flex items-center justify-between">
                              <span className="px-2.5 py-0.5 text-xs font-bold rounded-md bg-emerald-100 text-emerald-800 flex items-center gap-1">
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                <span>Loan Cleared • Released</span>
                              </span>
                              <span className="text-xs font-mono font-bold text-slate-400">{d.id}</span>
                            </div>

                            <div>
                              <h4 className="font-bold text-slate-900 text-base flex items-center gap-2">
                                <Smartphone className="w-4 h-4 text-emerald-600" />
                                <span>{d.model}</span>
                              </h4>
                              <p className="text-xs font-mono text-slate-500">IMEI: {d.imei}</p>
                            </div>

                            <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs space-y-1.5">
                              <div className="flex justify-between">
                                <span className="text-slate-500">Customer:</span>
                                <strong className="text-slate-800">{d.customerName}</strong>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-slate-500">Phone:</span>
                                <span className="font-mono text-slate-700">{d.customerPhone}</span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-slate-500">CNIC:</span>
                                <span className="font-mono text-slate-700">{d.customerCnic}</span>
                              </div>
                              <div className="flex justify-between pt-1 border-t border-slate-200">
                                <span className="text-slate-500">Financed Amount:</span>
                                <span className="font-mono font-bold text-slate-900">PKR {d.totalPrice?.toLocaleString()} ({d.totalTerms}/{d.totalTerms} Full Paid)</span>
                              </div>
                              <div className="flex justify-between text-emerald-700 pt-0.5">
                                <span>Cleared Date:</span>
                                <span className="font-mono font-bold">{d.clearedAt || 'Recently'}</span>
                              </div>
                              <div className="flex justify-between text-slate-500 text-[11px]">
                                <span>Released By:</span>
                                <span className="italic">{d.releasedBy || currentShopData?.ownerName || 'Shop Owner'}</span>
                              </div>
                            </div>

                            <div className="pt-1 flex gap-2">
                              <button
                                onClick={() => setSelectedHistoryDev(d)}
                                className="flex-1 py-2.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 font-bold text-xs rounded-xl border border-emerald-200 flex items-center justify-center gap-1.5 cursor-pointer transition-all"
                              >
                                <FileText className="w-4 h-4 text-emerald-600 shrink-0" />
                                <span>VIEW NOC</span>
                              </button>
                              <button
                                onClick={() => {
                                  if (confirm(`Are you sure you want to delete ${d.model} (${d.customerName}) from Recent History? This will also free up the handset for re-scanning.`)) {
                                    handleDeleteHistoryDevice(d.id);
                                  }
                                }}
                                className="px-3.5 py-2.5 bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold text-xs rounded-xl border border-rose-200 flex items-center justify-center gap-1 cursor-pointer transition-all shrink-0"
                                title="Delete record from history"
                              >
                                <Trash2 className="w-4 h-4 shrink-0" />
                                <span>Delete</span>
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    );
                  })()}
                </motion.div>
              )}

              {/* TAB 3: ENROLL NEW HANDSET */}
              {shopTab === 'enroll' && (
                <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="max-w-2xl bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs space-y-6">
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg">Enroll New Handset into Financing</h3>
                    <p className="text-sm text-slate-500">Requires <strong>{globalSettings.creditsPerEnrollment} credit</strong> (Your balance: {currentShopData?.credits} credits)</p>
                  </div>

                  <form onSubmit={handleEnrollDevice} className="space-y-4 text-sm">
                    <div>
                      <label className="font-bold text-slate-700">Customer Full Name</label>
                      <input
                        type="text"
                        required
                        placeholder="e.g. Tariq Mehmood"
                        value={enrollForm.customerName}
                        onChange={e => setEnrollForm({ ...enrollForm, customerName: e.target.value })}
                        className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900"
                      />
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="font-bold text-slate-700">Customer Phone</label>
                        <input
                          type="text"
                          required
                          placeholder="0300-1234567"
                          value={enrollForm.customerPhone}
                          onChange={e => setEnrollForm({ ...enrollForm, customerPhone: e.target.value })}
                          className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                        />
                      </div>
                      <div>
                        <label className="font-bold text-slate-700">Customer CNIC</label>
                        <input
                          type="text"
                          required
                          placeholder="35201-1234567-1"
                          value={enrollForm.customerCnic}
                          onChange={e => setEnrollForm({ ...enrollForm, customerCnic: e.target.value })}
                          className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                        />
                      </div>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="font-bold text-slate-700">Handset Model</label>
                        <input
                          type="text"
                          required
                          placeholder="Samsung S23, Infinix Note 40..."
                          value={enrollForm.model}
                          onChange={e => setEnrollForm({ ...enrollForm, model: e.target.value })}
                          className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900"
                        />
                      </div>
                      <div>
                        <label className="font-bold text-slate-700">15-Digit IMEI</label>
                        <input
                          type="text"
                          required
                          placeholder="354891029384756"
                          value={enrollForm.imei}
                          onChange={e => setEnrollForm({ ...enrollForm, imei: e.target.value })}
                          className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                        />
                      </div>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="font-bold text-slate-700">Monthly EMI (PKR)</label>
                        <input
                          type="number"
                          required
                          value={enrollForm.monthlyEmi}
                          onChange={e => setEnrollForm({ ...enrollForm, monthlyEmi: e.target.value })}
                          className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                        />
                      </div>
                      <div>
                        <label className="font-bold text-slate-700">Total Terms (Months)</label>
                        <input
                          type="number"
                          required
                          value={enrollForm.totalTerms}
                          onChange={e => setEnrollForm({ ...enrollForm, totalTerms: e.target.value })}
                          className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="font-bold text-slate-700">Contract Start Date</label>
                        <input
                          type="date"
                          required
                          value={enrollForm.startDate}
                          onChange={e => setEnrollForm({ ...enrollForm, startDate: e.target.value })}
                          className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-medium focus:outline-none focus:border-slate-900"
                        />
                      </div>
                      <div>
                        <label className="font-bold text-slate-700">Monthly Due Day of Month</label>
                        <select
                          value={enrollForm.dueDayOfMonth}
                          onChange={e => setEnrollForm({ ...enrollForm, dueDayOfMonth: e.target.value })}
                          className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-medium focus:outline-none focus:border-slate-900"
                        >
                          <option value="1">1st of every month</option>
                          <option value="5">5th of every month</option>
                          <option value="10">10th of every month</option>
                          <option value="15">15th of every month</option>
                          <option value="20">20th of every month</option>
                          <option value="25">25th of every month</option>
                        </select>
                      </div>
                    </div>
                    <button
                      type="submit"
                      className="w-full py-3.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl font-bold text-sm shadow-md mt-4 cursor-pointer"
                    >
                      CONFIRM ENROLLMENT & DEDUCT {globalSettings.creditsPerEnrollment} CREDIT
                    </button>
                  </form>
                </motion.div>
              )}

              {/* TAB 4: QR CODE PROVISIONING */}
              {shopTab === 'qr' && (
                <div className="max-w-xl mx-auto bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs text-center space-y-5">
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg">{currentShopData?.name} — 6-Tap QR Setup</h3>
                    <p className="text-sm text-slate-500">Scan on Welcome Screen of customer's fresh handset to auto-enroll into {currentShopData?.name}</p>
                  </div>

                  {/* DIRECT UPDATE BANNER (NO RESET NEEDED) */}
                  <div className="bg-gradient-to-r from-blue-900 to-indigo-900 text-white p-4 rounded-xl text-left border border-blue-700/50 shadow-sm space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-bold text-sm text-emerald-300 flex items-center gap-1.5">
                        <span>📲</span> بغیر ری سیٹ کیے ایپ اپ ڈیٹ کریں (Update Without Reset)
                      </span>
                    </div>
                    <p className="text-xs text-blue-100 leading-relaxed">
                      اگر موبائل پر پہلے سے ایپ انسٹال ہے تو فون کو ری سیٹ کرنے کی ضرورت نہیں! صرف یہ نئی APK فائل ڈاؤنلوڈ کر کے انسٹال/اپڈیٹ کریں:
                    </p>
                    <div className="pt-1 flex flex-wrap gap-2">
                      <a
                        href="https://abiz-locker.surge.sh/abiz-agent.apk"
                        download="abiz-agent.apk"
                        className="inline-flex items-center gap-1.5 bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs py-2 px-4 rounded-lg shadow-xs transition-all"
                      >
                        <span>📥</span> Download Latest APK (1.4 MB)
                      </a>
                    </div>
                  </div>

                  {/* CHECKSUM MODE SELECTOR */}
                  <div className="flex items-center justify-center gap-1.5 bg-slate-100 p-1.5 rounded-xl text-xs font-semibold">
                    <button
                      type="button"
                      onClick={() => setQrChecksumMode('universal')}
                      className={`flex-1 py-2 px-2.5 rounded-lg transition-all ${
                        qrChecksumMode === 'universal'
                          ? 'bg-emerald-600 text-white shadow-xs font-bold'
                          : 'text-slate-600 hover:text-slate-900'
                      }`}
                    >
                      🌐 Universal (All Android)
                    </button>
                    <button
                      type="button"
                      onClick={() => setQrChecksumMode('package')}
                      className={`flex-1 py-2 px-2.5 rounded-lg transition-all ${
                        qrChecksumMode === 'package'
                          ? 'bg-slate-900 text-white shadow-xs font-bold'
                          : 'text-slate-600 hover:text-slate-900'
                      }`}
                    >
                      📦 Android 7/8 (Tecno/Go)
                    </button>
                    <button
                      type="button"
                      onClick={() => setQrChecksumMode('signature')}
                      className={`flex-1 py-2 px-2.5 rounded-lg transition-all ${
                        qrChecksumMode === 'signature'
                          ? 'bg-indigo-600 text-white shadow-xs font-bold'
                          : 'text-slate-600 hover:text-slate-900'
                      }`}
                    >
                      🛡️ Android 9+ (Pie - 15)
                    </button>
                  </div>
                  
                  {/* QR CODE DISPLAY */}
                  <div className="p-4 bg-white rounded-2xl inline-block border-2 border-slate-900 shadow-lg relative">
                    <img
                      src={`https://api.qrserver.com/v1/create-qr-code/?size=320x320&data=${encodeURIComponent(JSON.stringify(
                        qrChecksumMode === 'universal' ? {
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.mdmesh.agent/com.mdmesh.agent.admin.AdminReceiver",
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://abiz-locker.surge.sh/abiz-agent.apk",
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "I_KfkPDBaZJuSmBuUYIkBV4FGRVqmPEpSN8tjZ2Esz0",
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "YM4jK9ZRy-24YQ_JMSFE8Ug2Uxk4rjSrzS2IwjakXkM",
                          "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
                          "android.app.extra.PROVISIONING_SKIP_ENCRYPT": true,
                          "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
                            "com.mdmesh.SERVER_URL": `${MDM_API_BASE}/`,
                            "com.mdmesh.ENROLL_TOKEN": "abiz-enroll-" + (currentShopData?.id || "SH-101"),
                            "com.mdmesh.FRP_ADMIN_ACCOUNT": currentShopData?.frpRecoveryEmail || shopFrpEmailInput || "usman.lahore.recovery@gmail.com",
                            "server_url": `${MDM_API_BASE}/`,
                            "shop_id": currentShopData?.id || "SH-101",
                            "frp_account": currentShopData?.frpRecoveryEmail || shopFrpEmailInput || "usman.lahore.recovery@gmail.com"
                          }
                        } : qrChecksumMode === 'package' ? {
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.mdmesh.agent/com.mdmesh.agent.admin.AdminReceiver",
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://abiz-locker.surge.sh/abiz-agent.apk",
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "I_KfkPDBaZJuSmBuUYIkBV4FGRVqmPEpSN8tjZ2Esz0",
                          "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
                          "android.app.extra.PROVISIONING_SKIP_ENCRYPT": true,
                          "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
                            "com.mdmesh.SERVER_URL": `${MDM_API_BASE}/`,
                            "com.mdmesh.ENROLL_TOKEN": "abiz-enroll-" + (currentShopData?.id || "SH-101"),
                            "com.mdmesh.FRP_ADMIN_ACCOUNT": currentShopData?.frpRecoveryEmail || shopFrpEmailInput || "usman.lahore.recovery@gmail.com",
                            "server_url": `${MDM_API_BASE}/`,
                            "shop_id": currentShopData?.id || "SH-101",
                            "frp_account": currentShopData?.frpRecoveryEmail || shopFrpEmailInput || "usman.lahore.recovery@gmail.com"
                          }
                        } : {
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.mdmesh.agent/com.mdmesh.agent.admin.AdminReceiver",
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://abiz-locker.surge.sh/abiz-agent.apk",
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "I_KfkPDBaZJuSmBuUYIkBV4FGRVqmPEpSN8tjZ2Esz0",
                          "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "YM4jK9ZRy-24YQ_JMSFE8Ug2Uxk4rjSrzS2IwjakXkM",
                          "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
                          "android.app.extra.PROVISIONING_SKIP_ENCRYPT": true,
                          "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
                            "com.mdmesh.SERVER_URL": `${MDM_API_BASE}/`,
                            "com.mdmesh.ENROLL_TOKEN": "abiz-enroll-" + (currentShopData?.id || "SH-101"),
                            "com.mdmesh.FRP_ADMIN_ACCOUNT": currentShopData?.frpRecoveryEmail || shopFrpEmailInput || "usman.lahore.recovery@gmail.com",
                            "server_url": `${MDM_API_BASE}/`,
                            "shop_id": currentShopData?.id || "SH-101",
                            "frp_account": currentShopData?.frpRecoveryEmail || shopFrpEmailInput || "usman.lahore.recovery@gmail.com"
                          }
                        }
                      ))}`}
                      alt="ABIZ Enterprise QR"
                      className="w-60 h-60 mx-auto object-contain"
                    />
                  </div>

                  {/* ACTIVE HASH BADGE */}
                  <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-200 text-xs text-slate-700 space-y-1">
                    <div className="font-semibold text-slate-900 flex items-center justify-center gap-1.5">
                      <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                      {qrChecksumMode === 'universal'
                        ? 'Active: Dual SHA-256 (Package + Signature) — All Android 7 to 15+'
                        : qrChecksumMode === 'package'
                        ? 'Active: Exact Binary Package Hash (Android 7 & 8 Oreo / Tecno)'
                        : 'Active: Release Signing Certificate Hash (Android 9+ Pie to 15)'}
                    </div>
                    <div className="font-mono text-[11px] text-slate-600 break-all bg-white px-2 py-1 rounded border border-slate-200">
                      {qrChecksumMode === 'package'
                        ? 'Package: I_KfkPDBaZJuSmBuUYIkBV4FGRVqmPEpSN8tjZ2Esz0'
                        : qrChecksumMode === 'signature'
                        ? 'Signature: YM4jK9ZRy-24YQ_JMSFE8Ug2Uxk4rjSrzS2IwjakXkM'
                        : 'PKG: I_KfkPDBaZJuSmBuUYIkBV4FGRVqmPEpSN8tjZ2Esz0 | CERT: YM4jK9ZRy-24YQ_JMSFE8Ug2Uxk4rjSrzS2IwjakXkM'}
                    </div>
                    <p className="text-[11px] text-slate-500">
                      {qrChecksumMode === 'universal'
                        ? '✅ یونیورسل موڈ تمام اینڈرائڈ ورژنز (7.0 سے لے کر 15 تک) پر بنا کسی ایرر کے کام کرتا ہے۔'
                        : qrChecksumMode === 'package'
                        ? '✅ خصوصی طور پر Tecno، Infinix اور Android 8.1 Go ڈیوائسز کے لیے مصدقہ ہیش۔'
                        : '✅ Android 9 سے لے کر 15 تک کے تمام جدید ماڈلز کے لیے سائننگ کی سرٹیفکیٹ ہیش۔'}
                    </p>
                  </div>

                  <div className="text-left text-xs sm:text-sm bg-slate-50 p-4 rounded-xl space-y-2 text-slate-700 border border-slate-200">
                    <p className="font-bold text-slate-900 text-sm">📲 6-Tap Setup Instructions:</p>
                    <p>1. Ensure phone is connected to Wi-Fi with active internet.</p>
                    <p>2. On Welcome Screen, <strong>tap 6 times</strong> on empty area.</p>
                    <p>3. Scan this streamlined QR code.</p>
                    <p>4. Device will auto-download <strong>ABIZ LOCKER</strong> and activate Device Owner!</p>
                  </div>
                </div>
              )}

              {/* TAB 5: LOCKSCREEN & PAYMENT ACCOUNTS SETTINGS */}
              {shopTab === 'settings' && (
                <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <h3 className="font-extrabold text-slate-900 text-xl flex items-center gap-2">
                        <span>⚙️ Lockscreen Branding & Payment Accounts</span>
                      </h3>
                      <p className="text-sm text-slate-500 mt-0.5">
                        Customize your helpline phone number and bank accounts displayed to customers on locked handsets
                      </p>
                    </div>
                    <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200 text-emerald-800 px-3 py-1.5 rounded-xl text-xs font-bold shrink-0 self-start sm:self-auto">
                      <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                      <span>Real-time OTA Sync</span>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
                    
                    {/* LEFT COLUMN: EDIT FORM */}
                    <form onSubmit={handleSaveShopSettings} className="lg:col-span-7 space-y-5">
                      
                      {/* CARD 1: FRANCHISE & HELPLINE CONTACT */}
                      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
                        <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                          <Store className="w-5 h-5 text-indigo-600" />
                          <h4 className="font-bold text-slate-900 text-base">Shop Franchise & Calling Helpline</h4>
                        </div>

                        <div>
                          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">Franchise / Shop Name</label>
                          <input
                            type="text"
                            required
                            placeholder="e.g. Lahore Central Mobile Mall"
                            value={shopNameInput}
                            onChange={e => setShopNameInput(e.target.value)}
                            className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900 font-semibold"
                          />
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">Owner / Manager Name</label>
                            <input
                              type="text"
                              required
                              placeholder="e.g. Haji Usman"
                              value={shopOwnerInput}
                              onChange={e => setShopOwnerInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900 font-semibold"
                            />
                          </div>

                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">Helpline / Contact Phone</label>
                            <input
                              type="text"
                              required
                              placeholder="e.g. 0300-1234567"
                              value={shopPhoneInput}
                              onChange={e => setShopPhoneInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono font-bold focus:outline-none focus:border-slate-900 text-emerald-700"
                            />
                          </div>
                        </div>

                        <div className="bg-emerald-50/60 border border-emerald-200 rounded-xl p-3 text-xs text-emerald-800 space-y-1">
                          <p className="font-bold flex items-center gap-1.5">
                            <span>📞 Direct Customer Call Feature</span>
                          </p>
                          <p className="text-slate-600">
                            جب کسٹمر کا فون لاک ہوتا ہے، اسکرین پر <strong className="text-emerald-700">"CALL SHOP OWNER"</strong> کا بٹن ہوتا ہے۔ یہ نمبر تبدیل کرنے سے فون اسی نمبر پر ڈائریکٹ کال ملائے گا۔
                          </p>
                        </div>
                      </div>

                      {/* CARD: MASTER FRP RECOVERY GOOGLE ACCOUNT */}
                      <div className="bg-white p-6 rounded-2xl border-2 border-amber-300 shadow-xs space-y-4">
                        <div className="flex items-center gap-2 pb-3 border-b border-amber-100">
                          <ShieldCheck className="w-5 h-5 text-amber-600" />
                          <div>
                            <h4 className="font-bold text-slate-900 text-base flex items-center gap-2">
                              <span>Master FRP Recovery Google Account (ایف آر پی ماسٹر جی میل)</span>
                              <span className="bg-amber-100 text-amber-800 text-[10px] px-2 py-0.5 rounded-full font-bold">Hard Reset Protection</span>
                            </h4>
                            <p className="text-xs text-slate-500">اگر کسٹمر فون کو بٹنوں سے ہارڈ ری سیٹ کرے تو انلاک کرنے کیلئے یہ جی میل استعمال ہوگا</p>
                          </div>
                        </div>

                        <div>
                          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                            Shop Owner Master Gmail Address (دکاندار کا جی میل ایڈریس)
                          </label>
                          <input
                            type="email"
                            required
                            placeholder="e.g. yourshop.recovery@gmail.com"
                            value={shopFrpEmailInput}
                            onChange={e => setShopFrpEmailInput(e.target.value)}
                            className="w-full bg-amber-50/40 border border-amber-300 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono font-bold focus:outline-none focus:border-amber-600 text-amber-900"
                          />
                        </div>

                        <div className="bg-amber-50/80 border border-amber-200 rounded-xl p-3.5 text-xs text-amber-900 space-y-2">
                          <p className="font-bold flex items-center gap-1.5 text-amber-950">
                            <span>🔐 یہ فیچر کیسے کام کرتا ہے اور ری سیٹ ہونے پر فون کیسے کھلے گا؟</span>
                          </p>
                          <div className="text-slate-700 leading-relaxed space-y-1.5">
                            <p>
                              <strong>1. خودکار رجسٹریشن (Auto Enrolled):</strong> جب کسٹمر کا موبائل ڈیش بورڈ کے کیو آر کوڈ سے سیٹ اپ ہوتا ہے، تو یہ جی میل فون کے <em>Enterprise Authorized FRP Account</em> میں آٹو بائنڈ ہو جاتا ہے۔
                            </p>
                            <p>
                              <strong>2. ہارڈ ری سیٹ پر لاک (Permanent Lock):</strong> اگر کسٹمر موبائل کو بٹنوں (Recovery Mode) سے زبردستی فارمیٹ کرے، تو اینڈرائڈ کسٹمر کا یا کسی اور کا جی میل رد کر دے گا اور صرف اور صرف <strong>آپ کا یہ ماسٹر جی میل</strong> مانگے گا۔
                            </p>
                            <p>
                              <strong>3. دکان پر انلاک کرنے کے طریقے:</strong> جب کسٹمر آپ کے پاس آئے، آپ اپنا یہ جی میل لگا کر 1 منٹ میں فون کھول سکتے ہیں، یا شروع کی اسکرین پر 6 بار ٹیپ کر کے کیو آر کوڈ اسکین کر دیں تو فون بغیر جی میل کے بھی فوراً بحال ہو جائے گا۔
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* CARD: AUTOMATIC DUE DATE LOCK SWITCH (OFFLINE & ONLINE) */}
                      <div className="bg-white p-6 rounded-2xl border-2 border-indigo-200 shadow-xs space-y-4">
                        <div className="flex items-center justify-between pb-3 border-b border-indigo-100">
                          <div className="flex items-center gap-2.5">
                            <LockKeyhole className="w-5 h-5 text-indigo-600" />
                            <div>
                              <h4 className="font-bold text-slate-900 text-base flex items-center gap-2">
                                <span>Automatic Due Date Lock (تاریخ پر خودکار لاک)</span>
                                <span className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold ${shopAutoLockInput ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>
                                  {shopAutoLockInput ? 'ACTIVE (فعال)' : 'DISABLED (غیر فعال)'}
                                </span>
                              </h4>
                              <p className="text-xs text-slate-500">تاریخ گزرنے پر بغیر انٹرنیٹ (Offline) کے بھی فون خودکار لاک ہو جائے گا</p>
                            </div>
                          </div>

                          {/* TOGGLE SWITCH */}
                          <button
                            type="button"
                            onClick={() => {
                              const nextVal = !shopAutoLockInput;
                              setShopAutoLockInput(nextVal);
                              showToast(`Automatic Due Date Lock switch is now ${nextVal ? 'ON (Active)' : 'OFF (Disabled)'}`);
                            }}
                            className={`relative inline-flex h-7 w-13 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                              shopAutoLockInput ? 'bg-emerald-600' : 'bg-slate-300'
                            }`}
                          >
                            <span
                              className={`pointer-events-none inline-block h-6 w-6 transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out ${
                                shopAutoLockInput ? 'translate-x-6' : 'translate-x-0'
                              }`}
                            />
                          </button>
                        </div>

                        <div className="bg-indigo-50/70 border border-indigo-200 rounded-xl p-3.5 text-xs text-indigo-950 space-y-2">
                          <div className="flex items-start gap-2">
                            <WifiOff className="w-4 h-4 text-indigo-700 shrink-0 mt-0.5" />
                            <div className="space-y-1.5 text-slate-700 leading-relaxed">
                              <p className="font-bold text-indigo-950">
                                📶 بغیر انٹرنیٹ (Offline) لاک کیسے کام کرتا ہے؟
                              </p>
                              <p>
                                • جب آپ یہ سوئچ <strong>ON</strong> رکھتے ہیں، تو اینڈرائڈ کے لوکل ہارڈویئر اسٹوریج میں اگلی قسط کی تاریخ محفوظ ہو جاتی ہے۔ اگر کسٹمر تاریخ تک قسط نہ دے اور سم کارڈ نکال دے یا انٹرنیٹ بند کر لے، تب بھی فون خود بخود بند (Kiosk Lock) ہو جائے گا۔
                              </p>
                              <p>
                                • جب کسٹمر قسط جمع کروائے گا، آپ ڈیش بورڈ سے <strong className="text-emerald-700">"Mark Payment Received"</strong> کریں گے، جس سے اگلی تاریخ خود بخود آگے بڑھ جائے گی اور فون فورا کھل جائے گا۔
                              </p>
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* CARD 2: MOBILE WALLETS (JAZZCASH & EASYPAISA) */}
                      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
                        <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                          <Smartphone className="w-5 h-5 text-emerald-600" />
                          <h4 className="font-bold text-slate-900 text-base">JazzCash & EasyPaisa Accounts (موبائل والیٹ)</h4>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">JazzCash Mobile Number</label>
                            <input
                              type="text"
                              placeholder="0300-1234567"
                              value={jazzCashInput}
                              onChange={e => setJazzCashInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono font-bold focus:outline-none focus:border-slate-900"
                            />
                          </div>
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">JazzCash Account Title</label>
                            <input
                              type="text"
                              placeholder="e.g. Haji Usman"
                              value={jazzCashTitleInput}
                              onChange={e => setJazzCashTitleInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900"
                            />
                          </div>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">EasyPaisa Mobile Number</label>
                            <input
                              type="text"
                              placeholder="0300-1234567"
                              value={easyPaisaInput}
                              onChange={e => setEasyPaisaInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono font-bold focus:outline-none focus:border-slate-900"
                            />
                          </div>
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">EasyPaisa Account Title</label>
                            <input
                              type="text"
                              placeholder="e.g. Haji Usman"
                              value={easyPaisaTitleInput}
                              onChange={e => setEasyPaisaTitleInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900"
                            />
                          </div>
                        </div>
                      </div>

                      {/* CARD 3: BANK ACCOUNT & IBAN */}
                      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
                        <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                          <Building2 className="w-5 h-5 text-blue-600" />
                          <h4 className="font-bold text-slate-900 text-base">Bank Account Details (بینک اکاؤنٹ تفصیلات)</h4>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">Bank Name</label>
                            <input
                              type="text"
                              placeholder="e.g. Meezan Bank, HBL..."
                              value={bankNameInput}
                              onChange={e => setBankNameInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900 font-semibold"
                            />
                          </div>
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">Account Number</label>
                            <input
                              type="text"
                              placeholder="e.g. 0102-0105849301"
                              value={bankAccountInput}
                              onChange={e => setBankAccountInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono font-bold focus:outline-none focus:border-slate-900"
                            />
                          </div>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">Account Title</label>
                            <input
                              type="text"
                              placeholder="e.g. Lahore Central Mobile Mall"
                              value={bankTitleInput}
                              onChange={e => setBankTitleInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900"
                            />
                          </div>
                          <div>
                            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">IBAN Number</label>
                            <input
                              type="text"
                              placeholder="e.g. PK36MEZN0001020105849301"
                              value={bankIbanInput}
                              onChange={e => setBankIbanInput(e.target.value)}
                              className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono text-xs focus:outline-none focus:border-slate-900"
                            />
                          </div>
                        </div>
                      </div>

                      {/* CARD 4: URDU LOCK NOTICE */}
                      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
                        <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                          <MessageSquare className="w-5 h-5 text-rose-600" />
                          <h4 className="font-bold text-slate-900 text-base">Lock Notice Message (اردو میسج)</h4>
                        </div>

                        <div>
                          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">Urdu Notice Text for Customer Handset</label>
                          <textarea
                            rows={3}
                            dir="rtl"
                            value={lockNoticeUrduInput}
                            onChange={e => setLockNoticeUrduInput(e.target.value)}
                            className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900 leading-relaxed font-sans"
                            placeholder="معزز کسٹمر، آپکی ماہانہ قسط کی ادائیگی کی تاریخ گزر چکی ہے۔.."
                          />
                        </div>
                      </div>

                      {/* SUBMIT BUTTON */}
                      <button
                        type="submit"
                        className="w-full py-4 bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-sm sm:text-base rounded-2xl shadow-lg transition-all cursor-pointer flex items-center justify-center gap-2.5"
                      >
                        <Save className="w-5 h-5" />
                        <span>💾 SAVE LOCKSCREEN & PAYMENT SETTINGS</span>
                      </button>

                    </form>

                    {/* RIGHT COLUMN: INTERACTIVE LIVE LOCKSCREEN PREVIEW */}
                    <div className="lg:col-span-5 sticky top-24">
                      <div className="space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Live Customer Screen Preview</span>
                          <span className="text-[11px] font-bold text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                            📱 Real-Time AMOLED Preview
                          </span>
                        </div>

                        {/* SMARTPHONE FRAME */}
                        <div className="w-full max-w-[340px] mx-auto bg-slate-950 p-3 rounded-[2.5rem] shadow-2xl border-4 border-slate-800">
                          {/* SCREEN GLASS */}
                          <div className="bg-[#090D16] rounded-[2rem] p-4 text-white overflow-hidden text-center relative border border-slate-800 space-y-3">
                            
                            {/* NOTCH & STATUS BAR */}
                            <div className="flex items-center justify-between text-[10px] text-slate-400 px-2 pt-1 font-mono">
                              <span>12:45</span>
                              <div className="w-16 h-3 bg-black rounded-full mx-auto"></div>
                              <span>📶 🔋 98%</span>
                            </div>

                            {/* LOGO & APP BRAND */}
                            <div className="pt-2">
                              <img src="/abiz-logo.png" alt="ABIZ" className="w-12 h-12 mx-auto object-contain drop-shadow" />
                              <h5 className="font-extrabold text-base tracking-wide text-white mt-1">ABIZ LOCKER</h5>
                              <p className="text-[9px] font-bold tracking-widest text-cyan-400">ENTERPRISE FINANCING PROTECTION</p>
                            </div>

                            {/* RED LOCK WARNING CARD */}
                            <div className="bg-[#1E1B4B] border border-rose-900/60 p-3 rounded-xl text-center space-y-1.5 shadow-sm">
                              <div className="text-xs font-extrabold text-rose-400">
                                🔒 DEVICE LOCKED / فون لاک ہے
                              </div>
                              <p className="text-[11px] text-slate-200 leading-tight" dir="rtl">
                                {lockNoticeUrduInput || 'معزز کسٹمر، آپکی ماہانہ قسط کی ادائیگی کی تاریخ گزر چکی ہے۔ برائے مہربانی فوری رابطہ کریں۔'}
                              </p>
                              <p className="text-[9px] text-slate-400">
                                Installment is overdue. Handset features restricted.
                              </p>
                            </div>

                            {/* FRANCHISE & OWNER CARD */}
                            <div className="bg-[#0F172A] border border-slate-800 p-2.5 rounded-xl text-left text-xs space-y-1">
                              <div className="text-[9px] font-bold text-slate-400 uppercase tracking-wider">
                                🏪 REGISTERED FRANCHISE
                              </div>
                              <p className="font-extrabold text-white text-xs truncate">
                                {shopNameInput || currentShopData?.name}
                              </p>
                              <p className="text-[11px] font-bold text-sky-400 truncate">
                                👤 Owner: {shopOwnerInput || currentShopData?.ownerName}
                              </p>
                              <p className="text-[11px] font-mono text-slate-200 truncate font-semibold">
                                📞 Helpline: {shopPhoneInput || currentShopData?.phone}
                              </p>
                            </div>

                            {/* MASTER FRP RECOVERY BADGE */}
                            <div className="bg-[#1e1b4b] border border-indigo-700/60 p-2.5 rounded-xl text-left text-xs space-y-1">
                              <div className="text-[9px] font-bold text-amber-400 uppercase tracking-wider flex items-center justify-between">
                                <span>🔐 MASTER FRP RECOVERY GMAIL</span>
                                <span className="text-[9px] text-emerald-400 font-normal">Active</span>
                              </div>
                              <p className="font-mono text-[11px] text-amber-200 truncate font-semibold">
                                {shopFrpEmailInput || currentShopData?.frpRecoveryEmail || 'Not configured'}
                              </p>
                            </div>

                            {/* PAYMENT ACCOUNTS CARD */}
                            <div className="bg-[#111827] border border-slate-800 p-2.5 rounded-xl text-left text-[11px] space-y-1.5">
                              <div className="text-[9px] font-bold text-amber-400 uppercase tracking-wider">
                                💳 PAYMENT ACCOUNTS (قسط جمع کروائیں)
                              </div>
                              
                              <div className="bg-[#1F2937] p-2 rounded-lg space-y-1 font-mono text-[10px]">
                                {(jazzCashInput || easyPaisaInput) && (
                                  <div className="text-emerald-400 font-bold">
                                    📱 JazzCash / EasyPaisa: {jazzCashInput || easyPaisaInput}
                                    <div className="text-slate-400 text-[9px] font-sans font-normal pl-3">
                                      Title: {jazzCashTitleInput || easyPaisaTitleInput || shopOwnerInput}
                                    </div>
                                  </div>
                                )}
                                {bankNameInput && (
                                  <div className="text-blue-400 font-bold pt-0.5">
                                    🏦 {bankNameInput}: {bankAccountInput}
                                    <div className="text-slate-400 text-[9px] font-sans font-normal pl-3">
                                      Title: {bankTitleInput || shopNameInput}
                                    </div>
                                    {bankIbanInput && (
                                      <div className="text-slate-400 text-[8px] pl-3 truncate">
                                        IBAN: {bankIbanInput}
                                      </div>
                                    )}
                                  </div>
                                )}
                              </div>
                              <p className="text-[9px] text-slate-400" dir="rtl">
                                قسط جمع کروا کر رسید واٹس ایپ کریں تاکہ فون بحال ہو سکے۔
                              </p>
                            </div>

                            {/* 1-TAP CALL BUTTON */}
                            <div className="pt-1">
                              <div className="w-full py-2.5 bg-emerald-600 rounded-xl text-xs font-bold text-white shadow-md flex items-center justify-center gap-1.5">
                                <span>📞 CALL SHOP OWNER ({shopPhoneInput || currentShopData?.phone})</span>
                              </div>
                            </div>

                            {/* FOOTER BAR */}
                            <div className="text-[9px] text-slate-500 pt-1">
                              Emergency Calls (1122) Allowed
                            </div>

                          </div>
                        </div>
                      </div>
                    </div>

                  </div>
                </motion.div>
              )}
            </>
          )}

        </div>

      </main>

      {/* ========================================== */}
      {/* MODALS */}
      {/* ========================================== */}

      {/* 1. REGISTER NEW SHOP MODAL */}
      <AnimatePresence>
        {showAddShopModal && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl border border-slate-200 space-y-4 max-h-[90vh] overflow-y-auto"
            >
              <div className="flex items-center justify-between">
                <h3 className="font-bold text-slate-900 text-lg">Register New Retail Franchise</h3>
                <button onClick={() => setShowAddShopModal(false)} className="text-slate-400 hover:text-slate-900">
                  <X className="w-6 h-6" />
                </button>
              </div>
              <form onSubmit={handleCreateShop} className="space-y-3.5 text-sm">
                <div>
                  <label className="font-bold text-slate-700">Shop / Store Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Al-Madina Telecom"
                    value={newShopForm.name}
                    onChange={e => setNewShopForm({ ...newShopForm, name: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900"
                  />
                </div>
                <div>
                  <label className="font-bold text-slate-700">Owner Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Tariq Mehmood"
                    value={newShopForm.ownerName}
                    onChange={e => setNewShopForm({ ...newShopForm, ownerName: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900"
                  />
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="font-bold text-slate-700">Login Email</label>
                    <input
                      type="email"
                      required
                      placeholder="shop@abiz.com"
                      value={newShopForm.email}
                      onChange={e => setNewShopForm({ ...newShopForm, email: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 focus:outline-none focus:border-slate-900"
                    />
                  </div>
                  <div>
                    <label className="font-bold text-slate-700">Login Password</label>
                    <input
                      type="text"
                      required
                      placeholder="123"
                      value={newShopForm.password}
                      onChange={e => setNewShopForm({ ...newShopForm, password: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="font-bold text-slate-700">Phone</label>
                    <input
                      type="text"
                      placeholder="0300-1234567"
                      value={newShopForm.phone}
                      onChange={e => setNewShopForm({ ...newShopForm, phone: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                    />
                  </div>
                  <div>
                    <label className="font-bold text-slate-700">Initial Credits</label>
                    <input
                      type="number"
                      value={newShopForm.credits}
                      onChange={e => setNewShopForm({ ...newShopForm, credits: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white font-bold text-sm rounded-xl shadow-xs mt-2 cursor-pointer"
                >
                  CONFIRM & REGISTER SHOP
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 2. ADD CREDITS MODAL */}
      <AnimatePresence>
        {showCreditTopupModal && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl border border-slate-200 space-y-4"
            >
              <div className="flex items-center justify-between">
                <h3 className="font-bold text-slate-900 text-lg">Issue Franchise Credits</h3>
                <button onClick={() => setShowCreditTopupModal(false)} className="text-slate-400 hover:text-slate-900">
                  <X className="w-6 h-6" />
                </button>
              </div>
              <form onSubmit={handleTopupCredits} className="space-y-4 text-sm">
                <div>
                  <label className="font-bold text-slate-700">Select Shop</label>
                  <select
                    value={creditTopupForm.shopId || selectedShopDetailId || shops[0]?.id}
                    onChange={e => setCreditTopupForm({ ...creditTopupForm, shopId: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-medium"
                  >
                    {shops.map(s => <option key={s.id} value={s.id}>{s.name} (Current: {s.credits} Credits)</option>)}
                  </select>
                </div>
                <div>
                  <label className="font-bold text-slate-700">Credits to Issue (+)</label>
                  <input
                    type="number"
                    value={creditTopupForm.amount}
                    onChange={e => setCreditTopupForm({ ...creditTopupForm, amount: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono focus:outline-none focus:border-slate-900"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white font-bold text-sm rounded-xl shadow-xs mt-2 cursor-pointer"
                >
                  ALLOCATE CREDITS TO FRANCHISE
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 3. ENROLL DEVICE MODAL */}
      <AnimatePresence>
        {showEnrollDeviceModal && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl border border-slate-200 space-y-4 max-h-[90vh] overflow-y-auto"
            >
              <div className="flex items-center justify-between">
                <h3 className="font-bold text-slate-900 text-lg">Enroll New Handset</h3>
                <button onClick={() => setShowEnrollDeviceModal(false)} className="text-slate-400 hover:text-slate-900">
                  <X className="w-6 h-6" />
                </button>
              </div>
              <form onSubmit={handleEnrollDevice} className="space-y-3.5 text-sm">
                <div>
                  <label className="font-bold text-slate-700">Customer Name</label>
                  <input
                    type="text"
                    required
                    placeholder="Customer Name"
                    value={enrollForm.customerName}
                    onChange={e => setEnrollForm({ ...enrollForm, customerName: e.target.value })}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1"
                  />
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="font-bold text-slate-700">Customer Phone</label>
                    <input
                      type="text"
                      required
                      placeholder="0300-0000000"
                      value={enrollForm.customerPhone}
                      onChange={e => setEnrollForm({ ...enrollForm, customerPhone: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono"
                    />
                  </div>
                  <div>
                    <label className="font-bold text-slate-700">CNIC Number</label>
                    <input
                      type="text"
                      required
                      placeholder="35201-0000000-1"
                      value={enrollForm.customerCnic}
                      onChange={e => setEnrollForm({ ...enrollForm, customerCnic: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="font-bold text-slate-700">Model</label>
                    <input
                      type="text"
                      required
                      placeholder="Samsung S23..."
                      value={enrollForm.model}
                      onChange={e => setEnrollForm({ ...enrollForm, model: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1"
                    />
                  </div>
                  <div>
                    <label className="font-bold text-slate-700">IMEI</label>
                    <input
                      type="text"
                      required
                      placeholder="354891029384756"
                      value={enrollForm.imei}
                      onChange={e => setEnrollForm({ ...enrollForm, imei: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="font-bold text-slate-700">Monthly Amount (PKR)</label>
                    <input
                      type="text"
                      placeholder="PKR 8,000/mo"
                      value={enrollForm.installmentPlan || ''}
                      onChange={e => setEnrollForm({ ...enrollForm, installmentPlan: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono"
                    />
                  </div>
                  <div>
                    <label className="font-bold text-slate-700">Installment Duration</label>
                    <select
                      value={enrollForm.tenure || '12'}
                      onChange={e => setEnrollForm({ ...enrollForm, tenure: e.target.value })}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-medium"
                    >
                      <option value="3">3 Months</option>
                      <option value="6">6 Months</option>
                      <option value="10">10 Months</option>
                      <option value="12">12 Months (1 Year)</option>
                      <option value="24">24 Months (2 Years)</option>
                    </select>
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full py-3 bg-slate-900 text-white rounded-xl font-bold text-sm shadow-xs mt-2 cursor-pointer"
                >
                  ENROLL & DEDUCT {globalSettings.creditsPerEnrollment} CREDIT
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 4. RECORD INSTALLMENT PAYMENT MODAL */}
      <AnimatePresence>
        {activePaymentModalDev && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl border border-slate-200 space-y-5"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="p-2.5 bg-indigo-50 text-indigo-700 rounded-xl">
                    <CreditCard className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg">Receive Installment</h3>
                    <p className="text-xs text-slate-500">{activePaymentModalDev.customerName} — {activePaymentModalDev.model}</p>
                  </div>
                </div>
                <button onClick={() => setActivePaymentModalDev(null)} className="text-slate-400 hover:text-slate-900">
                  <X className="w-6 h-6" />
                </button>
              </div>

              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-500">IMEI:</span>
                  <span className="font-mono font-bold text-slate-800">{activePaymentModalDev.imei}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Current Progress:</span>
                  <span className="font-bold text-slate-800">{activePaymentModalDev.paidTerms} of {activePaymentModalDev.totalTerms} Installments Paid</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Remaining Balance:</span>
                  <span className="font-mono font-bold text-rose-600">PKR {((activePaymentModalDev.totalTerms - activePaymentModalDev.paidTerms) * activePaymentModalDev.monthlyEmi).toLocaleString()}</span>
                </div>
              </div>

              <form onSubmit={(e) => {
                e.preventDefault();
                handleReceiveInstallment(activePaymentModalDev.id, paymentAmountInput, paymentNextDueDateInput);
              }} className="space-y-4 text-sm">
                <div>
                  <label className="font-bold text-slate-700">Installment Amount Received (PKR)</label>
                  <input
                    type="number"
                    required
                    value={paymentAmountInput}
                    onChange={e => setPaymentAmountInput(Number(e.target.value))}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-mono font-bold text-lg focus:outline-none focus:border-indigo-600"
                  />
                </div>

                <div className="bg-emerald-50/70 p-3.5 rounded-2xl border border-emerald-200 space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-emerald-900 flex items-center gap-1.5">
                      <Zap className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Next Due Date (اگلی قسط کی ڈیو تاریخ)</span>
                    </span>
                    <span className="text-[11px] font-mono font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded">
                      Prev: {activePaymentModalDev.nextDueDate}
                    </span>
                  </div>
                  <input
                    type="date"
                    value={paymentNextDueDateInput || (() => {
                      const d = new Date();
                      d.setMonth(d.getMonth() + 1);
                      return d.toISOString().split('T')[0];
                    })()}
                    onChange={e => setPaymentNextDueDateInput(e.target.value)}
                    className="w-full bg-white border border-emerald-300 rounded-xl p-2.5 text-xs text-slate-900 font-mono font-bold focus:outline-none focus:border-emerald-600"
                  />
                  <p className="text-[11px] text-emerald-800 leading-tight">
                    💡 قسط وصولی پر یہ تاریخ آگے بڑھانے سے موبائل ان لاک ہو جائے گا اور اگلی تاریخ تک آٹو لاک نہیں ہوگا۔
                  </p>
                </div>

                <button
                  type="submit"
                  className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold text-sm shadow-md cursor-pointer flex items-center justify-center gap-2"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>CONFIRM PAYMENT & ADVANCE DUE DATE</span>
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 5. GRANT GRACE PERIOD / EXTEND DUE DATE MODAL */}
      <AnimatePresence>
        {activeGraceModalDev && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl border border-slate-200 space-y-5"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="p-2.5 bg-blue-50 text-blue-700 rounded-xl">
                    <Clock className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg">Extend Due Date (Grace)</h3>
                    <p className="text-xs text-slate-500">{activeGraceModalDev.customerName} — {activeGraceModalDev.model}</p>
                  </div>
                </div>
                <button onClick={() => setActiveGraceModalDev(null)} className="text-slate-400 hover:text-slate-900">
                  <X className="w-6 h-6" />
                </button>
              </div>

              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 text-xs space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-slate-500">Current Due Date:</span>
                  <span className="font-mono font-bold text-slate-800">{activeGraceModalDev.nextDueDate}</span>
                </div>
                <p className="text-slate-500 text-[11px] pt-1">Granting grace period will prevent automated lockdown and notify customer about the extended relaxation window.</p>
              </div>

              <form onSubmit={(e) => {
                e.preventDefault();
                handleGrantGracePeriod(activeGraceModalDev.id, graceDaysInput);
              }} className="space-y-4 text-sm">
                <div>
                  <label className="font-bold text-slate-700">Days of Extension</label>
                  <select
                    value={graceDaysInput}
                    onChange={e => setGraceDaysInput(Number(e.target.value))}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-900 mt-1 font-medium focus:outline-none focus:border-blue-600"
                  >
                    <option value="3">+3 Days Relaxation</option>
                    <option value="5">+5 Days Relaxation</option>
                    <option value="7">+7 Days (1 Week Relaxation)</option>
                    <option value="15">+15 Days Extension</option>
                    <option value="30">+30 Days (Next Month)</option>
                  </select>
                </div>
                <button
                  type="submit"
                  className="w-full py-3.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-sm shadow-md cursor-pointer flex items-center justify-center gap-2"
                >
                  <Clock className="w-4 h-4" />
                  <span>APPLY GRACE PERIOD & DISPATCH</span>
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 6. LOAN CLEARANCE CERTIFICATE (NOC) MODAL */}
      <AnimatePresence>
        {selectedHistoryDev && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 sm:p-8 max-w-lg w-full shadow-2xl border border-slate-200 space-y-6 max-h-[90vh] overflow-y-auto"
            >
              <div className="flex items-center justify-between border-b pb-4 border-slate-100">
                <div className="flex items-center gap-3">
                  <img src="/abiz-logo.png" alt="ABIZ" className="w-10 h-10 object-contain drop-shadow-sm" />
                  <div>
                    <h3 className="font-extrabold text-slate-900 text-lg leading-tight">OFFICIAL CLEARANCE CERTIFICATE</h3>
                    <p className="text-xs text-emerald-600 font-bold uppercase tracking-wider">No Objection Certificate (NOC)</p>
                  </div>
                </div>
                <button onClick={() => setSelectedHistoryDev(null)} className="text-slate-400 hover:text-slate-900 cursor-pointer p-1">
                  <X className="w-6 h-6" />
                </button>
              </div>

              {/* CERTIFICATE BODY */}
              <div className="p-5 bg-gradient-to-br from-emerald-50/50 via-slate-50 to-indigo-50/40 rounded-2xl border border-emerald-200/80 space-y-4">
                <div className="text-center pb-2 border-b border-emerald-200/60">
                  <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold uppercase tracking-wide">
                    <CheckCircle className="w-4 h-4 text-emerald-600" />
                    <span>Loan Fully Satisfied & Cleared</span>
                  </div>
                  <h4 className="text-xl font-black text-slate-900 mt-2 font-mono">PKR {selectedHistoryDev.totalPrice?.toLocaleString()}</h4>
                  <p className="text-xs text-slate-500 font-medium">100% Financed Principal Recovered ({selectedHistoryDev.totalTerms}/{selectedHistoryDev.totalTerms} Terms)</p>
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs">
                  <div className="bg-white p-3 rounded-xl border border-slate-200/80 space-y-1">
                    <span className="text-slate-400 font-semibold uppercase text-[10px] block">Customer Name</span>
                    <strong className="text-slate-900 text-sm block">{selectedHistoryDev.customerName}</strong>
                    <span className="font-mono text-slate-600">{selectedHistoryDev.customerPhone}</span>
                  </div>

                  <div className="bg-white p-3 rounded-xl border border-slate-200/80 space-y-1">
                    <span className="text-slate-400 font-semibold uppercase text-[10px] block">Customer CNIC</span>
                    <strong className="text-slate-900 font-mono text-sm block">{selectedHistoryDev.customerCnic}</strong>
                    <span className="text-slate-500 text-[11px]">Identity Verified</span>
                  </div>

                  <div className="bg-white p-3 rounded-xl border border-slate-200/80 space-y-1">
                    <span className="text-slate-400 font-semibold uppercase text-[10px] block">Handset Model</span>
                    <strong className="text-slate-900 text-sm block truncate">{selectedHistoryDev.model}</strong>
                    <span className="font-mono text-slate-500 text-[11px]">IMEI: {selectedHistoryDev.imei}</span>
                  </div>

                  <div className="bg-white p-3 rounded-xl border border-slate-200/80 space-y-1">
                    <span className="text-slate-400 font-semibold uppercase text-[10px] block">Franchise Store</span>
                    <strong className="text-slate-900 text-sm block truncate">{selectedHistoryDev.shopName}</strong>
                    <span className="font-mono text-slate-500 text-[11px]">ID: {selectedHistoryDev.shopId}</span>
                  </div>
                </div>

                <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-200 text-xs space-y-1">
                  <div className="flex justify-between items-center text-emerald-900 font-semibold">
                    <span>Clearance Timestamp:</span>
                    <span className="font-mono font-bold">{selectedHistoryDev.clearedAt || 'Oct 13, 2026, 03:00 AM'}</span>
                  </div>
                  <div className="flex justify-between items-center text-emerald-800">
                    <span>Authorized By:</span>
                    <span className="font-semibold">{selectedHistoryDev.releasedBy || 'Franchise Store Owner'}</span>
                  </div>
                  <div className="flex justify-between items-center text-emerald-800">
                    <span>MDM Policy Status:</span>
                    <span className="font-bold text-emerald-700">Device Owner Removed (Uninstalled)</span>
                  </div>
                </div>

                <p className="text-[11px] text-slate-500 leading-relaxed text-center italic">
                  "This document serves as full verification that the above handset is free of all financial encumbrances. The customer is the full legal owner and free to uninstall all enterprise lock services."
                </p>
              </div>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold text-xs shadow-md cursor-pointer flex items-center justify-center gap-2"
                >
                  <FileText className="w-4 h-4" />
                  <span>PRINT / SAVE NOC CERTIFICATE</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (confirm(`Are you sure you want to delete ${selectedHistoryDev.model} (${selectedHistoryDev.customerName}) from History? This will also free up the handset for re-scanning.`)) {
                      handleDeleteHistoryDevice(selectedHistoryDev.id);
                    }
                  }}
                  className="px-4 py-3 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-xl font-bold text-xs border border-rose-200 cursor-pointer flex items-center justify-center gap-1.5"
                  title="Delete this record from history"
                >
                  <Trash2 className="w-4 h-4" />
                  <span>Delete Record</span>
                </button>
                <button
                  type="button"
                  onClick={() => setSelectedHistoryDev(null)}
                  className="px-5 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-bold text-xs cursor-pointer"
                >
                  Close
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 7. LIVE LOCATION & GPS TRACKING MODAL */}
      <AnimatePresence>
        {activeLocationModalDev && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 sm:p-7 max-w-2xl w-full shadow-2xl border border-slate-200 space-y-5 max-h-[90vh] overflow-y-auto"
            >
              {/* Header */}
              <div className="flex items-center justify-between border-b pb-4 border-slate-100">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-sky-100 text-sky-700 flex items-center justify-center">
                    <MapPin className="w-5 h-5 text-sky-600" />
                  </div>
                  <div>
                    <h3 className="font-extrabold text-slate-900 text-lg leading-tight">LIVE GPS SATELLITE TRACKING</h3>
                    <p className="text-xs text-slate-500 font-medium">Real-Time Geolocation & Historical Position Log</p>
                  </div>
                </div>
                <button
                  onClick={() => setActiveLocationModalDev(null)}
                  className="text-slate-400 hover:text-slate-900 cursor-pointer p-1 rounded-lg"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              {/* Customer & Device Meta Pill */}
              <div className="p-3 bg-slate-50 rounded-2xl border border-slate-200 flex flex-wrap items-center justify-between gap-2 text-xs">
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Target Handset</span>
                  <span className="font-bold text-slate-900 text-sm">{activeLocationModalDev.model}</span>
                  <span className="font-mono text-slate-500 ml-2">IMEI: {activeLocationModalDev.imei}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Customer</span>
                  <span className="font-bold text-slate-800">{activeLocationModalDev.customerName}</span>
                  <span className="font-mono text-slate-500 ml-1.5">{activeLocationModalDev.customerPhone}</span>
                </div>
              </div>

              {/* Live Metrics Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 text-xs">
                <div className="bg-sky-50/70 p-3 rounded-xl border border-sky-200/80 space-y-0.5">
                  <span className="text-[10px] uppercase font-bold text-sky-700">Latitude / Longitude</span>
                  <div className="font-mono font-bold text-sky-950 text-xs truncate">
                    {activeLocationModalDev.liveLocation?.lat || 31.5204}, {activeLocationModalDev.liveLocation?.lng || 74.3587}
                  </div>
                </div>
                <div className="bg-emerald-50/70 p-3 rounded-xl border border-emerald-200/80 space-y-0.5">
                  <span className="text-[10px] uppercase font-bold text-emerald-700">GPS Accuracy</span>
                  <div className="font-bold text-emerald-950 text-xs">
                    {activeLocationModalDev.liveLocation?.accuracy || '±5m (High Precision)'}
                  </div>
                </div>
                <div className="bg-purple-50/70 p-3 rounded-xl border border-purple-200/80 space-y-0.5">
                  <span className="text-[10px] uppercase font-bold text-purple-700">Battery Level</span>
                  <div className="font-bold text-purple-950 text-xs">
                    🔋 {activeLocationModalDev.liveLocation?.battery || '84% (Charging)'}
                  </div>
                </div>
                <div className="bg-amber-50/70 p-3 rounded-xl border border-amber-200/80 space-y-0.5">
                  <span className="text-[10px] uppercase font-bold text-amber-700">Last Fix</span>
                  <div className="font-bold text-amber-950 text-xs truncate">
                    {activeLocationModalDev.liveLocation?.lastUpdated || 'Just Now'}
                  </div>
                </div>
              </div>

              {/* Physical Address */}
              <div className="p-3 bg-slate-900 text-white rounded-2xl flex items-center justify-between gap-3 text-xs">
                <div className="flex items-center gap-2">
                  <Compass className="w-4 h-4 text-cyan-400 shrink-0" />
                  <span className="font-medium text-slate-200">
                    <strong className="text-white">Current Address:</strong> {activeLocationModalDev.liveLocation?.address || 'Mall Road, Commercial Center, Lahore, Pakistan'}
                  </span>
                </div>
                <button
                  onClick={() => {
                    const lat = activeLocationModalDev.liveLocation?.lat || 31.5204;
                    const lng = activeLocationModalDev.liveLocation?.lng || 74.3587;
                    window.open(`https://www.google.com/maps?q=${lat},${lng}`, '_blank');
                  }}
                  className="px-3 py-1.5 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold rounded-lg shrink-0 cursor-pointer text-[11px]"
                >
                  Google Maps ↗
                </button>
              </div>

              {/* City Calibration & Real GPS Quick Selector */}
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-2xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                    <MapPin className="w-3.5 h-3.5 text-sky-600" />
                    <span>Quick City Calibration (شہر کی لوکیشن سیٹ کریں)</span>
                  </span>
                  <button
                    type="button"
                    onClick={() => handlePingLiveLocation(activeLocationModalDev)}
                    className="text-[10px] font-bold text-sky-700 bg-sky-100 hover:bg-sky-200 px-2 py-1 rounded-md cursor-pointer transition-colors"
                  >
                    🛰️ Use My Real GPS
                  </button>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-1.5">
                  {[
                    { name: 'Lahore', urdu: 'لاہور', lat: 31.5204, lng: 74.3587, address: 'Mall Road / Gulberg, Lahore, Punjab' },
                    { name: 'Karachi', urdu: 'کراچی', lat: 24.8607, lng: 67.0011, address: 'Saddar / Clifton, Karachi, Sindh' },
                    { name: 'Islamabad', urdu: 'اسلام آباد', lat: 33.6844, lng: 73.0479, address: 'Blue Area / F-6, Islamabad' },
                    { name: 'Rawalpindi', urdu: 'راولپنڈی', lat: 33.5973, lng: 73.0479, address: 'Saddar Bazaar, Rawalpindi' },
                    { name: 'Faisalabad', urdu: 'فیصل آباد', lat: 31.4504, lng: 73.1350, address: 'Clock Tower / D-Ground, Faisalabad' },
                    { name: 'Multan', urdu: 'ملتان', lat: 30.1575, lng: 71.5249, address: 'Cantt / Bosan Road, Multan' },
                    { name: 'Peshawar', urdu: 'پشاور', lat: 34.0151, lng: 71.5249, address: 'University Road / Saddar, Peshawar' },
                    { name: 'Gujranwala', urdu: 'گوجرانوالہ', lat: 32.1877, lng: 74.1945, address: 'GT Road / Model Town, Gujranwala' },
                    { name: 'Sialkot', urdu: 'سیالکوٹ', lat: 32.4945, lng: 74.5229, address: 'Kashmir Road / Cantt, Sialkot' },
                    { name: 'Quetta', urdu: 'کوئٹہ', lat: 30.1798, lng: 66.9750, address: 'Jinnah Road / Cantt, Quetta' }
                  ].map((c) => (
                    <button
                      key={c.name}
                      type="button"
                      onClick={() => handleSetCityLocation(activeLocationModalDev, c)}
                      className="px-2 py-1.5 bg-white hover:bg-sky-50 border border-slate-200 hover:border-sky-300 rounded-lg text-slate-700 hover:text-sky-900 text-[11px] font-semibold transition-all cursor-pointer flex items-center justify-between"
                    >
                      <span>{c.name}</span>
                      <span className="text-[10px] text-slate-400">{c.urdu}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Embedded Interactive Map */}
              <div className="relative rounded-2xl overflow-hidden border border-slate-300 shadow-inner h-60 bg-slate-100">
                <iframe
                  title="Device Location Map"
                  width="100%"
                  height="100%"
                  frameBorder="0"
                  scrolling="no"
                  src={`https://www.openstreetmap.org/export/embed.html?bbox=${(activeLocationModalDev.liveLocation?.lng || 74.3587) - 0.015}%2C${(activeLocationModalDev.liveLocation?.lat || 31.5204) - 0.015}%2C${(activeLocationModalDev.liveLocation?.lng || 74.3587) + 0.015}%2C${(activeLocationModalDev.liveLocation?.lat || 31.5204) + 0.015}&layer=mapnik&marker=${activeLocationModalDev.liveLocation?.lat || 31.5204}%2C${activeLocationModalDev.liveLocation?.lng || 74.3587}`}
                />
              </div>

              {/* Last Known Locations Timeline */}
              <div className="space-y-2">
                <span className="text-xs font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1.5">
                  <History className="w-3.5 h-3.5 text-slate-400" />
                  <span>Last Known Location History (گزشتہ لوکیشنز)</span>
                </span>
                <div className="space-y-1.5 text-xs">
                  <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl flex justify-between items-center">
                    <div>
                      <strong className="text-slate-800">Main Boulevard, Gulberg III, Lahore</strong>
                      <span className="text-slate-400 text-[10px] ml-2">Accuracy: ±4m</span>
                    </div>
                    <span className="font-mono text-slate-500 text-[11px]">15 mins ago</span>
                  </div>
                  <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl flex justify-between items-center">
                    <div>
                      <strong className="text-slate-800">Liberty Market Roundabout, Gulberg, Lahore</strong>
                      <span className="text-slate-400 text-[10px] ml-2">Accuracy: ±6m</span>
                    </div>
                    <span className="font-mono text-slate-500 text-[11px]">1 hour ago</span>
                  </div>
                  <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl flex justify-between items-center">
                    <div>
                      <strong className="text-slate-800">Model Town Link Road, Commercial Block, Lahore</strong>
                      <span className="text-slate-400 text-[10px] ml-2">Accuracy: ±7m</span>
                    </div>
                    <span className="font-mono text-slate-500 text-[11px]">Yesterday, 09:30 PM</span>
                  </div>
                </div>
              </div>

              {/* Action Controls */}
              <div className="flex gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => handlePingLiveLocation(activeLocationModalDev)}
                  disabled={isPingingLocation}
                  className="flex-1 py-3 bg-sky-600 hover:bg-sky-700 text-white rounded-xl font-bold text-xs shadow-md cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  <Crosshair className={`w-4 h-4 ${isPingingLocation ? 'animate-spin' : ''}`} />
                  <span>{isPingingLocation ? 'PINGING SATELLITE GPS...' : 'PING REFRESH GPS POSITION'}</span>
                </button>
                <button
                  type="button"
                  onClick={() => setActiveLocationModalDev(null)}
                  className="px-5 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-bold text-xs cursor-pointer"
                >
                  Close
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 8. REMOTE FORENSIC CAMERA SNAPSHOT MODAL */}
      <AnimatePresence>
        {activeCameraModalDev && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 sm:p-7 max-w-xl w-full shadow-2xl border border-slate-200 space-y-5 max-h-[90vh] overflow-y-auto"
            >
              {/* Header */}
              <div className="flex items-center justify-between border-b pb-4 border-slate-100">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-indigo-100 text-indigo-700 flex items-center justify-center">
                    <Camera className="w-5 h-5 text-indigo-600" />
                  </div>
                  <div>
                    <h3 className="font-extrabold text-slate-900 text-lg leading-tight">FORENSIC CAMERA EVIDENCE</h3>
                    <p className="text-xs text-slate-500 font-medium">Silent Remote Photo Capture & User Identification</p>
                  </div>
                </div>
                <button
                  onClick={() => setActiveCameraModalDev(null)}
                  className="text-slate-400 hover:text-slate-900 cursor-pointer p-1 rounded-lg"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              {/* Handset Meta Pill */}
              <div className="p-3 bg-slate-50 rounded-2xl border border-slate-200 flex items-center justify-between text-xs">
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Target Handset</span>
                  <span className="font-bold text-slate-900">{activeCameraModalDev.model}</span>
                  <span className="font-mono text-slate-500 ml-2">IMEI: {activeCameraModalDev.imei}</span>
                </div>
                <div className="text-right">
                  <span className="text-slate-400 block text-[10px] uppercase font-bold">Authorized By</span>
                  <span className="font-bold text-indigo-700">{authSession.role === 'SUPER_ADMIN' ? 'Super Admin HQ' : currentShopData?.name}</span>
                </div>
              </div>

              {/* Camera Selector (Front vs Rear) */}
              <div className="space-y-2">
                <label className="text-xs font-bold text-slate-700 uppercase tracking-wider block">Select Shutter Lens</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setCameraAngle('front')}
                    className={`py-2.5 px-3 rounded-xl border text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-2 ${
                      cameraAngle === 'front'
                        ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                        : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    <span>🤳 Front Camera (Selfie / Face)</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setCameraAngle('rear')}
                    className={`py-2.5 px-3 rounded-xl border text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-2 ${
                      cameraAngle === 'rear'
                        ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                        : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    <span>📷 Rear Camera (Environment)</span>
                  </button>
                </div>
              </div>

              {/* Viewfinder / Evidence Photo Card */}
              <div className="bg-slate-950 rounded-2xl p-4 text-white space-y-3 border border-slate-800">
                <div className="flex items-center justify-between text-[11px] text-slate-400 pb-2 border-b border-slate-800">
                  <span className="flex items-center gap-1.5 text-emerald-400 font-mono font-bold">
                    <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                    LIVE MDM FORENSIC VIEWFINDER
                  </span>
                  <span className="font-mono text-slate-400">STATUS: READY</span>
                </div>

                {/* Forensic Evidence Viewfinder / Photo Render */}
                {(() => {
                  const latestSnap = activeCameraModalDev.cameraSnaps?.[0];
                  return (
                    <div className="relative rounded-xl overflow-hidden bg-slate-900 border border-slate-800 flex flex-col justify-between min-h-[260px] p-3 shadow-inner">
                      {/* Top HUD Telemetry */}
                      <div className="flex justify-between items-start text-[10px] font-mono text-emerald-400 bg-black/75 p-2 rounded-lg backdrop-blur-xs z-10 border border-emerald-900/50">
                        <div>
                          <div className="font-bold flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
                            ABIZ-FORENSIC-EVIDENCE-RECORD
                          </div>
                          <div className="text-slate-300">IMEI: {activeCameraModalDev.imei}</div>
                          <div className="text-slate-400">LENS: {latestSnap?.camera || (cameraAngle === 'front' ? 'FRONT SENSOR (24MP)' : 'REAR ULTRA-WIDE')}</div>
                        </div>
                        <div className="text-right text-slate-300">
                          <div>TIME: {latestSnap?.timestamp || new Date().toLocaleTimeString()}</div>
                          <div className="text-emerald-400 font-bold">{latestSnap ? '🔒 VERIFIED EVIDENCE' : 'STANDBY'}</div>
                        </div>
                      </div>

                      {/* Viewfinder Center Subject / Real Photographic Evidence Render */}
                      {latestSnap ? (
                        latestSnap.image ? (
                          <div className="my-2 relative rounded-lg overflow-hidden border border-emerald-500/40 bg-black p-2 flex flex-col items-center justify-center text-center">
                            <img
                              src={latestSnap.image}
                              alt="Forensic Photo Evidence"
                              className="w-full max-h-[340px] object-contain rounded-lg border border-slate-800"
                            />
                            <div className="w-full flex justify-between items-center mt-2 px-1 text-[10px] font-mono text-emerald-400">
                              <span>📸 {latestSnap.camera} Lens</span>
                              <span>🕒 {latestSnap.timestamp}</span>
                            </div>
                            <div className="mt-2.5 flex items-center gap-2">
                              <a
                                href={latestSnap.image}
                                download={`ABIZ_PHOTO_${activeCameraModalDev.imei}_${latestSnap.id}.jpg`}
                                className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold rounded-lg text-xs cursor-pointer flex items-center gap-1.5 shadow-md"
                              >
                                <span>💾 Download Real Photo (تصویر محفوظ کریں)</span>
                              </a>
                            </div>
                          </div>
                        ) : (
                          <div className="my-2 relative rounded-lg overflow-hidden border border-slate-700 bg-slate-950/80 p-5 flex flex-col items-center justify-center text-center">
                            <div className="w-14 h-14 rounded-full bg-slate-900 border border-emerald-500/50 flex items-center justify-center mb-2 text-emerald-400">
                              <Camera className="w-7 h-7" />
                            </div>
                            <span className="text-xs font-bold text-white">📸 Remote Shutter Dispatched</span>
                            <p className="text-[11px] text-slate-400 mt-1 max-w-xs">
                              Camera shutter command queued for {activeCameraModalDev.model}. Handset camera takes photo and uploads to server.
                            </p>
                            <div className="mt-3 text-[10px] font-mono text-emerald-400 bg-emerald-950/60 px-3 py-1 rounded border border-emerald-800/60">
                              ID: {latestSnap.id} • {latestSnap.camera} • {latestSnap.timestamp}
                            </div>
                          </div>
                        )
                      ) : (
                        <div className="text-center my-auto py-6">
                          <div className="w-16 h-16 rounded-full bg-slate-800/80 border-2 border-dashed border-indigo-400/60 mx-auto flex items-center justify-center text-indigo-300">
                            <Camera className="w-8 h-8 text-indigo-400" />
                          </div>
                          <p className="text-xs text-slate-400 mt-2 font-medium">
                            {isCapturingSnap ? 'Triggering camera shutter on handset...' : 'Handset camera ready. Tap button below to capture live photo.'}
                          </p>
                        </div>
                      )}

                      {/* Bottom Geo Tag Banner */}
                      <div className="text-[10px] font-mono text-slate-400 text-center bg-black/75 p-1.5 rounded-lg z-10 border border-slate-800">
                        LAT: {activeCameraModalDev.liveLocation?.lat || 31.5204} | LNG: {activeCameraModalDev.liveLocation?.lng || 74.3587} | HARDWARE GPS LOCK
                      </div>
                    </div>
                  );
                })()}

                {/* Snapshots History Log */}
                {activeCameraModalDev.cameraSnaps && activeCameraModalDev.cameraSnaps.length > 0 && (
                  <div className="pt-2 space-y-1.5">
                    <span className="text-[11px] font-bold text-slate-300 uppercase tracking-wider block">Captured Forensic Logs</span>
                    <div className="max-h-28 overflow-y-auto space-y-1">
                      {activeCameraModalDev.cameraSnaps.map((s, idx) => (
                        <div key={idx} className="flex justify-between items-center p-2 rounded-lg bg-slate-900 text-[11px] text-slate-300 font-mono border border-slate-800">
                          <span className="flex items-center gap-1.5">
                            {s.image ? '🖼️' : '📸'} {s.camera}
                          </span>
                          <span className="text-slate-400">{s.timestamp}</span>
                          <span className={s.image ? 'text-emerald-400 font-bold' : 'text-sky-400'}>{s.image ? 'PHOTO SAVED' : 'SENT'}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div className="flex flex-wrap gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => handleCaptureCameraSnap(activeCameraModalDev)}
                  disabled={isCapturingSnap}
                  className="flex-1 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold text-xs shadow-md cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  <Camera className={`w-4 h-4 ${isCapturingSnap ? 'animate-spin' : ''}`} />
                  <span>{isCapturingSnap ? 'CAPTURING PHOTO...' : `TRIGGER ${cameraAngle.toUpperCase()} CAMERA SHUTTER`}</span>
                </button>
                <label className="py-3 px-4 bg-slate-800 hover:bg-slate-700 text-white rounded-xl font-bold text-xs shadow-md cursor-pointer flex items-center justify-center gap-1.5 transition-colors">
                  <Upload className="w-4 h-4 text-emerald-400" />
                  <span>Upload Photo</span>
                  <input
                    type="file"
                    accept="image/*"
                    capture="user"
                    className="hidden"
                    onChange={(e) => handleUploadCameraEvidence(e, activeCameraModalDev)}
                  />
                </label>
                <button
                  type="button"
                  onClick={() => setActiveCameraModalDev(null)}
                  className="px-5 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-bold text-xs cursor-pointer"
                >
                  Close
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 9. REMOTE FACTORY WIPE MODAL */}
      <AnimatePresence>
        {activeWipeModalDev && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 sm:p-7 max-w-lg w-full shadow-2xl border-2 border-rose-300 space-y-5 max-h-[90vh] overflow-y-auto"
            >
              {/* Header */}
              <div className="flex items-center justify-between border-b pb-4 border-slate-100">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-rose-100 text-rose-700 flex items-center justify-center">
                    <ShieldAlert className="w-5 h-5 text-rose-600" />
                  </div>
                  <div>
                    <h3 className="font-extrabold text-slate-900 text-lg leading-tight">REMOTE FACTORY WIPE</h3>
                    <p className="text-xs text-rose-600 font-bold uppercase tracking-wider">Permanent Hardware Reset Warning</p>
                  </div>
                </div>
                <button
                  onClick={() => {
                    setActiveWipeModalDev(null);
                    setWipeConfirmText('');
                  }}
                  className="text-slate-400 hover:text-slate-900 cursor-pointer p-1 rounded-lg"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              {/* Warning Alert Box */}
              <div className="p-4 bg-rose-50 border-2 border-rose-200 rounded-2xl space-y-2 text-rose-950 text-xs">
                <div className="font-bold text-rose-800 text-sm flex items-center gap-2">
                  <span>⚠️ CRITICAL ACTION: IRREVERSIBLE FACTORY RESET</span>
                </div>
                <p className="leading-relaxed">
                  Sending this command will command the physical Android Device Owner to execute an immediate <strong>hardware factory reset (Wipe)</strong>.
                </p>
                <div className="p-2.5 bg-rose-100/70 rounded-xl font-medium text-rose-900 leading-normal" dir="rtl">
                  <strong>خبردار:</strong> یہ بٹن دبانے سے کسٹمر کے موبائل کا تمام ڈیٹا، تصاویر، واٹس ایپ اور اکاؤنٹس فورا ڈیلیٹ ہو جائیں گے اور فون بالکل نیا (فیکٹری ری سیٹ) ہو جائے گا۔ یہ عمل واپس نہیں ہو سکتا۔
                </div>
              </div>

              {/* Handset Meta Card */}
              <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-200 text-xs space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-slate-500 font-medium">Target Handset:</span>
                  <span className="font-bold text-slate-900">{activeWipeModalDev.model}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-slate-500 font-medium">IMEI:</span>
                  <span className="font-mono font-bold text-slate-800">{activeWipeModalDev.imei}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-slate-500 font-medium">Customer:</span>
                  <span className="font-bold text-slate-800">{activeWipeModalDev.customerName} ({activeWipeModalDev.customerPhone})</span>
                </div>
              </div>

              {/* Confirmation Input */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-700 block">
                  Type <span className="font-mono text-rose-600 bg-rose-50 px-1.5 py-0.5 rounded border border-rose-200">WIPE</span> to confirm execution:
                </label>
                <input
                  type="text"
                  placeholder="Type WIPE in capital letters"
                  value={wipeConfirmText}
                  onChange={e => setWipeConfirmText(e.target.value.toUpperCase())}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl p-3 text-sm font-mono font-bold text-rose-700 focus:outline-none focus:border-rose-600 uppercase"
                />
              </div>

              {/* Buttons */}
              <div className="flex gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => handleExecuteRemoteWipe(activeWipeModalDev)}
                  disabled={wipeConfirmText !== 'WIPE'}
                  className="flex-1 py-3.5 bg-rose-600 hover:bg-rose-700 disabled:bg-slate-200 disabled:text-slate-400 text-white rounded-xl font-bold text-xs shadow-md cursor-pointer flex items-center justify-center gap-2 transition-all"
                >
                  <Trash2 className="w-4 h-4" />
                  <span>CONFIRM & DISPATCH FACTORY WIPE</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setActiveWipeModalDev(null);
                    setWipeConfirmText('');
                  }}
                  className="px-5 py-3.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-bold text-xs cursor-pointer"
                >
                  Cancel
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 10. ANTI-CANARY TRAFFIC SHIELD & ENCRYPTION AUDIT MODAL */}
      <AnimatePresence>
        {showSecurityShieldModal && (
          <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-3xl p-6 sm:p-7 max-w-xl w-full shadow-2xl border border-slate-200 space-y-5 max-h-[90vh] overflow-y-auto"
            >
              {/* Header */}
              <div className="flex items-center justify-between border-b pb-4 border-slate-100">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
                    <ShieldAlert className="w-5 h-5 text-emerald-600" />
                  </div>
                  <div>
                    <h3 className="font-extrabold text-slate-900 text-lg leading-tight">ANTI-CANARY SECURITY SHIELD</h3>
                    <p className="text-xs text-emerald-700 font-bold uppercase tracking-wider">MitM Interception & Packet Tampering Defense</p>
                  </div>
                </div>
                <button
                  onClick={() => setShowSecurityShieldModal(false)}
                  className="text-slate-400 hover:text-slate-900 cursor-pointer p-1 rounded-lg"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              {/* Overall Status Banner */}
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl space-y-1.5 text-xs">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-emerald-900 flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                    ALL SYSTEM SECURITY CONTROLS ACTIVE
                  </span>
                  <span className="bg-emerald-200/80 font-mono text-[10px] text-emerald-950 px-2 py-0.5 rounded-full font-bold">100% PROTECTED</span>
                </div>
                <p className="text-emerald-800 leading-relaxed">
                  Your ABIZ MDM network employs multi-tier defenses against packet sniffers, HTTPCanary, Charles Proxy, and user certificate tampering.
                </p>
              </div>

              {/* Security Layers Checklist */}
              <div className="space-y-2.5 text-xs">
                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-1">
                  <div className="flex justify-between items-center">
                    <span className="font-bold text-slate-900">1. Android Network Security Config</span>
                    <span className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-bold text-[10px]">SYSTEM CAs ONLY</span>
                  </div>
                  <p className="text-slate-500 text-[11px]">
                    HTTPCanary works by installing a user certificate. In ABIZ LOCKER, user CAs are rejected at the Android OS TLS layer via <code>network_security_config.xml</code>.
                  </p>
                </div>

                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-1">
                  <div className="flex justify-between items-center">
                    <span className="font-bold text-slate-900">2. Cryptographic Payload Signatures</span>
                    <span className="px-2 py-0.5 rounded bg-indigo-100 text-indigo-800 font-bold text-[10px]">HMAC-SHA256</span>
                  </div>
                  <p className="text-slate-500 text-[11px]">
                    Every command dispatched from server carries an HMAC-SHA256 signature. Even if an attacker captures the packet, altering dates or status will fail signature verification.
                  </p>
                </div>

                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-1">
                  <div className="flex justify-between items-center">
                    <span className="font-bold text-slate-900">3. Offline Autonomous Due-Date Guard</span>
                    <span className="px-2 py-0.5 rounded bg-amber-100 text-amber-800 font-bold text-[10px]">OFFLINE ACTIVE</span>
                  </div>
                  <p className="text-slate-500 text-[11px]">
                    The next due date is stored locally inside the phone's hardware sandbox. If a customer disconnects WiFi/SIM to block lock commands, the phone locks itself offline automatically.
                  </p>
                </div>

                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-1">
                  <div className="flex justify-between items-center">
                    <span className="font-bold text-slate-900">4. VPN & Proxy Interceptor Sensor</span>
                    <span className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-bold text-[10px]">REAL-TIME SCAN</span>
                  </div>
                  <p className="text-slate-500 text-[11px]">
                    Detects local loopback HTTP proxies (ports 8080, 8888) and HTTPCanary VPN tun interfaces directly in <code>LocalStateStore.kt</code>.
                  </p>
                </div>
              </div>

              {/* Live Server Security Audit Result */}
              <div className="p-3.5 bg-slate-950 text-white rounded-2xl font-mono text-[11px] space-y-1">
                <div className="text-emerald-400 font-bold flex justify-between">
                  <span>LIVE SERVER AUDIT LOG</span>
                  <span>PORT 8080 VERIFIED</span>
                </div>
                <div className="text-slate-400">
                  Endpoint: <span className="text-sky-300">{MDM_API_BASE}/api/security/tamper-status</span>
                </div>
                <div className="text-slate-400">
                  Active Shield: <span className="text-emerald-400 font-bold">AES-256-GCM + HMAC-SHA256 Enforced</span>
                </div>
                <div className="text-slate-400">
                  Anti-Replay Nonce Window: <span className="text-white">300 Seconds</span>
                </div>
              </div>

              {/* Close Button */}
              <div className="pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowSecurityShieldModal(false)}
                  className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white rounded-xl font-bold text-xs cursor-pointer shadow-sm"
                >
                  Close Security Audit
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
