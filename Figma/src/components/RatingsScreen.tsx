import React, { useState } from 'react';
import { MobileFrame } from './MobileFrame';
import { BottomNavigation } from './BottomNavigation';
import { LumiSoundLogo } from './LumiSoundLogo';
import { Star, TrendingUp, Award } from 'lucide-react';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface RatedTrack {
  id: number;
  title: string;
  artist: string;
  cover: string;
  rating: number;
  ratedAt: string;
}

interface RatingsScreenProps {
  onNavigate: (screen: 'home' | 'search' | 'playlists' | 'profile') => void;
  onTrackClick: (trackId: number) => void;
  ratedTracks: RatedTrack[];
}

export function RatingsScreen({ onNavigate, onTrackClick, ratedTracks }: RatingsScreenProps) {
  const [filterRating, setFilterRating] = useState<number | 'all'>('all');

  const filteredTracks = filterRating === 'all' 
    ? ratedTracks 
    : ratedTracks.filter(t => t.rating === filterRating);

  const averageRating = ratedTracks.length > 0
    ? (ratedTracks.reduce((sum, t) => sum + t.rating, 0) / ratedTracks.length).toFixed(1)
    : 0;

  const highRatedCount = ratedTracks.filter(t => t.rating >= 8).length;

  return (
    <MobileFrame>
      <div className="h-full flex flex-col bg-[#0F1020]">
        {/* Header */}
        <div className="px-4 py-3 bg-[#0F1020]/80 backdrop-blur-sm border-b border-[#2A2D3E]/20">
          <div className="flex items-center justify-between mb-4">
            <div className="w-28">
              <LumiSoundLogo width={112} />
            </div>
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center border border-[#7B6DFF]/30">
              <Award className="w-5 h-5 text-[#7B6DFF]" />
            </div>
          </div>
          <h1 className="text-[#E6E6EB] text-2xl mb-1">
            Мои оценки
          </h1>
          <p className="text-[#9A9AB0] text-sm">
            Аналитика вашего музыкального вкуса
          </p>
        </div>

        {/* Stats Cards */}
        <div className="px-4 pt-4 pb-2">
          <div className="grid grid-cols-3 gap-2">
            <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-xl p-3 border border-[#2A2D3E]/30">
              <div className="flex items-center gap-1 mb-1">
                <Star className="w-3 h-3 text-[#FF5C6C]" />
                <span className="text-[#9A9AB0] text-xs">Всего</span>
              </div>
              <p className="text-[#E6E6EB] text-xl">{ratedTracks.length}</p>
            </div>

            <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-xl p-3 border border-[#2A2D3E]/30">
              <div className="flex items-center gap-1 mb-1">
                <TrendingUp className="w-3 h-3 text-[#7B6DFF]" />
                <span className="text-[#9A9AB0] text-xs">Средняя</span>
              </div>
              <p className="text-[#E6E6EB] text-xl">{averageRating}</p>
            </div>

            <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-xl p-3 border border-[#2A2D3E]/30">
              <div className="flex items-center gap-1 mb-1">
                <Award className="w-3 h-3 text-[#FF5C6C]" />
                <span className="text-[#9A9AB0] text-xs">Топ</span>
              </div>
              <p className="text-[#E6E6EB] text-xl">{highRatedCount}</p>
            </div>
          </div>
        </div>

        {/* Filter Buttons */}
        <div className="px-4 py-3">
          <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
            <button
              onClick={() => setFilterRating('all')}
              className={`
                px-4 py-2 rounded-full text-xs whitespace-nowrap transition-all
                ${filterRating === 'all'
                  ? 'bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] text-white shadow-[0_4px_12px_rgba(123,109,255,0.3)]'
                  : 'bg-[#1A1B2E] text-[#9A9AB0] border border-[#2A2D3E]/40'
                }
              `}
            >
              Все оценки
            </button>
            {[10, 9, 8, 7, 6, 5].map(rating => (
              <button
                key={rating}
                onClick={() => setFilterRating(rating)}
                className={`
                  px-4 py-2 rounded-full text-xs whitespace-nowrap transition-all
                  ${filterRating === rating
                    ? 'bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] text-white shadow-[0_4px_12px_rgba(123,109,255,0.3)]'
                    : 'bg-[#1A1B2E] text-[#9A9AB0] border border-[#2A2D3E]/40'
                  }
                `}
              >
                {rating}/10
              </button>
            ))}
          </div>
        </div>

        {/* Rated Tracks List */}
        <div className="flex-1 overflow-y-auto px-4 pb-4">
          {filteredTracks.length === 0 ? (
            <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-2xl p-8 text-center border border-[#2A2D3E]/30 mt-4">
              <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center border border-[#7B6DFF]/20">
                <Star className="w-8 h-8 text-[#7B6DFF]" />
              </div>
              <p className="text-[#E6E6EB]/80 text-sm mb-2">
                Нет оценённых треков
              </p>
              <p className="text-[#9A9AB0] text-xs">
                Начните оценивать музыку, чтобы увидеть статистику
              </p>
            </div>
          ) : (
            <div className="space-y-3 mb-20">
              {filteredTracks.map((track) => (
                <div
                  key={track.id}
                  onClick={() => onTrackClick(track.id)}
                  className="bg-gradient-to-br from-[#1A1B2E]/80 to-[#16182A]/80 backdrop-blur-sm rounded-xl p-3 border border-[#2A2D3E]/30 active:scale-98 transition-transform cursor-pointer"
                >
                  <div className="flex items-center gap-3">
                    {/* Cover */}
                    <div className="relative w-14 h-14 rounded-lg overflow-hidden flex-shrink-0 shadow-lg">
                      <ImageWithFallback
                        src={track.cover}
                        alt={track.title}
                        className="w-full h-full object-cover"
                      />
                    </div>

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                      <h3 className="text-[#E6E6EB] text-sm mb-1 truncate">
                        {track.title}
                      </h3>
                      <p className="text-[#9A9AB0] text-xs truncate">
                        {track.artist}
                      </p>
                      <p className="text-[#7B6DFF]/60 text-xs mt-1">
                        {track.ratedAt}
                      </p>
                    </div>

                    {/* Rating Badge */}
                    <div className="flex-shrink-0">
                      <div className={`
                        w-12 h-12 rounded-xl flex flex-col items-center justify-center
                        ${track.rating >= 8 
                          ? 'bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] shadow-[0_4px_12px_rgba(123,109,255,0.3)]'
                          : 'bg-[#1A1B2E] border border-[#2A2D3E]/40'
                        }
                      `}>
                        <span className={`text-lg ${track.rating >= 8 ? 'text-white' : 'text-[#E6E6EB]'}`}>
                          {track.rating}
                        </span>
                        <span className={`text-[10px] ${track.rating >= 8 ? 'text-white/70' : 'text-[#9A9AB0]'}`}>
                          /10
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Bottom Navigation */}
        <BottomNavigation 
          activeItem="profile" 
          onItemClick={onNavigate}
        />
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
