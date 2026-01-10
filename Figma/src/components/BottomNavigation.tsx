import React from 'react';
import { Home, Search, ListMusic, User } from 'lucide-react';

type NavItem = 'home' | 'search' | 'playlists' | 'profile';

interface BottomNavigationProps {
  activeItem?: NavItem;
  onItemClick?: (item: NavItem) => void;
}

export function BottomNavigation({ activeItem = 'home', onItemClick }: BottomNavigationProps) {
  const navItems = [
    { id: 'home' as NavItem, icon: Home, label: 'Домой' },
    { id: 'search' as NavItem, icon: Search, label: 'Поиск' },
    { id: 'playlists' as NavItem, icon: ListMusic, label: 'Плейлисты' },
    { id: 'profile' as NavItem, icon: User, label: 'Профиль' },
  ];

  return (
    <div className="w-full bg-[#0F1020]/95 backdrop-blur-lg border-t border-[#2A2D3E]/40">
      <div className="flex items-center justify-around px-4 py-3">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeItem === item.id;
          
          return (
            <button
              key={item.id}
              onClick={() => onItemClick?.(item.id)}
              className="flex flex-col items-center gap-1 min-w-[60px] group"
            >
              <div className={`p-2 rounded-xl transition-all duration-300 ${
                isActive ? 'bg-gradient-to-r from-[#7B6DFF] to-[#FF5C6C] shadow-[0_4px_16px_rgba(255,92,108,0.3)]' : ''
              }`}>
                <Icon 
                  size={22} 
                  className={`transition-all duration-300 ${
                    isActive 
                      ? 'text-white' 
                      : 'text-[#9A9AB0] group-hover:text-[#E6E6EB]'
                  }`}
                />
              </div>
              <span className={`text-xs transition-all duration-300 ${
                isActive 
                  ? 'text-[#FF5C6C]' 
                  : 'text-[#9A9AB0] group-hover:text-[#E6E6EB]'
              }`}>
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}