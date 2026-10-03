package com.mit.tushar_kaldate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.mit.tushar_kaldate.ml.ModelInputProcessor;
import com.mit.tushar_kaldate.model.Emotion;
import com.mit.tushar_kaldate.model.User;

import org.junit.Test;

public class ModelUnitTest {

    @Test
    public void testUserCreation() {
        User user = new User("tushar", "tushar@example.com", "secure123");
        user.setId(101);
        assertEquals(101, user.getId());
        assertEquals("tushar", user.getUsername());
        assertEquals("tushar@example.com", user.getEmail());
        assertEquals("secure123", user.getPassword());
    }

    @Test
    public void testEmotionCreation() {
        Emotion emotion = new Emotion(
                101,
                "/data/user/0/com.mit.tushar_kaldate/files/emotions/photo_01.jpg",
                "HAPPY",
                0.91f,
                "Feeling peaceful today.",
                "03 Oct 2026",
                "06:12 PM"
        );
        emotion.setId(1);

        assertEquals(1, emotion.getId());
        assertEquals(101, emotion.getUserId());
        assertEquals("HAPPY", emotion.getEmotion());
        assertEquals(0.91f, emotion.getEmotionConfidence(), 0.001f);
        assertEquals("Feeling peaceful today.", emotion.getNote());
        assertEquals("03 Oct 2026", emotion.getDate());
        assertEquals("06:12 PM", emotion.getTime());
    }

    @Test
    public void testModelInputProcessorConstants() {
        assertEquals(224, ModelInputProcessor.INPUT_IMAGE_SIZE);
    }
}
