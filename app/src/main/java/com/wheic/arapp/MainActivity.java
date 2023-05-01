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
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.exceptions.NotTrackingException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ArFragment arCam; //object of ArFragment Class

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


        //to inherit the above firebase storage reference

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        Trace myTrace = FirebasePerformance.getInstance().newTrace("my_trace");


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

            StorageReference modelRef = storageRef.child("models/Rover.glb");
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);



                    arCam = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCameraArea);
            //ArFragment is linked up with its respective id used in the activity_main.xml

            arCam.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
                clickNo++;
                //the 3d model comes to the scene only when clickNo is one that means once
                if (clickNo == 1) {
                    Anchor anchor = hitResult.createAnchor(); // declare a local variable here
                    FirebasePerformance.getInstance().startTrace("network_request");
                    ModelRenderable.builder()
                            .setSource(this, Uri.parse("https://firebasestorage.googleapis.com/v0/b/webarvsar.appspot.com/o/models%2Fhummingbird.glb?alt=media&token=c98041b4-aff3-4187-93b4-cdf50bdbde2c"))
                            .build()
                            .thenAccept(modelRenderable -> {
                                //FirebasePerformance.getInstance().stopTrace("network_request");
                                addModel(modelRenderable); // pass the anchor variable as an argument
                            })
                            .exceptionally(throwable -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setMessage("Something went wrong: " + throwable.getMessage()).show();
                                return null;
                            });
                }
            });


        } else {

            return;

        }


    }

    private void addModel(ModelRenderable modelRenderable) {

        // Convert Sceneform Vector3 to ARCore Vector3
        Vector3 worldPosition = arCam.getArSceneView().getScene().getCamera().getWorldPosition();
        Vector3 forward = arCam.getArSceneView().getScene().getCamera().getForward();
        float[] arCoreWorldPosition = new float[]{worldPosition.x, worldPosition.y, -worldPosition.z};
        float[] arCoreForward = new float[]{forward.x, forward.y, -forward.z};

        // Create a pose at the camera position and facing forward
        Pose cameraPose = new Pose(arCoreWorldPosition, arCoreForward);

        // Create an anchor at the camera pose
        Anchor anchor = Objects.requireNonNull(arCam.getArSceneView().getSession()).createAnchor(cameraPose);

        // Creating a new anchor at the camera position
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arCam.getArSceneView().getScene());

        // Create a TransformableNode for the model
        TransformableNode model = new TransformableNode(arCam.getTransformationSystem());
        model.setParent(anchorNode);
        model.setRenderable(modelRenderable);
        model.select();
    }


}