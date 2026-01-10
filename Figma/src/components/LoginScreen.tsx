import React, { useState } from 'react';
import { MobileFrame } from './MobileFrame';
import { GradientButton } from './GradientButton';
import { InputField } from './InputField';
import logoImage from 'figma:asset/45ae6842bb44478fd4df1c7862ebe9dd59c9a04b.png';

interface LoginScreenProps {
  onSignUp: () => void;
}

export function LoginScreen({ onSignUp }: LoginScreenProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  return (
    <MobileFrame>
      <div className="h-full flex flex-col px-6 py-8 bg-[#0F1020] relative">
        {/* Subtle gradient background */}
        <div className="absolute top-20 left-1/2 -translate-x-1/2 w-64 h-64 bg-gradient-to-br from-[#7B6DFF]/20 to-transparent rounded-full blur-[80px]"></div>
        
        {/* Логотип вверху */}
        <div className="flex justify-center mb-12 z-10">
          <img 
            src={logoImage} 
            alt="LumiSound" 
            className="w-48 h-auto drop-shadow-[0_0_20px_rgba(123,109,255,0.2)]"
          />
        </div>

        {/* Форма входа */}
        <div className="flex-1 flex flex-col z-10">
          <div className="space-y-4 mb-6">
            <InputField 
              type="email"
              placeholder="Email или логин"
              value={email}
              onChange={setEmail}
            />
            <InputField 
              type="password"
              placeholder="Пароль"
              value={password}
              onChange={setPassword}
            />
          </div>

          <GradientButton fullWidth>
            Войти
          </GradientButton>

          <button className="text-[#7B6DFF] text-sm mt-4 hover:text-[#FF5C6C] transition-colors">
            Забыли пароль?
          </button>
        </div>

        {/* Регистрация внизу */}
        <div className="text-center pt-6 border-t border-[#2A2D3E]/40 z-10">
          <span className="text-[#9A9AB0] text-sm">Нет аккаунта? </span>
          <button 
            onClick={onSignUp}
            className="text-[#E6E6EB] text-sm hover:text-[#FF5C6C] transition-colors"
          >
            Регистрация
          </button>
        </div>
      </div>
    </MobileFrame>
  );
}