package com.example.imagegallery;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_CODE_PICK_FOLDER = 102;
    private static final int REQUEST_CODE_PICK_FOLDER_FOR_SAVE = 103;
    private static final int REQUEST_CODE_IMAGE_DETAIL = 104;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String PREFS_NAME = "image_gallery_prefs";
    private static final String KEY_SAVE_FOLDER_URI = "save_folder_uri";

    private Uri currentFolderUri;
    private Uri saveFolderUri;
    private String currentPhotoPath;
    private RecyclerView recyclerView;
    private TextView tvImageCount;
    private TextView tvFolderName;
    private TextView tvFolderPath;
    private ImageAdapter adapter;
    private final List<Uri> imageUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewGallery);
        tvImageCount = findViewById(R.id.tvImageCount);
        tvFolderName = findViewById(R.id.tvFolderName);
        tvFolderPath = findViewById(R.id.tvFolderPath);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ImageAdapter(this, imageUris, this::openImageDetail);
        recyclerView.setAdapter(adapter);

        restoreSaveFolderUri();
        updateSelectedFolderLabel();
        if (saveFolderUri != null) {
            currentFolderUri = saveFolderUri;
            loadImagesFromFolder(currentFolderUri);
        }

        // Part A: Take Photo -> Choose Folder -> Camera
        findViewById(R.id.fabCamera).setOnClickListener(v -> {
            if (saveFolderUri != null) {
                checkPermissionAndOpenFile();
            } else {
                openFolderPickerForSaving();
            }
        });

        // Part B: View Folder
        findViewById(R.id.titleText).setOnClickListener(v -> openFolderPicker());
        findViewById(R.id.cardSelectedFolder).setOnClickListener(v -> openFolderPickerForSaving());
    }

    private void restoreSaveFolderUri() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUri = prefs.getString(KEY_SAVE_FOLDER_URI, null);
        if (savedUri != null) {
            saveFolderUri = Uri.parse(savedUri);
        }
    }

    private void persistSaveFolderUri(Uri uri) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVE_FOLDER_URI, uri.toString())
                .apply();
    }

    private void updateSelectedFolderLabel() {
        if (saveFolderUri == null) {
            tvFolderName.setText("SELECT FOLDER");
            tvFolderPath.setText("Tap to choose");
            return;
        }

        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, saveFolderUri);
        String folderName = pickedDir != null && pickedDir.getName() != null
                ? pickedDir.getName()
                : "Saved folder";
        tvFolderName.setText(folderName.toUpperCase());
        tvFolderPath.setText(saveFolderUri.toString());
    }

    private void openFolderPickerForSaving() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER_FOR_SAVE);
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    private void openImageDetail(Uri uri) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("image_path", uri.toString());
        try {
            startActivityForResult(intent, REQUEST_CODE_IMAGE_DETAIL);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Unable to open details", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImagesFromFolder(Uri folderUri) {
        imageUris.clear();
        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, folderUri);
        if (pickedDir != null) {
            List<DocumentFile> files = new ArrayList<>();
            for (DocumentFile file : pickedDir.listFiles()) {
                if (file.getType() != null && file.getType().startsWith("image/")) {
                    files.add(file);
                }
            }

            // Newest first so recent images are always shown on top.
            Collections.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (DocumentFile file : files) {
                imageUris.add(file.getUri());
            }
        }
        adapter.notifyDataSetChanged();
        updateImageCount();
    }

    private void checkPermissionAndOpenFile() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }

        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File vaultDir = new File(storageDir, "UserChoice");
        if (!vaultDir.exists()) vaultDir.mkdirs();

        File image = File.createTempFile("IMG_" + timeStamp + "_", ".jpg", vaultDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void saveImageToChosenFolder(File sourceFile, Uri destFolderUri) {
        try {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, destFolderUri);
            if (pickedDir == null) {
                Toast.makeText(this, "Selected folder is not accessible", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentFile newFile = pickedDir.createFile("image/jpeg", sourceFile.getName());

            if (newFile != null) {
                InputStream in = new FileInputStream(sourceFile);
                OutputStream out = getContentResolver().openOutputStream(newFile.getUri());

                if (out == null) {
                    in.close();
                    Toast.makeText(this, "Unable to create destination file", Toast.LENGTH_SHORT).show();
                    return;
                }

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                sourceFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateImageCount() {
        String countText = String.format(getString(R.string.images_count), imageUris.size());
        tvImageCount.setText(countText);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK_FOLDER_FOR_SAVE && data != null) {
                saveFolderUri = data.getData();
                if (saveFolderUri == null) {
                    return;
                }
                // Grant permanent access to this folder
                getContentResolver().takePersistableUriPermission(saveFolderUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                persistSaveFolderUri(saveFolderUri);
                updateSelectedFolderLabel();
                currentFolderUri = saveFolderUri;
                loadImagesFromFolder(currentFolderUri);
                checkPermissionAndOpenFile();

            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // PHOTO TAKEN! Now copy it to the chosen folder URI
                if (saveFolderUri != null && currentPhotoPath != null) {
                    saveImageToChosenFolder(new File(currentPhotoPath), saveFolderUri);
                    Toast.makeText(this, "Photo Saved to Chosen Folder!", Toast.LENGTH_SHORT).show();
                    currentFolderUri = saveFolderUri;
                    loadImagesFromFolder(currentFolderUri);
                }

            } else if (requestCode == REQUEST_CODE_PICK_FOLDER && data != null) {
                currentFolderUri = data.getData();
                if (currentFolderUri != null) {
                    loadImagesFromFolder(currentFolderUri);
                }
            } else if (requestCode == REQUEST_CODE_IMAGE_DETAIL) {
                if (resultCode == RESULT_OK) {
                    Uri folderToReload = currentFolderUri != null ? currentFolderUri : saveFolderUri;
                    if (folderToReload != null) {
                        loadImagesFromFolder(folderToReload);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        }
    }
}