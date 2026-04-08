package com.example.imagegallery;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_detail);

        ImageView ivImage = findViewById(R.id.ivDetailImage);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvPath = findViewById(R.id.tvPath);
        TextView tvSize = findViewById(R.id.tvSize);
        TextView tvDate = findViewById(R.id.tvDate);
        Button btnDelete = findViewById(R.id.btnDelete);

        // Get URI from Intent
        String uriString = getIntent().getStringExtra("image_path");
        if (uriString == null) return;

        Uri imageUri = Uri.parse(uriString);
        DocumentFile documentFile = DocumentFile.fromSingleUri(this, imageUri);

        if (documentFile != null && documentFile.exists()) {
            ivImage.setImageURI(imageUri);

            // View details using DocumentFile
            tvName.setText("Name: " + documentFile.getName());
            tvPath.setText("URI: " + imageUri.toString());

            long sizeKb = documentFile.length() / 1024;
            tvSize.setText("Size: " + sizeKb + " KB");

            String lastMod = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    .format(new Date(documentFile.lastModified()));
            tvDate.setText("Date: " + lastMod);
        }

        // Delete button
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to delete this image?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (documentFile != null && documentFile.delete()) {
                            Toast.makeText(this, "Image Deleted", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to delete (Permission Denied)", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}