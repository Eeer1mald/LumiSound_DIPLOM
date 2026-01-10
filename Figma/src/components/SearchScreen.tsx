import React, { useState } from 'react';
import { MobileFrame } from './MobileFrame';
import { BottomNavigation } from './BottomNavigation';
import { SearchBar } from './SearchBar';
import { LumiSoundLogo } from './LumiSoundLogo';
import { TrendingUp, Music2, Mic2, Disc3 } from 'lucide-react';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface Track {
  id: number;
  title: string;
  artist: string;
  cover: string;
}

interface SearchScreenProps {
  onNavigate: (screen: 'home' | 'search' | 'playlists' | 'profile') => void;
  onTrackClick: (trackId: number) => void;
  trendingTracks: Track[];
}

export function SearchScreen({ onNavigate, onTrackClick, trendingTracks }: SearchScreenProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState<'all' | 'tracks' | 'artists' | 'albums'>('all');

  const genres = [
    { name: 'Поп', gradient: 'from-[#FF5C6C] to-[#FF8A94]' },
    { name: 'Рок', gradient: 'from-[#7B6DFF] to-[#9D8FFF]' },
    { name: 'Хип-хоп', gradient: 'from-[#FF5C6C] to-[#7B6DFF]' },
    { name: 'Электроника', gradient: 'from-[#00C6FF] to-[#7B6DFF]' },
    { name: 'Джаз', gradient: 'from-[#FFB347] to-[#FF5C6C]' },
    { name: 'Классика', gradient: 'from-[#7B6DFF] to-[#FF5C6C]' },
  ];

  const filters = [
    { id: 'all', label: 'Всё', icon: Music2 },
    { id: 'tracks', label: 'Треки', icon: Music2 },
    { id: 'artists', label: 'Артисты', icon: Mic2 },
    { id: 'albums', label: 'Альбомы', icon: Disc3 },
  ];

  return (
    <MobileFrame>
      <div className="h-full flex flex-col bg-[#0F1020]">
        {/* Header */}
        <div className="px-4 py-3 bg-[#0F1020]/80 backdrop-blur-sm border-b border-[#2A2D3E]/20">
          <div className="w-28 mb-4">
            <LumiSoundLogo width={112} />
          </div>
          <h1 className="text-[#E6E6EB] text-2xl mb-4">
            Поиск
          </h1>
          <SearchBar 
            value={searchQuery} 
            onChange={setSearchQuery}
            autoFocus={true}
          />
        </div>

        {/* Filter Tabs */}
        <div className="px-4 pt-3 pb-2">
          <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
            {filters.map((filter) => {
              const Icon = filter.icon;
              return (
                <button
                  key={filter.id}
                  onClick={() => setActiveFilter(filter.id as any)}
                  className={`
                    flex items-center gap-2 px-4 py-2 rounded-full text-xs whitespace-nowrap transition-all
                    ${activeFilter === filter.id
                      ? 'bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] text-white shadow-[0_4px_12px_rgba(123,109,255,0.3)]'
                      : 'bg-[#1A1B2E] text-[#9A9AB0] border border-[#2A2D3E]/40'
                    }
                  `}
                >
                  <Icon className="w-3.5 h-3.5" />
                  {filter.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-4 pt-2 pb-4">
          {searchQuery === '' ? (
            <>
              {/* Trending Section */}
              <div className="mb-6">
                <div className="flex items-center gap-2 mb-4">
                  <TrendingUp className="w-5 h-5 text-[#FF5C6C]" />
                  <h2 className="text-[#E6E6EB] text-lg">
                    В тренде
                  </h2>
                </div>
                <div className="space-y-2">
                  {trendingTracks.map((track, index) => (
                    <div
                      key={track.id}
                      onClick={() => onTrackClick(track.id)}
                      className="bg-gradient-to-br from-[#1A1B2E]/80 to-[#16182A]/80 backdrop-blur-sm rounded-xl p-3 border border-[#2A2D3E]/30 active:scale-98 transition-transform cursor-pointer flex items-center gap-3"
                    >
                      {/* Rank */}
                      <div className={`
                        w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0
                        ${index < 3 
                          ? 'bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] text-white' 
                          : 'bg-[#1A1B2E] text-[#9A9AB0]'
                        }
                      `}>
                        <span className="text-sm">{index + 1}</span>
                      </div>

                      {/* Cover */}
                      <div className="relative w-12 h-12 rounded-lg overflow-hidden flex-shrink-0 shadow-lg">
                        <ImageWithFallback
                          src={track.cover}
                          alt={track.title}
                          className="w-full h-full object-cover"
                        />
                      </div>

                      {/* Info */}
                      <div className="flex-1 min-w-0">
                        <h3 className="text-[#E6E6EB] text-sm mb-0.5 truncate">
                          {track.title}
                        </h3>
                        <p className="text-[#9A9AB0] text-xs truncate">
                          {track.artist}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Genres Section */}
              <div className="mb-20">
                <h2 className="text-[#E6E6EB] text-lg mb-4">
                  Жанры
                </h2>
                <div className="grid grid-cols-2 gap-3">
                  {genres.map((genre, index) => (
                    <div
                      key={index}
                      className={`
                        relative h-24 rounded-xl overflow-hidden cursor-pointer 
                        active:scale-95 transition-transform
                        bg-gradient-to-br ${genre.gradient}
                      `}
                    >
                      <div className="absolute inset-0 bg-black/20" />
                      <div className="relative h-full flex items-end p-4">
                        <h3 className="text-white text-base">
                          {genre.name}
                        </h3>
                      </div>
                      {/* Decorative pattern */}
                      <div className="absolute top-2 right-2 w-16 h-16 rounded-full bg-white/10 blur-xl" />
                    </div>
                  ))}
                </div>
              </div>
            </>
          ) : (
            /* Search Results */
            <div className="mb-20">
              <p className="text-[#9A9AB0] text-sm mb-4">
                Результаты для "{searchQuery}"
              </p>
              <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-2xl p-8 text-center border border-[#2A2D3E]/30">
                <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center border border-[#7B6DFF]/20">
                  <Music2 className="w-8 h-8 text-[#7B6DFF]" />
                </div>
                <p className="text-[#E6E6EB]/80 text-sm mb-2">
                  Поиск в разработке
                </p>
                <p className="text-[#9A9AB0] text-xs">
                  Скоро вы сможете искать треки, артистов и альбомы
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Bottom Navigation */}
        <BottomNavigation 
          activeItem="search" 
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
