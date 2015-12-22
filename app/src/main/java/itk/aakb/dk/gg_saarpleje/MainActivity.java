package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.google.android.glass.content.Intents;
import com.google.android.glass.view.WindowUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static String TAG = "saarpleje";
    private static final int TAKE_PICTURE_REQUEST = 101;
    private static final int RECORD_VIDEO_CAPTURE_REQUEST = 102;

    private int imageIndex = 0;
    private String[] imagePaths = new String[2];
    private List<String> videoPaths = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Requests a voice menu on this activity. As for any other
        // window feature, be sure to request this before
        // setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // Set the main activity view.
        setContentView(R.layout.activity_layout);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }

        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {
                case R.id.take_before_image_menu_item:
                    Log.i("saarpleje", "take before image");

                    imageIndex = 0;
                    takePicture();

                    break;
                case R.id.take_after_image_menu_item:
                    Log.i("saarpleje", "take after image");

                    imageIndex = 1;
                    takePicture();

                    break;
                case R.id.record_video_menu_item:
                    Log.i("saarpleje", "record video");

                    recordVideo();

                    break;
                case R.id.finish_menu_item:
                    Log.i("saarpleje", "finish report");

                    break;
                default:
                    return true;
            }
            return true;
        }

        // Pass through to super if not handled
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Launch a camera image capture intent.
     */
    private void takePicture() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, RECORD_VIDEO_CAPTURE_REQUEST);
//        Intent intent = new Intent(this, VideoActivity.class);
//        startActivityForResult(intent, RECORD_VIDEO_CAPTURE_REQUEST);
    }

    /**
     * When the camera activity returns, it is intercepted in this method.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received result");
            Log.i(TAG, data.getStringExtra("path"));

            processPictureWhenReady(data.getStringExtra("path"));
        }
        else if (requestCode == RECORD_VIDEO_CAPTURE_REQUEST && resultCode == RESULT_OK) {
            String videoPath = data.getStringExtra(Intents.EXTRA_VIDEO_FILE_PATH);

            processVideoWhenReady(videoPath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Update the UI when a step has been completed.
     *
     * @param step that step that has been completed.
     */
    private void setStepAccept(int step) {
        Log.i(TAG, "Step " + step +  " has been completed.");

        ImageView imageView = null;

        if (step == 0) {
            imageView = (ImageView) findViewById(R.id.image_view_1);
        }
        else if (step == 1) {
            imageView = (ImageView) findViewById(R.id.image_view_2);
        }
        else if (step == 2) {
            imageView = (ImageView) findViewById(R.id.image_view_3);
        }

        if (imageView != null) {
            imageView.setImageResource(R.drawable.ic_accept);
            imageView.invalidate();
        }
    }

    /**
     * Process the picture.
     *
     * @param picturePath path to the image.
     */
    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
            // The picture is ready. We are not gonna work with it, but now we know it has been
            // saved to disc.

            imagePaths[imageIndex] = picturePath;

            Log.i(TAG, "Picture " + imageIndex + " ready, with path: " + picturePath);

            if (imageIndex == 0) {
                setStepAccept(0);
            }
            else if (imageIndex == 1) {
                setStepAccept(2);
            }
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).
            // @TODO: Add progress bar. Return to main menu when image is ready.

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }


    /**
     * Process the video.
     *
     * @param videoPath path to the image.
     */
    private void processVideoWhenReady(final String videoPath) {
        final File videoFile = new File(videoPath);

        if (videoFile.exists()) {
            // The video is ready. We are not gonna work with it, but now we know it has been
            // saved to disc.
            videoPaths.add(videoPath);

            Log.i(TAG, "Video ready, with path: " + videoPath);

            setStepAccept(1);
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).
            // @TODO: Add progress bar. Return to main menu when video is ready.

            final File parentDirectory = videoFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(videoFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processVideoWhenReady(videoPath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }
}