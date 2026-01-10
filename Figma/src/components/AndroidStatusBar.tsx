import React from 'react';
import { Battery, Signal, Wifi } from 'lucide-react';

export function AndroidStatusBar() {
  return (
    <div className="w-full h-6 bg-[#0F1020] flex items-center justify-between px-4 text-[#E6E6EB] text-xs">
      <span>12:34</span>
      <div className="flex items-center gap-1">
        <Signal size={14} />
        <Wifi size={14} />
        <Battery size={14} />
      </div>
    </div>
  );
}