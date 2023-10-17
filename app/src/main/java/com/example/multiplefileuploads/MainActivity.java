package com.example.multiplefileuploads;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    TextView uploadFilesButton;
    Dialog d;
    Context context;
    String fileType, currentPhotoPath;
    boolean isImageTaken;
    Bitmap rotatedBitmap, insurer;
    File destination;
    GridView imagesGrid;
    Activity activity;
    ArrayList<imagesModel> myImageArrayList;
    int num=0;

    imagesAdapter myImagesAdapter;

    public static int PICKFILE_IMAGE =100, PICKFILE_DOCUMENT =101, PICKFILE_VIDEO =102, PICKFILE_AUDIO =103;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context=this;
        activity=this;

        uploadFilesButton=findViewById(R.id.uploadFiles);
        imagesGrid=findViewById(R.id.imagesGrid);
        myImageArrayList=new ArrayList<>();

        uploadFilesButton.setOnClickListener(l->{
            try{
                if(myImageArrayList.size()==0){
                    num=0;
                }
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                //inflating the custom layout otp_pop_up
                View v = inflater.inflate(R.layout.upload_dialog_fvo, null, false);
                d = new Dialog(context);
                //setting the pop-up layout to Dialog d
                d.setContentView(v);
                d.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                d.create();
                d.setCancelable(false);
                d.show();

                ImageView closeDialog=v.findViewById(R.id.closeDialog);
                LinearLayout cameraLayout,imageLayout,docLayout;
                cameraLayout=v.findViewById(R.id.cameraLayout);
                imageLayout=v.findViewById(R.id.imageLayout);
                docLayout=v.findViewById(R.id.docLayout);
                cameraLayout.setOnClickListener(a->{
                    try{
                        dispatchTakePictureIntent();
                        if(d.isShowing()) {
                            d.dismiss();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
                imageLayout.setOnClickListener(a->{
                    try{
                        uploadImage();
                        if(d.isShowing()) {
                            d.dismiss();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
                docLayout.setOnClickListener(a->{
                    try{
                        uploadDocuments();
                        if(d.isShowing()) {
                            d.dismiss();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
                closeDialog.setOnClickListener(a->{
                    try{
                        if(d.isShowing()) {
                            d.dismiss();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    public void createFileFromDocument(DocumentFile documentFile) throws IOException {
        // Get the file name and extension
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";

        File destinationDirectory=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/android/data/com.example.multiplefileuploads/files/images");
        if(!destinationDirectory.exists()){
            destinationDirectory.mkdirs();
        }
        // Create a new file in the specified directory
        File file = new File(destinationDirectory, imageFileName);

        // Get the input stream from the document file
        InputStream inputStream = getContentResolver().openInputStream(documentFile.getUri());
//        Bitmap currentImage = BitmapFactory.decodeFile();
//        currentImage = rotateImage(currentImage, 90);
        // Create the output stream for the new file
        OutputStream outputStream = new FileOutputStream(file);

        // Copy the contents of the document file to the new file
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }




        num=num+1;

        uploadFileImages(file,num);

        // Close the streams
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void processUriToFile(List<Uri> uris) {
        new Thread(() -> {
            try {
                for (Uri uri : uris) {
                    DocumentFile pickedFile = DocumentFile.fromSingleUri(this, uri);
                    if (pickedFile != null) {
                        createFileFromDocument(pickedFile);
                    }
                }
                /*runOnUiThread(() -> {
                    imageFiles = loadImagesFromDirectory();
                    imageAdapter.notifyDataSetChanged();
                    loader.dismiss();
                });*/
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    void uploadImage(){
        try{
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
            else {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                String[] imgTypes = {"image/png", "image/jpg", "image/jpeg"};
                chooseFile.setType("*/*");
                chooseFile.putExtra(Intent.EXTRA_MIME_TYPES, imgTypes);
                chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(
                        Intent.createChooser(chooseFile, "Choose file/s"),
                        PICKFILE_IMAGE);
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    void uploadDocuments(){
        try{
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
            else {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.setType("application/pdf");
                startActivityForResult(
                        Intent.createChooser(chooseFile, "Choose a file"),
                        PICKFILE_DOCUMENT);
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_IMAGE && resultCode == Activity.RESULT_OK){

            try {
                List<Uri> uris = new ArrayList<>();
                ClipData clipData = data.getClipData();

                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        try {
                            uris.add((data.getClipData().getItemAt(i).getUri()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // Single file selected
                    try {
                        uris.add(data.getData());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                    Collections.reverse(uris);
                    processUriToFile(uris);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (requestCode == PICKFILE_DOCUMENT && resultCode == Activity.RESULT_OK){

            try {
                Uri content_describer = data.getData();

                Cursor returnCursor =
                        getContentResolver().query(content_describer, null, null, null, null);
                int fileNameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                String fileName = returnCursor.getString(fileNameIndex);


                File destinationDirectory=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/android/data/com.starhealth.intelligentfvr/files/documents");
                if(!destinationDirectory.exists()){
                    destinationDirectory.mkdirs();
                }
                destination = new File(destinationDirectory + "/" + fileName);

                ContentResolver cr = getContentResolver();
                InputStream inputStream = cr.openInputStream(content_describer);

                FileOutputStream outputStream = new FileOutputStream(destination);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                String[] type=fileName.split("\\.");
                fileType=type[type.length-1];
                //uploadFile("application/"+fileType);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (requestCode == PICKFILE_VIDEO && resultCode == Activity.RESULT_OK){

            try {
                Uri content_describer = data.getData();

                Cursor returnCursor =
                        getContentResolver().query(content_describer, null, null, null, null);
                int fileNameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                String fileName = returnCursor.getString(fileNameIndex);


                File destinationDirectory=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/android/data/com.starhealth.intelligentfvr/files/video");
                if(!destinationDirectory.exists()){
                    destinationDirectory.mkdirs();
                }
                destination = new File(destinationDirectory + "/" + fileName);

                ContentResolver cr = getContentResolver();
                InputStream inputStream = cr.openInputStream(content_describer);

                FileOutputStream outputStream = new FileOutputStream(destination);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                String[] type=fileName.split("\\.");
                fileType=type[type.length-1];
                //uploadFile("video/"+fileType);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (requestCode == PICKFILE_AUDIO && resultCode == Activity.RESULT_OK){

            try {
                Uri content_describer = data.getData();

                Cursor returnCursor =
                        getContentResolver().query(content_describer, null, null, null, null);
                int fileNameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                String fileName = returnCursor.getString(fileNameIndex);


                File destinationDirectory=new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/android/data/com.starhealth.intelligentfvr/files/audio");
                if(!destinationDirectory.exists()){
                    destinationDirectory.mkdirs();
                }
                destination = new File(destinationDirectory + "/" + fileName);

                ContentResolver cr = getContentResolver();
                InputStream inputStream = cr.openInputStream(content_describer);

                FileOutputStream outputStream = new FileOutputStream(destination);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                String[] type=fileName.split("\\.");
                fileType=type[type.length-1];
                //uploadFile("audio/"+fileType);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File

                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.starhealth.intelligentfvr.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                    getResult.launch(takePictureIntent);

                }
            }
            else{
                //for pixel mobile phones
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File

                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.starhealth.intelligentfvr.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                    getResult.launch(takePictureIntent);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Method to create a file where the captured images is going to save.
    private File createImageFile() throws IOException {
        try {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */

            );

            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    } // End of createImageFile().

    // Method to convert captured image to bitmap.
    public Bitmap getBitmap(String path) {
        Bitmap bitmap = null;
        try {
            File f = new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
            //image.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    } // End of getBitmap().

    // Getting the stored image from the path as bitmap.
    public Bitmap getValidBitmap(String path) {
        Bitmap bitmap = null;
        try {
            File f = new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);

            //image.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }// End of getValidBitmap.

        return bitmap;
    }

    public ActivityResultLauncher<Intent> getResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                try {
                    if (result.getResultCode() == RESULT_OK) {
                        // Checking if the image has a value or not.
                        if (!(getValidBitmap(currentPhotoPath) == null)) {
                            insurer = getBitmap(currentPhotoPath);
                            ExifInterface ei = null;
                            try {
                                ei = new ExifInterface(currentPhotoPath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            assert ei != null;
                            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_UNDEFINED);


                            // To rotate the image based on the image angle.
                            switch (orientation) {

                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    try {
                                        rotatedBitmap = rotateImage(insurer, 90);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;

                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    try {
                                        rotatedBitmap = rotateImage(insurer, 180);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;

                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    try {
                                        rotatedBitmap = rotateImage(insurer, 270);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;

                                case ExifInterface.ORIENTATION_NORMAL:
                                default:
                                    try {
                                        rotatedBitmap = insurer;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                            }

                            try {
                                // Assigning the visibility based on the image.
                                if (rotatedBitmap != null) {
                                    isImageTaken = true;
                                    //uploadImageFile(rotatedBitmap);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    void uploadFileImages(File file,int number){
        try{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    myImageArrayList.add(new imagesModel("",number,file));
                    myImagesAdapter=new imagesAdapter(context,myImageArrayList,activity);
                    imagesGrid.setAdapter(myImagesAdapter);
                    imagesGrid.setVisibility(View.VISIBLE);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}