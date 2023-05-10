package com.wheic.arapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {

    private ArFragment arCam;

    static Trace myTrace = FirebasePerformance.getInstance().newTrace("my_trace");
    Trace network_requests = FirebasePerformance.getInstance().newTrace("Network_Requests");



//object of ArFragment Class


    private int clickNo = 0; //helps to render the 3d model only once when we tap the screen

    public static boolean checkSystemSupport(Activity activity) {
        myTrace.start();

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

            myTrace.getAppState();

            network_requests.start();
            final String GLTF_ASSET_1 = "https://firebasestorage.googleapis.com/v0/b/webarvsar.appspot.com/o/models%2Fhummingbird.glb?alt=media&token=c98041b4-aff3-4187-93b4-cdf50bdbde2c";

            OkHttpClient client = new OkHttpClient.Builder()

                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request request = chain.request();

                            // Start timing the request
                            long startTime = System.nanoTime();

                            // Make the request
                            Response response = chain.proceed(request);

                            // Stop timing the request
                            long endTime = System.nanoTime();
                            long duration = endTime - startTime;

                            // Log the request and response
                            Log.d("NetworkTrace", request.url().toString() + " took " + duration + "ns");

                            return response;
                        }
                    })
                    .build();





            StorageReference modelRef = storageRef.child("https://firebasestorage.googleapis.com/v0/b/webarvsar.appspot.com/o/models%2Fhummingbird.glb?alt=media&token=c98041b4-aff3-4187-93b4-cdf50bdbde2c");
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);
            final String GLTF_ASSET =
                    "https://firebasestorage.googleapis.com/v0/b/webarvsar.appspot.com/o/test_models%2Fmodel_1_m.glb?alt=media&token=fd67766e-dda3-4ee0-b38b-9c131aad92db";          Request request = new Request.Builder()
                    .url(GLTF_ASSET)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Handle failure
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // Handle success
                }
            });


            //Response response = client.newCall(request).execute();





            arCam = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCameraArea);
            //ArFragment is linked up with its respective id used in the activity_main.xml

            arCam.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
                Anchor anchor = hitResult.createAnchor();

                ModelRenderable.builder()
                        .setSource(this, Uri.parse(String.valueOf(GLTF_ASSET)))
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




    }

    private void addModel(Anchor anchor, ModelRenderable modelRenderable) {
        network_requests.start();

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
        network_requests.stop();

    }

    public Trace getNetwork_requests() {

        return network_requests;
    }
}