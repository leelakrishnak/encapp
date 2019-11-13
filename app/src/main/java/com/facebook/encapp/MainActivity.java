package com.facebook.encapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.TextView;

import com.facebook.encapp.utils.SizeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "encapp";
    private HashMap<String, String> mExtraDataHashMap;
    private int mDefaultWidth = 1920;
    private int mDefaultHeight = 1080;
    private Size mVideoSize = new Size(mDefaultWidth, mDefaultHeight);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = retrieveNotGrantedPermissions(this);

        if (permissions != null && permissions.length > 0) {
            int REQUEST_ALL_PERMISSIONS = 0x4562;
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSIONS);
        }


        if (getInstrumentedTest()) {
            TextView mTvTestRun = (TextView)findViewById(R.id.tv_testrun);
            mTvTestRun.setVisibility(View.VISIBLE);
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    performInstrumentedTest();
                }
            })).start();


        }

    }


    private static String[] retrieveNotGrantedPermissions(Context context) {
        ArrayList<String> nonGrantedPerms = new ArrayList<>();
        try {
            String[] manifestPerms = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
            if (manifestPerms == null || manifestPerms.length == 0) {
                return null;
            }

            for (String permName : manifestPerms) {
                int permission = ActivityCompat.checkSelfPermission(context, permName);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    nonGrantedPerms.add(permName);
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return nonGrantedPerms.toArray(new String[nonGrantedPerms.size()]);
    }
    /**
     * Check if a test has fired up this activity.
     *
     * @return
     */
    private boolean getInstrumentedTest() {
        Intent intent = getIntent();
        mExtraDataHashMap = (HashMap<String, String>) intent.getSerializableExtra("map");

        return mExtraDataHashMap != null;
    }

    /**
     * Start automated test run.
     *
     *
     */
    private void performInstrumentedTest() {
        Log.d(TAG, "Intrumentation test - let us start!");

        //Need to set source
        if(!mExtraDataHashMap.containsKey("file")) {
           Log.e(TAG, "Need filename!");
           return;
        }
        String filename = mExtraDataHashMap.get("file");

        //Get default resolution
        if(mExtraDataHashMap.containsKey("res")) {
            String resolution = mExtraDataHashMap.get("res");
            mVideoSize = SizeUtils.parseXString(resolution);
            mDefaultWidth = mVideoSize.getWidth();
            mDefaultHeight = mVideoSize.getHeight();
        }

        final VideoConstraints[] vcCombinations = gatherUserSelectionsOnAllResolutions();
        if (vcCombinations == null || vcCombinations.length <= 0) {
            Log.e(TAG, "Invalid Video parameters");
            return;
        }

        String dynamicData = null;
        if(mExtraDataHashMap.containsKey("dyn")) {
            dynamicData = mExtraDataHashMap.get("dyn");
        }

        int timeout = 20;
        if(mExtraDataHashMap.containsKey("video_timeout")) {
            timeout = Integer.parseInt(mExtraDataHashMap.get("video_timeout"));
        }

        final TextView logText = (TextView)findViewById(R.id.logText);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logText.append("\nNumber of encodings: "+vcCombinations.length);
            }
        });
        for(VideoConstraints vc: vcCombinations) {
            final String settings = vc.getSettings();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logText.append("\n\nStart encoding: "+settings);
                }
            });

            int framesToDecode = (int)(timeout * vc.getFPS());
            Log.d(TAG, "frames to transcode: "+framesToDecode);
            Transcoder transcoder = new Transcoder();
            transcoder.transcode(vc, filename, framesToDecode, dynamicData);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logText.append("\nDone encoding: "+settings);
                }
            });
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView mTvTestRun = (TextView)findViewById(R.id.tv_testrun);
                mTvTestRun.setVisibility(View.GONE);
            }
        });
    }
    private VideoConstraints[] gatherUserSelectionsOnAllResolutions() {
        String[] bitrates = getResources().getStringArray(R.array.bitrate_array);
        String[] resolutions = getResources().getStringArray(R.array.resolutions_array);
        String[] encoders = getResources().getStringArray(R.array.mime_array);
        String[] keys = {(mExtraDataHashMap.get("key") != null) ?mExtraDataHashMap.get("key") : "2"};

        if (getInstrumentedTest()) {
            //Check if there are settings
            if(mExtraDataHashMap.containsKey("resolutions")) {
                String data = mExtraDataHashMap.get("resolutions");
                resolutions = data.split(",");
            }
            if(mExtraDataHashMap.containsKey("encoders")) {
                String data = mExtraDataHashMap.get("encoders");
                encoders = data.split(",");
            }
            if(mExtraDataHashMap.containsKey("bitrates")) {
                String data = mExtraDataHashMap.get("bitrates");
                bitrates = data.split(",");
            }
            if(mExtraDataHashMap.containsKey("res")) {
                String data = mExtraDataHashMap.get("res");
                Size videoSize = SizeUtils.parseXString(data);
                mDefaultWidth = videoSize.getWidth();
                mDefaultHeight = videoSize.getHeight();
            }
            if(mExtraDataHashMap.containsKey("keys")) {
                String data = mExtraDataHashMap.get("keys");
                keys = data.split(",");
            }

        }

        VideoConstraints[] allVideoConstrs = new VideoConstraints[encoders.length * bitrates.length * resolutions.length * keys.length];
        int index = 0;
        for (int kC = 0; kC < keys.length; kC++) {
            for (int eC = 0; eC < encoders.length; eC++) {
                for (int bC = 0; bC < bitrates.length; bC++) {
                    for (int vC = 0; vC < resolutions.length; vC++) {
                        VideoConstraints contraints = new VideoConstraints();
                        Size videoSize = SizeUtils.parseXString(resolutions[vC]);

                        contraints.setVideoSize(videoSize);
                        contraints.setVideoScaleSize(mVideoSize);

                        float bitRate = Float.parseFloat(bitrates[bC]) * 1000;
                        contraints.setBitRate(Math.round(bitRate));

                        int keyFrameInterval = Integer.parseInt(keys[kC]);
                        Log.d(TAG, kC + " Set key frame interval to "+keyFrameInterval+" res: "+videoSize+", bitr: "+bitRate);
                        contraints.setKeyframeRate(keyFrameInterval);
                        int fps = (mExtraDataHashMap.get("fps") != null) ? Integer.parseInt(mExtraDataHashMap.get("fps")) : 30;
                        contraints.setFPS(fps);
                        int ref_fps = (mExtraDataHashMap.get("ref_fps") != null) ? Integer.parseInt(mExtraDataHashMap.get("ref_fps")) : 30;
                        contraints.setReferenceFPS(ref_fps);
                        int ltrCount = (mExtraDataHashMap.get("ltrc") != null) ? Integer.parseInt(mExtraDataHashMap.get("ltrc")) : 6;
                        contraints.setLTRCount(ltrCount);
                        int hierLayerCount = (mExtraDataHashMap.get("hierl") != null) ? Integer.parseInt(mExtraDataHashMap.get("hierl")) : 0;
                        contraints.setHierStructLayerCount(hierLayerCount);
                        //Mime is parsed from the position in the
                        String[] encs = getResources().getStringArray(R.array.codecs_array);
                        String[] matching_mimes = getResources().getStringArray(R.array.mime_array);

                        String codecMime = matching_mimes[0];//If all fail we will take the first one...
                        int c = 0;
                        for (; c < encs.length; c++) {
                            if (encs[c].equals(encoders[eC].toUpperCase())) {
                                codecMime = matching_mimes[c];
                                break;
                            }
                        }

                        contraints.setVideoEncoderMime(codecMime);

                        //Check for mode
                        contraints.setConstantBitrate(false);

                        if (mExtraDataHashMap.containsKey("mode")) {
                            String mode = mExtraDataHashMap.get("mode");
                            if (mode.equals("cbr")) {
                                Log.d(TAG, "Set cbr!");
                                contraints.setConstantBitrate(true);
                            }
                        }
                        boolean keySkipFrames = (mExtraDataHashMap.containsKey("skip_frames")) ? Boolean.parseBoolean( mExtraDataHashMap.get("skip_frames")) : false;
                        contraints.setSkipFrames(keySkipFrames);
                        allVideoConstrs[index++] = contraints;
                    }
                }
            }
        }

        return allVideoConstrs;
    }

}