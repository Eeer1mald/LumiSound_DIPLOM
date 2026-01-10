import React from 'react';
import { Search } from 'lucide-react';

interface SearchBarProps {
  placeholder?: string;
  value?: string;
  onChange?: (value: string) => void;
  autoFocus?: boolean;
}

export function SearchBar({ 
  placeholder = 'Поиск музыки, исполнителей, жанров', 
  value, 
  onChange,
  autoFocus = false
}: SearchBarProps) {
  return (
    <div className="relative w-full">
      <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-[#9A9AB0]" size={20} />
      <input
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        autoFocus={autoFocus}
        className="w-full pl-12 pr-4 py-3.5 rounded-2xl bg-[#1A1B2E]/80 border border-[#2A2D3E]/40 text-[#E6E6EB] placeholder:text-[#9A9AB0] focus:outline-none focus:border-[#7B6DFF] focus:shadow-[0_0_16px_rgba(123,109,255,0.25)] focus:bg-[#1A1B2E] transition-all duration-200 backdrop-blur-sm"
      />
    </div>
  );
}