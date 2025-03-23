/**
 * Copyright (c) 2024 Sami Menik, PhD. All rights reserved.
 *
 *  *This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
 */
package uga.menik.cs4370.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.cs4370.models.Post;
import uga.menik.cs4370.services.PostService;

/**
 * Handles /hashtagsearch URL and possibly others. At this point no other URLs.
 */
@Controller
@RequestMapping("/hashtagsearch")
public class HashtagSearchController {

    private final DataSource dataSource;
    private final PostService postService;

    @Autowired
    public HashtagSearchController(DataSource dataSource, PostService postService) {
        this.dataSource = dataSource;
        this.postService = postService;
    }

    /**
     * This function handles the /hashtagsearch URL itself. This URL can process
     * a request parameter with name hashtags. In the browser the URL will look
     * something like below:
     * http://localhost:8081/hashtagsearch?hashtags=%23amazing+%23fireworks
     * Note: the value of the hashtags is URL encoded.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "hashtags") String hashtags) {
        System.out.println("User is searching: " + hashtags);
        final String queryString = "SELECT h.postId"
                + " FROM hashtag h, post p"
                + " WHERE h.postId = p.postId"
                + " AND h.hashTag IN (<PLACEHOLDER>)"
                + " GROUP BY h.postId"
                + " HAVING COUNT(h.hashTag) = ?";

        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");

        String[] hashTagsList = hashtags.split(" ");
        for (int i = 0; i < hashTagsList.length; i++) {
            if (hashTagsList[i].startsWith("#")) {
                hashTagsList[i] = hashTagsList[i].substring(1);
            }
        }

        // ... AND h.hashTag IN (?, ?, ...) GROUP BY ...
        // Create a string in this format with as many question marks as there are hash tags
        final String placeholderString = queryString.replace("<PLACEHOLDER>",
                String.join(",", Collections.nCopies(hashTagsList.length, "?")));

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(placeholderString);
            for (int i = 0; i < hashTagsList.length; i++) {
                statement.setString(i + 1, hashTagsList[i]);
            }
            statement.setInt(hashTagsList.length + 1, hashTagsList.length);
			System.out.println(statement.toString());
            ResultSet results = statement.executeQuery();
            List<Integer> postIds = new ArrayList<>();
            while (results.next()) {
                postIds.add(results.getInt(1));
            }

            List<Post> posts = postService.getPostsFromIds(postIds);
            if (!posts.isEmpty()) {
                mv.addObject("posts", posts);
            } else {
                mv.addObject("isNoContent", true);
            }

        } catch (SQLException sqle) {
			sqle.printStackTrace();
            String errorMessage = "An error occurred. Please try again.";
            mv.addObject("errorMessage", errorMessage);
        }

        return mv;
    }
}
