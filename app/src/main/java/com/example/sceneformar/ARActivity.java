package com.example.sceneformar;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.PlaybackFailedException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class ARActivity extends AppCompatActivity {
    private String name;
    private ArFragment arCam;
    HashMap<String, ARProductData> hashMap = new HashMap();
    // helps to render the 3d model
    // only once when we tap the screen
    private int clickNo = 0;
    private static final String TAG = ARActivity.class.getSimpleName();

    // Coverage manager for handling JaCoCo coverage collection

    public static boolean checkSystemSupport(Activity activity) {

        // checking whether the API version of the running Android >= 24
        // that means Android Nougat 7.0


        String openGlVersion = ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE))).getDeviceConfigurationInfo().getGlEsVersion();

        // checking whether the OpenGL version >= 3.0
        if (Double.parseDouble(openGlVersion) >= 3.0) {
            return true;
        } else {
            Toast.makeText(activity, "App needs OpenGl Version 3.0 or later", Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_ar);
        hashMap.put("Laptop (Windows 10)", new ARProductData(R.raw.laptop_ar_flipkart, 0.3f, 1.5f));
        hashMap.put("Smart TV (Android)", new ARProductData(R.raw.monitor_ar_flipkart, 0.4f, 2.0f));
        hashMap.put("Gaming PC (Windows 11, AMD)", new ARProductData(R.raw.pc_ar_flipkart, 0.3f, 1.8f));
        hashMap.put("Washing Machine (5kg)", new ARProductData(R.raw.fridge_ar_flipkart, 0.5f, 2.5f));
        hashMap.put("Wooden bed (Double)", new ARProductData(R.raw.wodden_beg_ar_flipkart, 0.4f, 2.0f));
        hashMap.put("Swing chair", new ARProductData(R.raw.swing_chair_ar_flipkart, 0.3f, 2.0f));
        hashMap.put("Sofa (black)", new ARProductData(R.raw.sofa_ar_flipkart, 0.4f, 2.5f));
        hashMap.put("Bed lamp", new ARProductData(R.raw.lamp_ar_flipkart, 0.2f, 1.5f));
        hashMap.put("Soldier toy", new ARProductData(R.raw.soldier_ar_flipkart, 0.2f, 2.0f));
        hashMap.put("Teddy bear (15 cm x40 cm)", new ARProductData(R.raw.teddy_ar_flipkart, 0.2f, 3.0f));
        hashMap.put("Bicycle (5 gears)", new ARProductData(R.raw.cycle_ar_view, 0.3f, 2.0f));
        hashMap.put("Chick toy", new ARProductData(R.raw.chick, 0.1f, 2.0f));


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            name = extras.getString("name");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
            }, 0);
        } else {
            // For Android versions below Marshmallow, permissions are granted at install time
            Log.d(TAG, "Permissions not required for Android versions below Marshmallow");
        }

        if (checkSystemSupport(this)) {
            arCam = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCameraArea);

            // Set up session initialization listener
            arCam.setOnSessionInitializationListener(session -> {
                //String playbackDatasetPath = "/storage/emulated/0/Download/ARVideos/ritgarden_bright_big_outdoor.mp4";
                String playbackDatasetPath = copyAssetToCache("ritgarden_bright_big_outdoor.mp4");
                try {
                    File playbackFile = new File(playbackDatasetPath);
                    if (!playbackFile.exists()) {
                        throw new RuntimeException("Playback file not found: " + playbackDatasetPath);
                    }

                    // Get the existing config from fragment instead of creating new
                    Config config = session.getConfig();

                    // Add playback config to existing session configuration
                    config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                    session.setPlaybackDatasetUri(Uri.fromFile(playbackFile));

                    session.configure(config);

                } catch (PlaybackFailedException | RuntimeException e) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "AR Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );

                }
            });

            assert arCam != null;
            arCam.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
                clickNo++;

                // the 3d model comes to the scene only
                // when clickNo is one that means once
                if (clickNo == 1) {
                    Anchor anchor = hitResult.createAnchor();
                    ModelRenderable.builder()
                            .setSource(this, hashMap.get(name).getId())
                            .setIsFilamentGltf(true)
                            .build()
                            .thenAccept(modelRenderable -> {
                                addModel(anchor, modelRenderable);
                                // Show gesture instructions to user
                                runOnUiThread(() -> {
                                    Toast.makeText(this,
                                            "Gestures: Pinch to scale, Rotate with two fingers, Drag to move",
                                            Toast.LENGTH_LONG).show();
                                });
                            })
                            .exceptionally(throwable -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setMessage("Something is not right" + throwable.getMessage()).show();
                                return null;
                            });
                }
            });
        }
    }

    private void addModel(Anchor anchor, ModelRenderable modelRenderable) {

        // Creating a AnchorNode with a specific anchor
        AnchorNode anchorNode = new AnchorNode(anchor);

        // attaching the anchorNode with the ArFragment
        anchorNode.setParent(arCam.getArSceneView().getScene());

        // attaching the anchorNode with the TransformableNode
        TransformableNode model = new TransformableNode(arCam.getTransformationSystem());
        model.setParent(anchorNode);
        // Enable scaling with proper min/max limits
        model.getScaleController().setMaxScale(hashMap.get(name).getMax());
        model.getScaleController().setMinScale(hashMap.get(name).getMin());

        // attaching the 3d model with the TransformableNode
        // that is already attached with the node
        model.setRenderable(modelRenderable);
        model.select();


    }

    /**
     * Copy video file from assets to cache directory
     */
    private String copyAssetToCache(String assetName) {
        File cacheFile = new File(getCacheDir(), assetName);

        // If already cached, return the path
        if (cacheFile.exists()) {
            Log.d(TAG, "Using cached video: " + cacheFile.getAbsolutePath());
            return cacheFile.getAbsolutePath();
        }

        try (InputStream in = getAssets().open(assetName);
             FileOutputStream out = new FileOutputStream(cacheFile)) {

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            Log.d(TAG, "Video copied to cache: " + cacheFile.getAbsolutePath());
            return cacheFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error copying asset: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

