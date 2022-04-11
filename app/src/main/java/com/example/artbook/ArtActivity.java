package com.example.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.example.artbook.databinding.ActivityArtBinding;
import com.example.artbook.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
        registerLauncher();
        Intent intent=getIntent();
        String info=intent.getStringExtra("info");
        if(info.equals("new")){
            binding.textName.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);

        }else{
            binding.button.setVisibility(View.INVISIBLE);
            int artId=intent.getIntExtra("artId",0);

            try {
                Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id=?",new String[]{String.valueOf(artId)});
                int artIx=cursor.getColumnIndex("artname");
                int painterIx=cursor.getColumnIndex("paintername");
                int yearIx=cursor.getColumnIndex("year");
                int imageIx=cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.textName.setText(cursor.getString(artIx));
                    binding.artistText.setText(cursor.getString(painterIx));
                    binding.yearText.setText(cursor.getString(yearIx));
                    byte[] bytes=cursor.getBlob(imageIx);
                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public void save(View view){
        String name=binding.textName.getText().toString();
        String artist=binding.artistText.getText().toString();
        String year=binding.yearText.getText().toString();
        if(name.equals("")){
            Toast.makeText(this,"Adı Boş Olamaz",Toast.LENGTH_LONG).show();
            return;
        }
        if(artist.equals("")){
            Toast.makeText(this,"Artist Adı Boş Olamaz",Toast.LENGTH_LONG).show();
            return;
        }
        if(year.equals("")){
            Toast.makeText(this,"Yıl Boş Olamaz",Toast.LENGTH_LONG).show();
            return;
        }
        if(selectedImage==null){
            Toast.makeText(this,"Resim Boş Olamaz",Toast.LENGTH_LONG).show();
            return;
        }
        Bitmap smallImage=makeSmallerImage(selectedImage,300);

        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] array=outputStream.toByteArray();

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,paintername VARCHAR,year VARCHAR,image BLOB)");

            String sql="INSERT INTO arts (artname,paintername,year,image) VALUES (?,?,?,?)";

            SQLiteStatement sqLiteStatement=database.compileStatement(sql);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artist);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,array);
            sqLiteStatement.execute();
        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent=new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Toast.makeText(this,"Kayıt Başarılı Ana Sayfaya Yönlendiriliyorsunuz.",Toast.LENGTH_LONG).show();
        startActivity(intent);


        

    }

    public Bitmap makeSmallerImage(Bitmap image,int maxSize){
        int width= image.getWidth();
        int height=image.getHeight();
        float ratio=(float)width/(float)height;

        if(ratio>1){
            //landscape
            width=maxSize;
            height= (int) (width/ratio);
        }else{
            //portrait
            height=maxSize;
            width= (int) (height*ratio);
        }
        Bitmap resize=Bitmap.createScaledBitmap(image,width,height,true);
        return resize;
    }

    public void selectImage(View view){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //request persmission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            }else{
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);


            }
        }else{
             //process
            Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }
    }

    private void registerLauncher(){

        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()==RESULT_OK){
                    Intent intentFromResult=result.getData();
                    if(intentFromResult!=null){
                        Uri imageData=intentFromResult.getData();
                        //binding.imageView.setImageURI(imageData);
                        try {
                            if(Build.VERSION.SDK_INT>=28){
                                ImageDecoder.Source source=ImageDecoder.createSource(getContentResolver(),imageData);
                                selectedImage=ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            }else{
                                selectedImage=MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //perm. granted
                    Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }else{
                    //denied
                    Toast.makeText(ArtActivity.this,"Permission needed",Toast.LENGTH_LONG).show();
                }
            }
        });
    }


}