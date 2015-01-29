package obpro.snscamera;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.WeakHashMap;


public class CameraActivity extends ActionBarActivity {
    private Camera myCamera = null;
    private Preview preview = null;
    private ImageView imgView;
    private boolean standView;
    private boolean takeFlag;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int cameraId = 0;
        try {
            // use camera
            myCamera = Camera.open();
            setCameraDisplayOrientation(cameraId);
        } catch (Exception e) {
            this.finish();
        }

        // set view
        imgView = new ImageView(this.getApplicationContext());
        preview = new Preview(this, myCamera);
        setContentView(preview);

        // initialize flag
        takeFlag = false;
    }

    public void setCameraDisplayOrientation(int cameraId) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        // get display orientation
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            // stand view
            case Surface.ROTATION_0:
                degrees = 0;
                standView = true;
                break;
            // side view
            case Surface.ROTATION_90:
                degrees = 90;
                standView = false;
                break;
            // stand view
            case Surface.ROTATION_180:
                degrees = 180;
                standView = true;
                break;
            // side view
            case Surface.ROTATION_270:
                degrees = 270;
                standView = false;
                break;
        }
        // calculate preview degrees
        int result;
        if (cameraInfo.facing == cameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        }
        else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        // set display orientation
        myCamera.setDisplayOrientation(result);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (myCamera == null) {
            int cameraId = 0;
            try {
                // use camera
                myCamera = Camera.open();
                setCameraDisplayOrientation(cameraId);
            } catch (Exception e) {
                this.finish();
            }
        }

        // set view
        imgView = new ImageView(this.getApplicationContext());
        preview = new Preview(this, myCamera);
        setContentView(preview);

        // set listener for touch event (take picture on touch)
        preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (takeFlag) {
                    return true;
                } else {
                    takeFlag = true;
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        myCamera.autoFocus(mAutoFocusListener);
                    }
                    return true;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (myCamera != null) {
            preview.setCamera(null);
            myCamera.release();
            myCamera = null;
        }
    }

    private Camera.AutoFocusCallback mAutoFocusListener = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            myCamera.takePicture(null, null, mPicJpgListener);
            takeFlag = false;
        }
    };

    private Camera.PictureCallback mPicJpgListener = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data == null) {
                return;
            }

            editPost(data);

            preview.setVisibility(View.GONE);
            setContentView(imgView);
        }
    };

    public void editPost(final byte[] data) {
        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = layoutInflater.inflate(R.layout.dialog, (ViewGroup)findViewById(R.id.dialog_layout));
        final EditText editText = (EditText)layout.findViewById(R.id.editText);

        byte[] imgBytes = data;

        // adjust image depending on device orientation
        if (standView) {
            Bitmap tmp_bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            int width = tmp_bitmap.getWidth();
            int height = tmp_bitmap.getHeight();
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap bmp = Bitmap.createBitmap(tmp_bitmap, 0, 0, width, height, matrix, true);
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOS);
            imgBytes = byteArrayOS.toByteArray();
        }

        // save temporary image file
        String imgPath = "";
        try {
            File tmp = File.createTempFile("tmpImage", ".jpg", new File("/sdcard/tmp"));
            tmp.deleteOnExit();
            imgPath = tmp.getAbsolutePath();

            OutputStream outputStream = new FileOutputStream(tmp);
            outputStream.write(imgBytes);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String finalImgPath = imgPath;

        // create alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(layout);
        // set button "Send"
        alertDialogBuilder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(CameraActivity.this);
                Map<String,String> authInfo = new WeakHashMap<String, String>();
                authInfo.put("consumer_key", preferences.getString("consumer_key", "no_key"));
                authInfo.put("consumer_secret", preferences.getString("consumer_secret", "no_secret"));
                authInfo.put("access_token", preferences.getString("access_token", "no_token"));
                authInfo.put("access_token_secret", preferences.getString("access_token_secret", "no_secret"));

                String text = editText.getText().toString();

                PictureSender pictureSender = new PictureSender(CameraActivity.this, authInfo);
                pictureSender.execute(text, finalImgPath);

                dialog.dismiss();
                restartApp();
            }
        });
        // set button "Cancel"
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                restartApp();
            }
        });
        alertDialogBuilder.setCancelable(false);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    // restart the application
    public void restartApp() {
        recreate();
    }

        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.account_settings:
                Intent userProfileManager = new android.content.Intent(this, UserProfileManager.class);
                startActivity(userProfileManager);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }


        return super.onOptionsItemSelected(item);
    }
}
