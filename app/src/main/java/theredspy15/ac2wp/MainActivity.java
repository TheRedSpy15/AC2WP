package theredspy15.ac2wp;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import theredspy15.ac2wp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    protected static final String[] BASE_PROJECTION = new String[]{
            MediaStore.Audio.AudioColumns.ALBUM_ID,
            MediaStore.Audio.AudioColumns.ALBUM
    };
    final Uri BASE_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    final String BASE_SELECTION = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
    final String BASE_SORTORDER = MediaStore.Audio.Media.TITLE + " ASC";

    Bitmap originalWallpaper;

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.revertButton.setOnClickListener(this::revert);

        requestWriteExternalPermission();
        addAlbumCovers(); // TODO: move this!

        originalWallpaper = ((BitmapDrawable) getCurrentWallpaper()).getBitmap();

        String appId = "";
        if (BuildConfig.BUILD_TYPE.contentEquals("debug")) {
            appId = "ca-app-pub-3940256099942544/6300978111";
        } else appId = "ca-app-pub-5128547878021429~1004953500";

        MobileAds.initialize(this, initializationStatus -> { });
        AdRequest adRequest = new AdRequest.Builder().build();
        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        binding.addLayout.addView(adView);
        adView.loadAd(adRequest);
    }

    public void revert(View view) {

        if (((BitmapDrawable) getCurrentWallpaper()).getBitmap() != originalWallpaper) {

            Toast.makeText(this,"Changing",Toast.LENGTH_SHORT).show();
            new Thread(() -> changeWallpaper(originalWallpaper)).start();
        } else Toast.makeText(this,"Original is already applied",Toast.LENGTH_LONG).show();
    }

    private void addAlbumCovers() {

        Toast.makeText(this,"Scanning",Toast.LENGTH_SHORT).show();

        Cursor cursor = getContentResolver().query(BASE_URI, BASE_PROJECTION, BASE_SELECTION, null, BASE_SORTORDER);
        int count;

        if (cursor != null) {

            count = cursor.getCount();

            if (count > 0) while (cursor.moveToNext()) createImage(cursor);
            else Toast.makeText(this,"Found Nothing",Toast.LENGTH_SHORT).show();
        }
    }

    private void createImage(Cursor cursor) {

        final Long ALBUM_ID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        final String ALBUM_NAME = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));

        final Uri ALBUM_COVER = getAlbumCover(ALBUM_ID);
        final ImageView imageView = new ImageView(this);

        if (ALBUM_COVER != null) {
            // applying
            imageView.setImageURI(ALBUM_COVER);
            imageView.setOnClickListener(view -> {
                Toast.makeText(this, "Changing", Toast.LENGTH_SHORT).show();
                new Thread(() -> changeWallpaper(uriToBitmap(ALBUM_COVER))).start();
            });

            // saving
            imageView.setOnLongClickListener(view -> {
                Toast.makeText(MainActivity.this, "Saving", Toast.LENGTH_SHORT).show();
                new Thread(() -> saveCover(uriToBitmap(ALBUM_COVER), ALBUM_NAME)).start();
                return true;
            });
            binding.albumView.addView(imageView);
        }
    }

    public Uri getAlbumCover(long albumId){
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(artworkUri, albumId);
    }

    private Bitmap uriToBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver() , uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void changeWallpaper(Bitmap bitmap) {

        Looper.prepare();

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        try {
            wallpaperManager.setBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this,"Done",Toast.LENGTH_SHORT).show();

        Looper.loop();
    }

    private Drawable getCurrentWallpaper() {

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);

        return wallpaperManager.getDrawable();
    }

    private void saveCover(Bitmap bitmap, String imageName) {

        Looper.prepare();

        imageName = imageName.replace("/","\\");

        String root = Environment.getExternalStorageDirectory().toString();
        File directory = new File(root);
        directory.mkdirs();
        String fileName = "Image-" + imageName + ".jpg";
        File file = new File(directory, fileName);
        if (file.exists()) file.delete();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Toast.makeText(this,"Done",Toast.LENGTH_SHORT).show();

        Looper.loop();
    }

    public synchronized void requestWriteExternalPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // android 11 and up
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                    1);

            if (!Environment.isExternalStorageManager()) { // all files
                Toast.makeText(this, "Permission needed!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
    }
}
