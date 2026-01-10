import React from 'react';
import { HomeScreen } from './HomeScreen';
import { SearchScreen } from './SearchScreen';
import { PlaylistsScreen } from './PlaylistsScreen';
import { ProfileScreen } from './ProfileScreen';
import { NowPlayingScreen } from './NowPlayingScreen';
import { RatingsScreen } from './RatingsScreen';
import { mockTracks, mockRatedTracks, mockPlaylists, mockUserStats } from '../utils/mockData';

/**
 * Компонент для демонстрации всех экранов приложения одновременно
 * Используется для просмотра дизайна всех экранов в одном месте
 */

export function AllScreensDemo() {
  return (
    <div className="flex flex-wrap justify-center items-start min-h-screen bg-[#0B0C10] p-4 md:p-8 gap-4 md:gap-6">
      <div className="text-center w-full mb-4">
        <h1 className="text-[#E6E6EB] text-3xl mb-2">LumiSound - Все экраны</h1>
        <p className="text-[#9A9AB0]">Демонстрация дизайна всех экранов приложения</p>
      </div>

      <div>
        <p className="text-[#9A9AB0] text-sm mb-2 text-center">Главный экран</p>
        <HomeScreen 
          userName="Александр" 
          recommendations={mockTracks}
        />
      </div>

      <div>
        <p className="text-[#9A9AB0] text-sm mb-2 text-center">Поиск</p>
        <SearchScreen
          onNavigate={() => {}}
          onTrackClick={() => {}}
          trendingTracks={mockTracks}
        />
      </div>

      <div>
        <p className="text-[#9A9AB0] text-sm mb-2 text-center">Плейлисты</p>
        <PlaylistsScreen
          onNavigate={() => {}}
          playlists={mockPlaylists}
          onPlaylistClick={() => {}}
          onCreatePlaylist={() => {}}
        />
      </div>

      <div>
        <p className="text-[#9A9AB0] text-sm mb-2 text-center">Профиль</p>
        <ProfileScreen
          userName="Александр"
          onNavigate={() => {}}
          onRatingsClick={() => {}}
          stats={mockUserStats}
        />
      </div>

      <div>
        <p className="text-[#9A9AB0] text-sm mb-2 text-center">Воспроизведение</p>
        <NowPlayingScreen
          track={mockTracks[0]}
          onClose={() => {}}
          onNavigate={() => {}}
        />
      </div>

      <div>
        <p className="text-[#9A9AB0] text-sm mb-2 text-center">Мои оценки</p>
        <RatingsScreen
          onNavigate={() => {}}
          onTrackClick={() => {}}
          ratedTracks={mockRatedTracks}
        />
      </div>
    </div>
  );
}