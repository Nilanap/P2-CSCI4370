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

import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.models.User;

@Service
public class PostService {

    private final DataSource dataSource;
    private final UserService userService;

    @Autowired
    public PostService(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
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
                + " u.firstName,"
                + " u.lastName,"
                + " (SELECT COUNT(userId) FROM heart WHERE postId = p.postId) AS heartsCount,"
                + " (SELECT COUNT(userId) FROM comment WHERE postId = p.postId) AS commentsCount,"
                + " ((p.postId, ?) IN (SELECT * FROM heart)) AS isHearted,"
                + " ((p.postId, ?) IN (SELECT * FROM bookmark)) AS isBookmarked"
                + " FROM post p, user u"
                + " WHERE p.userId = u.userId"
                + " AND p.postId IN (<PLACEHOLDER>)"
                + " ORDER BY p.postDate DESC";

        final String placeholderString = queryString.replace("<PLACEHOLDER>",
                String.join(",", Collections.nCopies(postIds.size(), "?")));
        ArrayList<Post> posts = new ArrayList<>();
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
                User user = new User(
                        results.getString("userId"),
                        results.getString("firstName"),
                        results.getString("lastName")
                );
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
}