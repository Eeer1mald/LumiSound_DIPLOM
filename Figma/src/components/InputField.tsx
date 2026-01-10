import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

interface InputFieldProps {
  type?: string;
  placeholder: string;
  value?: string;
  onChange?: (value: string) => void;
}

export function InputField({ type = 'text', placeholder, value, onChange }: InputFieldProps) {
  const [showPassword, setShowPassword] = useState(false);
  const isPasswordField = type === 'password';
  const inputType = isPasswordField && showPassword ? 'text' : type;

  return (
    <div className="relative w-full">
      <input
        type={inputType}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        className="w-full px-4 py-3.5 rounded-2xl bg-[#1A1B2E]/80 border border-[#2A2D3E]/60 text-[#E6E6EB] placeholder:text-[#9A9AB0] focus:outline-none focus:border-[#7B6DFF] focus:shadow-[0_0_16px_rgba(123,109,255,0.3)] focus:bg-[#1A1B2E] transition-all duration-200 backdrop-blur-sm"
      />
      {isPasswordField && (
        <button
          type="button"
          onClick={() => setShowPassword(!showPassword)}
          className="absolute right-4 top-1/2 -translate-y-1/2 text-[#9A9AB0] hover:text-[#7B6DFF] transition-colors"
        >
          {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
        </button>
      )}
    </div>
  );
}