import re

# Read the file
with open(r'c:\Lupin\lupin\src\components\Dashboard.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# Define the replacement
replacement = '''        {/* Feed - Reels Style with Snap Scroll - Full Height */}
        {selectedNav === "reels" && (
          <Reels
            allFeeds={allFeeds}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
            showSearch={showSearch}
            setShowSearch={setShowSearch}
            getFeedImageIndex={getFeedImageIndex}
            setFeedImageIndex={setFeedImageIndex}
            hasLiked={hasLiked}
            handleLike={handleLike}
            feedComments={feedComments}
            showCommentsInReels={showCommentsInReels}
            setShowCommentsInReels={setShowCommentsInReels}
            selectedFeed={selectedFeed}
            setSelectedFeed={setSelectedFeed}
            replyingTo={replyingTo}
            setReplyingTo={setReplyingTo}
            newComment={newComment}
            setNewComment={setNewComment}
            handleAddComment={handleAddComment}
          />
        )}

        {/* Ranking - Very Compact */}'''

# Use regex to find and replace the Reels section
pattern = r'(\s+)\{/\* Feed - Reels Style with Snap Scroll - Full Height \*/\}.*?\{/\* Ranking - Very Compact \*/\}'
content = re.sub(pattern, replacement, content, flags=re.DOTALL)

# Write the file back
with open(r'c:\Lupin\lupin\src\components\Dashboard.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

print("Replacement completed successfully!")
