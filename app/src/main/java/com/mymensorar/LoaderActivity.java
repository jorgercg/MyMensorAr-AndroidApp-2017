package com.mymensorar;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
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

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.mymensorar.cognitoclient.AmazonSharedPreferencesWrapper;
import com.mymensorar.cognitoclient.AwsUtil;
import com.mymensorar.cognitoclient.CognitoSampleDeveloperAuthenticationService;

import java.io.File;

public class LoaderActivity extends Activity {
    private static final String TAG = "LoaderActvty";

    private String activityToBeCalled = null;
    private String mymensorAccount = null;
    private String mymensorUserGroup = null;
    private String origMymAcc;
    private String deviceId;
    private String descvpRemotePath;
    private String vpsRemotePath;
    private String vpsCheckedRemotePath;
    private String markervpRemotePath;
    private String serverConnection;

    private boolean finishApp = false;

    private boolean clockSetSuccess = false;
    private static long back_pressed;
    private int dciNumber = 1;

    private BackgroundLoader backgroundLoader;

    private long sntpReference;
    private long sntpTime;

    ImageView mymensorLogoTxt;
    ImageView mymensorLogoCircles;
    LinearLayout logoLinearLayout;
    //FloatingActionButton fab;

    Animation rotationCircles;

    SharedPreferences sharedPref;

    private String appStartState;

    private AmazonS3 s3Amazon;
    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

    SharedPreferences amazonSharedPref;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityToBeCalled = getIntent().getExtras().get("activitytobecalled").toString();

        appStartState = getIntent().getExtras().get("appstartstate").toString();
        Log.d(TAG, "OnCreate: appStartState: " + appStartState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        s3Amazon = CognitoSyncClientManager.getInstance();

        // Creating AsyncTask
        backgroundLoader = new BackgroundLoader();

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

        /*

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getText(R.string.loadingcfgactvty), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.undo), undoOnClickListener).show();
                Log.d(TAG, "Changing the call to configactivity");
                activityToBeCalled = "configactivity";
            }
        });
        */


    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): CALLED");
        startUpLoader();

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
        backgroundLoader.cancel(true);
        Log.d(TAG, "onDestroy(): cancelled backgroundLoader = " + backgroundLoader.getStatus());
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Snackbar.make(logoLinearLayout, getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();
        back_pressed = System.currentTimeMillis();
    }


    private void startUpLoader() {
        // Retrieving SeaMensor Account information,
        mymensorAccount = getIntent().getExtras().get("account").toString();
        deviceId = getIntent().getExtras().get("deviceid").toString();

        Log.d(TAG, "startUpLoader: MyMensor Account from Mobile App: " + mymensorAccount);

        amazonSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Log.d(TAG, "startUpLoader: Before COG response: isApprovedByCognitoState=" + CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState);
        Log.d(TAG, "startUpLoader: Before COG response: qtyClientsExceededState=" + CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState);

        Long loopStart = System.currentTimeMillis();

        do {
            //nada!!!!
        }
        while (((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 0) || (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 0)) && ((System.currentTimeMillis() - loopStart) < 3000));

        Log.d(TAG, "startUpLoader: After COG response: isApprovedByCognitoState=" + CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState);
        Log.d(TAG, "startUpLoader: After COG response: qtyClientsExceededState=" + CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState);

        serverConnection = Constants.MYM_SERVERCONN_NORMAL;

        if ((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 1) && (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 2)) {
            Log.d(TAG, "startUpLoader: finishing");
            Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_mob_client_qty_exceeded), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
            toast.show();
            finish();
            return;
        } else {
            if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 2)) {
                Log.d(TAG, "startUpLoader: finishing");
                Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_no_server_connection), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
                finish();
                return;
            }
            if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 3)&&((appStartState.equalsIgnoreCase(Constants.MYM_STSTATE_FIRSTEVER))||(appStartState.equalsIgnoreCase(Constants.MYM_STSTATE_FIRSTTHISVERSION)))) {
                Log.d(TAG, "startUpLoader: TRIAL EXP continuing without server connection");
                Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_trial_expired_cannotinstall), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
                finish();
                return;
            }
            if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 4)&&((appStartState.equalsIgnoreCase(Constants.MYM_STSTATE_FIRSTEVER))||(appStartState.equalsIgnoreCase(Constants.MYM_STSTATE_FIRSTTHISVERSION)))) {
                Log.d(TAG, "startUpLoader: SUB EXP continuing without server connection");
                Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_sub_expired_cannotinstall), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
                finish();
                return;
            }
            if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 3)) {
                Log.d(TAG, "startUpLoader: TRIAL EXP continuing without server connection");
                Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_trial_exp_continuing_with_no_server_connection), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
                serverConnection = Constants.MYM_SERVERCONN_TRIALEXPIRED;
            }
            if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 4)) {
                Log.d(TAG, "startUpLoader: SUB EXP continuing without server connection");
                Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_sub_exp_continuing_with_no_server_connection), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
                serverConnection = Constants.MYM_SERVERCONN_SUBEXPIRED;
            }
        }

        // || ((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 0) && (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 0))

        do {
            mymensorUserGroup = AmazonSharedPreferencesWrapper.getGroupForUser(amazonSharedPref);
        } while (mymensorUserGroup.equalsIgnoreCase(""));

        Log.d(TAG, "startUpLoader: MYM_USR_GROUP: " + mymensorUserGroup);

        if (mymensorUserGroup.equalsIgnoreCase("mymARmobileapp")) {
            origMymAcc = mymensorAccount;
            mymensorAccount = mymensorAccount.substring(7, mymensorAccount.length());
        } else {
            origMymAcc = mymensorAccount;
        }

        descvpRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/" + "dsc" + "/";
        markervpRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/" + "mrk" + "/";
        vpsRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/";
        vpsCheckedRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "chk" + "/" + dciNumber + "/";

        Log.d(TAG, "startUpLoader: appStartState =" + appStartState);

        Log.d(TAG, "startUpLoader: END");

        backgroundLoader.execute();

    }


    private void firstTimeLoader() {
        try {
            File vpsFile = new File(getApplicationContext().getFilesDir(), Constants.vpsConfigFileName);
            if (!vpsFile.exists()) {
                ConfigFileCreator.createVpsfile(getApplicationContext(),
                        getApplicationContext().getFilesDir(),
                        Constants.vpsConfigFileName,
                        transferUtility,
                        vpsRemotePath,
                        mymensorAccount);
            }
            File vpsCheckedFile = new File(getApplicationContext().getFilesDir(), Constants.vpsCheckedConfigFileName);
            if (!vpsCheckedFile.exists()) {
                ConfigFileCreator.createVpsCheckedFile(getApplicationContext(),
                        getApplicationContext().getFilesDir(),
                        Constants.vpsCheckedConfigFileName,
                        transferUtility,
                        vpsCheckedRemotePath,
                        mymensorAccount);
            }
            for (int j = 0; j < (Constants.maxQtyVps); j++) {
                File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp" + (j) + ".png");
                if (!descvpFile.exists()) {
                    ConfigFileCreator.createDescvpFile(getApplicationContext(),
                            getApplicationContext().getFilesDir(),
                            "descvp" + (j) + ".png",
                            transferUtility,
                            descvpRemotePath,
                            0,
                            mymensorAccount);
                }
                File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (j) + ".png");
                if (!markervpFile.exists()) {
                    ConfigFileCreator.createMarkervpFile(getApplicationContext(),
                            getApplicationContext().getFilesDir(),
                            "markervp" + (j) + ".png",
                            transferUtility,
                            markervpRemotePath,
                            0,
                            mymensorAccount);
                }
            }
            Log.d(TAG, "firstTimeLoader: Waiting for initial config files and image to be created");

            Boolean configFilesOK = false;

            do {
                File vpsFileCHK = new File(getApplicationContext().getFilesDir(), Constants.vpsConfigFileName);
                File vpsCheckedFileCHK = new File(getApplicationContext().getFilesDir(), Constants.vpsCheckedConfigFileName);
                configFilesOK = ((vpsFileCHK.exists()) && (vpsCheckedFileCHK.exists()));
            } while (!configFilesOK);

            Log.d(TAG, "firstTimeLoader: initial config files CREATION DONE: configFilesOK=" + configFilesOK);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "firstTimeLoader(): failed to create all config files, see stack trace " + e.toString());
            finish();
        }
    }


    public class BackgroundLoader extends AsyncTask<Void, String, Void> {

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "backgroundLoader: onPreExecute()");
            clockSetSuccess = false;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            TextView message = (TextView) findViewById(R.id.bottom_message);
            message.setText(progress[0]);

        }

        @Override
        protected Void doInBackground(Void... params) {
            long now = 0;
            Long loopStart = System.currentTimeMillis();
            Log.d(TAG, "backgroundLoader: Calling SNTP");
            SntpClient sntpClient = new SntpClient();
            do {
                if (isCancelled()) {
                    Log.i("AsyncTask", "backgroundLoader: cancelled");
                    break;
                }
                if (sntpClient.requestTime("pool.ntp.org", 5000)) {
                    sntpTime = sntpClient.getNtpTime();
                    sntpReference = sntpClient.getNtpTimeReference();
                    now = sntpTime + SystemClock.elapsedRealtime() - sntpReference;
                    Log.i("SNTP", "SNTP Present Time =" + now);
                    Log.i("SNTP", "System Present Time =" + System.currentTimeMillis());
                    clockSetSuccess = true;
                }
                if (now != 0)
                    Log.d(TAG, "backgroundLoader: ntp:now=" + now);

            } while ((now == 0) && ((System.currentTimeMillis() - loopStart) < 5000));
            Log.d(TAG, "backgroundLoader: ending the loop querying pool.ntp.org for 10 seconds max:" + (System.currentTimeMillis() - loopStart) + " millis:" + now);
            if (clockSetSuccess) {
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() before setTime=" + System.currentTimeMillis());
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() AFTER setTime=" + MymUtils.timeNow(clockSetSuccess, sntpTime, sntpReference));
            } else {
                sntpTime = 0;
                sntpReference = 0;
            }


            // Loading App Assets
            try {
                Log.d(TAG, "loadFinalDefinitions: backgroundLoader:####### LOADING: LOCAL ASSETS");
                MymUtils.extractAllAssets(getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "loadFinalDefinitions: backgroundLoader extractAllAssets failed:" + e.toString());
                publishProgress(getString(R.string.checkcfgfiles));
                finishApp = true;
                finish();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "onPostExecute CALLED: finishApp=" + finishApp);
            super.onPostExecute(result);
            if (finishApp) {
                MymUtils.showToastMessage(getApplicationContext(), getString(R.string.checkcfgfiles_online));
                finish();
            } else {
                callingActivities(Constants.maxQtyVps);
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
