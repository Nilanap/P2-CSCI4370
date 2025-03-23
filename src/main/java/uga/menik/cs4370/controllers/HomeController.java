/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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
import uga.menik.cs4370.utility.Utility;

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
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("home_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.
        List<Post> posts = Utility.createSamplePostsListWithoutComments();
        mv.addObject("posts", posts);

        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // An error message can be optionally specified with a url query parameter too.
        String errorMessage = error;
        mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);

        return mv;
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
            createPostStatement.setDate(2, new Date(new java.util.Date().getTime()));
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

}
