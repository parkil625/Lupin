import { Card } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { Input } from "../ui/input";
import { ScrollArea } from "../ui/scroll-area";
import {
  Heart,
  MessageCircle,
  Search,
  X,
  Sparkles,
  Send,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { Feed, Comment } from "@/types/dashboard.types";

interface ReelsProps {
  allFeeds: Feed[];
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  showSearch: boolean;
  setShowSearch: (show: boolean) => void;
  getFeedImageIndex: (feedId: number) => number;
  setFeedImageIndex: (feedId: number, index: number) => void;
  hasLiked: (feedId: number) => boolean;
  handleLike: (feedId: number) => void;
  feedComments: { [key: number]: Comment[] };
  showCommentsInReels: boolean;
  setShowCommentsInReels: (show: boolean) => void;
  selectedFeed: Feed | null;
  setSelectedFeed: (feed: Feed | null) => void;
  replyingTo: number | null;
  setReplyingTo: (id: number | null) => void;
  newComment: string;
  setNewComment: (comment: string) => void;
  handleAddComment: (feedId: number) => void;
  feedContainerRef: React.RefObject<HTMLDivElement>;
}

export default function Reels({
  allFeeds,
  searchQuery,
  setSearchQuery,
  showSearch,
  setShowSearch,
  getFeedImageIndex,
  setFeedImageIndex,
  hasLiked,
  handleLike,
  feedComments,
  showCommentsInReels,
  setShowCommentsInReels,
  selectedFeed,
  setSelectedFeed,
  replyingTo,
  setReplyingTo,
  newComment,
  setNewComment,
  handleAddComment,
  feedContainerRef,
}: ReelsProps) {
  return (
    <div className="h-full relative flex items-center justify-center">
      <div
        ref={feedContainerRef}
        className="h-full w-full overflow-y-scroll snap-y snap-mandatory scrollbar-hide"
        style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
      >
        <style>{`
          .scrollbar-hide::-webkit-scrollbar {
            display: none;
          }
        `}</style>

        <div className="flex flex-col items-center">
          {/* Search Overlay */}
          {showSearch && (
            <div
              className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40"
              onClick={() => setShowSearch(false)}
            >
              <div
                className="absolute top-8 left-1/2 -translate-x-1/2 w-full max-w-2xl px-4"
                onClick={(e) => e.stopPropagation()}
              >
                <div className="relative">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-600" />
                  <Input
                    type="text"
                    placeholder="피드 검색..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    autoFocus
                    className="pl-12 pr-12 h-14 rounded-2xl backdrop-blur-2xl bg-white/80 border border-gray-300 font-medium shadow-2xl"
                  />
                  <button
                    onClick={() => {
                      setSearchQuery("");
                      setShowSearch(false);
                    }}
                    className="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors"
                  >
                    <X className="w-4 h-4 text-gray-600" />
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Feed Cards */}
          {allFeeds
            .filter(
              (feed) =>
                feed.content.toLowerCase().includes(searchQuery.toLowerCase()) ||
                feed.author.toLowerCase().includes(searchQuery.toLowerCase()) ||
                feed.activity.toLowerCase().includes(searchQuery.toLowerCase())
            )
            .map((feed) => {
              const currentIndex = getFeedImageIndex(feed.id);
              const liked = hasLiked(feed.id);
              return (
                <div
                  key={feed.id}
                  className="snap-start snap-always flex-shrink-0 w-full h-screen flex items-center justify-center py-4"
                >
                  <div className="w-[400px] h-full max-h-[95vh]">
                    <Card className="h-full overflow-hidden backdrop-blur-2xl bg-white/70 border border-gray-200 shadow-2xl relative flex flex-col">
                      {/* Image Carousel */}
                      <div className="relative flex-[2]">
                        <img
                          src={feed.images[currentIndex] || feed.images[0]}
                          alt={feed.activity}
                          className="w-full h-full object-cover"
                        />

                        {feed.images.length > 1 && (
                          <>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setFeedImageIndex(
                                  feed.id,
                                  Math.max(0, currentIndex - 1)
                                );
                              }}
                              className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70"
                            >
                              <ChevronLeft className="w-5 h-5" />
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setFeedImageIndex(
                                  feed.id,
                                  Math.min(feed.images.length - 1, currentIndex + 1)
                                );
                              }}
                              className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70"
                            >
                              <ChevronRight className="w-5 h-5" />
                            </button>
                            <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                              {feed.images.map((_, idx) => (
                                <div
                                  key={idx}
                                  className={`w-1.5 h-1.5 rounded-full ${
                                    idx === currentIndex ? "bg-white" : "bg-white/50"
                                  }`}
                                ></div>
                              ))}
                            </div>
                          </>
                        )}

                        {/* Author Info */}
                        <div className="absolute top-4 left-4 flex items-center gap-3 backdrop-blur-xl bg-white/20 rounded-full px-4 py-2 border border-white/30">
                          <Avatar className="w-8 h-8 border-2 border-white">
                            <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white font-black text-sm">
                              {feed.avatar}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <div className="text-white text-xs font-bold">
                              {feed.author}
                            </div>
                            <div className="text-white/80 text-xs">{feed.time}</div>
                          </div>
                        </div>

                        {/* Right Actions */}
                        <div className="absolute right-4 bottom-4 flex flex-col gap-4">
                          <button
                            onClick={() => handleLike(feed.id)}
                            className="flex flex-col items-center gap-1 group"
                          >
                            <div className="w-12 h-12 rounded-full backdrop-blur-xl bg-white/20 border border-white/30 flex items-center justify-center hover:scale-110 transition-transform">
                              <Heart
                                className={`w-5 h-5 ${
                                  liked ? "fill-red-500 text-red-500" : "text-white"
                                }`}
                              />
                            </div>
                            <span className="text-white text-xs font-bold">
                              {feed.likes}
                            </span>
                          </button>

                          <button
                            className="flex flex-col items-center gap-1 group"
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedFeed(feed);
                              setShowCommentsInReels(true);
                            }}
                          >
                            <div className="w-12 h-12 rounded-full backdrop-blur-xl bg-white/20 border border-white/30 flex items-center justify-center hover:scale-110 transition-transform">
                              <MessageCircle className="w-5 h-5 text-white" />
                            </div>
                            <span className="text-white text-xs font-bold">
                              {feed.comments}
                            </span>
                          </button>
                        </div>
                      </div>

                      {/* Content */}
                      <div className="p-6 space-y-3 flex-1 overflow-auto">
                        <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1 font-bold border-0">
                          <Sparkles className="w-3 h-3 mr-1" />
                          +{feed.points}
                        </Badge>

                        <p className="text-gray-700 font-medium text-sm leading-relaxed">
                          {feed.content}
                        </p>

                        <div className="flex gap-2 flex-wrap">
                          <Badge className="bg-white border border-gray-300 text-gray-700 px-3 py-1 font-bold text-xs">
                            {feed.duration}
                          </Badge>
                          {Object.entries(feed.stats).map(([key, value]) => (
                            <Badge
                              key={key}
                              className="bg-red-50 border border-red-200 text-[#C93831] px-3 py-1 font-bold text-xs"
                            >
                              {value}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    </Card>
                  </div>
                </div>
              );
            })}
        </div>
      </div>