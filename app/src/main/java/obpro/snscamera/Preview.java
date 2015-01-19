package obpro.snscamera;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by shinny on 2015/01/10.
 */
public class Preview extends SurfaceView implements SurfaceHolder.Callback {

    private boolean mProgressFlag = false;
    private Camera mCamera;

    public Preview(Context context, Camera c) {
        super(context);
        mCamera = c;

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera c) {
        mCamera = c;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}

    private final Camera.PreviewCallback editPreviewImage =
            new Camera.PreviewCallback() {

                public void onPreviewFrame(byte[] data, Camera camera) {
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    mCamera.startPreview();

                    mProgressFlag = false;
                }
            };
}
