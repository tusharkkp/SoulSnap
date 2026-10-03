package com.mit.tushar_kaldate.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "emotions")
public class Emotion {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private String photoPath;
    private String emotion;
    private float emotionConfidence;
    private String note;
    private String date;
    private String time;

    public Emotion(int userId, String photoPath, String emotion, float emotionConfidence, String note, String date, String time) {
        this.userId = userId;
        this.photoPath = photoPath;
        this.emotion = emotion;
        this.emotionConfidence = emotionConfidence;
        this.note = note;
        this.date = date;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public float getEmotionConfidence() {
        return emotionConfidence;
    }

    public void setEmotionConfidence(float emotionConfidence) {
        this.emotionConfidence = emotionConfidence;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
