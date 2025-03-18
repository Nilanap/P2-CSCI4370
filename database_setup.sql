-- Create the database.
create database if not exists cs4370_mb_platform;

-- Use the created database.
use cs4370_mb_platform;

-- Create the user table.
create table if not exists user (
    userId int auto_increment,
    username varchar(255) not null,
    password varchar(255) not null,
    firstName varchar(255) not null,
    lastName varchar(255) not null,
    primary key (userId),
    unique (username),
    constraint userName_min_length check (char_length(trim(userName)) >= 2),
    constraint firstName_min_length check (char_length(trim(firstName)) >= 2),
    constraint lastName_min_length check (char_length(trim(lastName)) >= 2)
);

CREATE TABLE IF NOT EXISTS post (
    postId int AUTO_INCREMENT,
    userId int NOT NULL,
    postDate date NOT NULL,
    postText varchar(500) NOT NULL,
    PRIMARY KEY (postId),
    FOREIGN KEY (userId) REFERENCES user(userId)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT postText_min_length CHECK (char_length(trim(postText)) > 0)
);

CREATE TABLE IF NOT EXISTS comment (
    commentId int AUTO_INCREMENT,
    postId int NOT NULL,
    userId int NOT NULL,
    commentDate date NOT NULL,
    commentText varchar(255) NOT NULL,
    PRIMARY KEY (commentId),
    FOREIGN KEY (postId) REFERENCES post(postId)
        ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES user(userId),
    CONSTRAINT commentText_min_length CHECK (char_length(trim(commentText)) > 0)
);

CREATE TABLE IF NOT EXISTS heart (
    postId int NOT NULL,
    userId int NOT NULL,
    PRIMARY KEY (postId, userId),
    FOREIGN KEY (postId) REFERENCES post(postId)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES user(userId)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bookmark (
    postId int NOT NULL,
    userId int NOT NULL,
    PRIMARY KEY (postId, userId),
    FOREIGN KEY (postId) REFERENCES post(postId)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES user(userId)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS hashtag (
    hashTag varchar(50) NOT NULL,
    postId int NOT NULL,
    PRIMARY KEY (hashTag, postId),
    FOREIGN KEY (postId) REFERENCES post(postId)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS follow (
    followerUserId int NOT NULL,
    followeeUserId int NOT NULL,
    PRIMARY KEY (followerUserId, followeeUserId),
    FOREIGN KEY (followerUserId) REFERENCES user(userId)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (followeeUserId) REFERENCES user(userId)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);