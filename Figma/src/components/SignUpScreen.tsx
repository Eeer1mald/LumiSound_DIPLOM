import React, { useState } from 'react';
import { MobileFrame } from './MobileFrame';
import { GradientButton } from './GradientButton';
import { InputField } from './InputField';
import logoImage from 'figma:asset/45ae6842bb44478fd4df1c7862ebe9dd59c9a04b.png';

interface SignUpScreenProps {
  onLogin: () => void;
}

export function SignUpScreen({ onLogin }: SignUpScreenProps) {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [agreed, setAgreed] = useState(false);

  return (
    <MobileFrame>
      <div className="h-full flex flex-col px-6 py-8 bg-[#0F1020] relative">
        {/* Subtle gradient background */}
        <div className="absolute top-20 right-0 w-48 h-48 bg-gradient-to-bl from-[#FF5C6C]/20 to-transparent rounded-full blur-[80px]"></div>
        
        {/* Логотип вверху */}
        <div className="flex justify-center mb-8 z-10">
          <img 
            src={logoImage} 
            alt="LumiSound" 
            className="w-48 h-auto drop-shadow-[0_0_20px_rgba(255,92,108,0.2)]"
          />
        </div>

        {/* Форма регистрации */}
        <div className="flex-1 flex flex-col z-10">
          <div className="space-y-4 mb-4">
            <InputField 
              type="text"
              placeholder="Имя пользователя"
              value={username}
              onChange={setUsername}
            />
            <InputField 
              type="email"
              placeholder="Email"
              value={email}
              onChange={setEmail}
            />
            <InputField 
              type="password"
              placeholder="Пароль"
              value={password}
              onChange={setPassword}
            />
            <InputField 
              type="password"
              placeholder="Подтверждение пароля"
              value={confirmPassword}
              onChange={setConfirmPassword}
            />
          </div>

          {/* Чекбокс */}
          <label className="flex items-start gap-3 mb-6 cursor-pointer group">
            <div className="relative flex-shrink-0 mt-0.5">
              <input 
                type="checkbox"
                checked={agreed}
                onChange={(e) => setAgreed(e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-5 h-5 border-2 border-[#2A2D3E]/60 rounded-md bg-[#1A1B2E]/60 peer-checked:border-[#7B6DFF] peer-checked:bg-[#7B6DFF] transition-all duration-200 flex items-center justify-center">
                {agreed && (
                  <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                  </svg>
                )}
              </div>
            </div>
            <span className="text-[#9A9AB0] text-xs leading-tight">
              Я принимаю условия использования и политику конфиденциальности
            </span>
          </label>

          <GradientButton fullWidth>
            Создать аккаунт
          </GradientButton>
        </div>

        {/* Вход внизу */}
        <div className="text-center pt-6 border-t border-[#2A2D3E]/40 z-10">
          <span className="text-[#9A9AB0] text-sm">Уже есть аккаунт? </span>
          <button 
            onClick={onLogin}
            className="text-[#E6E6EB] text-sm hover:text-[#FF5C6C] transition-colors"
          >
            Войти
          </button>
        </div>
      </div>
    </MobileFrame>
  );
}