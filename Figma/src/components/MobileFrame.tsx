import React from 'react';
import { AndroidStatusBar } from './AndroidStatusBar';

interface MobileFrameProps {
  children: React.ReactNode;
}

export function MobileFrame({ children }: MobileFrameProps) {
  return (
    <div className="w-[360px] h-[800px] bg-[#0F1020] overflow-hidden rounded-3xl flex flex-col relative shadow-[0_8px_32px_0_rgba(123,109,255,0.15),0_0_0_1px_rgba(123,109,255,0.1)]">
      <AndroidStatusBar />
      <div className="flex-1 overflow-y-auto">
        {children}
      </div>
    </div>
  );
}