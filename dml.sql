-- USed in HomeController to retrieve all posts by users the logged-in user follows.
-- URL: http://localhost:8081/
-- The "?" is replaced with the  logged-in user's userId.
SELECT p.postId, p.postText, p.postDate, p.userId, u.firstName, u.lastName FROM post p JOIN follow f ON p.userId = f.followeeUserId JOIN user u ON p.userId = u.userId WHERE f.followerUserId = ? ORDER BY p.postDate DESC;

-- Used in HomeController to implement creating posts.
-- URL: http://localhost:8081
-- The three "?"s are replaced with data provided by the user when creating a post.
INSERT INTO post (userId, postDate, postText) VALUES (?, ?, ?);

-- Used in HomeController to create hashtags when creating posts.
-- URL: http://localhost:8081
-- The two "?"s are replaced with a hashtag in the created post and the created post's postId, respectively.
INSERT INTO hashtag (hashTag, postId) VALUES (?, ?);

-- Used in HomeController to count the hearts associated with one post.
-- URL: http://localhost:8081
-- The "?" is replaced with the postId of the aforementioned post.
SELECT COUNT(*) AS heartsCount FROM heart WHERE postId = ?;

-- Used in HomeController to count the comments associated with one post.
-- URL: http://localhost:8081
-- The "?" is replaced with the postId of the aforementioned post.
SELECT COUNT(*) AS commentsCount FROM comment WHERE postId = ?;

-- Used in HomeController and in PostController to determine is a post is liked by the logged-in user.
-- URLs: http://localhost:8081, http://localhost:8081/{postId}/heart/{isAdd}
-- The "?"s are replaced with the postId of the aforementioned post and the logged-in user's userId, respectively.
SELECT 1 FROM heart WHERE postId = ? AND userId = ?;

-- Used in HomeController, PostController, and UserService to determine is a post is bookmarked by the logged-in user.
-- URL: http://localhost:8081, http://localhost:8081/{postId}/bookmark/{isAdd}
-- The "?"s are replaced with the postId of the aforementioned post and the logged-in user's userId, respectively.
SELECT 1 FROM bookmark WHERE postId = ? AND userId = ?;

-- Used in PostController to like and unlike posts.
-- URL: http://localhost:8081/{postId}/heart/{isAdd}
-- The "?"s in both statements are replaced with the postId of the aforementioned post and the logged-in user's userId, respectively.
INSERT INTO heart (postId, userId) VALUES (?, ?);
DELETE FROM heart WHERE postId = ? AND userId = ?;

-- Used in PostController, ProfileController, and UserService to bookmark and unbookmark posts.
-- URL: http://localhost:8081/{postId}/bookmark/{isAdd}
-- The "?"s in both statements are replaced with the postId of the aforementioned post and the logged-in user's userId, respectively.
INSERT INTO bookmark (userId, postId) VALUES (?, ?);
DELETE FROM bookmark WHERE userId = ? AND postId = ?;

-- Used in PeopleService and UserService to check if the logged-in user follows the given user.
-- URL: http://localhost:8081/people
-- The "?"s are replaced with the logged-in user's userId and the given user's userId, respectively.
SELECT 1 FROM follow WHERE followerUserId = ? AND followeeUserId = ?;

-- Used in UserService to (un)follow accounts.
-- URL: http://localhost:8081/
-- The "?"s in both statements are replaced with the userId of the following user and the followee user's userId, respectively.
INSERT INTO follow (followerUserId, followeeUserId) VALUES (?, ?);
DELETE FROM follow WHERE followerUserId = ? AND followeeUserId = ?;

-- Used in PeopleService to retrieve all users to later check if they are followed by the logged-in user.
-- URL: http://localhost:8081/people
-- The "?" is replaced with the logged-in user's userId.
SELECT u.userId, u.firstName, u.lastName FROM user u WHERE u.userId != ?;

-- Used in PostService to get and create posts given a list of postIds.
-- URLs: 
-- "<PLACEHOLDER>" replaced with a variable number (n) of substitutions (e.g., "?, ?" or "?" or "?, ?, ?", etc.)
-- Each of these "?"s replaced with a hashtag
-- The final "?" replaced with the number n (see above)
SELECT p.postId, p.postText AS content, p.postDate, u.userId, (SELECT COUNT(userId) FROM heart WHERE postId = p.postId) AS heartsCount, (SELECT COUNT(userId) FROM comment WHERE postId = p.postId) AS commentsCount, ((p.postId, ?) IN (SELECT * FROM heart)) AS isHearted, ((p.postId, ?) IN (SELECT * FROM bookmark)) AS isBookmarked FROM post p, user u WHERE p.userId = u.userId AND p.postId IN (<PLACEHOLDER>) ORDER BY p.postDate DESC;

-- Used in PostService to get all comments associated with a post.
-- URL: http://localhost:8081/post/{postId}
-- The "?" is replaced with the aforementioned postId.
SELECT commentId, userId, commentDate, commentText FROM comment WHERE postId = ? ORDER BY commentDate ASC;

-- Used in PostService to get all postIds bookmarked by the current user.
-- URL: http://localhost:8081/bookmarks
-- The "?" is replaced with the logged-in user's userId.
SELECT postId FROM bookmark WHERE userId = ?;

-- Used in PostService to get all posts by a single user.
-- URLs: http://localhost:8081/profile/{userId}
-- The "?" is replaced with the aforementioned user's userId.
SELECT postId FROM post WHERE userId = ?;

-- Used in PostService to add a comment to a post.
-- URL: http://localhost:8081/post/{postId}
-- The "?"s are replaced with data provided by the user and retrieved from the app's state when the statement is executed.
INSERT INTO comment (postId, userId, commentDate, commentText) VALUES (?, ?, ?, ?);

-- The following SQL statements were included with the project and are used for login, registration, and authentication.
select * from user where username = ?;
insert into user (username, password, firstName, lastName) values (?, ?, ?, ?);