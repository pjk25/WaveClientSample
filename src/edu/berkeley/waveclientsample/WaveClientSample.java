package edu.berkeley.waveclientsample;

import edu.berkeley.androidwave.waveclient.IWaveServicePublic;
import edu.berkeley.androidwave.waveclient.IWaveRecipeOutputDataListener;
import edu.berkeley.androidwave.waveclient.WaveRecipeOutputDataImpl;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class WaveClientSample extends Activity
{
    private static final int REQUEST_CODE_AUTH = 1;
    private final String RECIPE_ID = "edu.berkeley.waverecipe.AccelerometerMagnitude";
    
    private IWaveServicePublic mWaveService;
    private boolean mBound;
    
    Button authRequestButton;
    TextView messageTextView;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        authRequestButton = (Button) findViewById(R.id.auth_request_button);
        messageTextView = (TextView) findViewById(R.id.message_textview);
        
        authRequestButton.setEnabled(false);

        // connect to the service

        // check if we are authorized for the recipe and update the UI
        //  - if we are already authorized, let the user switch to the WaveUI
        //    to deauthorize
        //  - if we are not authorized, let the user request it

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName("edu.berkeley.androidwave", "edu.berkeley.androidwave.waveservice.WaveService");
        if (bindService(i, mConnection, Context.BIND_AUTO_CREATE)) {
            mBound = true;
        } else {
            Log.d(getClass().getSimpleName(), "Could not bind with "+i);
            // TODO: replace this Toast with a dialog that allows quitting
            Toast.makeText(WaveClientSample.this, "Could not connect to the WaveService!", Toast.LENGTH_SHORT).show();
            messageTextView.setText("ERROR:\n\nFailed to bind to the WaveService.\n\nIs AndroidWave installed on this device?\n\nPlease address this issue and restart this Application.");
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    
    private void afterBind() {
        Toast.makeText(WaveClientSample.this, "Connected to WaveService", Toast.LENGTH_SHORT).show();
        
        try {
            if (mWaveService.isAuthorized(RECIPE_ID)) {
                Toast.makeText(WaveClientSample.this, "Already authorized for Recipe "+RECIPE_ID, Toast.LENGTH_SHORT).show();
            
                // we should configure the button to take us to the Wave UI
                authRequestButton.setOnClickListener(waveUiRequestListener);
                authRequestButton.setEnabled(true);
            
                // we should request that data be streamed and start displaying it in the log
                beginStreamingRecipeData();
            } else {
                if (mWaveService.recipeExists(RECIPE_ID, false)) {
                    authRequestButton.setOnClickListener(authRequestListener);
                    authRequestButton.setEnabled(true);
                } else {
                    // TODO: replace this Toast with a dialog that allows quitting
                    Toast.makeText(WaveClientSample.this, "WaveService can't find Recipe\n"+RECIPE_ID, Toast.LENGTH_SHORT).show();
                    messageTextView.setText("ERROR:\n\nThe WaveService cannot locate Recipe "+RECIPE_ID+"\n\nIs that ID correct, and is the recipe server reachable?\n\nPlease address this issue and restart this Application.");
                }
            }
        } catch (RemoteException e) {
            Log.d("WaveClientSample", "lost connection to the service");
        }
    }
    
    private void beginStreamingRecipeData() {
        // should actually do that here
        Toast.makeText(WaveClientSample.this, "NOT IMPLEMENTED YET!", Toast.LENGTH_LONG).show();
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_AUTH) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(WaveClientSample.this, "Authorization Successful!", Toast.LENGTH_SHORT).show();
                
                // reassign the auth button
                authRequestButton.setOnClickListener(waveUiRequestListener);
                authRequestButton.setEnabled(true);
                
                beginStreamingRecipeData();
            } else {
                Toast.makeText(WaveClientSample.this, "Authorization Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private OnClickListener authRequestListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                // get an auth intent from the service
                Intent i = mWaveService.getAuthorizationIntent(RECIPE_ID);
            
                // then run it looking for a result
                startActivityForResult(i, REQUEST_CODE_AUTH);
            } catch (RemoteException e) {
                Log.d("WaveClientSample", "lost connection to the service");
            }
        }
    };
    
    private OnClickListener waveUiRequestListener = new OnClickListener() {
        public void onClick(View v) {
            // set up an intent to switch to the Wave UI
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setClassName("edu.berkeley.androidwave", "edu.berkeley.androidwave.waveui.AndroidWaveActivity");
            startActivity(i);
        }
    };
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mWaveService = IWaveServicePublic.Stub.asInterface(service);
            
            try {
                mWaveService.registerRecipeOutputListener(outputListener, true);
                
                afterBind();
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }
        
        public void onServiceDisconnected(ComponentName className) {
            mWaveService = null;
        }
    };
    
    private IWaveRecipeOutputDataListener outputListener = new IWaveRecipeOutputDataListener.Stub() {
        public void receiveWaveRecipeOutputData(WaveRecipeOutputDataImpl wrOutput) {
            // update the log text
        }
    };
    
    public boolean isBound() {
        return (mBound && (mWaveService != null));
    }
}