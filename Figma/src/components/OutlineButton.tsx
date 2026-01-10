import React from 'react';

interface OutlineButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  fullWidth?: boolean;
}

export function OutlineButton({ children, onClick, fullWidth = false }: OutlineButtonProps) {
  return (
    <button
      onClick={onClick}
      className={`${fullWidth ? 'w-full' : ''} px-6 py-3.5 rounded-2xl text-[#E6E6EB] bg-transparent border border-[#7B6DFF]/40 hover:border-[#7B6DFF] hover:bg-[#7B6DFF]/10 hover:shadow-[0_0_20px_rgba(123,109,255,0.2)] transition-all duration-300 active:scale-[0.98]`}
    >
      {children}
    </button>
  );
}