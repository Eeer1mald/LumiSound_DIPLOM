import React from 'react';
import { MobileFrame } from './MobileFrame';
import { BottomNavigation } from './BottomNavigation';
import { 
  User, 
  Music, 
  Star, 
  Clock, 
  Settings, 
  ChevronRight,
  Award,
  TrendingUp,
  Heart
} from 'lucide-react';

interface ProfileScreenProps {
  userName: string;
  onNavigate: (screen: 'home' | 'search' | 'playlists' | 'profile') => void;
  onRatingsClick: () => void;
  stats: {
    tracksListened: number;
    tracksRated: number;
    playlistsCreated: number;
    likedTracks: number;
  };
}

export function ProfileScreen({ 
  userName, 
  onNavigate, 
  onRatingsClick,
  stats 
}: ProfileScreenProps) {
  const menuItems = [
    { 
      icon: Star, 
      label: 'Мои оценки', 
      subtitle: `${stats.tracksRated} треков оценено`,
      onClick: onRatingsClick,
      color: 'from-[#7B6DFF] to-[#FF5C6C]'
    },
    { 
      icon: Heart, 
      label: 'Любимые треки', 
      subtitle: `${stats.likedTracks} треков`,
      onClick: () => {},
      color: 'from-[#FF5C6C] to-[#FF5C6C]'
    },
    { 
      icon: Clock, 
      label: 'История прослушивания', 
      subtitle: 'Последние 30 дней',
      onClick: () => {},
      color: 'from-[#7B6DFF] to-[#7B6DFF]'
    },
    { 
      icon: Settings, 
      label: 'Настройки', 
      subtitle: 'Аккаунт и приложение',
      onClick: () => {},
      color: 'from-[#9A9AB0] to-[#9A9AB0]'
    },
  ];

  return (
    <MobileFrame>
      <div className="h-full flex flex-col bg-[#0F1020]">
        {/* Header with Gradient */}
        <div className="relative bg-gradient-to-b from-[#1A1B2E] to-[#0F1020] px-4 pt-6 pb-8 border-b border-[#2A2D3E]/20">
          <div className="absolute top-0 left-0 right-0 h-32 bg-gradient-to-br from-[#7B6DFF]/10 to-[#FF5C6C]/10 blur-3xl" />
          
          {/* Profile Avatar */}
          <div className="relative flex flex-col items-center mb-4">
            <div className="w-24 h-24 rounded-full bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] flex items-center justify-center shadow-[0_12px_32px_rgba(123,109,255,0.4)] mb-4 border-4 border-[#0F1020]">
              <span className="text-white text-3xl">
                {userName.charAt(0).toUpperCase()}
              </span>
            </div>
            <h1 className="text-[#E6E6EB] text-2xl mb-1">
              {userName}
            </h1>
            <p className="text-[#9A9AB0] text-sm">
              Меломан
            </p>
          </div>

          {/* Stats Grid */}
          <div className="grid grid-cols-4 gap-2 mt-6">
            <div className="bg-[#1A1B2E]/60 backdrop-blur-sm rounded-xl p-3 border border-[#2A2D3E]/30 text-center">
              <div className="w-8 h-8 mx-auto mb-2 rounded-lg bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center">
                <Music className="w-4 h-4 text-[#7B6DFF]" />
              </div>
              <p className="text-[#E6E6EB] text-lg mb-0.5">
                {stats.tracksListened}
              </p>
              <p className="text-[#9A9AB0] text-[10px]">
                треков
              </p>
            </div>

            <div className="bg-[#1A1B2E]/60 backdrop-blur-sm rounded-xl p-3 border border-[#2A2D3E]/30 text-center">
              <div className="w-8 h-8 mx-auto mb-2 rounded-lg bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center">
                <Star className="w-4 h-4 text-[#FF5C6C]" />
              </div>
              <p className="text-[#E6E6EB] text-lg mb-0.5">
                {stats.tracksRated}
              </p>
              <p className="text-[#9A9AB0] text-[10px]">
                оценок
              </p>
            </div>

            <div className="bg-[#1A1B2E]/60 backdrop-blur-sm rounded-xl p-3 border border-[#2A2D3E]/30 text-center">
              <div className="w-8 h-8 mx-auto mb-2 rounded-lg bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center">
                <TrendingUp className="w-4 h-4 text-[#7B6DFF]" />
              </div>
              <p className="text-[#E6E6EB] text-lg mb-0.5">
                {stats.playlistsCreated}
              </p>
              <p className="text-[#9A9AB0] text-[10px]">
                плейлистов
              </p>
            </div>

            <div className="bg-[#1A1B2E]/60 backdrop-blur-sm rounded-xl p-3 border border-[#2A2D3E]/30 text-center">
              <div className="w-8 h-8 mx-auto mb-2 rounded-lg bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center">
                <Award className="w-4 h-4 text-[#FF5C6C]" />
              </div>
              <p className="text-[#E6E6EB] text-lg mb-0.5">
                12
              </p>
              <p className="text-[#9A9AB0] text-[10px]">
                наград
              </p>
            </div>
          </div>
        </div>

        {/* Menu Items */}
        <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4">
          <div className="space-y-2 mb-20">
            {menuItems.map((item, index) => {
              const Icon = item.icon;
              return (
                <button
                  key={index}
                  onClick={item.onClick}
                  className="w-full bg-gradient-to-br from-[#1A1B2E]/80 to-[#16182A]/80 backdrop-blur-sm rounded-xl p-4 border border-[#2A2D3E]/30 active:scale-98 transition-transform flex items-center gap-4"
                >
                  {/* Icon */}
                  <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${item.color} flex items-center justify-center shadow-lg flex-shrink-0`}>
                    <Icon className="w-6 h-6 text-white" />
                  </div>

                  {/* Text */}
                  <div className="flex-1 text-left">
                    <h3 className="text-[#E6E6EB] text-sm mb-0.5">
                      {item.label}
                    </h3>
                    <p className="text-[#9A9AB0] text-xs">
                      {item.subtitle}
                    </p>
                  </div>

                  {/* Arrow */}
                  <ChevronRight className="w-5 h-5 text-[#9A9AB0] flex-shrink-0" />
                </button>
              );
            })}
          </div>

          {/* Achievement Section */}
          <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-2xl p-6 border border-[#2A2D3E]/30 mb-20">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] flex items-center justify-center">
                <Award className="w-5 h-5 text-white" />
              </div>
              <h3 className="text-[#E6E6EB] text-base">
                Достижения
              </h3>
            </div>
            <div className="grid grid-cols-4 gap-3">
              {['🎵', '⭐', '🔥', '💎', '🎧', '🌟', '🎸', '🎹'].map((emoji, i) => (
                <div 
                  key={i}
                  className={`
                    aspect-square rounded-xl flex items-center justify-center text-2xl
                    ${i < 4 
                      ? 'bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 border border-[#7B6DFF]/30' 
                      : 'bg-[#1A1B2E]/40 opacity-40 border border-[#2A2D3E]/20'
                    }
                  `}
                >
                  {emoji}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Bottom Navigation */}
        <BottomNavigation 
          activeItem="profile" 
          onItemClick={onNavigate}
        />
      </div>
    </MobileFrame>
  );
}
