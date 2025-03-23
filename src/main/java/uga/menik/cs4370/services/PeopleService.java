/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.cs4370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uga.menik.cs4370.models.FollowableUser;
import uga.menik.cs4370.services.UserService;

@Service
public class PeopleService {
    private final DataSource dataSource;
    private final UserService userService;

    @Autowired
    public PeopleService(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }



    public boolean checkIfUserIsFollowed(String loggedInUserId, String targetUserId) {
        String checkSql = "SELECT 1 FROM follow WHERE followerUserId = ? AND followeeUserId = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, Integer.parseInt(loggedInUserId));
            checkStmt.setInt(2, Integer.parseInt(targetUserId));

            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    public List<FollowableUser> getFollowableUsers(String userIdToExclude) {
        List<FollowableUser> followableUsers = new ArrayList<>();


        String sql = "SELECT u.userId, u.firstName, u.lastName " +
                "FROM user u " +
                "WHERE u.userId != ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userIdToExclude);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");

                    // Get the follow status for the user
                    boolean isFollowed = checkIfUserIsFollowed(userIdToExclude, userId);

                    // Creating FollowableUser with the correct follow status
                    FollowableUser user = new FollowableUser(userId, firstName, lastName, isFollowed, "N/A");
                    followableUsers.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return followableUsers;
    }


}