/**
 * Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

 *  *This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
 */
package uga.menik.cs4370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.cs4370.models.ExpandedPost;
import uga.menik.cs4370.services.PostService;
import uga.menik.cs4370.services.UserService;

/**
 * Handles /post URL and its sub urls.
 */
@Controller
@RequestMapping("/post")
public class PostController {

    @Autowired
    private UserService userService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PostService postService;

    /**
     * Handles the /post/{postId} URL to display a specific post.
     */
    @GetMapping("/{postId}")
    public ModelAndView webpage(@PathVariable("postId") String postId,
                                @RequestParam(name = "error", required = false) String error) {
        System.out.println("The user is attempting to view post with id: " + postId);
        ModelAndView mv = new ModelAndView("posts_page");

        Integer postIdInt = Integer.valueOf(postId);
        List<ExpandedPost> posts = postService.getExpandedPostsByIds(List.of(postIdInt));

        mv.addObject("posts", posts);
        mv.addObject("errorMessage", error);
        return mv;
    }

    /**
     * Handles comment creation, mirroring the "Make Post" process.
     */
    @PostMapping("/{postId}/comment")
    public String postComment(@PathVariable("postId") String postId,
            @RequestParam(name = "comment") String comment) {
        System.out.println("The user is attempting add a comment:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tcomment: " + comment);

        String error = null;
        try {
            postService.commentOnPost(postId, comment);
        } catch (UnsupportedOperationException uoe) {
            error = "Must be logged in to comment.";
        } catch (IllegalArgumentException iae) {
            error = "Cannot create blank comments.";
        } catch (SQLException sqle) {
            error = "An error occurred. Please try again.";
        }

        if (error == null) {
            return "redirect:/post/" + postId;
        } else {
            return "redirect:/post/" + postId + "?error=" + error;
        }
    }

    /**
     * Handles likes added on posts.
     */
    @GetMapping("/{postId}/heart/{isAdd}")
    public String addOrRemoveHeart(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a heart:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        try (Connection conn = dataSource.getConnection()) {
            String loggedInUserId = userService.getLoggedInUser().getUserId();

            if (isAdd) {
                String checkSql = "SELECT 1 FROM heart WHERE postId = ? AND userId = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, Integer.parseInt(postId));
                    checkStmt.setInt(2, Integer.parseInt(loggedInUserId));
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            String insertSql = "INSERT INTO heart (postId, userId) VALUES (?, ?)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                                insertStmt.setInt(1, Integer.parseInt(postId));
                                insertStmt.setInt(2, Integer.parseInt(loggedInUserId));
                                insertStmt.executeUpdate();
                                System.out.println("Successfully liked post with ID: " + postId);
                            }
                        } else {
                            System.out.println("Post already liked; skipping insert.");
                        }
                    }
                }
            } else {
                String deleteSql = "DELETE FROM heart WHERE postId = ? AND userId = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, Integer.parseInt(postId));
                    deleteStmt.setInt(2, Integer.parseInt(loggedInUserId));
                    deleteStmt.executeUpdate();
                    System.out.println("Successfully unliked post with ID: " + postId);
                }
            }
            return "redirect:/post/" + postId;
        } catch (SQLException e) {
            System.err.println("SQL Exception in addOrRemoveHeart: " + e.getMessage());
            return "redirect:/post/" + postId + "?error=" + URLEncoder.encode("Failed to (un)like the post.", StandardCharsets.UTF_8);
        }
    }

    /**
     * Handles bookmarking posts.
     */
    @GetMapping("/{postId}/bookmark/{isAdd}")
    public String addOrRemoveBookmark(@PathVariable("postId") String postId,
                                      @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a bookmark:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        try (Connection conn = dataSource.getConnection()) {
            String loggedInUserId = userService.getLoggedInUser().getUserId();

            if (isAdd) {

                String checkSql = "SELECT 1 FROM bookmark WHERE userId = ? AND postId = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, Integer.parseInt(loggedInUserId));
                    checkStmt.setInt(2, Integer.parseInt(postId));

                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {

                            String insertSql = "INSERT INTO bookmark (userId, postId) VALUES (?, ?)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                                insertStmt.setInt(1, Integer.parseInt(loggedInUserId));
                                insertStmt.setInt(2, Integer.parseInt(postId));
                                insertStmt.executeUpdate();
                                System.out.println("Successfully added bookmark for post with ID: " + postId);
                            }
                        } else {
                            System.out.println("Bookmark already exists; skipping insert.");
                        }
                    }
                }
            } else {

                String deleteSql = "DELETE FROM bookmark WHERE userId = ? AND postId = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, Integer.parseInt(loggedInUserId));
                    deleteStmt.setInt(2, Integer.parseInt(postId));
                    deleteStmt.executeUpdate();
                    System.out.println("Successfully removed bookmark for post with ID: " + postId);
                }
            }
            return "redirect:/post/" + postId;
        } catch (SQLException e) {
            System.err.println("SQL Exception in addOrRemoveBookmark: " + e.getMessage());
            return "redirect:/post/" + postId + "?error=" + URLEncoder.encode("Failed to (un)bookmark the post.", StandardCharsets.UTF_8);
        }
    }
}
