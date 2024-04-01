package com.cmpe277.ocrapp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/**
 * Main activity class that serves as the entry point for the app.
 * This class handles image input for OCR (Optical Character Recognition),
 * requests necessary permissions, and displays the recognized text.
 */
public class MainActivity extends AppCompatActivity {

    private ShapeableImageView imageView;
    private TextView recognizedTextView;
    private ProgressDialog progressDialog;

    private Uri imageUri = null;

    private static final int CAMERA_REQUEST_CODE = 100;

    private String[] cameraPermissions;

    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button inputImageBtn = findViewById(R.id.inputImageBtn);
        Button orcBtn = findViewById(R.id.orcBtn);
        imageView = findViewById(R.id.imageIv);
        recognizedTextView = findViewById(R.id.recognizedText);

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        inputImageBtn.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                pickImageCamera();
            }
            else {
                requestCameraPermission();
            }
        });

        orcBtn.setOnClickListener(v -> {
            if (imageUri == null) {
                Toast.makeText(MainActivity.this, "Select An Image", Toast.LENGTH_SHORT).show();
            } else {
                recognizedImageText();
            }
        });

    }

    /**
     * Starts the process of recognizing text from the image captured or selected by the user.
     * Displays a progress dialog while processing and shows the recognized text or error messages.
     */
    private void recognizedImageText() {
        progressDialog.setMessage("Setting image.....");
        progressDialog.show();

        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            progressDialog.setMessage("Detecting Text.....");

            textRecognizer.process(inputImage)
                    .addOnSuccessListener(text -> {
                        progressDialog.dismiss();
                        recognizedTextView.setText(text.getText());
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                    });

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Launches the camera intent to capture an image.
     * Prepares ContentValues for image meta-data and handles the camera result.
     */
    private void pickImageCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Test Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Test DESC");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    /**
     * Checks if the application has been granted camera and storage permissions.
     * @return True if all required permissions are granted, false otherwise.
     */
    private boolean checkCameraPermission() {
        boolean camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return camera && storage;
    }

    /**
     * Requests runtime permissions necessary for the app, specifically camera and storage permissions.
     */
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            o -> {
                if (o.getResultCode() == Activity.RESULT_OK) {
                    imageView.setImageURI(imageUri);
                } else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on requestPermissions(android.app.Activity, String[], int).
     *
     * @param requestCode The request code passed in requestPermissions(android.app.Activity, String[], int).
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean cameraAllowed = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                if (cameraAllowed) {
                    pickImageCamera();
                } else {
                    Toast.makeText(this, "Permission Required", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
