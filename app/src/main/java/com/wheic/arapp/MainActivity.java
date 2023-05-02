package com.wheic.arapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ArFragment arCam;

    Trace myTrace = FirebasePerformance.getInstance().newTrace("my_trace");

//object of ArFragment Class


    private int clickNo = 0; //helps to render the 3d model only once when we tap the screen

    public static boolean checkSystemSupport(Activity activity) {

        //checking whether the API version of the running Android >= 24 that means Android Nougat 7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            String openGlVersion = ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE))).getDeviceConfigurationInfo().getGlEsVersion();

            //checking whether the OpenGL version >= 3.0
            if (Double.parseDouble(openGlVersion) >= 3.0) {
                return true;
            } else {
                Toast.makeText(activity, "App needs OpenGl Version 3.0 or later", Toast.LENGTH_SHORT).show();
                activity.finish();
                return false;
            }
        } else {
            Toast.makeText(activity, "App does not support required Build Version", Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //firebase performance SDK
        FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);
        myTrace.start();


        //to inherit the above firebase storage reference

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        //button for the crash in the app

        Button crashButton = new Button(this);
        crashButton.setText("Crash");
        crashButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                throw new RuntimeException("Crash"); // Force a crash
            }
        });
        addContentView(crashButton, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        if (checkSystemSupport(this)) {



            StorageReference modelRef = storageRef.child("https://firebasestorage.googleapis.com/v0/b/webarvsar.appspot.com/o/models%2Fhummingbird.glb?alt=media&token=c98041b4-aff3-4187-93b4-cdf50bdbde2c");
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);
            final String GLTF_ASSET =
                    "https://firebasestorage.googleapis.com/v0/b/webarvsar.appspot.com/o/models%2FDragon%20animation%20standing.glb?alt=media&token=092320bb-4f0d-40c4-95b8-8015736d17ff";
            final String GLTF_ASSET_1 = "https://firebasestorage.googleapis.com/v0/b/webarvsar.appspot.com/o/models%2Fmilk_delivery.glb?alt=media&token=5c6a0bc5-25e2-4802-a251-ddd719501a8b";




            arCam = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCameraArea);
            //ArFragment is linked up with its respective id used in the activity_main.xml

            arCam.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
                FirebasePerformance.getInstance().startTrace("network_request");
                Anchor anchor = hitResult.createAnchor();
                myTrace.putAttribute("url", GLTF_ASSET);
                myTrace.putAttribute("method", "GET");

                ModelRenderable.builder()
                        .setSource(this, Uri.parse(GLTF_ASSET))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept(modelRenderable -> {
                            addModel(anchor, modelRenderable);
                            clickNo++;
                        })
                        .exceptionally(throwable -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage("Somthing is not right" + throwable.getMessage()).show();
                            return null;
                        });
            });



        }

        myTrace.stop();


    }

    private void addModel(Anchor anchor, ModelRenderable modelRenderable) {

        AnchorNode anchorNode = new AnchorNode(anchor);
        // Creating a AnchorNode with a specific anchor
        anchorNode.setParent(arCam.getArSceneView().getScene());
        //attaching the anchorNode with the ArFragment
        TransformableNode model = new TransformableNode(arCam.getTransformationSystem());
        model.setParent(anchorNode);
        //attaching the anchorNode with the TransformableNode
        model.setRenderable(modelRenderable);
        //attaching the 3d model with the TransformableNode that is already attached with the node
        model.select();

    }


}