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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.models.User;
import uga.menik.cs4370.services.UserService;

/**
 * This controller handles the home page and some of it's sub URLs.
 */
@Controller
@RequestMapping
public class HomeController {

    /** A regular expression that matches a valid hashtag */
    private final Pattern hashTagPattern = Pattern.compile("\\B#([\\w-]+)");

    /** The connector to the database. */
    private final DataSource dataSource;

    /** For accessing the logged-in user. */
    private final UserService userService;

    @Autowired
    public HomeController(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }

    /**
     * This is the specific function that handles the root URL itself.
     * 
     * Note that this accepts a URL parameter called error.
     * The value to this parameter can be shown to the user as an error message.
     * See notes in HashtagSearchController.java regarding URL parameters.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("home_page");

        // Get the logged-in user's ID
        User loggedInUser = userService.getLoggedInUser();
        if (loggedInUser == null) {
            // Redirect to login if the user is not logged in
            return new ModelAndView("redirect:/login");
        }
        String loggedInUserId = loggedInUser.getUserId();

        // Fetch posts from users that the logged-in user follows
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT p.postId, p.postText, p.postDate, p.userId, "
                + "u.firstName, u.lastName "
                + "FROM post p "
                + "JOIN follow f ON p.userId = f.followeeUserId "
                + "JOIN user u ON p.userId = u.userId "
                + "WHERE f.followerUserId = ? "
                + "ORDER BY p.postDate DESC";

        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(loggedInUserId));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String postId = rs.getString("postId");
                    String postText = rs.getString("postText");
                    String postDate = formatDateTime(rs.getTimestamp("postDate"));
                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");

                    // Fetch user details
                    User user = new User(userId, firstName, lastName);

                    // Fetch additional post details (hearts, comments, etc.)
                    int heartsCount = getHeartsCount(postId); // Implement this method
                    int commentsCount = getCommentsCount(postId); // Implement this method
                    boolean isHearted = isPostHeartedByUser(postId, loggedInUserId); // Implement this method
                    boolean isBookmarked = isPostBookmarkedByUser(postId, loggedInUserId); // Implement this method

                    // Create a Post object
                    Post post = new Post(postId, postText, postDate, user, heartsCount, commentsCount, isHearted, isBookmarked);
                    posts.add(post);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to fetch posts. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/?error=" + message);
        }

        // Add posts to the ModelAndView
        mv.addObject("posts", posts);

        // Add error message if any
        mv.addObject("errorMessage", error);

        // Show "no content" message if there are no posts
        if (posts.isEmpty()) {
            mv.addObject("isNoContent", true);
        }

        return mv;
    }

    private String formatDateTime(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return timestamp.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a"));
    }

    /**
     * This function handles the /createpost URL.
     * This handles a post request that is going to be a form submission.
     * The form for this can be found in the home page. The form has a
     * input field with name = posttext. Note that the @RequestParam
     * annotation has the same name. This makes it possible to access the value
     * from the input from the form after it is submitted.
     */
    @PostMapping("/createpost")
    public String createPost(@RequestParam(name = "posttext") String postText) {
        System.out.println("User is creating post: " + postText);
        final String createPostStatementString = "INSERT INTO post (userId, postDate, postText) VALUES (?, ?, ?)";
        final String createHashtagStatementString = "INSERT INTO hashtag (hashTag, postId) VALUES (?, ?)";

        if (postText.isBlank()) {
            final String blankErrorMessage = URLEncoder.encode("Cannot create blank posts.",
                    StandardCharsets.UTF_8);
            return "redirect:/?error=" + blankErrorMessage;
        }

        if (!userService.isAuthenticated()) {
            final String message = URLEncoder.encode("Must be logged in to create posts. Please log in.",
                    StandardCharsets.UTF_8);
            return "redirect:/?error=" + message;
        }

        final User user = userService.getLoggedInUser();
        try (final Connection connection = dataSource.getConnection()) {
            final PreparedStatement createPostStatement = connection.prepareStatement(createPostStatementString, Statement.RETURN_GENERATED_KEYS);
            createPostStatement.setString(1, user.getUserId());
            createPostStatement.setTimestamp(2, new Timestamp(new java.util.Date().getTime()));
            createPostStatement.setString(3, postText);
            createPostStatement.execute(); // Throws on error

            // Get the ID of the post we just made
            final ResultSet addedRows = createPostStatement.getGeneratedKeys();
            if (!addedRows.next()) {
                // Exit try block
                throw new Exception();
            }

            final int postId = addedRows.getInt(1);
            final Matcher hashTagMatcher = hashTagPattern.matcher(postText);
            final PreparedStatement createHashtagStatement = connection.prepareStatement(createHashtagStatementString);
            final HashSet<String> addedHashTags = new HashSet<>();
            createHashtagStatement.setInt(2, postId);
            while (hashTagMatcher.find()) {
                final String hashTagName = hashTagMatcher.group(1);
                if (addedHashTags.add(hashTagName)) {
                    createHashtagStatement.setString(1, hashTagName);
                    createHashtagStatement.executeUpdate();
                }
            }

            // Redirect the user if the post creation is a success.
            return "redirect:/";

        } catch (Exception e) {
            System.out.println("Exception in /createpost: " + e.toString());
            // Fall out of the try block to the error return.
        }

        final String message = URLEncoder.encode("Failed to create the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/?error=" + message;
    }

    private int getHeartsCount(String postId) throws SQLException {
        String sql = "SELECT COUNT(*) AS heartsCount FROM heart WHERE postId = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("heartsCount");
                }
            }
        }
        return 0;
    }

    private int getCommentsCount(String postId) throws SQLException {
        String sql = "SELECT COUNT(*) AS commentsCount FROM comment WHERE postId = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("commentsCount");
                }
            }
        }
        return 0;
    }

    private boolean isPostHeartedByUser(String postId, String userId) throws SQLException {
        String sql = "SELECT 1 FROM heart WHERE postId = ? AND userId = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isPostBookmarkedByUser(String postId, String userId) throws SQLException {
        String sql = "SELECT 1 FROM bookmark WHERE postId = ? AND userId = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
