package com.mymensorar;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.services.s3.AmazonS3Client;
import com.mymensorar.cognitoclient.AmazonSharedPreferencesWrapper;
import com.mymensorar.cognitoclient.CognitoSampleDeveloperAuthenticationService;

public class LoaderActivity extends Activity {
    private static final String TAG = "LoaderActvty";

    private String activityToBeCalled = null;
    private String mymensorAccount = null;
    private String mymensorUserGroup = null;
    private String origMymAcc;
    private String deviceId;
    private String serverConnection;

    private static boolean finishApp = false;

    private static boolean clockSetSuccess = false;
    private static long back_pressed;
    private int dciNumber = 1;

    private BackgroundLoader backgroundLoader;

    private static long sntpReference;
    private static long sntpTime;

    ImageView mymensorLogoTxt;
    ImageView mymensorLogoCircles;
    LinearLayout logoLinearLayout;

    Animation rotationCircles;

    SharedPreferences sharedPref;

    private String appStartState;

    private AmazonS3Client s3Client;

    SharedPreferences amazonSharedPref;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityToBeCalled = getIntent().getExtras().get("activitytobecalled").toString();

        appStartState = getIntent().getExtras().get("appstartstate").toString();
        Log.d(TAG, "OnCreate: appStartState: " + appStartState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        s3Client = CognitoSyncClientManager.getInstance();

        Log.d(TAG, "onCreate(): calling checkConnectionToServer().");

        setContentView(R.layout.activity_loader);
        logoLinearLayout = (LinearLayout) findViewById(R.id.MyMensorLogoLinearLayout1);
        logoLinearLayout.setVisibility(View.VISIBLE);

        //fab = (FloatingActionButton) findViewById(R.id.fab);

        mymensorLogoTxt = (ImageView) findViewById(R.id.mymensor_logo_txt);
        mymensorLogoTxt.setVisibility(View.VISIBLE);

        mymensorLogoCircles = (ImageView) findViewById(R.id.mymensor_rot_logo);
        mymensorLogoCircles.setVisibility(View.VISIBLE);

        rotationCircles = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        mymensorLogoCircles.startAnimation(rotationCircles);

        final View.OnClickListener undoOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityToBeCalled = "imagecapactivity";
                Snackbar.make(view, getText(R.string.loadingimgcapactvty), Snackbar.LENGTH_LONG).show();
                Log.d(TAG, "Reverting the call back to imagecapactivity");
            }
        };


        // Retrieving SeaMensor Account information,
        try {
            mymensorAccount = getIntent().getExtras().get("account").toString();
            deviceId = getIntent().getExtras().get("deviceid").toString();
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }

        Log.d(TAG, "onCreate: MyMensor Account from Mobile App: " + mymensorAccount);

        amazonSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Creating AsyncTask
        backgroundLoader = new BackgroundLoader(this);

        backgroundLoader.execute();


    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): CALLED");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume CALLED");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy(): CALLED");
        if (backgroundLoader != null) {
            backgroundLoader.setActivityBackgroundLoader(null);
            backgroundLoader.cancel(true);
            Log.d(TAG, "onDestroy(): cancelled backgroundLoader = " + backgroundLoader.getStatus());
        }
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Snackbar.make(logoLinearLayout, getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();
        back_pressed = System.currentTimeMillis();
    }


    private static class BackgroundLoader extends AsyncTask<Void, String, Void> {

        private LoaderActivity mActivity;
        private String finishAppReason = "";

        public BackgroundLoader(LoaderActivity activity) {
            mActivity = activity;
        }

        public void setActivityBackgroundLoader(LoaderActivity activity) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "backgroundLoader: onPreExecute()");
            clockSetSuccess = false;
        }


        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            if (mActivity != null) {
                TextView message = (TextView) mActivity.findViewById(R.id.bottom_message);
                message.setText(progress[0]);
            }

        }


        @Override
        protected Void doInBackground(Void... params) {
            long now = 0;
            Long loopStart = System.currentTimeMillis();
            Log.d(TAG, "backgroundLoader: Calling SNTP");
            SntpClient sntpClient = new SntpClient();
            do {
                if (isCancelled()) {
                    Log.i(TAG, "backgroundLoader: cancelled");
                    break;
                }
                if (sntpClient.requestTime("pool.ntp.org", 5000)) {
                    sntpTime = sntpClient.getNtpTime();
                    sntpReference = sntpClient.getNtpTimeReference();
                    now = sntpTime + SystemClock.elapsedRealtime() - sntpReference;
                    Log.i(TAG, "backgroundLoader: SNTP Present Time =" + now);
                    Log.i(TAG, "backgroundLoader: System Present Time =" + System.currentTimeMillis());
                    clockSetSuccess = true;
                }
                if (now != 0)
                    Log.d(TAG, "backgroundLoader: ntp:now=" + now);

            } while ((now == 0) && ((System.currentTimeMillis() - loopStart) < 5000));
            Log.d(TAG, "backgroundLoader: ending the loop querying pool.ntp.org: ms=" + (System.currentTimeMillis() - loopStart) + " millis:" + now);
            if (clockSetSuccess) {
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() before setTime=" + System.currentTimeMillis());
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() AFTER setTime=" + MymUtils.timeNow(clockSetSuccess, sntpTime, sntpReference));
            } else {
                sntpTime = 0;
                sntpReference = 0;
            }

            // Loading App Assets

            if (mActivity != null) {
                try {
                    Log.d(TAG, "backgroundLoader: ####### LOADING: LOCAL ASSETS");
                    MymUtils.extractAllAssets(mActivity.getBaseContext());
                } catch (Exception e) {
                    Log.e(TAG, "backgroundLoader:  extractAllAssets failed:" + e.toString());
                    publishProgress(mActivity.getString(R.string.checkcfgfiles));
                    finishApp = true;
                    finishAppReason = mActivity.getString(R.string.checkcfgfiles_online);
                    return null;
                }
            }

            Log.d(TAG, "backgroundLoader:  Before COG response: isApprovedByCognitoState=" + CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState);
            Log.d(TAG, "backgroundLoader:  Before COG response: qtyClientsExceededState=" + CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState);

            Long loopStart2 = System.currentTimeMillis();

            do {
                //nada!!!!
            }
            while (((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 0) || (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 0)) && ((System.currentTimeMillis() - loopStart2) < 5000));

            Log.d(TAG, "backgroundLoader:  After COG response: isApprovedByCognitoState=" + CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState + " ms=" + (System.currentTimeMillis() - loopStart2));
            Log.d(TAG, "backgroundLoader:  After COG response: qtyClientsExceededState=" + CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState + " ms=" + (System.currentTimeMillis() - loopStart2));

            if (mActivity != null) {
                mActivity.serverConnection = Constants.MYM_SERVERCONN_NORMAL;
            }


            if (mActivity != null) {
                if (((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 0) || (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 0))&& (mActivity.appStartState.equalsIgnoreCase(Constants.MYM_STSTATE_FIRSTEVER) )) {
                    Log.d(TAG, "startUpLoader: finishing");
                    finishApp = true;
                    finishAppReason = mActivity.getString(R.string.error_no_server_connection_cannotinstall);
                    return null;
                }

                if ((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 1) && (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 2)) {
                    Log.d(TAG, "backgroundLoader: finishing - MAX QTY CLIENTS EXCEEDED");
                    finishApp = true;
                    finishAppReason = mActivity.getString(R.string.error_mob_client_qty_exceeded);
                    return null;
                } else {
                    if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 2)) {
                        Log.d(TAG, "backgroundLoader: finishing - NO SERVER CONNECTION");
                        finishApp = true;
                        finishAppReason = mActivity.getString(R.string.error_no_server_connection);
                        return null;
                    }
                    if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 3) && (mActivity.appStartState.equalsIgnoreCase(Constants.MYM_STSTATE_FIRSTEVER))) {
                        Log.d(TAG, "backgroundLoader: TRIAL EXP CANNOT INSTALL without server connection");
                        finishApp = true;
                        finishAppReason = mActivity.getString(R.string.error_trial_expired_cannotinstall);
                        return null;
                    }
                    if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 4) && (mActivity.appStartState.equalsIgnoreCase(Constants.MYM_STSTATE_FIRSTEVER))) {
                        Log.d(TAG, "backgroundLoader: SUB EXP CANNOT INSTALL without server connection");
                        finishApp = true;
                        finishAppReason = mActivity.getString(R.string.error_sub_expired_cannotinstall);
                        return null;
                    }
                    if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 3)) {
                        Log.d(TAG, "backgroundLoader: TRIAL EXP continuing without server connection");
                        mActivity.serverConnection = Constants.MYM_SERVERCONN_TRIALEXPIRED;
                        finishApp = false;
                        finishAppReason = mActivity.getString(R.string.error_trial_exp_continuing_with_no_server_connection);
                    }
                    if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 4)) {
                        Log.d(TAG, "backgroundLoader: SUB EXP continuing without server connection");
                        mActivity.serverConnection = Constants.MYM_SERVERCONN_SUBEXPIRED;
                        finishApp = false;
                        finishAppReason = mActivity.getString(R.string.error_sub_exp_continuing_with_no_server_connection);
                    }
                }
            }

            if (mActivity != null) {
                Long loopStart3 = System.currentTimeMillis();
                do {
                    mActivity.mymensorUserGroup = AmazonSharedPreferencesWrapper.getGroupForUser(mActivity.amazonSharedPref);
                }
                while ((mActivity.mymensorUserGroup.equalsIgnoreCase("")) && ((System.currentTimeMillis() - loopStart3) < 5000));

                Log.d(TAG, "backgroundLoader: MYM_USR_GROUP: [" + mActivity.mymensorUserGroup + "] - ms=" + (System.currentTimeMillis() - loopStart3));
            }

            if (mActivity != null) {
                if (mActivity.mymensorUserGroup.equalsIgnoreCase("mymARmobileapp")) {
                    mActivity.origMymAcc = mActivity.mymensorAccount;
                    mActivity.mymensorAccount = mActivity.mymensorAccount.substring(7, mActivity.mymensorAccount.length());
                } else {
                    mActivity.origMymAcc = mActivity.mymensorAccount;
                }
            }

            Log.d(TAG, "backgroundLoader: appStartState =" + mActivity.appStartState);

            Log.d(TAG, "backgroundLoader: END");


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.d(TAG, "backgroundLoader: onPostExecute CALLED: finishApp=" + finishApp);
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finishApp) {
                            MymUtils.showToastMessage(mActivity.getBaseContext(), finishAppReason);
                            mActivity.finish();
                        } else {
                            if (!finishAppReason.equalsIgnoreCase("")) {
                                MymUtils.showToastMessage(mActivity.getBaseContext(), finishAppReason);
                            }
                            mActivity.callingActivities(Constants.maxQtyVps);
                        }
                    }
                });
            }
        }
    }

    public void callingActivities(int qtyVps) {

        Log.d(TAG, "callingActivities");
        Log.d(TAG, "callingActivities:####### LOADING: onPostExecute: callingARVewactivity: isTimeCertified=" + clockSetSuccess);
        Log.d(TAG, "callingActivities:####### LOADING: onPostExecute: callingARVewactivity: activityToBeCalled=" + activityToBeCalled);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.MYM_LAST_USER, origMymAcc);
        editor.commit();
        if (activityToBeCalled.equalsIgnoreCase("configactivity")) {
            try {
                Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
                intent.putExtra("mymensoraccount", mymensorAccount);
                intent.putExtra("origmymacc", origMymAcc);
                intent.putExtra("deviceid", deviceId);
                intent.putExtra("dcinumber", dciNumber);
                intent.putExtra("QtyVps", qtyVps);
                intent.putExtra("sntpTime", sntpTime);
                intent.putExtra("sntpReference", sntpReference);
                intent.putExtra("isTimeCertified", clockSetSuccess);
                intent.putExtra("lastVpSelectedByUser", 0);
                intent.putExtra("appStartState", appStartState);
                intent.putExtra("previousactivity", "loader");
                intent.putExtra("serverConnection", serverConnection);
                startActivity(intent);
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            } finally {
                finish();
            }
        }
        if (activityToBeCalled.equalsIgnoreCase("imagecapactivity")) {
            try {
                Intent intent = new Intent(getApplicationContext(), ImageCapActivity.class);
                intent.putExtra("mymensoraccount", mymensorAccount);
                intent.putExtra("origmymacc", origMymAcc);
                intent.putExtra("deviceid", deviceId);
                intent.putExtra("dcinumber", dciNumber);
                intent.putExtra("QtyVps", Constants.maxQtyVps);
                intent.putExtra("sntpTime", sntpTime);
                intent.putExtra("sntpReference", sntpReference);
                intent.putExtra("isTimeCertified", clockSetSuccess);
                intent.putExtra("appStartState", appStartState);
                intent.putExtra("previousactivity", "loader");
                intent.putExtra("serverConnection", serverConnection);
                startActivity(intent);
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            } finally {
                finish();
            }
        }

    }

}
