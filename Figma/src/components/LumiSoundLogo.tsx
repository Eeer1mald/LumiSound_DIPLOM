import React from 'react';

interface LumiSoundLogoProps {
  width?: number;
  height?: number;
}

export function LumiSoundLogo({ width = 600, height }: LumiSoundLogoProps) {
  const aspectRatio = 4.5; // Примерное соотношение сторон логотипа
  const calculatedHeight = height || width / aspectRatio;

  return (
    <svg
      id="lumisound-logo-svg"
      width={width}
      height={calculatedHeight}
      viewBox="0 0 900 200"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        {/* Градиент для иконки волны */}
        <linearGradient id="wave-gradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#7B2FF7" />
          <stop offset="50%" stopColor="#5A6FDB" />
          <stop offset="100%" stopColor="#00C6FF" />
        </linearGradient>
        
        {/* Градиент для текста */}
        <linearGradient id="text-gradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#7B2FF7" />
          <stop offset="30%" stopColor="#6B4BC9" />
          <stop offset="60%" stopColor="#4A8ED8" />
          <stop offset="100%" stopColor="#00C6FF" />
        </linearGradient>
      </defs>

      {/* Иконка волны */}
      <g transform="translate(20, 50)">
        {/* Левая часть волны */}
        <path
          d="M 0 50 Q 15 20, 30 35 Q 45 50, 55 45 Q 65 40, 70 50 Q 75 60, 85 55 Q 95 50, 100 60 L 100 80 Q 95 70, 85 75 Q 75 80, 70 70 Q 65 60, 55 65 Q 45 70, 30 55 Q 15 40, 0 70 Z"
          fill="url(#wave-gradient)"
          opacity="0.9"
        />
        
        {/* Правая часть волны */}
        <path
          d="M 0 50 Q 15 80, 30 65 Q 45 50, 55 55 Q 65 60, 70 50 Q 75 40, 85 45 Q 95 50, 100 40 L 100 20 Q 95 30, 85 25 Q 75 20, 70 30 Q 65 40, 55 35 Q 45 30, 30 45 Q 15 60, 0 30 Z"
          fill="url(#wave-gradient)"
          opacity="0.8"
        />

        {/* Центральные круги для объема */}
        <circle cx="25" cy="50" r="18" fill="#7B2FF7" opacity="0.6" />
        <circle cx="50" cy="50" r="14" fill="#5A6FDB" opacity="0.5" />
        <circle cx="75" cy="50" r="16" fill="#00C6FF" opacity="0.6" />
      </g>

      {/* Текст LumiSound */}
      <g transform="translate(160, 55)">
        <text
          x="0"
          y="65"
          fontSize="85"
          fontFamily="Inter, system-ui, -apple-system, sans-serif"
          fontWeight="700"
          fill="url(#text-gradient)"
          letterSpacing="-2"
        >
          LumiSound
        </text>
      </g>
    </svg>
  );
}
