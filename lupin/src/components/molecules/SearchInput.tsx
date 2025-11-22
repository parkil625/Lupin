/**
 * SearchInput - 검색 입력 컴포넌트
 * Molecule: Input + Search Icon + Clear Button + Autocomplete
 */

import { useState } from "react";
import { Search, X } from "lucide-react";

interface SearchInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  suggestions?: string[];
}

export default function SearchInput({
  value,
  onChange,
  placeholder = "검색...",
  className = "",
  suggestions = [],
}: SearchInputProps) {
  const [isFocused, setIsFocused] = useState(false);

  // 입력값과 매칭되는 제안 필터링 (최대 8개)
  const filteredSuggestions = value
    ? suggestions
        .filter((s) => s.toLowerCase().includes(value.toLowerCase()))
        .slice(0, 8)
    : [];

  const showSuggestions = isFocused && value && filteredSuggestions.length > 0;

  return (
    <div className={`relative ${className}`}>
      <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-6 h-6 text-gray-500 z-30 pointer-events-none" />
      <input
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={(e) => {
          setIsFocused(true);
          e.target.style.boxShadow = '0 0 20px 5px rgba(201, 56, 49, 0.35)';
        }}
        onBlur={(e) => {
          setTimeout(() => setIsFocused(false), 200);
          e.target.style.boxShadow = '';
        }}
        className="w-full h-10 pl-12 pr-14 text-sm backdrop-blur-xl bg-white/40 border-2 border-gray-300 rounded-full shadow-sm transition-all duration-300 focus:border-[#C93831] focus:outline-none focus:ring-0"
        style={{ paddingLeft: '44px', outline: 'none' }}
      />
      {value && (
        <button
          onClick={() => onChange("")}
          className="absolute right-3 top-1/2 -translate-y-1/2 w-6 h-6 rounded-full bg-gray-300/80 hover:bg-gray-400/80 flex items-center justify-center z-10"
        >
          <X className="w-6 h-6 text-gray-600" />
        </button>
      )}

      {/* Autocomplete Suggestions */}
      {showSuggestions && (
        <div className="absolute top-full left-0 right-0 mt-2 bg-white/90 backdrop-blur-xl rounded-2xl shadow-lg border border-gray-200/50 overflow-hidden z-50">
          {filteredSuggestions.map((suggestion, index) => (
            <button
              key={index}
              onClick={() => {
                onChange(suggestion);
                setIsFocused(false);
              }}
              className="w-full px-4 py-2.5 flex items-center gap-3 hover:bg-gray-100/80 transition-colors text-left"
            >
              <Search className="w-4 h-4 text-gray-400 flex-shrink-0" />
              <span className="text-sm text-gray-700 truncate">{suggestion}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
