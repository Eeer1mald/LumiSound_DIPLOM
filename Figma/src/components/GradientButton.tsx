import React from 'react';

interface GradientButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  fullWidth?: boolean;
}

export function GradientButton({ children, onClick, fullWidth = false }: GradientButtonProps) {
  return (
    <button
      onClick={onClick}
      className={`${fullWidth ? 'w-full' : ''} px-6 py-3.5 rounded-2xl text-white bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] hover:shadow-[0_8px_24px_rgba(255,92,108,0.4)] transition-all duration-300 active:scale-[0.98] relative overflow-hidden group`}
    >
      <span className="relative z-10">{children}</span>
      <div className="absolute inset-0 bg-gradient-to-r from-[#FF5C6C] to-[#7B6DFF] opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
    </button>
  );
}