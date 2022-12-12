package com.garen.gettamperswitch.ota;


import android.content.Context;
import android.os.RecoverySystem;
import android.util.Log;

import java.io.File;

public class OtaHr40 {
    private static final String TAG = "OtaHr40";
    private static Context mContext = null;
    private static String mOtaFileName = null;

    public OtaHr40(Context context){
        mContext = context;
    }


    public void otaUpdate(String file){

        File otafile = null;

        mOtaFileName = file;
        if(mOtaFileName != null){
            otafile = new File(mOtaFileName);
        }else{
            Log.d(TAG,"mOtaFileName is null");
            return;
        }

        if(!otafile.exists()){
            Log.d(TAG,"not exist");

        }else {
            Log.d(TAG,"exist");
            try {

                RecoverySystem.verifyPackage(otafile, null, null);
                Log.d(TAG, "Successfuly verified ota package.");
            }
            catch (Exception e) {
                Log.e(TAG, "Corrupted package: " + e);
                e.printStackTrace();
            }

            try {
                RecoverySystem.installPackage(mContext, otafile);
            }
            catch (Exception e) {
                Log.e(TAG, "Error while install OTA package: " + e);
                Log.e(TAG, "Will retry download");
                e.printStackTrace();
            }
        }
    }
}
