package com.example.bruintrash;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionCloudImageLabelerOptions;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity
{
    int foundType;
    ArrayList<String> landfill;
    ArrayList<String> compost;
    ArrayList<String> recycling;
    int counter;
    private static final String TAG = "Nice";
    ImageView imageView;
    FirebaseVisionImage image;
    Button Analyze;
    // Access a Cloud Firestore instance from your Activity
    FirebaseFirestore db;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        counter=0;
        foundType=-1;
        landfill=new ArrayList<String>();
        compost=new ArrayList<String>();
        recycling=new ArrayList<String>();

        db= FirebaseFirestore.getInstance();
        db.collection("trash")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful())
                        {
                            for (QueryDocumentSnapshot document : task.getResult())
                            {
                                if (counter==0)
                                {
                                    String innerCount;
                                    for (int i = 0; i<document.getData().size();i++)
                                    {
                                        innerCount=Integer.toString(i+1);
                                        compost.add((document.getData()).get(innerCount).toString());
                                        Log.e("Compost",compost.get(i));
                                    }

                                }

                                if (counter==1)
                                {
                                    String innerCount;
                                    for (int i = 0; i<document.getData().size();i++)
                                    {
                                        innerCount=Integer.toString(i+1);
                                        landfill.add(document.getData().get(innerCount).toString());
                                        Log.e("Landfill",landfill.get(i));
                                    }

                                }

                                if (counter==2)
                                {
                                    String innerCount;
                                    for (int i = 0; i<document.getData().size();i++)
                                    {
                                        innerCount=Integer.toString(i+1);
                                        recycling.add(document.getData().get(innerCount).toString());
                                        Log.e("Recycling",recycling.get(i));
                                    }

                                }
                                counter++;
                                //Log.e(TAG, document.getId() + " => " + document.getData());
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
        ///////////////////////////////////////////////////////////////////
        //these lines are actually a work around because I am using Picasso
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        //end of workaround lines
        ///////////////////////////////////////////////////////////////////
        //Initialisation of global variables can only take place inside the onCreate method
        imageView=findViewById(R.id.image);
        Analyze=findViewById(R.id.analyze);
        Button photoButton = (Button) findViewById(R.id.photobutton);
        photoButton.setOnClickListener(new  View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dispatchTakePictureIntent();

            }
        });
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            //imageView.setImageBitmap(imageBitmap);
            galleryAddPic();
        }
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        File storageDir = Environment.getExternalStorageDirectory();
        File image = File.createTempFile(
                "example",  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        final Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        Picasso.get().load(contentUri).into(imageView);
        Analyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    image = FirebaseVisionImage.fromFilePath(getBaseContext(), contentUri);
                    FirebaseVisionCloudImageLabelerOptions options = new FirebaseVisionCloudImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.7f)
                    .build();
                    FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getCloudImageLabeler(options);

                    labeler.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                    // Task completed successfully
                                    // ...
                                    for (FirebaseVisionImageLabel label: labels)
                                    {
                                            if (foundType==-1)
                                            {
                                                String text = label.getText();
                                                for (int i = 0; i < 3; i++) {
                                                    for (int j = 0; i < landfill.size(); i++) {
                                                        if (text.equals(landfill.get(i))) {
                                                            Log.e("FINAL", "landfill");
                                                            Intent intent = new Intent(getBaseContext(), landfill.class);
                                                            startActivity(intent);
                                                            foundType = 0;
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (foundType!=-1) {

                                                    for (int i = 0; i < 3; i++) {
                                                        for (int j = 0; i < compost.size(); i++) {
                                                            if (text.equals(compost.get(i))) {
                                                                foundType = 1;
                                                                Log.e("FINAL", "compost");
                                                                Intent intent = new Intent(getBaseContext(), compost.class);
                                                                startActivity(intent);

                                                                break;
                                                            }
                                                        }
                                                    }
                                                }

                                                if (foundType!=-1) {

                                                    for (int i = 0; i < 3; i++) {
                                                        for (int j = 0; i < recycling.size(); i++) {
                                                            if (text.equals(recycling.get(i))) {
                                                                Log.e("FINAL", "recycling");
                                                                Intent intent = new Intent(getBaseContext(), recycling.class);
                                                                startActivity(intent);
                                                                foundType = 2;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }

                                            }
                                            String text=label.getText();
                                        Log.e("Labels",text);
                                    }
                                    if (foundType==-1)
                                    {
                                        Intent intent = new Intent(getApplicationContext(), none.class);
                                        startActivity(intent);

                                    }
                                }


                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    // ...
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }
}



