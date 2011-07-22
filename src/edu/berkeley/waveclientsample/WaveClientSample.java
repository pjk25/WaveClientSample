// 
//  WaveClientSample.java
//  WaveClientSample
//  
//  Created by Philip Kuryloski on 2011-03-22.
//  Copyright 2011 University of California, Berkeley. All rights reserved.
// 

package edu.berkeley.waveclientsample;

import edu.berkeley.androidwave.waveclient.IWaveServicePublic;
import edu.berkeley.androidwave.waveclient.IWaveRecipeOutputDataListener;
import edu.berkeley.androidwave.waveclient.ParcelableWaveRecipeOutputData;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class WaveClientSample extends Activity {
    
    private static final String TAG = WaveClientSample.class.getSimpleName();
    
    private static final String ACTION_WAVE_SERVICE = "edu.berkeley.androidwave.intent.action.WAVE_SERVICE";
    private static final String ACTION_DID_AUTHORIZE = "edu.berkeley.androidwave.intent.action.DID_AUTHORIZE";
    private static final String ACTION_DID_DENY = "edu.berkeley.androidwave.intent.action.DID_DENY";
    private static final int REQUEST_CODE_AUTH = 1;
    private final String API_KEY = "snathtosoeaseseoadrc,h.bmte";
    protected final int VISIBLE_LOG_LINES = 8;
    
    private IWaveServicePublic mWaveService;
    private boolean mBound;
    
    // NOTE: ideally, recipeIds would be stored in preferences, and editable by the user
    protected String[] recipeIds = {"edu.berkeley.waverecipe.AccelerometerMagnitude",
                                    "edu.berkeley.waverecipe.Kcal"};
    protected String chosenRecipeId;
    protected List<String> logLines;
    
    protected boolean logging;
    
    Button startButton;
    TextView messageTextView;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle(R.string.main_activity_name);
        setContentView(R.layout.main);
        
        startButton = (Button) findViewById(R.id.start_button);
        messageTextView = (TextView) findViewById(R.id.message_textview);
        
        logging = false;
        
        logLines = new ArrayList<String>(VISIBLE_LOG_LINES);
        
        // disable the button until we have connected to the service
        startButton.setEnabled(false);
        startButton.setOnClickListener(startButtonListener);

        // connect to the service
        Intent i = new Intent(ACTION_WAVE_SERVICE);
        try {
            if (bindService(i, mConnection, Context.BIND_AUTO_CREATE)) {
                mBound = true;
                Toast.makeText(WaveClientSample.this, "Connected to WaveService", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(getClass().getSimpleName(), "Could not bind with "+i);
                // TODO: replace this Toast with a dialog that allows quitting
                Toast.makeText(WaveClientSample.this, "Could not connect to the WaveService!", Toast.LENGTH_SHORT).show();
                messageTextView.setText("ERROR:\n\nFailed to bind to the WaveService.\n\nIs AndroidWave installed on this device?\n\nPlease address this issue and restart this Application.");
            }
        } catch (SecurityException se) {
            Log.d(TAG, "SecurityException on bind", se);
            AlertDialog.Builder builder = new AlertDialog.Builder(WaveClientSample.this);
            builder.setMessage("Security Exception: "+se)
                   .setCancelable(false)
                   .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           WaveClientSample.this.finish();
                       }
                   });
            AlertDialog alert = builder.show();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        
        if (logging) {
            try {
                mWaveService.unregisterRecipeOutputListener(API_KEY, chosenRecipeId);
            } catch (RemoteException e) {
                Log.d("WaveClientSample", "lost connection to the service");
            }
        }

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    
    protected void startLogging() {
        logging = true;
        
        messageTextView.setText("Logging started...");
        
        try {
            boolean didRegister = mWaveService.registerRecipeOutputListener(API_KEY, chosenRecipeId, outputListener);
            if (!didRegister) {
                Toast.makeText(WaveClientSample.this, "Error requesting recipe data stream.", Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            Log.d("WaveClientSample", "lost connection to the service");
        }
    }
    
    private void setButtonForWaveUi() {
        startButton.setText("Deauthorize in Wave UI");
        startButton.setOnClickListener(waveUiRequestListener);
        startButton.setEnabled(true);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_AUTH) {
            if (resultCode == RESULT_OK) {
                if (data.getAction().equals(ACTION_DID_AUTHORIZE)) {
                    Toast.makeText(WaveClientSample.this, "Authorization Successful!", Toast.LENGTH_SHORT).show();
                    
                    setButtonForWaveUi();
                    startLogging();
                } else {
                    Toast.makeText(WaveClientSample.this, "Authorization Denied!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(WaveClientSample.this, "Authorization process canceled.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private OnClickListener startButtonListener = new OnClickListener() {
        public void onClick(View v) {
            startButton.setEnabled(false);
            
            // let the user choose a recipe
            AlertDialog.Builder builder = new AlertDialog.Builder(WaveClientSample.this);
            builder.setTitle("Select Recipe");
            builder.setItems(recipeIds, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    chosenRecipeId = recipeIds[item];
                    
                    try {
                        if (mWaveService.isAuthorized(API_KEY, chosenRecipeId)) {
                            startLogging();
                            setButtonForWaveUi();
                        } else {
                            // get an auth intent from the service
                            Intent i = mWaveService.getAuthorizationIntent(chosenRecipeId, API_KEY);
            
                            // then run it looking for a result
                            try {
                                startActivityForResult(i, REQUEST_CODE_AUTH);
                            } catch (ActivityNotFoundException anfe) {
                                anfe.printStackTrace();
                                Toast.makeText(WaveClientSample.this, "Error launching authorization UI", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (RemoteException re) {
                        Log.d(TAG, "lost connection to the service", re);
                        Toast.makeText(WaveClientSample.this, "Lost connection to WaveService", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            AlertDialog alert = builder.show();
        }
    };
    
    private OnClickListener waveUiRequestListener = new OnClickListener() {
        public void onClick(View v) {
            // TODO: make this call up the specific authorization via ACTION_EDIT + appropriate extras
            // set up an intent to switch to the Wave UI
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setPackage("edu.berkeley.androidwave");
            try {
                startActivity(i);
            } catch (ActivityNotFoundException anfe) {
                anfe.printStackTrace();
                Toast.makeText(WaveClientSample.this, "Error launching Wave UI", Toast.LENGTH_SHORT).show();
            }
        }
    };
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mWaveService = IWaveServicePublic.Stub.asInterface(service);
            
            startButton.setEnabled(true);
        }
        
        public void onServiceDisconnected(ComponentName className) {
            mWaveService = null;
        }
    };
    
    private IWaveRecipeOutputDataListener outputListener = new IWaveRecipeOutputDataListener.Stub() {
        public void receiveWaveRecipeOutputData(ParcelableWaveRecipeOutputData wrOutput) {
            // Log.v(TAG, "IWaveRecipeOutputDataListener.Stub got "+wrOutput);
            // update the log text
            // final ParcelableWaveRecipeOutputData o = wrOutput;
            synchronized(this) {
                if (logLines.size() > VISIBLE_LOG_LINES - 1) {
                    logLines.remove(0);
                }
                logLines.add(String.format("time: %d, values: %s", wrOutput.getTime(), wrOutput.valuesAsMap()));
            }
            messageTextView.post(new Runnable() {
                public void run() {
                    messageTextView.setText(TextUtils.join("\n", logLines));
                }
            });
        }
    };
    
    public boolean isBound() {
        return (mBound && (mWaveService != null));
    }
}