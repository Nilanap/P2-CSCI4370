/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;

import uga.menik.cs4370.models.ExpandedPost;
import uga.menik.cs4370.utility.Utility;
import uga.menik.cs4370.models.Comment;
import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.models.User;
import uga.menik.cs4370.services.UserService;
import uga.menik.cs4370.utility.Utility;

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

    /**
     * This function handles the /post/{postId} URL.
     * This handlers serves the web page for a specific post.
     * Note there is a path variable {postId}.
     * An example URL handled by this function looks like below:
     * http://localhost:8081/post/1
     * The above URL assigns 1 to postId.
     * 
     * See notes from HomeController.java regardig error URL parameter.
     */
    @GetMapping("/{postId}")
    public ModelAndView webpage(@PathVariable("postId") String postId,
                                @RequestParam(name = "error", required = false) String error) {
        System.out.println("The user is attempting to view post with id: " + postId);
        ModelAndView mv = new ModelAndView("posts_page");


        Post post = getPostById(postId);
        mv.addObject("post", post);

        mv.addObject("errorMessage", error);
        return mv;
    }

    /**
     * Handles comments added on posts.
     * See comments on webpage function to see how path variables work here.
     * This function handles form posts.
     * See comments in HomeController.java regarding form submissions.
     */
    @PostMapping("/{postId}/comment")
    public String postComment(@PathVariable("postId") String postId,
            @RequestParam(name = "comment") String comment) {
        System.out.println("The user is attempting add a comment:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tcomment: " + comment);

        // Redirect the user if the comment adding is a success.
        // return "redirect:/post/" + postId;

        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to post the comment. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

    /**
     * Handles likes added on posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions and how path variables work.
     */
    @GetMapping("/{postId}/heart/{isAdd}")
    public String addOrRemoveHeart(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a heart:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        // Redirect the user if the comment adding is a success.
        // return "redirect:/post/" + postId;

        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to (un)like the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

    /**
     * Handles bookmarking posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions.
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
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to (un)bookmark the post. Please try again.", StandardCharsets.UTF_8);
            return "redirect:/post/" + postId + "?error=" + message;
        }
    }

    /**
     * Get a single post by the id.
     */
    private Post getPostById(String postId) {
        Post post = null;
        String sql = "SELECT p.postId, p.postText, p.postDate, p.userId, " +
                "u.firstName, u.lastName " +
                "FROM post p " +
                "JOIN user u ON p.userId = u.userId " +
                "WHERE p.postId = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(postId));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String postText = rs.getString("postText");
                    String postDate = rs.getString("postDate");
                    String authorId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");

                    User author = new User(authorId, firstName, lastName);

                    post = new Post(
                            postId,
                            postText,
                            postDate,
                            author,
                            0,
                            0,
                            false,
                            true
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return post;
    }

}
