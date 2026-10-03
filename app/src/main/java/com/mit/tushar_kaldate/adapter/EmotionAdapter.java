package com.mit.tushar_kaldate.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mit.tushar_kaldate.R;
import com.mit.tushar_kaldate.model.Emotion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmotionAdapter extends RecyclerView.Adapter<EmotionAdapter.EmotionViewHolder> {

    public interface OnEmotionItemListener {
        void onDeleteClick(Emotion emotion);
    }

    private final Context context;
    private final List<Emotion> emotionList = new ArrayList<>();
    private final OnEmotionItemListener listener;

    public EmotionAdapter(Context context, OnEmotionItemListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setEmotions(List<Emotion> emotions) {
        emotionList.clear();
        if (emotions != null) {
            emotionList.addAll(emotions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_emotion, parent, false);
        return new EmotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmotionViewHolder holder, int position) {
        Emotion emotion = emotionList.get(position);
        holder.bind(emotion);
    }

    @Override
    public int getItemCount() {
        return emotionList.size();
    }

    class EmotionViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPhoto;
        private final TextView tvEmoji;
        private final TextView tvEmotion;
        private final TextView tvNote;
        private final TextView tvDateTime;
        private final TextView tvConfidence;
        private final ImageButton btnDelete;
        private final View llEmotionBadge;

        public EmotionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivItemPhoto);
            tvEmoji = itemView.findViewById(R.id.tvItemEmoji);
            tvEmotion = itemView.findViewById(R.id.tvItemEmotion);
            tvNote = itemView.findViewById(R.id.tvItemNote);
            tvDateTime = itemView.findViewById(R.id.tvItemDateTime);
            tvConfidence = itemView.findViewById(R.id.tvItemConfidence);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
            llEmotionBadge = itemView.findViewById(R.id.llEmotionBadge);
        }

        public void bind(Emotion emotion) {
            String emotionName = emotion.getEmotion();
            String emoji = getEmojiForEmotion(emotionName);

            tvEmoji.setText(emoji);
            tvEmotion.setText(emotionName);
            tvNote.setText(emotion.getNote());
            tvDateTime.setText(String.format("📅 %s   •   🕐 %s", emotion.getDate(), emotion.getTime()));

            int confidencePercent = Math.round(emotion.getEmotionConfidence() * 100f);
            tvConfidence.setText(String.format(Locale.getDefault(), "%d%% confidence", confidencePercent));

            // Load photo from private internal storage safely
            if (emotion.getPhotoPath() != null) {
                File imgFile = new File(emotion.getPhotoPath());
                if (imgFile.exists()) {
                    Bitmap bitmap = decodeSampledBitmap(imgFile.getAbsolutePath(), 180, 180);
                    ivPhoto.setImageBitmap(bitmap);
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_camera);
                }
            } else {
                ivPhoto.setImageResource(R.drawable.ic_camera);
            }

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(emotion);
                }
            });
        }

        private String getEmojiForEmotion(String emotion) {
            if (emotion == null) return "✨";
            switch (emotion.toUpperCase()) {
                case "HAPPY": return "😊";
                case "SAD": return "😢";
                case "ANGRY": return "😠";
                case "CRY": return "😭";
                case "FEAR": return "😨";
                case "SURPRISE": return "😲";
                default: return "✨";
            }
        }

        private Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }
    }
}
