# SoulSnap — Emotion Recognition Machine Learning Model Documentation

## 1. Model Overview
- **Model Name:** VGG19 Facial Emotion Recognition (Optimized)
- **Model File:** `app/src/main/assets/emotion_model.tflite`
- **File Size:** ~19.2 MB
- **Model Architecture:** Convolutional Neural Network based on VGG19 (pre-trained on ImageNet, fine-tuned on CK+ and RAF-DB datasets)
- **Author/Creator:** Pasindu Sewmuthu Abewickrama Singhe
- **Source Repository:** Hugging Face (`PSewmuthu/vgg19-emotion-recognition-ckplus-rafdb`)
- **License:** Apache 2.0 (Permissive open-source license)
- **Test Accuracy:** 77.51% (Dual-dataset fine-tuning on CK+ and RAF-DB)

---

## 2. Input Specifications
- **Input Dimensions:** `[1, 224, 224, 3]` (Batch size: 1, Width: 224, Height: 224, Channels: 3 RGB)
- **Data Type:** Float32
- **Preprocessing & Normalization:**
  - Face is detected and localized via Google ML Kit Face Detection.
  - Bounding box is extracted with 15% contextual margin and cropped.
  - Cropped face is scaled to `224x224` pixels.
  - Pixel values are extracted as RGB floats and normalized:
    `normalized_pixel = raw_pixel_value / 255.0f` (Range: [0.0, 1.0]).

---

## 3. Output Specifications & Class Mapping
- **Output Dimensions:** `[1, 7]` (Float32 probabilities)
- **Raw Model Class Index Mapping:**
  - Index 0: `Angry`
  - Index 1: `Disgust` (facial grimace / distress)
  - Index 2: `Fear`
  - Index 3: `Happy`
  - Index 4: `Neutral`
  - Index 5: `Sad`
  - Index 6: `Surprise`

---

## 4. SoulSnap Emotion Journal Classification Mapping
The application classifies facial expressions into the six required emotion categories:

| Target Emotion | Emoji | Source Class / Logic | Confidence Derivation |
|---|---|---|---|
| **HAPPY** | 😊 | Index 3 (`Happy`) | Exact model confidence for Happy |
| **SAD** | 😢 | Index 5 (`Sad`) | Exact model confidence for Sad (< 0.70) |
| **ANGRY** | 😠 | Index 0 (`Angry`) | Exact model confidence for Angry |
| **CRY** | 😭 | Index 5 (`Sad` >= 0.70) or Index 1 (`Disgust`/Distress grimace) | Model output confidence for grief/distress |
| **FEAR** | 😨 | Index 2 (`Fear`) | Exact model confidence for Fear |
| **SURPRISE** | 😲 | Index 6 (`Surprise`) | Exact model confidence for Surprise |

- **Confidence Threshold:** `0.30` (30%).
- If the model's confidence is below the threshold or the facial expression is neutral/unexpressive, SoulSnap safely indicates:
  `Emotion unclear. Please try another photograph.`

---

## 5. Security & Isolation
- Camera photographs are stored only in the application's private/internal storage (`getFilesDir()/emotions/`).
- Only file paths and emotion metrics are saved in Room Database.
- Each emotion entry is isolated by `userId` in SQLite via Room DAO.
