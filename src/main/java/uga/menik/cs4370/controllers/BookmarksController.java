/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.models.User;
import uga.menik.cs4370.services.UserService;

/**
 * Handles /bookmarks and its sub URLs.
 * No other URLs at this point.
 * 
 * Learn more about @Controller here: 
 * https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html
 */
@Controller
@RequestMapping("/bookmarks")
public class BookmarksController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserService userService;
    /**
     * /bookmarks URL itself is handled by this.
     */
    @GetMapping
    public ModelAndView webpage() {
        ModelAndView mv = new ModelAndView("posts_page");


        List<Post> posts = getBookmarkedPosts();
        mv.addObject("posts", posts);


        if (posts.isEmpty()) {
            mv.addObject("isNoContent", true);
        }

        return mv;
    }



    private List<Post> getBookmarkedPosts() {
        List<Post> bookmarkedPosts = new ArrayList<>();

        String sql = "SELECT p.postId, p.postText, p.postDate, p.userId, "
                + "u.firstName, u.lastName "
                + "FROM post p "
                + "JOIN bookmark b ON p.postId = b.postId "
                + "JOIN user u ON p.userId = u.userId "
                + "WHERE b.userId = ? "
                + "ORDER BY p.postDate DESC";

        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String loggedInUserId = userService.getLoggedInUser().getUserId();
            pstmt.setInt(1, Integer.parseInt(loggedInUserId));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String postId = rs.getString("postId");
                    String postText = rs.getString("postText");
                    String postDate = rs.getString("postDate");
                    String authorId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");


                    User author = new User(authorId, firstName, lastName);


                    String heartsCountSql = "SELECT COUNT(*) FROM heart WHERE postId = ?";
                    int heartsCount = 0;
                    try (PreparedStatement heartsStmt = conn.prepareStatement(heartsCountSql)) {
                        heartsStmt.setString(1, postId);
                        try (ResultSet heartsRs = heartsStmt.executeQuery()) {
                            if (heartsRs.next()) {
                                heartsCount = heartsRs.getInt(1);
                            }
                        }
                    }


                    String commentsCountSql = "SELECT COUNT(*) FROM comment WHERE postId = ?";
                    int commentsCount = 0;
                    try (PreparedStatement commentsStmt = conn.prepareStatement(commentsCountSql)) {
                        commentsStmt.setString(1, postId);
                        try (ResultSet commentsRs = commentsStmt.executeQuery()) {
                            if (commentsRs.next()) {
                                commentsCount = commentsRs.getInt(1);
                            }
                        }
                    }


                    String isHeartedSql = "SELECT COUNT(*) FROM heart WHERE postId = ? AND userId = ?";
                    boolean isHearted = false;
                    try (PreparedStatement isHeartedStmt = conn.prepareStatement(isHeartedSql)) {
                        isHeartedStmt.setString(1, postId);
                        isHeartedStmt.setString(2, loggedInUserId);
                        try (ResultSet isHeartedRs = isHeartedStmt.executeQuery()) {
                            if (isHeartedRs.next() && isHeartedRs.getInt(1) > 0) {
                                isHearted = true;
                            }
                        }
                    }


                    Post post = new Post(
                            postId,
                            postText,
                            postDate,
                            author,
                            heartsCount,
                            commentsCount,
                            isHearted,
                            true
                    );


                    bookmarkedPosts.add(post);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bookmarkedPosts;
    }
    
}
