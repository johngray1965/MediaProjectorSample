package com.test.mediaprojectionsample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_SCREEN_SHARE = 1;

    private Intent mScreenshotPermission;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private Handler mHandler;
    private boolean mStartedRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setUpList();

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mStartedRecording) {
                    fab.setImageDrawable(getDrawable(R.drawable.ic_fiber_manual_record_red_500_48dp));
                    stopRecording();
                } else {
                    fab.setImageDrawable(getDrawable(R.drawable.ic_pause_black_48dp));
                    setupRecording();
                }
            }
        });

        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();


    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREEN_SHARE && resultCode == -1) {
            onPermissionSuccess(data);
        } else if (requestCode == REQUEST_SCREEN_SHARE && resultCode == 0) {
            stopRecording();
        }
    }

    void onPermissionSuccess(final Intent permissionIntent) {
        mScreenshotPermission = permissionIntent;
        setupRecording();
    }

    private void setUpList() {
        final List<String> data = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            data.add("Item number " + (i + 1));
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.list_view_item, parent, false)) {
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ((TextView) holder.itemView.findViewById(R.id.list_view_text)).setText(data.get(position));
            }

            @Override
            public int getItemCount() {
                return data.size();
            }
        });
    }

    void startRecording() {
        mStartedRecording = true;
        createVirtualDisplay();
    }

    private void createVirtualDisplay() {
        Point pt = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(pt);
        int width = pt.x;
        int height = pt.y;
        int divisor = 2;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Float density = metrics.density;

        Log.e("FOO", "In createVirtualDisplay XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        mImageReader = ImageReader.newInstance(width/ divisor, height/ divisor, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("test", width / divisor, height / divisor, density.intValue(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            mImageReader.getSurface(),
            null,
            mHandler);

        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
        mMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
    }

    void setupRecording() {
        try {
            if (mScreenshotPermission != null) {
                if (mMediaProjection != null) {
                    mMediaProjection.stop();
                    mMediaProjection = null;
                }

                if (mProjectionManager == null) {
                    mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                }

                mMediaProjection = mProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) mScreenshotPermission.clone());
                startRecording();
            } else {
                requestScreenshotPermission();
            }
        } catch (RuntimeException e) {
            requestScreenshotPermission();
        }
    }

    private void requestScreenshotPermission() {
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_SCREEN_SHARE);
    }

    void stopRecording() {
        mStartedRecording = false;

        if (mVirtualDisplay != null) mVirtualDisplay.release();
        if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
        mVirtualDisplay = null;
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {

        @Override
        public void onImageAvailable(final ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer byteBuffers = planes[0].getBuffer();
                    // You'd normally do something with the capture here
                }
            } catch (Exception e) {

            } finally {
                if (image != null) image.close();
            }
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mImageReader.setOnImageAvailableListener(null, null);
                    stopRecording();
                    if (mMediaProjection != null) mMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }
}
