// 
//  WaveClientSampleTest.java
//  WaveClientSample
//  
//  Created by Philip Kuryloski on 2011-04-02.
//  Copyright 2011 University of California, Berkeley. All rights reserved.
// 

package edu.berkeley.waveclientsample;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w -e class edu.berkeley.waveclientsample.WaveClientSampleTest edu.berkeley.waveclientsample.tests/android.test.InstrumentationTestRunner
 */
public class WaveClientSampleTest extends ActivityInstrumentationTestCase2<WaveClientSample> {
    
    boolean waveInstalled;

    public WaveClientSampleTest() {
        super("edu.berkeley.waveclientsample", WaveClientSample.class);
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        PackageManager pm = getInstrumentation().getContext().getPackageManager();        
        try {
            PackageInfo pi = pm.getPackageInfo("edu.berkeley.androidwave", 0);
            waveInstalled = (pi != null);
        } catch (PackageManager.NameNotFoundException e) {
            waveInstalled = false;
        }
    }

    /**
     * Verifies that activity under test can be launched.
     */
    public void testActivityTestCaseSetUpProperly() {
        Activity a = getActivity();
        assertNotNull("activity should be launched successfully", a);
    }
    
    /**
     * tests that we bind to the WaveService if it is installed.
     * 
     * Ideally we should control that condition and test both ways, but since
     * AndroidWave is another project, and this project depends on it, we do
     * it this way.
     */
    public void testPreconditions() {
        WaveClientSample a = getActivity();
        assertEquals("Should bind if wave is installed", waveInstalled, a.isBound());
    }
}
