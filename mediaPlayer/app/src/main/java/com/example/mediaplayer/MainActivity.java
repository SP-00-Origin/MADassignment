package com.example.mediaplayer;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.media.MediaPlayer;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.net.Uri;
import android.content.Intent;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.AdapterView;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    VideoView videoView;

    boolean isAudio = false;
    boolean isVideo = false;

    ArcSeekBar arcSeekBar;
    TextView txtStart, txtEnd;

    Handler handler = new Handler(Looper.getMainLooper());

        private String formatTime(int millis) {
            int seconds = millis / 1000;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }

    ActivityResultLauncher<Intent> audioPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                            Uri uri = result.getData().getData();

                            if (mediaPlayer != null) {
                                mediaPlayer.release();
                            }

                            try {
                                mediaPlayer = new MediaPlayer();
                                mediaPlayer.setDataSource(this, uri);
                                mediaPlayer.prepare();

                                isAudio = true;
                                isVideo = false;

                                Toast.makeText(this, "Audio loaded. Press Play.", Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                Toast.makeText(this, "Error loading audio", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    }
            );

    ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                            Uri uri = result.getData().getData();

                            videoView.setVisibility(View.VISIBLE);


                            videoView.setVideoURI(uri);

                            videoView.setOnPreparedListener(mp -> videoView.start());

                            isVideo = true;
                            isAudio = false;
                        }
                    }
            );
    Runnable updateProgress = new Runnable() {
        @Override
        public void run() {

            try {
                int current = 0;
                int duration = 0;

                if (isAudio && mediaPlayer != null) {
                    current = mediaPlayer.getCurrentPosition();
                    duration = mediaPlayer.getDuration();
                }

                if (isVideo) {
                    current = videoView.getCurrentPosition();
                    duration = videoView.getDuration();
                }

                if (duration > 0) {
                    float progress = (current * 100f) / duration;
                    arcSeekBar.setProgress(progress);

                    txtStart.setText(formatTime(current));
                    txtEnd.setText(formatTime(duration));
                }

                handler.postDelayed(this, 500);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        videoView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        Button btnAudio = findViewById(R.id.btnAudio);
        Button btnVideo = findViewById(R.id.btnVideo);

        ImageButton btnPlay = findViewById(R.id.btnPlay);
        ImageButton btnPause = findViewById(R.id.btnPause);
        ImageButton btnStop = findViewById(R.id.btnStop);
        ImageButton btnRestart = findViewById(R.id.btnRestart);
        ImageView audioPlaceholder = findViewById(R.id.audioPlaceholder);

        arcSeekBar = findViewById(R.id.arcSeekBar);
        txtStart = findViewById(R.id.txtStart);
        txtEnd = findViewById(R.id.txtEnd);




        btnAudio.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            if (videoView != null) {
                videoView.stopPlayback();
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            audioPickerLauncher.launch(intent);
            audioPlaceholder.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
        });


        btnVideo.setOnClickListener(v -> {

            // Stop previous media
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            if (videoView != null) {
                videoView.stopPlayback();
            }

            audioPlaceholder.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);

            String[] options = {"Play from URL", "Select from Device"};

            new AlertDialog.Builder(this)
                    .setTitle("Choose Video Source")
                    .setItems(options, (dialog, which) -> {

                        if (which == 0) {
                            // ✅ URL OPTION (your existing code goes here)

                            EditText input = new EditText(this);
                            input.setHint("Enter Video URL");

                            new AlertDialog.Builder(this)
                                    .setTitle("Video URL")
                                    .setView(input)
                                    .setPositiveButton("Play", (d, w) -> {

                                        String url = input.getText().toString();

                                        MediaController mediaController = new MediaController(this);
                                        mediaController.setAnchorView(videoView);
                                        videoView.setMediaController(mediaController);

                                        videoView.setZOrderOnTop(true);
                                        videoView.setVideoURI(Uri.parse(url));

                                        videoView.setOnPreparedListener(mp -> {
                                            mp.setLooping(false);
                                            videoView.post(() -> videoView.start());
                                        });

                                        videoView.setOnErrorListener((mp, what, extra) -> {
                                            Toast.makeText(this, "Error playing video", Toast.LENGTH_LONG).show();
                                            return true;
                                        });

                                        isVideo = true;
                                        isAudio = false;
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();

                        } else {
                            // ✅ FILE OPTION

                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("video/*");
                            videoPickerLauncher.launch(intent);
                        }
                    })
                    .show();
        });

        btnPlay.setOnClickListener(v -> {

            if (isAudio && mediaPlayer != null) {
                mediaPlayer.start();
            }

            if (isVideo) {
                videoView.start();
            }

            handler.post(updateProgress);
        });

        btnPause.setOnClickListener(v -> {

            if (isAudio && mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }

            if (isVideo && videoView.isPlaying()) {
                videoView.pause();
            }
        });

        btnStop.setOnClickListener(v -> {

            if (isAudio && mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;

                // Show placeholder again
                audioPlaceholder.setVisibility(View.GONE);
                videoView.setVisibility(View.GONE);
            }

            if (isVideo) {
                videoView.stopPlayback();

                // Reset video UI
                videoView.setVisibility(View.GONE);
                audioPlaceholder.setVisibility(View.GONE);
            }

            handler.removeCallbacks(updateProgress);
            arcSeekBar.setProgress(0);
            txtStart.setText("0:00");
            txtEnd.setText("0:00");

            isAudio = false;
            isVideo = false;
        });

        btnRestart.setOnClickListener(v -> {

            if (isAudio && mediaPlayer != null) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }

            if (isVideo) {
                videoView.seekTo(0);
                videoView.start();
            }
        });

        Runnable updateProgress = new Runnable() {
            @Override
            public void run() {

                try {
                    int current = 0;
                    int duration = 0;

                    if (isAudio && mediaPlayer != null) {
                        current = mediaPlayer.getCurrentPosition();
                        duration = mediaPlayer.getDuration();
                    }

                    if (isVideo) {
                        current = videoView.getCurrentPosition();
                        duration = videoView.getDuration();
                    }

                    if (duration > 0) {
                        float progress = (current * 100f) / duration;
                        arcSeekBar.setProgress(progress);

                        txtStart.setText(formatTime(current));
                        txtEnd.setText(formatTime(duration));
                    }

                    handler.postDelayed(this, 500);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };




    }
}