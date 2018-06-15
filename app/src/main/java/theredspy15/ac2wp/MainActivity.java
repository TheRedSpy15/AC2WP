package theredspy15.ac2wp;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.sdsmdg.tastytoast.TastyToast;

import net.alhazmy13.catcho.library.Catcho;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    protected static final String[] BASE_PROJECTION = new String[]{
            MediaStore.Audio.AudioColumns.ALBUM_ID,
            MediaStore.Audio.AudioColumns.ALBUM
    };
    final Uri BASE_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    final String BASE_SELECTION = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
    final String BASE_SORTORDER = MediaStore.Audio.Media.TITLE + " ASC";

    LinearLayout albums;

    Bitmap originalWallpaper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Catcho.Builder(this)
                .recipients("hjadar15@protonmail.com")
                .build();

        albums = findViewById(R.id.albumView);
        originalWallpaper = ((BitmapDrawable) getCurrentWallpaper()).getBitmap();

        try {
            requestPermissions();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void revert(View view) {

        if (((BitmapDrawable) getCurrentWallpaper()).getBitmap() != originalWallpaper) {

            TastyToast.makeText(this,"Changing",TastyToast.LENGTH_SHORT,TastyToast.DEFAULT).show();
            new Thread(() -> changeWallpaper(originalWallpaper)).start();
        } else TastyToast.makeText(this,"Original is already applied",TastyToast.LENGTH_LONG,TastyToast.ERROR).show();
    }

    private void addAlbumCovers() throws FileNotFoundException {

        TastyToast.makeText(this,"Scanning",TastyToast.LENGTH_SHORT,TastyToast.DEFAULT).show();

        ContentResolver cr = this.getContentResolver();

        Cursor cursor = cr.query(BASE_URI, BASE_PROJECTION, BASE_SELECTION, null, BASE_SORTORDER);
        int count;

        if (cursor != null) {

            count = cursor.getCount();

            if (count > 0) while (cursor.moveToNext()) createImage(cursor);
            else TastyToast.makeText(this,"Found Nothing",TastyToast.LENGTH_SHORT,TastyToast.ERROR).show();
        }
    }

    private void createImage(Cursor cursor) throws FileNotFoundException {

        final Long ALBUM_ID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        final String ALBUM_NAME = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));

        final Bitmap ALBUM_COVER = getAlbumCover(ALBUM_ID);
        final ImageView imageView = new ImageView(this);

        // applying
        imageView.setImageBitmap(ALBUM_COVER);
        imageView.setOnClickListener(view -> {
            TastyToast.makeText(this,"Changing",TastyToast.LENGTH_SHORT,TastyToast.DEFAULT).show();
            new Thread(() -> changeWallpaper(ALBUM_COVER)).start();
        });

        // saving
        imageView.setOnLongClickListener(view -> {
            TastyToast.makeText(MainActivity.this,"Saving",TastyToast.LENGTH_SHORT,TastyToast.DEFAULT).show();
            new Thread(() -> saveCover(ALBUM_COVER,ALBUM_NAME)).start();
            return true;
        });

        albums.addView(imageView);
    }

    private Bitmap getAlbumCover(Long album_id) throws FileNotFoundException {

        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        ContentResolver res = MainActivity.this.getContentResolver();
        InputStream in = res.openInputStream(uri);

        return BitmapFactory.decodeStream(in);
    }

    private void changeWallpaper(Bitmap bitmap) {

        Looper.prepare();

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        try {
            wallpaperManager.setBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TastyToast.makeText(this,"Done",TastyToast.LENGTH_SHORT,TastyToast.DEFAULT).show();

        Looper.loop();
    }

    private Drawable getCurrentWallpaper() {

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);

        return wallpaperManager.getDrawable();
    }

    private void saveCover(Bitmap finalBitmap, String image_name) {

        Looper.prepare();

        image_name = image_name.replace("/","\\");

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fileName = "Image-" + image_name+ ".jpg";
        File file = new File(myDir, fileName);
        if (file.exists()) file.delete();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            TastyToast.makeText(this,e.getLocalizedMessage(),TastyToast.LENGTH_LONG,TastyToast.ERROR).show();
        }

        TastyToast.makeText(this,"Done",TastyToast.LENGTH_SHORT,TastyToast.DEFAULT).show();

        Looper.loop();
    }

    public void requestPermissions() throws FileNotFoundException {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_CODE);
        else addAlbumCovers(); // granted
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0 // Granted
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        addAlbumCovers();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else System.exit(0); // Permission denied
                break;
        }
    }
}
