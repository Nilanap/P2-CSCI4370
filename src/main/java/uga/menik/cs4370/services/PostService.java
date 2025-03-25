/**
 * Copyright (c) 2024 Sami Menik, PhD. All rights reserved.
 *
 *  *This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
 */
package uga.menik.cs4370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uga.menik.cs4370.models.Comment;
import uga.menik.cs4370.models.ExpandedPost;
import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.models.User;

@Service
public class PostService {

    private final DataSource dataSource;
    private final PeopleService peopleService;
    private final UserService userService;

    @Autowired
    public PostService(DataSource dataSource, PeopleService peopleService, UserService userService) {
        this.dataSource = dataSource;
        this.peopleService = peopleService;
        this.userService = userService;
    }

    /**
     * Get a list of posts from their IDs. Posts are ordered newest to oldest.
     *
     * @param postIds
     * @return
     */
    public List<Post> getPostsFromIds(List<Integer> postIds) {
        final String queryString = "SELECT"
                + " p.postId,"
                + " p.postText AS content,"
                + " p.postDate,"
                + " u.userId,"
                + " (SELECT COUNT(userId) FROM heart WHERE postId = p.postId) AS heartsCount,"
                + " (SELECT COUNT(userId) FROM comment WHERE postId = p.postId) AS commentsCount,"
                + " ((p.postId, ?) IN (SELECT * FROM heart)) AS isHearted,"
                + " ((p.postId, ?) IN (SELECT * FROM bookmark)) AS isBookmarked"
                + " FROM post p, user u"
                + " WHERE p.userId = u.userId"
                + " AND p.postId IN (<PLACEHOLDER>)"
                + " ORDER BY p.postDate DESC";

        List<Post> posts = new ArrayList<>();
        if (postIds.isEmpty()) {
            return posts;
        }

        final String placeholderString = queryString.replace("<PLACEHOLDER>",
                String.join(",", Collections.nCopies(postIds.size(), "?")));
        try (final Connection connection = dataSource.getConnection()) {
            final String userId;
            if (userService.isAuthenticated()) {
                userId = userService.getLoggedInUser().getUserId();
            } else {
                userId = "-1";
            }
            PreparedStatement query = connection.prepareStatement(placeholderString);
            query.setString(1, userId);
            query.setString(2, userId);
            for (int i = 0; i < postIds.size(); i++) {
                query.setInt(3 + i, postIds.get(i));
            }

            ResultSet results = query.executeQuery();
            while (results.next()) {
                User user = peopleService.getUserById(results.getString("userId"));
                Post post = new Post(
                        results.getString("postId"),
                        results.getString("content"),
                        results.getString("postDate"),
                        user,
                        results.getInt("heartsCount"),
                        results.getInt("commentsCount"),
                        results.getBoolean("isHearted"),
                        results.getBoolean("isBookmarked")
                );
                posts.add(post);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return posts;
    }

    public List<ExpandedPost> getExpandedPostsByIds(List<Integer> postIds) {
        final String commentsQueryString = "SELECT commentId, userId, commentDate, commentText FROM comment WHERE postId = ? ORDER BY commentDate ASC";

        final List<Post> posts = getPostsFromIds(postIds);
        final List<ExpandedPost> expandedPosts = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement query = connection.prepareStatement(commentsQueryString);
            for (Post post : posts) {
                List<Comment> comments = new ArrayList<>();
                query.setString(1, post.getPostId());
                ResultSet results = query.executeQuery();
                while (results.next()) {
                    Comment comment = new Comment(
                            results.getString("commentId"),
                            results.getString("commentText"),
                            results.getString("commentDate"),
                            peopleService.getUserById(results.getString("userId"))
                    );

                    comments.add(comment);
                }
                ExpandedPost expandedPost = new ExpandedPost(
                        post.getPostId(),
                        post.getContent(),
                        post.getPostDate(),
                        post.getUser(),
                        post.getHeartsCount(),
                        post.getCommentsCount(),
                        post.getHearted(),
                        post.isBookmarked(),
                        comments
                );
                expandedPosts.add(expandedPost);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return expandedPosts;
    }

    public List<Post> getBookmarkedPosts() {
        final String bookmarkedPostsQueryString = "SELECT postId FROM bookmark WHERE userId = ?";
        List<Post> posts = new ArrayList<>();
        if (!userService.isAuthenticated()) {
            return posts;
        }

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareCall(bookmarkedPostsQueryString);
            statement.setString(1, userService.getLoggedInUser().getUserId());
            ResultSet results = statement.executeQuery();

            List<Integer> bookmarkedPostIds = new ArrayList<>();
            while (results.next()) {
                bookmarkedPostIds.add(results.getInt("postId"));
            }

            if (!bookmarkedPostIds.isEmpty()) {
                posts = getPostsFromIds(bookmarkedPostIds);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return posts;
    }

    public List<Post> getPostsByUser(String userId) {
        final String bookmarkedPostsQueryString = "SELECT postId FROM post WHERE userId = ?";
        List<Post> posts = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareCall(bookmarkedPostsQueryString);
            statement.setString(1, userId);
            ResultSet results = statement.executeQuery();

            List<Integer> userPostIds = new ArrayList<>();
            while (results.next()) {
                userPostIds.add(results.getInt("postId"));
            }

            if (!userPostIds.isEmpty()) {
                posts = getPostsFromIds(userPostIds);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return posts;
    }

    public void commentOnPost(String postId, String commentText)
            throws UnsupportedOperationException, IllegalArgumentException, SQLException {

        final String insertString = "INSERT INTO comment (postId, userId, commentDate, commentText) VALUES (?, ?, ?, ?)";

        if (commentText == null || commentText.isBlank()) {
            throw new IllegalArgumentException("Cannot post blank comments.");
        }

        if (!userService.isAuthenticated()) {
            throw new UnsupportedOperationException("Must be logged in to create comments.");
        }

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(insertString);
            statement.setString(1, postId);
            statement.setString(2, userService.getLoggedInUser().getUserId());
            statement.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
            statement.setString(4, commentText);
            statement.executeUpdate();
            System.out.println("Successfully created a comment for post with ID: " + postId);
        }
    }
}
