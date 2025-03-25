/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import uga.menik.cs4370.services.PeopleService;
import uga.menik.cs4370.services.PostService;
import uga.menik.cs4370.services.UserService;

/**
 * Handles /profile URL and its sub URLs.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final DataSource dataSource;
    private final PeopleService peopleService;
    private final PostService postService;
    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService, DataSource dataSource, PeopleService peopleService, PostService postService) {
        this.userService = userService;
        this.dataSource = dataSource;
        this.peopleService = peopleService;
        this.postService = postService;
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
        List<Post> posts = postService.getPostsByUser(userId);
        mv.addObject("posts", posts);

        // Add user details to display on the profile page (optional)
        User profileUser = peopleService.getUserById(userId);
        if (profileUser != null) {
            mv.addObject("profileUser", profileUser);
        } else {
            mv.addObject("errorMessage", "User not found.");
        }

        if (posts.isEmpty()) {
            mv.addObject("isNoContent", true);
        }
        return mv;
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