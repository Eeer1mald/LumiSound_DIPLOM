import React, { useState } from 'react';
import { HomeScreen } from './components/HomeScreen';
import { NowPlayingScreen } from './components/NowPlayingScreen';
import { RatingsScreen } from './components/RatingsScreen';
import { PlaylistsScreen } from './components/PlaylistsScreen';
import { ProfileScreen } from './components/ProfileScreen';
import { SearchScreen } from './components/SearchScreen';
import { 
  mockTracks, 
  mockRatedTracks, 
  mockPlaylists, 
  mockUserStats,
  getTrackById 
} from './utils/mockData';

type Screen = 'home' | 'search' | 'playlists' | 'profile' | 'now-playing' | 'ratings';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [currentTrack, setCurrentTrack] = useState(mockTracks[0]);

  const handleNavigate = (screen: 'home' | 'search' | 'playlists' | 'profile') => {
    setCurrentScreen(screen);
  };

  const handleTrackClick = (trackId: number) => {
    const track = getTrackById(trackId);
    if (track) {
      setCurrentTrack(track);
      setCurrentScreen('now-playing');
    }
  };

  const handlePlaylistClick = (playlistId: number) => {
    console.log('Открыть плейлист:', playlistId);
    // Здесь будет логика открытия плейлиста
  };

  const handleCreatePlaylist = () => {
    console.log('Создать новый плейлист');
    // Здесь будет логика создания плейлиста
  };

  return (
    <div className="flex justify-center items-center min-h-screen bg-[#0B0C10] p-4">
      {currentScreen === 'home' && (
        <HomeScreen
          userName="Александр"
          onNavigate={handleNavigate}
          onTrackClick={handleTrackClick}
          recommendations={mockTracks}
        />
      )}

      {currentScreen === 'search' && (
        <SearchScreen
          onNavigate={handleNavigate}
          onTrackClick={handleTrackClick}
          trendingTracks={mockTracks.slice(0, 5)}
        />
      )}

      {currentScreen === 'playlists' && (
        <PlaylistsScreen
          onNavigate={handleNavigate}
          playlists={mockPlaylists}
          onPlaylistClick={handlePlaylistClick}
          onCreatePlaylist={handleCreatePlaylist}
        />
      )}

      {currentScreen === 'profile' && (
        <ProfileScreen
          userName="Александр"
          onNavigate={handleNavigate}
          onRatingsClick={() => setCurrentScreen('ratings')}
          stats={mockUserStats}
        />
      )}

      {currentScreen === 'now-playing' && (
        <NowPlayingScreen
          track={currentTrack}
          onClose={() => setCurrentScreen('home')}
          onNavigate={handleNavigate}
        />
      )}

      {currentScreen === 'ratings' && (
        <RatingsScreen
          onNavigate={handleNavigate}
          onTrackClick={handleTrackClick}
          ratedTracks={mockRatedTracks}
        />
      )}
    </div>
  );
}