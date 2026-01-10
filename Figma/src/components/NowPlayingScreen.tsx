import React, { useState } from 'react';
import { MobileFrame } from './MobileFrame';
import { BottomNavigation } from './BottomNavigation';
import { Play, Pause, SkipBack, SkipForward, Heart, ListPlus, ChevronDown } from 'lucide-react';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface NowPlayingScreenProps {
  track: {
    id: number;
    title: string;
    artist: string;
    cover: string;
    duration: number;
  };
  onClose: () => void;
  onNavigate: (screen: 'home' | 'search' | 'playlists' | 'profile') => void;
}

export function NowPlayingScreen({ track, onClose, onNavigate }: NowPlayingScreenProps) {
  const [isPlaying, setIsPlaying] = useState(true);
  const [currentTime, setCurrentTime] = useState(45);
  const [isLiked, setIsLiked] = useState(false);
  const [userRating, setUserRating] = useState<number | null>(null);
  const [hoveredRating, setHoveredRating] = useState<number | null>(null);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const handleRatingClick = (rating: number) => {
    setUserRating(rating);
    // Здесь будет сохранение оценки в базу данных
  };

  return (
    <MobileFrame>
      <div className="h-full flex flex-col bg-gradient-to-b from-[#1A1B2E] via-[#0F1020] to-[#0F1020]">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3">
          <button
            onClick={onClose}
            className="w-10 h-10 rounded-full bg-[#1A1B2E]/60 backdrop-blur-sm flex items-center justify-center border border-[#2A2D3E]/30 active:scale-95 transition-transform"
          >
            <ChevronDown className="w-6 h-6 text-[#E6E6EB]" />
          </button>
          <span className="text-[#9A9AB0] text-sm">Сейчас играет</span>
          <div className="w-10" />
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto px-6 pb-4">
          {/* Album Cover */}
          <div className="mt-8 mb-8">
            <div className="relative w-full aspect-square rounded-3xl overflow-hidden shadow-[0_24px_48px_rgba(0,0,0,0.6)]">
              <ImageWithFallback
                src={track.cover}
                alt={track.title}
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#0F1020]/40 to-transparent" />
            </div>
          </div>

          {/* Track Info */}
          <div className="mb-6 text-center">
            <h1 className="text-[#E6E6EB] text-2xl mb-2">
              {track.title}
            </h1>
            <p className="text-[#9A9AB0] text-base">
              {track.artist}
            </p>
          </div>

          {/* Progress Bar */}
          <div className="mb-8">
            <div className="relative h-1 bg-[#2A2D3E] rounded-full mb-2 overflow-hidden">
              <div 
                className="absolute left-0 top-0 h-full bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] rounded-full shadow-[0_0_12px_rgba(123,109,255,0.5)]"
                style={{ width: `${(currentTime / track.duration) * 100}%` }}
              />
            </div>
            <div className="flex justify-between text-xs text-[#9A9AB0]">
              <span>{formatTime(currentTime)}</span>
              <span>{formatTime(track.duration)}</span>
            </div>
          </div>

          {/* Controls */}
          <div className="flex items-center justify-center gap-6 mb-8">
            <button 
              onClick={() => setIsLiked(!isLiked)}
              className="w-12 h-12 rounded-full bg-[#1A1B2E]/60 backdrop-blur-sm flex items-center justify-center border border-[#2A2D3E]/30 active:scale-95 transition-all"
            >
              <Heart 
                className={`w-5 h-5 ${isLiked ? 'fill-[#FF5C6C] text-[#FF5C6C]' : 'text-[#9A9AB0]'}`}
              />
            </button>

            <button className="w-14 h-14 rounded-full bg-[#1A1B2E]/60 backdrop-blur-sm flex items-center justify-center border border-[#2A2D3E]/30 active:scale-95 transition-transform">
              <SkipBack className="w-6 h-6 text-[#E6E6EB]" />
            </button>

            <button 
              onClick={() => setIsPlaying(!isPlaying)}
              className="w-20 h-20 rounded-full bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] flex items-center justify-center shadow-[0_12px_32px_rgba(123,109,255,0.4)] active:scale-95 transition-transform"
            >
              {isPlaying ? (
                <Pause className="w-8 h-8 text-white fill-white" />
              ) : (
                <Play className="w-8 h-8 text-white fill-white" />
              )}
            </button>

            <button className="w-14 h-14 rounded-full bg-[#1A1B2E]/60 backdrop-blur-sm flex items-center justify-center border border-[#2A2D3E]/30 active:scale-95 transition-transform">
              <SkipForward className="w-6 h-6 text-[#E6E6EB]" />
            </button>

            <button className="w-12 h-12 rounded-full bg-[#1A1B2E]/60 backdrop-blur-sm flex items-center justify-center border border-[#2A2D3E]/30 active:scale-95 transition-transform">
              <ListPlus className="w-5 h-5 text-[#9A9AB0]" />
            </button>
          </div>

          {/* Rating Section */}
          <div className="bg-gradient-to-br from-[#1A1B2E]/80 to-[#16182A]/80 backdrop-blur-sm rounded-2xl p-6 border border-[#2A2D3E]/40 mb-20">
            <div className="text-center mb-4">
              <h3 className="text-[#E6E6EB] text-base mb-1">
                Оцените этот трек
              </h3>
              <p className="text-[#9A9AB0] text-xs">
                Это поможет нам подбирать музыку для вас
              </p>
            </div>

            {/* Rating Scale 1-10 */}
            <div className="flex justify-center gap-2 mb-3">
              {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((rating) => {
                const isActive = userRating === rating;
                const isHovered = hoveredRating !== null && hoveredRating >= rating;
                const shouldHighlight = isActive || isHovered;

                return (
                  <button
                    key={rating}
                    onClick={() => handleRatingClick(rating)}
                    onMouseEnter={() => setHoveredRating(rating)}
                    onMouseLeave={() => setHoveredRating(null)}
                    className={`
                      w-8 h-12 rounded-lg flex items-center justify-center text-sm transition-all duration-200
                      ${shouldHighlight 
                        ? 'bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] text-white shadow-[0_4px_16px_rgba(123,109,255,0.4)] scale-110' 
                        : 'bg-[#1A1B2E] text-[#9A9AB0] border border-[#2A2D3E]/40'
                      }
                      active:scale-95
                    `}
                  >
                    {rating}
                  </button>
                );
              })}
            </div>

            {userRating && (
              <div className="text-center mt-3 animate-in fade-in duration-300">
                <p className="text-[#7B6DFF] text-sm">
                  ✨ Оценка: {userRating}/10
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Bottom Navigation */}
        <BottomNavigation 
          activeItem="home" 
          onItemClick={onNavigate}
        />
      </div>
    </MobileFrame>
  );
}
