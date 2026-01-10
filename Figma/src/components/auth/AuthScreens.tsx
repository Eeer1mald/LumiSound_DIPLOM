import React, { useState } from 'react';
import { WelcomeScreen } from '../WelcomeScreen';
import { LoginScreen } from '../LoginScreen';
import { SignUpScreen } from '../SignUpScreen';

/**
 * Компонент для демонстрации экранов авторизации (Welcome, Login, Sign Up)
 * Эти экраны были созданы ранее и теперь перенесены сюда для справки
 */
export function AuthScreens() {
  return (
    <div className="flex flex-wrap justify-center items-start min-h-screen bg-[#0B0C10] p-4 md:p-8 gap-4 md:gap-6">
      <WelcomeScreen onLogin={() => {}} onSignUp={() => {}} />
      <LoginScreen onSignUp={() => {}} />
      <SignUpScreen onLogin={() => {}} />
    </div>
  );
}
