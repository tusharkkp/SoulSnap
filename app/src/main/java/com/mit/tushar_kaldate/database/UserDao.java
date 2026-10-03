package com.mit.tushar_kaldate.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mit.tushar_kaldate.model.User;

@Dao
public interface UserDao {

    @Insert
    long insertUser(User user);

    @Query("SELECT * FROM users WHERE (username = :identifier OR email = :identifier) AND password = :password LIMIT 1")
    User authenticate(String identifier, String password);

    @Query("SELECT * FROM users WHERE username = :username OR email = :email LIMIT 1")
    User findByUsernameOrEmail(String username, String email);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    User getUserById(int userId);
}
