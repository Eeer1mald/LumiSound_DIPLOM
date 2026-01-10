import React from 'react';
import { MobileFrame } from './MobileFrame';
import { GradientButton } from './GradientButton';
import { OutlineButton } from './OutlineButton';
import logoImage from 'figma:asset/45ae6842bb44478fd4df1c7862ebe9dd59c9a04b.png';

interface WelcomeScreenProps {
  onLogin: () => void;
  onSignUp: () => void;
}

export function WelcomeScreen({ onLogin, onSignUp }: WelcomeScreenProps) {
  return (
    <MobileFrame>
      <div className="h-full flex flex-col items-center justify-between px-6 py-12 relative bg-[#0F1020]">
        {/* Неоновое свечение за логотипом */}
        <div className="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 bg-gradient-to-br from-[#7B6DFF]/40 via-[#FF5C6C]/30 to-transparent rounded-full blur-[100px]"></div>
        
        {/* Центральный контент */}
        <div className="flex-1 flex flex-col items-center justify-center z-10">
          <img 
            src={logoImage} 
            alt="LumiSound" 
            className="w-64 h-auto mb-6 drop-shadow-[0_0_30px_rgba(123,109,255,0.3)]"
          />
          <p className="text-[#E6E6EB]/70 text-center px-8 tracking-wide">
            Музыка, что звучит светом
          </p>
        </div>

        {/* Кнопки внизу */}
        <div className="w-full space-y-4 z-10">
          <GradientButton fullWidth onClick={onLogin}>
            Войти
          </GradientButton>
          <OutlineButton fullWidth onClick={onSignUp}>
            Регистрация
          </OutlineButton>
        </div>
      </div>
    </MobileFrame>
  );
}