/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.models.User;
import uga.menik.cs4370.services.UserService;

/**
 * Handles /profile URL and its sub URLs.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final DataSource dataSource;

    @Autowired
    public ProfileController(UserService userService, DataSource dataSource) {
        this.userService = userService;
        this.dataSource = dataSource;
    }

    /**
     * This function handles /profile URL itself.
     * This serves the webpage that shows posts of the logged in user.
     */
    @GetMapping
    public ModelAndView profileOfLoggedInUser() {
        System.out.println("User is attempting to view profile of the logged in user.");
        User loggedInUser = userService.getLoggedInUser();
        if (loggedInUser == null) {
            return new ModelAndView("redirect:/login");
        }
        return profileOfSpecificUser(loggedInUser.getUserId());
    }

    /**
     * This function handles /profile/{userId} URL.
     * This serves the webpage that shows posts of a specific user given by userId.
     */
    @GetMapping("/{userId}")
    public ModelAndView profileOfSpecificUser(@PathVariable("userId") String userId) {
        System.out.println("User is attempting to view profile: " + userId);
        
        ModelAndView mv = new ModelAndView("posts_page");

        // Fetch posts for the specific user
        List<Post> posts = getPostsByUserId(userId);
        mv.addObject("posts", posts);

        // Show "no content" message if there are no posts
        if (posts.isEmpty()) {
            mv.addObject("isNoContent", true);
        }
        
        return mv;
    }

    private List<Post> getPostsByUserId(String userId) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT p.postId, p.postText, p.postDate, p.userId, " +
                    "u.firstName, u.lastName, " +
                    "EXISTS(SELECT 1 FROM heart h WHERE h.postId = p.postId AND h.userId = ?) as isHearted, " +
                    "EXISTS(SELECT 1 FROM bookmark b WHERE b.postId = p.postId AND b.userId = ?) as isBookmarked, " +
                    "(SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) as heartsCount, " +
                    "(SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) as commentsCount " +
                    "FROM post p JOIN user u ON p.userId = u.userId " +
                    "ORDER BY p.postDate DESC";
    
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(userId));
            pstmt.setInt(2, Integer.parseInt(userId));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String postId = rs.getString("postId");
                    String postText = rs.getString("postText");
                    String postDate = formatDateTime(rs.getTimestamp("postDate"));
                    String authorId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    boolean isHearted = rs.getBoolean("isHearted");
                    boolean isBookmarked = rs.getBoolean("isBookmarked");
                    int heartsCount = rs.getInt("heartsCount");
                    int commentsCount = rs.getInt("commentsCount");
    
                    User author = new User(authorId, firstName, lastName);
                    Post post = new Post(postId, postText, postDate, author, 
                                        heartsCount, commentsCount, isHearted, isBookmarked);
                    posts.add(post);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }
    /**
     * Formats a Timestamp into a human-readable date and time string.
     */
    private String formatDateTime(java.sql.Timestamp timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        return timestamp.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a"));
    }

    /**
     * Checks if a post is bookmarked by the logged-in user.
     */
    private boolean isPostBookmarkedByUser(String postId, String userId) throws SQLException {
        String sql = "SELECT 1 FROM bookmark WHERE postId = ? AND userId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Handles bookmarking/unbookmarking a post.
     */
    @GetMapping("/{postId}/bookmark/{isAdd}")
    public String toggleBookmarkPost(@PathVariable("postId") String postId,
                                     @PathVariable("isAdd") Boolean isAdd) {
        try {
            String loggedInUserId = userService.getLoggedInUser().getUserId();
            if (isAdd) {
                bookmarkPost(postId, loggedInUserId);
            } else {
                unbookmarkPost(postId, loggedInUserId);
            }
            return "redirect:/"; // Redirect back to the home page
        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to (un)bookmark the post. Please try again.", StandardCharsets.UTF_8);
            return "redirect:/?error=" + message;
        }
    }

    /**
     * Bookmarks a post.
     */
    private void bookmarkPost(String postId, String userId) throws SQLException {
        String sql = "INSERT INTO bookmark (postId, userId) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Unbookmarks a post.
     */
    private void unbookmarkPost(String postId, String userId) throws SQLException {
        String sql = "DELETE FROM bookmark WHERE postId = ? AND userId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        }
    }

}