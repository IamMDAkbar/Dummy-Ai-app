CREATE DATABASE Akady;

USE Akady;

CREATE TABLE Users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(15),
    profile_picture VARCHAR(255),
    akady_points INT DEFAULT 0,
    description TEXT,
    favorite_language VARCHAR(50),
    hobby VARCHAR(50),
    location VARCHAR(100),
    experience VARCHAR(100)
);