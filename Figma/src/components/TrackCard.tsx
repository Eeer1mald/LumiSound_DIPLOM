import React from 'react';
import { Music, Play } from 'lucide-react';
import { ImageWithFallback } from './figma/ImageWithFallback';

interface TrackCardProps {
  title: string;
  artist?: string;
  imageUrl?: string;
  onClick?: () => void;
}

export function TrackCard({ title, artist, imageUrl, onClick }: TrackCardProps) {
  return (
    <div className="w-[140px] flex-shrink-0">
      <div 
        onClick={onClick}
        className="w-[140px] h-[140px] bg-gradient-to-br from-[#1A1B2E] to-[#16182A] rounded-2xl flex items-center justify-center mb-2 relative overflow-hidden group cursor-pointer border border-[#2A2D3E]/30 hover:border-[#7B6DFF]/40 transition-all duration-300 shadow-lg hover:shadow-[0_8px_24px_rgba(123,109,255,0.2)]"
      >
        {imageUrl ? (
          <ImageWithFallback src={imageUrl} alt={title} className="w-full h-full object-cover" />
        ) : (
          <Music size={48} className="text-[#7B6DFF]/40" />
        )}
        {/* Градиентный оверлей при наведении */}
        <div className="absolute inset-0 bg-gradient-to-br from-[#7B6DFF]/0 via-[#FF5C6C]/0 to-transparent group-hover:from-[#7B6DFF]/20 group-hover:via-[#FF5C6C]/10 transition-all duration-300" />
        
        {/* Play button on hover */}
        <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
          <div className="w-12 h-12 rounded-full bg-gradient-to-br from-[#7B6DFF] to-[#FF5C6C] flex items-center justify-center shadow-lg">
            <Play className="w-6 h-6 text-white fill-white ml-0.5" />
          </div>
        </div>
      </div>
      <h4 className="text-[#E6E6EB] text-sm truncate">{title}</h4>
      {artist && <p className="text-[#9A9AB0] text-xs truncate">{artist}</p>}
    </div>
  );
}