/**
 * Моковые данные для демонстрации приложения LumiSound
 */

export interface Track {
  id: number;
  title: string;
  artist: string;
  cover: string;
  duration: number;
}

export interface RatedTrack extends Track {
  rating: number;
  ratedAt: string;
}

export interface Playlist {
  id: number;
  name: string;
  trackCount: number;
  cover?: string;
  gradient: string;
}

export const mockTracks: Track[] = [
  {
    id: 1,
    title: 'Midnight Dreams',
    artist: 'Luna Eclipse',
    cover: 'https://images.unsplash.com/photo-1693434998054-2784e49ca563?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhbGJ1bSUyMGNvdmVyJTIwbXVzaWN8ZW58MXx8fHwxNzY3Nzc2NzU5fDA&ixlib=rb-4.1.0&q=80&w=1080',
    duration: 245,
  },
  {
    id: 2,
    title: 'Neon Lights',
    artist: 'Synthwave Collective',
    cover: 'https://images.unsplash.com/photo-1725457878302-295aabe05895?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhYnN0cmFjdCUyMG5lb24lMjBncmFkaWVudHxlbnwxfHx8fDE3Njc3ODc1NTN8MA&ixlib=rb-4.1.0&q=80&w=1080',
    duration: 198,
  },
  {
    id: 3,
    title: 'Vinyl Memories',
    artist: 'The Retro Band',
    cover: 'https://images.unsplash.com/photo-1619983081563-430f63602796?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx2aW55bCUyMHJlY29yZCUyMHB1cnBsZXxlbnwxfHx8fDE3Njc3ODc1NTN8MA&ixlib=rb-4.1.0&q=80&w=1080',
    duration: 210,
  },
  {
    id: 4,
    title: 'Electric Storm',
    artist: 'DJ Voltage',
    cover: 'https://images.unsplash.com/photo-1690013429722-87852aae164b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjb25jZXJ0JTIwc3RhZ2UlMjBsaWdodHN8ZW58MXx8fHwxNzY3NzgxOTkxfDA&ixlib=rb-4.1.0&q=80&w=1080',
    duration: 180,
  },
  {
    id: 5,
    title: 'Studio Sessions',
    artist: 'The Producers',
    cover: 'https://images.unsplash.com/photo-1610716632424-4d45990bcd48?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtdXNpYyUyMHByb2R1Y2VyfGVufDF8fHx8MTc2Nzc4NzU1NHww&ixlib=rb-4.1.0&q=80&w=1080',
    duration: 220,
  },
  {
    id: 6,
    title: 'Sunset Vibes',
    artist: 'Coastal Waves',
    cover: 'https://images.unsplash.com/photo-1760298783556-2f3945b8f435?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxzdW5zZXQlMjBvY2VhbiUyMGdyYWRpZW50fGVufDF8fHx8MTc2Nzc4NzU1NHww&ixlib=rb-4.1.0&q=80&w=1080',
    duration: 195,
  },
];

export const mockRatedTracks: RatedTrack[] = [
  { ...mockTracks[0], rating: 9, ratedAt: '5 января' },
  { ...mockTracks[1], rating: 8, ratedAt: '4 января' },
  { ...mockTracks[2], rating: 10, ratedAt: '3 января' },
  { ...mockTracks[4], rating: 7, ratedAt: '2 января' },
];

export const mockPlaylists: Playlist[] = [
  {
    id: 1,
    name: 'Вечерний чилл',
    trackCount: 24,
    cover: mockTracks[5].cover,
    gradient: 'from-[#7B6DFF] to-[#FF5C6C]',
  },
  {
    id: 2,
    name: 'Тренировка',
    trackCount: 18,
    gradient: 'from-[#FF5C6C] to-[#FF8A94]',
  },
  {
    id: 3,
    name: 'Фокус',
    trackCount: 32,
    gradient: 'from-[#00C6FF] to-[#7B6DFF]',
  },
];

export const mockUserStats = {
  tracksListened: 247,
  tracksRated: mockRatedTracks.length,
  playlistsCreated: mockPlaylists.length,
  likedTracks: 84,
};

/**
 * Утилита для получения трека по ID
 */
export function getTrackById(id: number): Track | undefined {
  return mockTracks.find(track => track.id === id);
}

/**
 * Утилита для получения плейлиста по ID
 */
export function getPlaylistById(id: number): Playlist | undefined {
  return mockPlaylists.find(playlist => playlist.id === id);
}

/**
 * Утилита для форматирования времени (секунды -> мм:сс)
 */
export function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}
