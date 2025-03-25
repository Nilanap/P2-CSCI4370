Fetch posts from users that the logged-in user follows
SELECT p.postId, p.postText, p.postDate, p.userId, "
                + "u.firstName, u.lastName "
                + "FROM post p "
                + "JOIN follow f ON p.userId = f.followeeUserId "
                + "JOIN user u ON p.userId = u.userId "
                + "WHERE f.followerUserId = ? "
                + "ORDER BY p.postDate DESC

  
This function handles the /hashtagsearch URL itself.
SELECT h.postId"
                + " FROM hashtag h, post p"
                + " WHERE h.postId = p.postId"
                + " AND h.hashTag IN (<PLACEHOLDER>)"
                + " GROUP BY h.postId"
                + " HAVING COUNT(h.hashTag) = ?

Liking/Unliking post queries:
SELECT 1 FROM heart WHERE postId = ? AND userId = ?
INSERT INTO heart (postId, userId) VALUES (?, ?);
DELETE FROM heart WHERE postId = ? AND userId = ?;

Bookmark / unbookmark:
SELECT 1 FROM bookmark WHERE userId = ? AND postId = ?
INSERT INTO bookmark (userId, postId) VALUES (?, ?)
DELETE FROM bookmark WHERE userId = ? AND postId = ?
INSERT INTO bookmark (postId, userId) VALUES (?, ?)
