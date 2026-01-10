import React from 'react';
import { Download } from 'lucide-react';
import { LumiSoundLogo } from './LumiSoundLogo';

export function LogoDownload() {
  const downloadSVG = () => {
    const svg = document.getElementById('lumisound-logo-svg');
    if (!svg) return;

    const svgData = new XMLSerializer().serializeToString(svg);
    const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' });
    const url = URL.createObjectURL(svgBlob);
    
    const link = document.createElement('a');
    link.href = url;
    link.download = 'lumisound-logo.svg';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="w-full min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 flex flex-col items-center justify-center p-8">
      <div className="max-w-4xl w-full space-y-8">
        <div className="text-center space-y-4">
          <h1 className="text-white text-4xl">LumiSound Логотип</h1>
          <p className="text-gray-400">Логотип без фона в формате SVG высокого качества</p>
        </div>

        {/* Превью на темном фоне */}
        <div className="bg-[#0B0C10] rounded-2xl p-12 flex items-center justify-center">
          <LumiSoundLogo width={600} />
        </div>

        {/* Превью на светлом фоне */}
        <div className="bg-white rounded-2xl p-12 flex items-center justify-center">
          <LumiSoundLogo width={600} />
        </div>

        {/* Превью на клетчатом фоне (показывает прозрачность) */}
        <div 
          className="rounded-2xl p-12 flex items-center justify-center"
          style={{
            backgroundImage: `
              linear-gradient(45deg, #ddd 25%, transparent 25%),
              linear-gradient(-45deg, #ddd 25%, transparent 25%),
              linear-gradient(45deg, transparent 75%, #ddd 75%),
              linear-gradient(-45deg, transparent 75%, #ddd 75%)
            `,
            backgroundSize: '20px 20px',
            backgroundPosition: '0 0, 0 10px, 10px -10px, -10px 0px',
            backgroundColor: '#fff'
          }}
        >
          <LumiSoundLogo width={600} />
        </div>

        {/* Кнопка скачивания */}
        <div className="flex justify-center">
          <button
            onClick={downloadSVG}
            className="flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-[#7B2FF7] to-[#00C6FF] text-white rounded-xl hover:shadow-[0_0_30px_rgba(123,47,247,0.6)] transition-all duration-300 active:scale-95"
          >
            <Download size={24} />
            <span className="text-lg">Скачать логотип (SVG)</span>
          </button>
        </div>

        {/* Информация */}
        <div className="bg-gray-800/50 rounded-xl p-6 text-gray-300 space-y-2">
          <h3 className="text-white text-lg mb-3">Преимущества SVG формата:</h3>
          <ul className="list-disc list-inside space-y-1 text-sm">
            <li>✨ Прозрачный фон (без белого фона)</li>
            <li>🎯 Векторный формат - масштабируется без потери качества</li>
            <li>📐 Можно использовать любого размера</li>
            <li>🎨 Сохраняет градиенты и цвета бренда</li>
            <li>💾 Маленький размер файла</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
