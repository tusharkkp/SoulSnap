package com.mit.tushar_kaldate.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.mit.tushar_kaldate.model.Emotion;

import java.util.List;

@Dao
public interface EmotionDao {

    @Insert
    long insertEmotion(Emotion emotion);

    @Query("SELECT * FROM emotions WHERE userId = :userId ORDER BY id DESC")
    List<Emotion> getEmotionsForUser(int userId);

    @Delete
    void deleteEmotion(Emotion emotion);
}
