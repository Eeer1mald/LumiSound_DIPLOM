import React from 'react';
import { MobileFrame } from './MobileFrame';
import { BottomNavigation } from './BottomNavigation';
import { LumiSoundLogo } from './LumiSoundLogo';
import { Plus, Music2, Play } from 'lucide-react';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface Playlist {
  id: number;
  name: string;
  trackCount: number;
  cover?: string;
  gradient: string;
}

interface PlaylistsScreenProps {
  onNavigate: (screen: 'home' | 'search' | 'playlists' | 'profile') => void;
  playlists: Playlist[];
  onCreatePlaylist: () => void;
  onPlaylistClick: (playlistId: number) => void;
}

export function PlaylistsScreen({ 
  onNavigate, 
  playlists, 
  onCreatePlaylist,
  onPlaylistClick 
}: PlaylistsScreenProps) {
  return (
    <MobileFrame>
      <div className="h-full flex flex-col bg-[#0F1020]">
        {/* Header */}
        <div className="px-4 py-3 bg-[#0F1020]/80 backdrop-blur-sm border-b border-[#2A2D3E]/20">
          <div className="flex items-center justify-between mb-4">
            <div className="w-28">
              <LumiSoundLogo width={112} />
            </div>
            <button 
              onClick={onCreatePlaylist}
              className="w-10 h-10 rounded-full bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] flex items-center justify-center shadow-[0_4px_16px_rgba(123,109,255,0.3)] active:scale-95 transition-transform"
            >
              <Plus className="w-5 h-5 text-white" />
            </button>
          </div>
          <h1 className="text-[#E6E6EB] text-2xl mb-1">
            Мои плейлисты
          </h1>
          <p className="text-[#9A9AB0] text-sm">
            {playlists.length} {playlists.length === 1 ? 'плейлист' : 'плейлистов'}
          </p>
        </div>

        {/* Playlists Content */}
        <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4">
          {playlists.length === 0 ? (
            /* Empty State */
            <div className="h-full flex items-center justify-center pb-20">
              <div className="bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-2xl p-8 text-center border border-[#2A2D3E]/30 max-w-sm">
                <div className="w-20 h-20 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center border border-[#7B6DFF]/20">
                  <Music2 className="w-10 h-10 text-[#7B6DFF]" />
                </div>
                <h3 className="text-[#E6E6EB] text-lg mb-2">
                  Создайте свой первый плейлист
                </h3>
                <p className="text-[#9A9AB0] text-sm mb-6">
                  Собирайте любимые треки в плейлисты и слушайте их когда угодно
                </p>
                <button
                  onClick={onCreatePlaylist}
                  className="w-full bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] text-white py-3 px-6 rounded-full shadow-[0_8px_24px_rgba(123,109,255,0.3)] active:scale-95 transition-transform"
                >
                  Создать плейлист
                </button>
              </div>
            </div>
          ) : (
            /* Playlists Grid */
            <div className="grid grid-cols-2 gap-4 mb-20">
              {playlists.map((playlist) => (
                <div
                  key={playlist.id}
                  onClick={() => onPlaylistClick(playlist.id)}
                  className="bg-gradient-to-br from-[#1A1B2E]/80 to-[#16182A]/80 backdrop-blur-sm rounded-xl overflow-hidden border border-[#2A2D3E]/30 active:scale-95 transition-transform cursor-pointer"
                >
                  {/* Playlist Cover */}
                  <div className="relative aspect-square">
                    {playlist.cover ? (
                      <ImageWithFallback
                        src={playlist.cover}
                        alt={playlist.name}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className={`w-full h-full bg-gradient-to-br ${playlist.gradient} flex items-center justify-center`}>
                        <Music2 className="w-12 h-12 text-white/70" />
                      </div>
                    )}
                    
                    {/* Play Button Overlay */}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 hover:opacity-100 transition-opacity flex items-end justify-end p-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] flex items-center justify-center shadow-lg">
                        <Play className="w-5 h-5 text-white fill-white ml-0.5" />
                      </div>
                    </div>
                  </div>

                  {/* Playlist Info */}
                  <div className="p-3">
                    <h3 className="text-[#E6E6EB] text-sm mb-1 truncate">
                      {playlist.name}
                    </h3>
                    <p className="text-[#9A9AB0] text-xs">
                      {playlist.trackCount} {playlist.trackCount === 1 ? 'трек' : 'треков'}
                    </p>
                  </div>
                </div>
              ))}

              {/* Create New Playlist Card */}
              <div
                onClick={onCreatePlaylist}
                className="bg-gradient-to-br from-[#1A1B2E]/60 to-[#16182A]/60 backdrop-blur-sm rounded-xl overflow-hidden border-2 border-dashed border-[#2A2D3E]/50 active:scale-95 transition-transform cursor-pointer flex items-center justify-center aspect-square"
              >
                <div className="text-center">
                  <div className="w-16 h-16 mx-auto mb-3 rounded-2xl bg-gradient-to-br from-[#7B6DFF]/20 to-[#FF5C6C]/20 flex items-center justify-center border border-[#7B6DFF]/30">
                    <Plus className="w-8 h-8 text-[#7B6DFF]" />
                  </div>
                  <p className="text-[#9A9AB0] text-xs">
                    Создать
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Bottom Navigation */}
        <BottomNavigation 
          activeItem="playlists" 
          onItemClick={onNavigate}
        />
      </div>
    </MobileFrame>
  );
}
