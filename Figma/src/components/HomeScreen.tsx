import React, { useState } from 'react';
import { MobileFrame } from './MobileFrame';
import { SearchBar } from './SearchBar';
import { TrackCard } from './TrackCard';
import { BottomNavigation } from './BottomNavigation';
import { LumiSoundLogo } from './LumiSoundLogo';

interface Track {
  id: number;
  title: string;
  artist: string;
  cover?: string;
}

interface HomeScreenProps {
  userName?: string;
  onNavigate?: (screen: 'home' | 'search' | 'playlists' | 'profile') => void;
  onTrackClick?: (trackId: number) => void;
  recommendations?: Track[];
}

export function HomeScreen({ 
  userName = 'Пользователь', 
  onNavigate,
  onTrackClick,
  recommendations = []
}: HomeScreenProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeNav, setActiveNav] = useState<'home' | 'search' | 'playlists' | 'profile'>('home');

  const handleNavClick = (item: 'home' | 'search' | 'playlists' | 'profile') => {
    setActiveNav(item);
    if (onNavigate) {
      onNavigate(item);
    }
  };

  return (
    <MobileFrame>
      <div className="h-full flex flex-col bg-[#0F1020]">
        {/* Top App Bar */}
        <div className="flex items-center justify-between px-4 py-3 bg-[#0F1020]/80 backdrop-blur-sm border-b border-[#2A2D3E]/20">
          <div className="w-28">
            <LumiSoundLogo width={112} />
          </div>
          {/* Avatar */}
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center text-[#E6E6EB] border border-[#7B6DFF]/30 shadow-lg cursor-pointer active:scale-95 transition-transform">
            <span className="text-sm">{userName.charAt(0).toUpperCase()}</span>
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto px-4 pb-4">
          {/* Greeting */}
          <div className="pt-6 pb-4">
            <h1 className="text-[#E6E6EB] text-2xl mb-1">
              Привет, {userName}!
            </h1>
            <p className="text-[#9A9AB0]">
              Что послушаем сегодня?
            </p>
          </div>

          {/* Search Bar */}
          <div className="mb-6">
            <SearchBar value={searchQuery} onChange={setSearchQuery} />
          </div>

          {/* Recommendations Section */}
          {recommendations.length > 0 && (
            <div className="mb-6">
              <h2 className="text-[#E6E6EB] text-lg mb-4">
                Рекомендации для вас
              </h2>
              <div className="flex gap-4 overflow-x-auto pb-2 -mx-4 px-4 scrollbar-hide">
                {recommendations.map((track) => (
                  <TrackCard
                    key={track.id}
                    title={track.title}
                    artist={track.artist}
                    imageUrl={track.cover}
                    onClick={() => onTrackClick && onTrackClick(track.id)}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Placeholder Message */}
          <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-2xl p-6 text-center border border-[#2A2D3E]/30 shadow-lg">
            <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] flex items-center justify-center opacity-80 shadow-[0_8px_24px_rgba(123,109,255,0.3)]">
              <svg 
                className="w-8 h-8 text-white" 
                fill="none" 
                viewBox="0 0 24 24" 
                stroke="currentColor"
              >
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  strokeWidth={2} 
                  d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" 
                />
              </svg>
            </div>
            <p className="text-[#E6E6EB]/80 text-sm mb-2">
              Скоро здесь появятся ваши рекомендации
            </p>
            <p className="text-[#9A9AB0] text-xs">
              Слушайте музыку и оценивайте треки, чтобы мы могли подобрать для вас идеальный плейлист
            </p>
          </div>

          {/* Recent Section (Optional placeholder) */}
          <div className="mt-6 mb-20">
            <h2 className="text-[#E6E6EB] text-lg mb-4">
              Недавно прослушанное
            </h2>
            <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-2xl p-8 text-center border border-[#2A2D3E]/30">
              <p className="text-[#9A9AB0] text-sm">
                Здесь будет история ваших прослушиваний
              </p>
            </div>
          </div>
        </div>

        {/* Bottom Navigation */}
        <BottomNavigation activeItem={activeNav} onItemClick={handleNavClick} />
      </div>

      <style jsx>{`
        .scrollbar-hide::-webkit-scrollbar {
          display: none;
        }
        .scrollbar-hide {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
      `}</style>
    </MobileFrame>
  );
}