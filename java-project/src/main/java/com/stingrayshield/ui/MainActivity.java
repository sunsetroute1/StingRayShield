package com.stingrayshield.ui; 
 
import android.app.Activity; 
import android.os.Bundle; 
import android.util.Log; 
 
/** 
 * Main activity for StingrayShield app - Android 15/16 compatible version 
 */ 
public class MainActivity extends Activity { 
    private static final String TAG = "StingrayShield"; 
 
    @Override 
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState); 
        Log.i(TAG, "Starting StingrayShield for Android 15/16"); 
        setContentView(R.layout.activity_main); 
    } 
} 
