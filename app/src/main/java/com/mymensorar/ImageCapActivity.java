package com.mymensorar;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mymensorar.cognitoclient.AmazonSharedPreferencesWrapper;
import com.mymensorar.cognitoclient.AwsUtil;
import com.mymensorar.filters.ARFilter;
import com.mymensorar.filters.Filter;
import com.mymensorar.filters.IdMarkerDetectionFilter;
import com.mymensorar.filters.ImageDetectionFilter;
import com.mymensorar.filters.NoneARFilter;

import org.apache.commons.io.FileUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

import static com.mymensorar.Constants.cameraWidthInPixels;
import static com.mymensorar.R.drawable.circular_button_gray;
import static com.mymensorar.R.drawable.circular_button_green;
import static com.mymensorar.R.drawable.circular_button_red;
import static java.nio.charset.StandardCharsets.UTF_8;

//import android.util.Log;


public class ImageCapActivity extends Activity implements
        CameraBridgeViewBase.CvCameraViewListener2,
        AdapterView.OnItemClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    static {
        System.loadLibrary("MyMensor");
    }

    private static final String TAG = "ImageCapActvty";

    private static long backPressed;

    private String mymensorAccount;
    private String origMymAcc;
    private String deviceId;
    private String previousActivity;
    private String appStartState;
    private String serverConnection;
    private int dciNumber;
    private short qtyVps = 0;

    private boolean localFilesExist = false;
    private boolean responseFromRemoteStorage = false;
    private Boolean descvpFileCHK[];
    private Boolean markervpFileCHK[];
    private Boolean reloadEnded;
    private Boolean loadingDescvpFile = false;
    private Boolean loadingMarkervpFile = false;
    private Boolean configFromRemoteStorageExistsAndAccessible = false;
    private Boolean comingFromConfigActivity = false;

    private String vpsCheckedRemotePath;

    private static Bitmap vpLocationDescImageFileContents;

    private short[] vpNumber;
    private boolean[] vpChecked;
    private boolean[] vpFlashTorchIsOn;
    private long[] photoTakenTimeMillis;
    private long[] vpNextCaptureMillis;
    private String[] vpLocationDesText;

    private boolean[] vpIsAmbiguous;
    private boolean[] vpIsSuperSingle;
    private int[] vpSuperMarkerId;

    private boolean inPosition = false;
    private boolean inRotation = false;
    private boolean isShowingVpPhoto = false;
    private boolean firstFrameAfterArSwitchOff = false;
    private int isHudOn = 1;
    private int inPosRotScore = 0;

    private boolean vpIsManuallySelected = false;

    private TrackingValues trackingValues;
    private int vpTrackedInPose;

    public boolean vpPhotoAccepted = false;
    public boolean vpPhotoRejected = false;
    public boolean vpPhotoTobeRemarked = false;
    public boolean vpVideoTobeReplayed = false;
    public boolean lastVpPhotoRejected = false;
    private String vpPhotoRemark = null;
    public int lastVpSelectedByUser;
    public int mediaSelected = 0;

    private short assetId;

    private boolean[] vpArIsConfigured;
    private boolean[] vpIsVideo;

    private int[] vpXCameraDistance;
    private int[] vpYCameraDistance;
    private int[] vpZCameraDistance;
    private int[] vpXCameraRotation;
    private int[] vpYCameraRotation;
    private int[] vpZCameraRotation;
    private String[] vpFrequencyUnit;
    private long[] vpFrequencyValue;

    private static float tolerancePosition;
    private static float toleranceRotation;

    private boolean waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
    private boolean vpIsDisambiguated = true;
    private boolean doubleCheckingProcedureFinalized = false;
    private boolean doubleCheckingProcedureStarted = false;
    private boolean resultSpecialTrk = false;
    private boolean singleImageTrackingIsSet = false;
    private boolean waitingUntilSingleImageTrackingIsSet = false;
    private boolean multipleImageTrackingIsSet = false;
    private boolean waitingUntilMultipleImageTrackingIsSet = false;
    private boolean idTrackingIsSet = false;
    private boolean validMarkerFound = false;

    private long millisWhenSingleImageTrackingWasSet = 0;

    private String frequencyUnit;
    private int frequencyValue;

    ListView vpsListView;
    ImageView radarScanImageView;
    ImageView mProgress;
    TouchImageView imageView;
    VideoView videoView;
    ImageView vpCheckedView;

    TextView vpAcquiredStatus;
    TextView vpAcquiredStatusHRZ;
    TextView idMarkerNumberTextView;
    TextView idMarkerNumberTextViewHRZ;

    TextView vpLocationDesTextView;
    TextView vpIdNumber;

    TextView recText;

    Animation rotationRadarScan;
    Animation rotationMProgress;
    Animation blinkingText;

    FloatingActionButton buttonCallConfig;
    FloatingActionButton buttonAlphaToggle;
    FloatingActionButton showVpCapturesButton;
    FloatingActionButton showVpCapturesMainScreenButton;
    FloatingActionButton buttonCallWebAppMainScreen;
    FloatingActionButton buttonShowHelpMainScreen;
    FloatingActionButton buttonShowHelpShowVPCapScreen;
    FloatingActionButton buttonShowHelpDescVPScreen;
    FloatingActionButton buttonDownloadPDFOnShowVpCaptures;

    FloatingActionButton deleteLocalMediaButton;
    FloatingActionButton shareMediaButton;
    FloatingActionButton shareMediaButton2;


    ImageButton showPreviousVpCaptureButton;
    ImageButton showNextVpCaptureButton;
    ImageButton acceptVpPhotoButton;
    ImageButton rejectVpPhotoButton;
    ImageButton buttonRemarkVpPhoto;
    ImageButton buttonReplayVpVideo;
    ImageButton buttonStartVideoInVpCaptures;

    LinearLayout arSwitchLinearLayout;
    LinearLayout uploadPendingLinearLayout;
    LinearLayout videoRecorderTimeLayout;
    LinearLayout linearLayoutButtonsOnShowVpCaptures;
    LinearLayout linearLayoutImageViewsOnShowVpCaptures;
    LinearLayout linearLayoutAmbiguousVp;
    LinearLayout linearLayoutSuperSingleVp;
    LinearLayout linearLayoutConfigCaptureVps;
    LinearLayout linearLayoutConfigCaptureVpsHRZ;
    LinearLayout linearLayoutVpArStatus;
    LinearLayout linearLayoutVpArStatusHRZ;
    LinearLayout linearLayoutMarkerId;
    LinearLayout linearLayoutMarkerIdHRZ;
    LinearLayout linearLayoutAcceptImgButtons;
    LinearLayout linearLayoutCallWebAppMainScreen;

    FloatingActionButton buttonAmbiguousVpToggle;
    FloatingActionButton buttonAmbiguousVpToggleHRZ;
    FloatingActionButton buttonSuperSingleVpToggle;
    FloatingActionButton buttonSuperSingleVpToggleHRZ;

    ImageView uploadPendingmageview;
    TextView uploadPendingText;

    ImageButton buttonPositionCertified;
    ImageButton buttonTimeCertified;

    Chronometer videoRecorderChronometer;

    Switch arSwitch;

    private boolean isArSwitchOn = false;
    private boolean isArConfigLoaded = false;
    private boolean isArConfigLoading = false;
    private boolean errorWhileArConfigLoading = false;
    private boolean isVpsCheckedInformationLost = false;
    private boolean isVpsConfigFileDownloaded = false;
    private boolean isVpsCheckedFileDownloaded = false;

    FloatingActionButton positionCertifiedButton;
    FloatingActionButton timeCertifiedButton;
    FloatingActionButton connectedToServerButton;
    FloatingActionButton cameraShutterButton;
    FloatingActionButton videoCameraShutterButton;
    FloatingActionButton videoCameraShutterStopButton;

    Drawable circularButtonGreen;
    Drawable circularButtonRed;
    Drawable circularButtonGray;


    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;
    private AmazonS3 s3Amazon;

    // A List of all transfers
    private List<TransferObserver> observers;

    private int pendingUploadTransfers = 0;

    SharedPreferences sharedPref;

    public long sntpTime;
    public long sntpTimeReference;
    public boolean isTimeCertified;
    public long videoCaptureStartmillis;

    private boolean askForManualPhoto = false;
    private boolean askForManualVideo = false;
    private boolean capturingManualVideo = false;
    private boolean videoRecorderPrepared = false;
    private boolean stopManualVideo = false;

    protected MediaRecorder mMediaRecorder;

    protected MediaController mMediaController;

    private String videoFileName;
    private String videoFileNameLong;
    private String videoThumbnailFileName;
    private String videoThumbnailFileNameLong;

    public boolean isPositionCertified = false;
    public boolean isConnectedToServer = false;

    // The camera view.
    private CameraBridgeViewBase mCameraView;

    // A matrix that is used when saving photos.
    private Mat mBgr;
    public List<Mat> markerBuffer;
    public List<Mat> markerBufferSingle;

    // Whether the next camera frame should be saved as a photo.
    private boolean vpPhotoRequestInProgress;

    // The filters.
    private ARFilter[] mImageDetectionFilters;

    // The indices of the active filters.
    private int mImageDetectionFilterIndex;

    // Keys for storing the indices of the active filters.
    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX = "imageDetectionFilterIndex";

    // Matrix to hold camera calibration
    // initially with absolute compute values
    private MatOfDouble mCameraMatrix;

    private SoundPool.Builder soundPoolBuilder;
    private SoundPool soundPool;
    private int camShutterSoundID;
    private int videoRecordStartedSoundID;
    private int videoRecordStopedSoundID;
    boolean camShutterSoundIDLoaded = false;
    boolean videoRecordStartedSoundIDLoaded = false;
    boolean videoRecordStopedSoundIDLoaded = false;

    Point pt1;
    Point pt2;
    Point pt3;
    Point pt4;
    Point pt5;
    Point pt6;
    Scalar color;

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;
    protected Boolean mLocationUpdated;

    /**
     * Time when the location was updated represented as a Long.
     */
    protected Long mLastUpdateTime;

    protected String[] locPhotoToExif;

    BroadcastReceiver receiver;

    protected String showingMediaFileName;
    protected String showingMediaType;

    protected Boolean mymIsRunningOnKitKat = false;
    protected Boolean mymIsRunningOnFlippedDisplay = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mymIsRunningOnKitKat = true;
        }

        //Log.d(TAG, "mymIsRunningOnKitKat = " + mymIsRunningOnKitKat);

        if (Build.MODEL.equals("Nexus 5X")) {
            mymIsRunningOnFlippedDisplay = true;
        }

        //Log.d(TAG, "mymIsRunningOnFlippedDisplay = " + mymIsRunningOnFlippedDisplay);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        //Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
        //Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
            //Log.d(TAG, "onCreate - Calling FULLSCREEN");
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }


        setContentView(R.layout.activity_imagecap);

        // Retrieve configuration info
        Bundle configBundle = getIntent().getExtras();

        if (configBundle.getString("mymensoraccount") != null)
            mymensorAccount = configBundle.getString("mymensoraccount");
        dciNumber = configBundle.getInt("dcinumber", 1);
        qtyVps = Constants.maxQtyVps;  //Short.parseShort(getIntent().getExtras().get("QtyVps").toString());
        sntpTime = configBundle.getLong("sntpTime");
        sntpTimeReference = configBundle.getLong("sntpReference");
        isTimeCertified = configBundle.getBoolean("isTimeCertified", false);
        if (configBundle.getString("origmymacc") != null)
            origMymAcc = configBundle.getString("origmymacc");
        if (configBundle.getString("deviceid") != null)
            deviceId = configBundle.getString("deviceid");
        if (configBundle.getString("previousactivity") != null)
            previousActivity = configBundle.getString("previousactivity");
        if (configBundle.getString("appStartState") != null)
            appStartState = configBundle.getString("appStartState");
        if (configBundle.getString("serverConnection") != null)
            serverConnection = configBundle.getString("serverConnection");

        //Log.d(TAG, "onCreate: Starting ImageCapActivity with qtyVps=" + qtyVps + " MyM Account=" + mymensorAccount + " Orig MyM Account=" + origMymAcc);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Create an instance of GoogleAPIClient and request Location Services API.
        buildGoogleApiClient();

        vpLocationDesText = new String[qtyVps];
        vpArIsConfigured = new boolean[qtyVps];
        vpIsVideo = new boolean[qtyVps];
        vpXCameraDistance = new int[qtyVps];
        vpYCameraDistance = new int[qtyVps];
        vpZCameraDistance = new int[qtyVps];
        vpXCameraRotation = new int[qtyVps];
        vpYCameraRotation = new int[qtyVps];
        vpZCameraRotation = new int[qtyVps];
        vpNumber = new short[qtyVps];
        vpFrequencyUnit = new String[qtyVps];
        vpFrequencyValue = new long[qtyVps];
        vpChecked = new boolean[qtyVps];
        vpIsAmbiguous = new boolean[qtyVps];
        vpFlashTorchIsOn = new boolean[qtyVps];
        vpIsSuperSingle = new boolean[qtyVps];
        vpSuperMarkerId = new int[qtyVps];
        photoTakenTimeMillis = new long[qtyVps];
        vpNextCaptureMillis = new long[qtyVps];

        mRequestingLocationUpdates = true;
        mLocationUpdated = false;

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        s3Amazon = CognitoSyncClientManager.getInstance();

        vpsCheckedRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "chk" + "/" + dciNumber + "/";

        if (savedInstanceState != null) {
            mImageDetectionFilterIndex = savedInstanceState.getInt(
                    STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
        } else {
            mImageDetectionFilterIndex = 0;
        }

        this.setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            AudioAttributes audioAttrib = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder().setAudioAttributes(audioAttrib).setMaxStreams(6).build();
        } else {

            soundPool = new SoundPool(6, AudioManager.STREAM_NOTIFICATION, 0);
        }

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                if (i == camShutterSoundID) camShutterSoundIDLoaded = true;
                if (i == videoRecordStartedSoundID) videoRecordStartedSoundIDLoaded = true;
                if (i == videoRecordStopedSoundID) videoRecordStopedSoundIDLoaded = true;
            }
        });

        camShutterSoundID = soundPool.load(this, R.raw.camerashutter, 1);
        videoRecordStartedSoundID = soundPool.load(this, R.raw.minidvcamerabeepchimeup, 1);
        videoRecordStopedSoundID = soundPool.load(this, R.raw.minidvcamerabeepchimedown, 1);

        pt1 = new Point((double) Constants.xAxisTrackingCorrection, (double) Constants.yAxisTrackingCorrection);
        pt2 = new Point((double) (Constants.xAxisTrackingCorrection + Constants.standardMarkerlessMarkerWidth), (double) (Constants.yAxisTrackingCorrection + Constants.standardMarkerlessMarkerHeigth));
        pt3 = new Point((double) (Constants.xAxisTrackingCorrection + (Constants.standardMarkerlessMarkerWidth / 2)), (double) Constants.yAxisTrackingCorrection);
        pt4 = new Point((double) (Constants.xAxisTrackingCorrection + (Constants.standardMarkerlessMarkerWidth / 2)), (double) (Constants.yAxisTrackingCorrection - 40));
        pt5 = new Point((double) (Constants.xAxisTrackingCorrection + (Constants.standardMarkerlessMarkerWidth / 2) - 20), (double) (Constants.yAxisTrackingCorrection) - 20);
        pt6 = new Point((double) (Constants.xAxisTrackingCorrection + (Constants.standardMarkerlessMarkerWidth / 2) + 20), (double) (Constants.yAxisTrackingCorrection) - 20);
        color = new Scalar((double) 168, (double) 207, (double) 69);


        final Camera camera;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(0, cameraInfo);
            camera = Camera.open(0);
        } else {
            Log.d(TAG, "OnCreate: No permission to use Camera, finishing the app");
            Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_nopermissiontousecamera), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
            toast.show();
            finish();
            return;
        }


        final Parameters parameters = camera.getParameters();
        camera.release();

        mCameraView = (CameraBridgeViewBase) findViewById(R.id.imagecap_javaCameraView);
        mCameraView.setCameraIndex(0);
        mCameraView.setMaxFrameSize(cameraWidthInPixels, Constants.cameraHeigthInPixels);
        mCameraView.setCvCameraViewListener(this);


        circularButtonGreen = ContextCompat.getDrawable(getApplicationContext(), circular_button_green);

        circularButtonRed = ContextCompat.getDrawable(getApplicationContext(), circular_button_red);

        circularButtonGray = ContextCompat.getDrawable(getApplicationContext(), circular_button_gray);

        trackingValues = new TrackingValues();

        if (isArSwitchOn) {
            mImageDetectionFilterIndex = 1;
        } else {
            mImageDetectionFilterIndex = 0;
        }

        String[] newVpsList = new String[qtyVps];
        for (int i = 0; i < (qtyVps); i++) {
            newVpsList[i] = getString(R.string.vp_name) + i;
        }
        vpsListView = (ListView) this.findViewById(R.id.vp_list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, newVpsList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);
                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.WHITE);
                //Drawable icon = getResources().getDrawable(R.drawable.ic_check_white_18dp);
                //tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder_white_18dp, 0, 0, 0);
                // Generate ListView Item using TextView
                return view;
            }
        };
        vpsListView.setAdapter(arrayAdapter);
        vpsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        //vpsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        vpsListView.setOnItemClickListener(this);
        vpsListView.setVisibility(View.VISIBLE);


        vpLocationDesTextView = (TextView) this.findViewById(R.id.textView1);
        vpIdNumber = (TextView) this.findViewById(R.id.textView2);

        recText = (TextView) this.findViewById(R.id.cronoText);

        linearLayoutAcceptImgButtons = (LinearLayout) this.findViewById(R.id.linearLayoutAcceptImgButtons);
        acceptVpPhotoButton = (ImageButton) this.findViewById(R.id.buttonAcceptVpPhoto);
        rejectVpPhotoButton = (ImageButton) this.findViewById(R.id.buttonRejectVpPhoto);
        buttonRemarkVpPhoto = (ImageButton) this.findViewById(R.id.buttonRemarkVpPhoto);
        buttonReplayVpVideo = (ImageButton) this.findViewById(R.id.buttonReplayVpVideo);

        radarScanImageView = (ImageView) this.findViewById(R.id.imageViewRadarScan);
        rotationRadarScan = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        radarScanImageView.setVisibility(View.VISIBLE);
        radarScanImageView.startAnimation(rotationRadarScan);

        mProgress = (ImageView) this.findViewById(R.id.waitingTrkLoading);
        rotationMProgress = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        mProgress.setVisibility(View.GONE);
        mProgress.startAnimation(rotationMProgress);

        blinkingText = AnimationUtils.loadAnimation(this, R.anim.textblink);

        imageView = (TouchImageView) this.findViewById(R.id.imageView1);

        videoView = (VideoView) this.findViewById(R.id.videoView1);

        vpCheckedView = (ImageView) this.findViewById(R.id.imageViewVpChecked);
        vpCheckedView.setVisibility(View.GONE);

        uploadPendingLinearLayout = (LinearLayout) this.findViewById(R.id.uploadPendingLinearLayout);

        arSwitchLinearLayout = (LinearLayout) this.findViewById(R.id.arSwitchLinearLayout);

        videoRecorderTimeLayout = (LinearLayout) this.findViewById(R.id.videoRecorderTimeLayout);

        linearLayoutButtonsOnShowVpCaptures = (LinearLayout) this.findViewById(R.id.linearLayoutButtonsOnShowVpCaptures);

        linearLayoutImageViewsOnShowVpCaptures = (LinearLayout) this.findViewById(R.id.linearLayoutImageViewsOnShowVpCaptures);

        linearLayoutConfigCaptureVps = (LinearLayout) this.findViewById(R.id.linearLayoutConfigCaptureVps);

        linearLayoutConfigCaptureVpsHRZ = (LinearLayout) this.findViewById(R.id.linearLayoutConfigCaptureVpsHRZ);

        linearLayoutAmbiguousVp = (LinearLayout) this.findViewById(R.id.linearLayoutAmbiguousVp);

        linearLayoutSuperSingleVp = (LinearLayout) this.findViewById(R.id.linearLayoutSuperSingleVp);

        linearLayoutVpArStatus = (LinearLayout) this.findViewById(R.id.linearLayoutVpArStatus);

        linearLayoutVpArStatusHRZ = (LinearLayout) this.findViewById(R.id.linearLayoutVpArStatusHRZ);

        linearLayoutMarkerId = (LinearLayout) this.findViewById(R.id.linearLayoutMarkerId);

        linearLayoutMarkerIdHRZ = (LinearLayout) this.findViewById(R.id.linearLayoutMarkerIdHRZ);

        linearLayoutCallWebAppMainScreen = (LinearLayout) this.findViewById(R.id.linearLayoutCallWebAppMainScreen);

        uploadPendingmageview = (ImageView) this.findViewById(R.id.uploadPendingmageview);

        uploadPendingText = (TextView) this.findViewById(R.id.uploadPendingText);

        buttonPositionCertified = (ImageButton) this.findViewById(R.id.buttonPositionCertified);

        buttonTimeCertified = (ImageButton) this.findViewById(R.id.buttonTimeCertified);

        showPreviousVpCaptureButton = (ImageButton) this.findViewById(R.id.buttonShowPreviousVpCapture);

        showNextVpCaptureButton = (ImageButton) this.findViewById(R.id.buttonShowNextVpCapture);

        buttonStartVideoInVpCaptures = (ImageButton) this.findViewById(R.id.buttonStartVideoInVpCaptures);

        videoRecorderChronometer = (Chronometer) this.findViewById(R.id.recordingChronometer);

        arSwitch = (Switch) findViewById(R.id.arSwitch);

        idMarkerNumberTextView = (TextView) findViewById(R.id.idMarkerNumberTextView);
        idMarkerNumberTextViewHRZ = (TextView) findViewById(R.id.idMarkerNumberTextViewHRZ);
        vpAcquiredStatus = (TextView) this.findViewById(R.id.vpAcquiredStatus);
        vpAcquiredStatusHRZ = (TextView) this.findViewById(R.id.vpAcquiredStatusHRZ);

        buttonAmbiguousVpToggle = (FloatingActionButton) findViewById(R.id.buttonAmbiguousVpToggle);
        buttonAmbiguousVpToggleHRZ = (FloatingActionButton) findViewById(R.id.buttonAmbiguousVpToggleHRZ);
        buttonSuperSingleVpToggle = (FloatingActionButton) findViewById(R.id.buttonSuperSingleVpToggle);
        buttonSuperSingleVpToggleHRZ = (FloatingActionButton) findViewById(R.id.buttonSuperSingleVpToggleHRZ);

        cameraShutterButton = (FloatingActionButton) findViewById(R.id.cameraShutterButton);
        videoCameraShutterButton = (FloatingActionButton) findViewById(R.id.videoCameraShutterButton);
        videoCameraShutterStopButton = (FloatingActionButton) findViewById(R.id.videoCameraShutterStopButton);

        positionCertifiedButton = (FloatingActionButton) findViewById(R.id.positionCertifiedButton);
        timeCertifiedButton = (FloatingActionButton) findViewById(R.id.timeCertifiedButton);
        connectedToServerButton = (FloatingActionButton) findViewById(R.id.connectedToServerButton);

        buttonCallConfig = (FloatingActionButton) findViewById(R.id.buttonCallConfig);
        buttonAlphaToggle = (FloatingActionButton) findViewById(R.id.buttonAlphaToggle);
        showVpCapturesButton = (FloatingActionButton) findViewById(R.id.buttonShowVpCaptures);
        showVpCapturesMainScreenButton = (FloatingActionButton) findViewById(R.id.buttonShowVpCapturesMainScreen);
        buttonShowHelpMainScreen = (FloatingActionButton) findViewById(R.id.buttonShowHelpMainScreen);
        buttonShowHelpShowVPCapScreen = (FloatingActionButton) findViewById(R.id.buttonShowHelpShowVPCapScreen);
        buttonShowHelpDescVPScreen = (FloatingActionButton) findViewById(R.id.buttonShowHelpDescVPScreen);
        buttonDownloadPDFOnShowVpCaptures = (FloatingActionButton) findViewById(R.id.buttonDownloadPDFOnShowVpCaptures);

        deleteLocalMediaButton = (FloatingActionButton) findViewById(R.id.deleteLocalMediaButton);
        shareMediaButton = (FloatingActionButton) findViewById(R.id.shareMediaButton);
        shareMediaButton2 = (FloatingActionButton) findViewById(R.id.shareMediaButton2);

        buttonCallWebAppMainScreen = (FloatingActionButton) findViewById(R.id.buttonCallWebAppMainScreen);

        arSwitch.setChecked(isArSwitchOn);

        arSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                if (isOn) {
                    if (isArConfigLoaded) {
                        isArSwitchOn = true;
                        ((TextView) findViewById(R.id.arSwitchText)).setTextColor(Color.parseColor("#00e5ff"));
                        cameraShutterButton.setVisibility(View.INVISIBLE);
                        if (!mymIsRunningOnKitKat) {
                            videoCameraShutterButton.setVisibility(View.INVISIBLE);
                        }
                        videoCameraShutterStopButton.setVisibility(View.GONE);
                        videoRecorderTimeLayout.setVisibility(View.GONE);
                        mImageDetectionFilterIndex = 1;
                        showVpCapturesMainScreenButton.setVisibility(View.GONE);
                        vpsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                        Snackbar mSnackBar = Snackbar.make(arSwitch.getRootView(), getText(R.string.arswitchison), Snackbar.LENGTH_LONG);
                        TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                        mainTextView.setTextColor(Color.WHITE);
                        mSnackBar.show();
                    } else {
                        arSwitch.setChecked(false);
                        if (isArConfigLoading) {
                            Snackbar mSnackBar = Snackbar.make(arSwitch.getRootView(), getText(R.string.arconfigloading), Snackbar.LENGTH_LONG);
                            TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                            mainTextView.setTextColor(Color.WHITE);
                            mSnackBar.show();
                        } else {
                            Snackbar mSnackBar = Snackbar.make(arSwitch.getRootView(), getText(R.string.arconfignotavailable), Snackbar.LENGTH_LONG);
                            TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                            mainTextView.setTextColor(Color.WHITE);
                            mSnackBar.show();
                        }
                    }

                } else {
                    isArSwitchOn = false;
                    ((TextView) findViewById(R.id.arSwitchText)).setTextColor(Color.parseColor("#616161"));
                    cameraShutterButton.setVisibility(View.VISIBLE);
                    if (!mymIsRunningOnKitKat) {
                        videoCameraShutterButton.setVisibility(View.VISIBLE);
                    }
                    mImageDetectionFilterIndex = 0;
                    askForManualPhoto = false;
                    vpIsManuallySelected = false;
                    firstFrameAfterArSwitchOff = true;
                    showVpCapturesMainScreenButton.setVisibility(View.VISIBLE);
                    clearVpsChecked();
                    if (mProgress.isShown()) {
                        mProgress.clearAnimation();
                        mProgress.setVisibility(View.GONE);
                    }
                    if (radarScanImageView.isShown()) {
                        radarScanImageView.clearAnimation();
                        radarScanImageView.setVisibility(View.GONE);
                    }
                    vpsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    Snackbar mSnackBar = Snackbar.make(arSwitch.getRootView(), getText(R.string.arswitchisoff), Snackbar.LENGTH_LONG);
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                }
                //Log.d(TAG, "isArSwitchOn=" + isArSwitchOn);
            }
        });

        // Camera Shutter Button

        cameraShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "Camera Button clicked!!!");
                //Log.d(TAG, "vpIsManuallySelected=" + vpIsManuallySelected);
                //Log.d(TAG, "vpTrackedInPose=" + vpTrackedInPose);
                //Log.d(TAG, "vpNumber[vpTrackedInPose]=" + vpNumber[vpTrackedInPose]);
                askForManualPhoto = true;
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                float volume = actualVolume / maxVolume;
                // Is the sound loaded already?
                if (camShutterSoundIDLoaded) {
                    soundPool.play(camShutterSoundID, volume, volume, 1, 0, 1f);
                    //Log.d(TAG, "cameraShutterButton.setOnClickListener: Played sound");
                }
            }
        });

        if (!mymIsRunningOnKitKat) {
            // videoCamera Shutter Button

            videoCameraShutterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Log.d(TAG, "Video Camera Start Button clicked!!!");

                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        askForManualVideo = true;
                        stopManualVideo = false;
                        videoCameraShutterButton.setVisibility(View.GONE);
                        videoCameraShutterStopButton.setVisibility(View.VISIBLE);
                        videoRecorderChronometer.setBase(SystemClock.elapsedRealtime());
                        videoRecorderChronometer.start();
                        videoRecorderTimeLayout.setVisibility(View.VISIBLE);
                        recText.startAnimation(blinkingText);
                        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                        float volume = actualVolume / maxVolume;
                        if (videoRecordStartedSoundIDLoaded) {
                            soundPool.play(videoRecordStartedSoundID, volume, volume, 1, 0, 1f);
                            //Log.d(TAG, "videoCameraShutterButton.setOnClickListener START: Played sound");
                        }
                    } else {
                        Log.d(TAG, "OnCreate: No permission to Record Audio, impossible to record video");
                        Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_nopermissiontorecordaudio), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                        toast.show();
                    }


                }
            });

            // videoCamera Shutter Stop Button

            videoCameraShutterStopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Log.d(TAG, "Video Camera Stop Button clicked!!!");
                    stopManualVideo = true;
                    videoCameraShutterButton.setVisibility(View.VISIBLE);
                    videoCameraShutterStopButton.setVisibility(View.GONE);
                    videoRecorderChronometer.stop();
                    recText.clearAnimation();
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                    float volume = actualVolume / maxVolume;
                    if (videoRecordStopedSoundIDLoaded) {
                        soundPool.play(videoRecordStopedSoundID, volume, volume, 1, 0, 1f);
                        //Log.d(TAG, "videoCameraShutterButton.setOnClickListener STOP: Played sound");
                    }
                    videoRecorderTimeLayout.setVisibility(View.GONE);

                }
            });

            buttonAmbiguousVpToggleHRZ.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar mSnackBar = Snackbar.make(v, getText(R.string.vpisambiguous), Snackbar.LENGTH_LONG);
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                }
            });

            buttonSuperSingleVpToggleHRZ.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar mSnackBar = Snackbar.make(v, getText(R.string.vpissupersingle), Snackbar.LENGTH_LONG);
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                }
            });

        }
        // Position Certified Button

        final View.OnClickListener turnOffClickListenerPositionButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationUpdates();
                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.position_not_certified), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
        };

        final View.OnClickListener turnOnClickListenerPositionButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationUpdates();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss zz");
                String lastUpdatedOn = sdf.format(mLastUpdateTime);
                lastUpdatedOn = " (" + lastUpdatedOn + ")";
                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.position_is_certified) + lastUpdatedOn, Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
        };

        positionCertifiedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLocationUpdated) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss zz");
                    String lastUpdatedOn = sdf.format(mLastUpdateTime);
                    lastUpdatedOn = " (" + lastUpdatedOn + ")";
                    Snackbar mSnackBar = Snackbar.make(view, getText(R.string.position_is_certified) + lastUpdatedOn, Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.turn_off_location_updates), turnOffClickListenerPositionButton);
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                } else {
                    Snackbar mSnackBar = Snackbar.make(view, getText(R.string.position_not_certified), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.turn_on_location_updates), turnOnClickListenerPositionButton);
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                }
            }
        });

        positionCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));

        // Time Certified Button
        if (isTimeCertified) {
            timeCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
        } else {
            timeCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
        }

        final View.OnClickListener actionOnClickListenerTimeButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callTimeServerInBackground();
                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.tryingtocertifytime), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
        };

        timeCertifiedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTimeCertified) {
                    Snackbar mSnackBar = Snackbar.make(view, getText(R.string.usingcerttimeistrue), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.ok), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            });
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                } else {
                    Snackbar mSnackBar = Snackbar.make(view, getText(R.string.usingcerttimeisfalse), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.certify), actionOnClickListenerTimeButton);
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                }
            }
        });

        // Connected to Server Button

        checkConnectionToServer();

        if (isConnectedToServer) {
            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
        } else {
            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
        }

        final View.OnClickListener undoOnClickListenerServerButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.tryingtoconnecttoserver), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
                checkConnectionToServer();
            }
        };

        connectedToServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnectedToServer) {
                    Snackbar mSnackBar = Snackbar.make(view, getText(R.string.connectedtoserver), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.ok), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            });
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                } else {
                    if (serverConnection.equals(Constants.MYM_SERVERCONN_TRIALEXPIRED)) {
                        Snackbar mSnackBar = Snackbar.make(view, getText(R.string.notconnectedtoservertrialexpired), Snackbar.LENGTH_LONG);
                        TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                        mainTextView.setTextColor(Color.WHITE);
                        mSnackBar.show();
                    }
                    if (serverConnection.equals(Constants.MYM_SERVERCONN_SUBEXPIRED)) {
                        Snackbar mSnackBar = Snackbar.make(view, getText(R.string.notconnectedtoserversubexpired), Snackbar.LENGTH_LONG);
                        TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                        mainTextView.setTextColor(Color.WHITE);
                        mSnackBar.show();
                    }
                    if (serverConnection.equals(Constants.MYM_SERVERCONN_NORMAL)) {
                        Snackbar mSnackBar = Snackbar.make(view, getText(R.string.notconnectedtoserver), Snackbar.LENGTH_LONG)
                                .setAction(getText(R.string.trytoconnect), undoOnClickListenerServerButton);
                        TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                        mainTextView.setTextColor(Color.WHITE);
                        mSnackBar.show();
                    }

                }
            }
        });

        // Call Config Button

        final View.OnClickListener confirmOnClickListenerCallConfigButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.callingconfigactivity), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();

                try {
                    Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
                    intent.putExtra("mymensoraccount", mymensorAccount);
                    intent.putExtra("dcinumber", dciNumber);
                    intent.putExtra("QtyVps", qtyVps);
                    intent.putExtra("sntpTime", sntpTime);
                    intent.putExtra("sntpReference", sntpTimeReference);
                    intent.putExtra("isTimeCertified", isTimeCertified);
                    intent.putExtra("lastVpSelectedByUser", lastVpSelectedByUser);
                    intent.putExtra("origmymacc", origMymAcc);
                    intent.putExtra("deviceid", deviceId);
                    intent.putExtra("previousactivity", "capture");
                    intent.putExtra("appStartState", appStartState);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                    toast.show();
                }
            }
        };

        buttonCallConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lastVpSelectedByUser == 0) {
                    String message = getString(R.string.vp_name) + lastVpSelectedByUser + " " + getString(R.string.vp_notconfigurable);
                    Snackbar mSnackBar = Snackbar.make(vpsListView.getRootView(), message, Snackbar.LENGTH_LONG);
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                    return;
                }
                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.confirmconfigloading), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.confirm), confirmOnClickListenerCallConfigButton);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
        });


        // Alpha Channel Toggle Button

        buttonAlphaToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "Toggling imageView Transparency");
                if (imageView.getImageAlpha() == 128) {
                    imageView.setImageAlpha(255);
                } else {
                    imageView.setImageAlpha(128);
                }
                if (imageView.getImageAlpha() == 128)
                    buttonAlphaToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                if (!(imageView.getImageAlpha() == 128))
                    buttonAlphaToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
            }
        });

        // Show VP captures gallery Button

        showVpCapturesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonAlphaToggle.setVisibility(View.GONE);
                showVpCapturesButton.setVisibility(View.GONE);
                buttonCallConfig.setVisibility(View.GONE);
                linearLayoutConfigCaptureVps.setVisibility(View.GONE);
                showPreviousVpCaptureButton.setVisibility(View.VISIBLE);
                showNextVpCaptureButton.setVisibility(View.VISIBLE);
                buttonShowHelpShowVPCapScreen.setVisibility(View.VISIBLE);
                imageView.resetZoom();
                if (imageView.getImageAlpha() == 128) {
                    imageView.setImageAlpha(255);
                }
                mediaSelected = -1;
                showVpCaptures(lastVpSelectedByUser);
            }
        });


        // Show VP captures gallery Button on Main Screen

        showVpCapturesMainScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TURNING OFF RADAR SCAN
                if (radarScanImageView.isShown()) {
                    radarScanImageView.clearAnimation();
                    radarScanImageView.setVisibility(View.GONE);
                }
                isShowingVpPhoto = true;
                //Log.d(TAG, "isShowingVpPhoto=" + isShowingVpPhoto);
                arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                arSwitch.setVisibility(View.INVISIBLE);
                uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                positionCertifiedButton.setVisibility(View.INVISIBLE);
                timeCertifiedButton.setVisibility(View.INVISIBLE);
                connectedToServerButton.setVisibility(View.INVISIBLE);
                cameraShutterButton.setVisibility(View.INVISIBLE);
                if (!mymIsRunningOnKitKat) {
                    videoCameraShutterButton.setVisibility(View.INVISIBLE);
                }
                // Setting the correct listview set position
                vpsListView.setVisibility(View.GONE);
                // Turning off tracking
                mImageDetectionFilterIndex = 0;
                //buttonAlphaToggle.setVisibility(View.GONE);
                showVpCapturesMainScreenButton.setVisibility(View.GONE);
                linearLayoutCallWebAppMainScreen.setVisibility(View.GONE);
                buttonShowHelpMainScreen.setVisibility(View.GONE);
                buttonShowHelpShowVPCapScreen.setVisibility(View.VISIBLE);
                //buttonCallConfig.setVisibility(View.GONE);
                //linearLayoutConfigCaptureVps.setVisibility(View.GONE);
                showPreviousVpCaptureButton.setVisibility(View.VISIBLE);
                showNextVpCaptureButton.setVisibility(View.VISIBLE);
                imageView.resetZoom();
                mediaSelected = -1;
                showVpCaptures(lastVpSelectedByUser);
            }
        });


        // Call WebApp Button

        final View.OnClickListener confirmOnClickListenerCallWebAppMainScreenButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.callingwebapp), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();

                try {
                    String key = AmazonSharedPreferencesWrapper.getKeyForUser(sharedPref);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://app.mymensor.com/mobiletowebapp/"));
                    Bundle bundle = new Bundle();
                    bundle.putString("Authorization", "Token " + key);
                    browserIntent.putExtra(Browser.EXTRA_HEADERS, bundle);
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                    toast.show();
                }
            }
        };

        buttonCallWebAppMainScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.confirmcallingwebapp), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.confirm), confirmOnClickListenerCallWebAppMainScreenButton);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
        });


        // DownloadPDFOnShowVpCaptures Button

        final View.OnClickListener confirmOnClickListenerDownloadPDFOnShowVpCapturesButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.downloadpdfcert), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
                String fileSha256Hash = "";
                try {
                    File inFile = new File(getApplicationContext().getFilesDir(), showingMediaFileName);
                    fileSha256Hash = MymUtils.getFileHash(inFile);
                } catch (IOException e) {
                    //Log.e(TAG, "shareMediaButton: Failed to hash Photo file to share");
                }
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://app.mymensor.com/mcpdf/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/" + fileSha256Hash));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                    toast.show();
                }


            }
        };

        buttonDownloadPDFOnShowVpCaptures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar mSnackBar = Snackbar.make(view, getText(R.string.confirmdownloadpdfcert), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.confirm), confirmOnClickListenerDownloadPDFOnShowVpCapturesButton);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
        });


        buttonShowHelpMainScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAppTour();
            }
        });

        buttonShowHelpShowVPCapScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startShowVpCapturesTour();
            }
        });

        buttonShowHelpDescVPScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startShowDescVPScreenTour();
            }
        });

        deleteLocalMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Snackbar snackbar = Snackbar.make(view, getText(R.string.deletinglocal), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.undo), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //Log.d(TAG, "UNDO deleteLocalMediaButton: File NOT DELETED");
                            }
                        }).addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int dismissType) {
                                super.onDismissed(snackbar, dismissType);
                                if (dismissType == DISMISS_EVENT_TIMEOUT ||
                                        dismissType == DISMISS_EVENT_SWIPE ||
                                        dismissType == DISMISS_EVENT_CONSECUTIVE ||
                                        dismissType == DISMISS_EVENT_MANUAL) {
                                    //Log.d(TAG, "deleteLocalMediaButton: File DELETED: dismissType=" + dismissType);
                                    deleteLocalShownCapture(lastVpSelectedByUser, view);
                                    showVpCaptures(lastVpSelectedByUser);
                                } else {
                                    //Log.d(TAG, "deleteLocalMediaButton: File NOT DELETED");
                                    Snackbar.make(view, getText(R.string.keepinglocal), Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });
                TextView mainTextView = (TextView) (snackbar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                snackbar.show();
            }
        });

        shareMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "shareMediaButton:");
                String fileSha256Hash = "";
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (showingMediaType.equalsIgnoreCase("p")) {
                    try {
                        File inFile = new File(getApplicationContext().getFilesDir(), showingMediaFileName);
                        fileSha256Hash = MymUtils.getFileHash(inFile);
                    } catch (IOException e) {
                        Log.e(TAG, "shareMediaButton: Failed to hash Photo file to share");
                    }
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "https://app.mymensor.com/mc/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/" + fileSha256Hash);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Photo shared by MyMensor Mobile App");
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingphotolinkusing)));
                }
                if (showingMediaType.equalsIgnoreCase("v")) {
                    try {
                        File inFile = new File(getApplicationContext().getFilesDir(), showingMediaFileName);
                        fileSha256Hash = MymUtils.getFileHash(inFile);
                    } catch (IOException e) {
                        Log.e(TAG, "shareMediaButton: Failed to hash Video file to share");
                    }
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "https://app.mymensor.com/mc/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/" + fileSha256Hash);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Video shared by MyMensor Mobile App");
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingvideolinkusing)));
                }
            }
        });


        shareMediaButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "shareMediaButton2:");
                String fileSha256Hash = "";
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (showingMediaType.equalsIgnoreCase("p")) {
                    shareIntent.setType("image/jpg");
                    try {
                        InputStream in = getApplicationContext().openFileInput(showingMediaFileName);
                        File outFile = new File(getApplicationContext().getFilesDir(), "MyMensorPhotoCaptureShare.jpg");
                        OutputStream out = new FileOutputStream(outFile);
                        MymUtils.copyFile(in, out);
                        fileSha256Hash = MymUtils.getFileHash(outFile);
                    } catch (IOException e) {
                        Log.e(TAG, "shareMediaButton2: Failed to copy Photo file to share");
                    }
                    File shareFile = new File(getApplicationContext().getFilesDir(), "MyMensorPhotoCaptureShare.jpg");
                    Uri shareFileUri = FileProvider.getUriForFile(getApplicationContext(), "com.mymensorar.fileprovider", shareFile);
                    List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, shareFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Media Shared by MyMensor Mobile App - https://app.mymensor.com/mc/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/" + fileSha256Hash);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingphotousing)));
                }
                if (showingMediaType.equalsIgnoreCase("v")) {
                    shareIntent.setType("video/*");
                    try {
                        InputStream in = getApplicationContext().openFileInput(showingMediaFileName);
                        File outFile = new File(getApplicationContext().getFilesDir(), "MyMensorVideoCaptureShare.mp4");
                        OutputStream out = new FileOutputStream(outFile);
                        MymUtils.copyFile(in, out);
                        fileSha256Hash = MymUtils.getFileHash(outFile);
                    } catch (IOException e) {
                        Log.e(TAG, "shareMediaButton2: Failed to copy Video file to share");
                    }
                    File shareFile = new File(getApplicationContext().getFilesDir(), "MyMensorVideoCaptureShare.mp4");
                    Uri shareFileUri = FileProvider.getUriForFile(getApplicationContext(), "com.mymensorar.fileprovider", shareFile);
                    List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, shareFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Media Shared by MyMensor Mobile App - https://app.mymensor.com/mc/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/ " + fileSha256Hash);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingvideousing)));
                }
            }
        });

        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "User has put device in airplane mode");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isConnectedToServer = false;
                        connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                    }
                });
            }
        };

        this.registerReceiver(receiver, intentFilter);

        mMediaController = new MediaController(this);

        loadingArConfig();

    }


    public void showCallMyMensorSite() {
        final android.support.v7.app.AlertDialog.Builder alert = new android.support.v7.app.AlertDialog.Builder(this);
        alert.setIcon(R.drawable.logo_mymensor);
        alert.setTitle(getText(R.string.welcometomymensor));
        alert.setMessage(R.string.helpmessage);

        alert.setPositiveButton(R.string.go, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                final Window window = getWindow();
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mymensor.com"));
                startActivity(browserIntent);
            }
        });

        alert.setNegativeButton(R.string.notnow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                final Window window = getWindow();
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                dialog.dismiss();
            }
        });

        alert.setNeutralButton(R.string.tips, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                final Window window = getWindow();
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                startAppTour();
            }
        });

        alert.show();
    }


    private void checkConnectionToServer() {

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                //Log.d(TAG, "checkConnectionToServer: onPreExecute");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = MymUtils.isS3Available(s3Amazon);
                return result;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                //Log.d(TAG, "checkConnectionToServer: onPostExecute: result=" + result);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result) {
                            isConnectedToServer = true;
                            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                        } else {
                            isConnectedToServer = false;
                            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                        }
                    }
                });
            }
        }.execute();
    }


    private void loadingArConfig() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                //Log.d(TAG, "loadingArConfig: onPreExecute");
                isArConfigLoading = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        (findViewById(R.id.arSwitchText)).setVisibility(View.GONE);
                        (findViewById(R.id.ar_load_progressbar)).setVisibility(View.VISIBLE);
                    }
                });
                //Log.d(TAG, "loadingArConfig: isArConfigLoading=" + isArConfigLoading);
                if (previousActivity.equalsIgnoreCase("config")) {
                    comingFromConfigActivity = true;
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MymUtils.showToastMessage(getApplicationContext(), getString(R.string.loadingarconfiginbackground));
                        }
                    });
                }
                if (!comingFromConfigActivity) {
                    File vpsFileCHK = new File(getApplicationContext().getFilesDir(), Constants.vpsConfigFileName);
                    if (vpsFileCHK.exists()) {
                        localFilesExist = true;
                    }
                } else {
                    localFilesExist = true;
                }

            }

            @Override
            protected Boolean doInBackground(Void... params) {

                String descvpRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/" + "dsc" + "/";
                String markervpRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/" + "mrk" + "/";
                String vpsRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/";

                if (!comingFromConfigActivity) {
                    //Log.d(TAG, "loadingArConfig: checking if files exist in Remote Storage");

                    int retries = 4;
                    try {
                        do {
                            responseFromRemoteStorage = s3Amazon.doesObjectExist(Constants.BUCKET_NAME, (vpsRemotePath + Constants.vpsConfigFileName));
                            if (responseFromRemoteStorage) {
                                configFromRemoteStorageExistsAndAccessible = true;
                            } else {
                                //Log.d(TAG, "loadingArConfig: ERROR configFromRemoteStorageExistsAndAccessible=" + configFromRemoteStorageExistsAndAccessible);
                            }
                        } while (retries-- > 0);
                    } catch (Exception es3) {
                        Log.e(TAG, "loadingArConfig: checking if files exist in Remote Storage error:" + es3.toString());
                    }

                    //Log.d(TAG, "loadingArConfig: configFromRemoteStorageExistsAndAccessible=" + configFromRemoteStorageExistsAndAccessible);
                }

            /*
            *********************************************************************************************************************
            Checking wheter there is a remote or at least local AR configuration, if none exists, AR cannot be used
            *********************************************************************************************************************
             */
                if (!comingFromConfigActivity) {
                    //Log.d(TAG, "loadingArConfig: Starting LOGIC to determine following steps");
                    //Log.d(TAG, "loadingArConfig: Approved by Cognito? : (1=true; 2=False)" + CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState);
                    //Log.d(TAG, "loadingArConfig: Local files exist? : " + localFilesExist);
                    //Log.d(TAG, "loadingArConfig: Remote files exist and are accessible? : " + configFromRemoteStorageExistsAndAccessible);

                    if ((!configFromRemoteStorageExistsAndAccessible) && (!localFilesExist)) {
                        configFromRemoteStorageExistsAndAccessible = false;
                        errorWhileArConfigLoading = true;
                        return false;
                    }
                } else {
                    //Log.d(TAG, "loadingArConfig: comingFromConfigActivity=" + comingFromConfigActivity);
                }

            /*
            *********************************************************************************************************************
            Loading and Checking VPS.XML and VPSCHECKED.XML
            *********************************************************************************************************************
             */
                if (!comingFromConfigActivity) {
                    //Log.d(TAG, "loadingArConfig: Loading Definitions from Remote Storage and writing to local storage");
                    try {
                        File vpsFile = new File(getApplicationContext().getFilesDir(), Constants.vpsConfigFileName);
                        if (MymUtils.isNewFileAvailable(s3Client,
                                Constants.vpsConfigFileName,
                                (vpsRemotePath + Constants.vpsConfigFileName),
                                Constants.BUCKET_NAME,
                                getApplicationContext())) {
                            //Log.d(TAG, "loadingArConfig: vpsFile isNewFileAvailable= TRUE");
                            TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (vpsRemotePath + Constants.vpsConfigFileName), Constants.BUCKET_NAME, vpsFile);
                            observer.setTransferListener(new TransferListener() {
                                @Override
                                public void onStateChanged(int id, TransferState state) {
                                    //Log.d(TAG, "loadingArConfig: Observer: vpsFile onStateChanged: " + id + ", " + state);
                                    if (state.equals(TransferState.COMPLETED)) {
                                        //Log.d(TAG, "loadingArConfig: Observer: vpsFile TransferListener=" + state.toString());
                                        isVpsConfigFileDownloaded = true;
                                    }
                                }

                                @Override
                                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                    //Log.d(TAG, String.format("loadingArConfig: Observer: vpsFile onProgressChanged: %d, total: %d, current: %d",id, bytesTotal, bytesCurrent));
                                }

                                @Override
                                public void onError(int id, Exception ex) {
                                    Log.e(TAG, "loadingArConfig: Observer: vpsFile onError: loading failed:" + id, ex);
                                    errorWhileArConfigLoading = true;
                                }
                            });
                        } else {
                            //Log.d(TAG, "loadingArConfig: vpsFile isNewFileAvailable= FALSE");
                            isVpsConfigFileDownloaded = true;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadingArConfig: vpsFile loading failed:" + e.toString());
                        return false;
                    }

                    try {
                        final File vpsCheckedFile = new File(getApplicationContext().getFilesDir(), Constants.vpsCheckedConfigFileName);
                        if (MymUtils.isNewFileAvailable(s3Client,
                                Constants.vpsCheckedConfigFileName,
                                (vpsCheckedRemotePath + Constants.vpsCheckedConfigFileName),
                                Constants.BUCKET_NAME,
                                getApplicationContext())) {
                            //Log.d(TAG, "loadingArConfig: vpsCheckedFile isNewFileAvailable= TRUE");
                            TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (vpsCheckedRemotePath + Constants.vpsCheckedConfigFileName), Constants.BUCKET_NAME, vpsCheckedFile);
                            observer.setTransferListener(new TransferListener() {
                                @Override
                                public void onStateChanged(int id, TransferState state) {
                                    //Log.d(TAG, "loadingArConfig: Observer: vpsCheckedFile onStateChanged: " + id + ", " + state);
                                    if (state.equals(TransferState.COMPLETED)) {
                                        //Log.d(TAG, "loadingArConfig: Observer: vpsCheckedFile TransferListener=" + state.toString());
                                        isVpsCheckedFileDownloaded = true;
                                    }
                                }

                                @Override
                                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                    //Log.d(TAG, String.format("loadingArConfig: Observer: vpsCheckedFile onProgressChanged: %d, total: %d, current: %d",id, bytesTotal, bytesCurrent));
                                }

                                @Override
                                public void onError(int id, Exception ex) {
                                    Log.e(TAG, "loadingArConfig: Observer: vpsCheckedFile onError: loading failed:" + id, ex);
                                    errorWhileArConfigLoading = true;
                                }
                            });
                        } else {
                            //Log.d(TAG, "loadingArConfig: vpsCheckedFile isNewFileAvailable= FALSE");
                            isVpsCheckedFileDownloaded = true;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadingArConfig: vpsCheckedFile loading failed:" + e.toString());
                        return false;
                    }


                    long startChk1 = System.currentTimeMillis();
                    do {
                        //Log.d(TAG,"Waiting vps and vpschecked files to load...");
                    }
                    while (((!isVpsConfigFileDownloaded) || (!isVpsCheckedFileDownloaded)) && ((System.currentTimeMillis() - startChk1) < 20000));


                    Boolean configFilesOK = false;

                    do {
                        File vpsFileCHK = new File(getApplicationContext().getFilesDir(), Constants.vpsConfigFileName);
                        File vpsCheckedFileCHK = new File(getApplicationContext().getFilesDir(), Constants.vpsCheckedConfigFileName);
                        configFilesOK = ((vpsFileCHK.exists()) && (vpsCheckedFileCHK.exists()));
                        if (errorWhileArConfigLoading) return false;
                    } while (!configFilesOK);

                    //Log.d(TAG, "loadingArConfig: vps and vpschecked: configFilesOK=" + configFilesOK);

            /*
            *********************************************************************************************************************
            Loading VpDescFileSize[] and VpMarkerFileSize[]
            *********************************************************************************************************************
            */

                    Long vpDescFileSize[] = new Long[Constants.maxQtyVps];
                    Long vpMarkerFileSize[] = new Long[Constants.maxQtyVps];
                    boolean vpArIsConfigured[] = new boolean[Constants.maxQtyVps];

                    short vpListOrder = -1;

                    try {
                        //Log.d(TAG, "loadingArConfig: Loading VpDescFileSize[] VpMarkerFileSize[] vpArIsConfigured[] FromVpsFile: File=" + Constants.vpsConfigFileName);
                        InputStream fis = MymUtils.getLocalFile(Constants.vpsConfigFileName, getApplicationContext());
                        XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                        XmlPullParser myparser = xmlFactoryObject.newPullParser();
                        myparser.setInput(fis, null);
                        int eventType = myparser.getEventType();
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_DOCUMENT) {
                            } else if (eventType == XmlPullParser.START_TAG) {
                                if (myparser.getName().equalsIgnoreCase("Vp")) {
                                    vpListOrder++;
                                } else if (myparser.getName().equalsIgnoreCase("VpDescFileSize")) {
                                    eventType = myparser.next();
                                    vpDescFileSize[vpListOrder] = Long.parseLong(myparser.getText());
                                    //Log.d(TAG, "loadingArConfig: vpDescFileSize[" + vpListOrder + "]=" + vpDescFileSize[vpListOrder]);
                                } else if (myparser.getName().equalsIgnoreCase("VpMarkerFileSize")) {
                                    eventType = myparser.next();
                                    vpMarkerFileSize[vpListOrder] = Long.parseLong(myparser.getText());
                                    //Log.d(TAG, "loadingArConfig: vpMarkerFileSize[" + vpListOrder + "]=" + vpMarkerFileSize[vpListOrder]);
                                } else if (myparser.getName().equalsIgnoreCase("VpArIsConfigured")) {
                                    eventType = myparser.next();
                                    vpArIsConfigured[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                                    //Log.d(TAG, "loadingArConfig: vpArIsConfigured[" + vpListOrder + "]=" + vpArIsConfigured[vpListOrder]);
                                }
                            } else if (eventType == XmlPullParser.END_TAG) {
                            } else if (eventType == XmlPullParser.TEXT) {
                            }
                            eventType = myparser.next();
                        }
                        //Log.d(TAG, "loadingArConfig: Loading VpDescFileSize[] VpMarkerFileSize[] vpArIsConfigured[] FromVpsFile: FINALIZED");
                        fis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "loadingArConfig: load vpArIsConfigured FromVpsFile loading failed. vpListOrder=" + vpListOrder);
                        return false;
                    }

            /*
            *********************************************************************************************************************
            Loading Vp Location Description Images from Remote Storage and writing to local storage.
            *********************************************************************************************************************
            */

                    descvpFileCHK = new Boolean[Constants.maxQtyVps];
                    try {
                        for (int j = 0; j < (Constants.maxQtyVps); j++) {
                            descvpFileCHK[j] = false;
                            final int j_inner = j;
                            if (vpArIsConfigured[j]) {
                                File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp" + (j) + ".png");
                                if (MymUtils.isNewFileAvailable(s3Client,
                                        ("descvp" + (j) + ".png"),
                                        (descvpRemotePath + "descvp" + (j) + ".png"),
                                        Constants.BUCKET_NAME,
                                        getApplicationContext())) {
                                    //Log.d(TAG, "loadingArConfig: descvp: vp[" + j + "]: isNewFileAvailable = TRUE");
                                    loadingDescvpFile = true;
                                    final TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (descvpRemotePath + "descvp" + j + ".png"), Constants.BUCKET_NAME, descvpFile);
                                    observer.setTransferListener(new TransferListener() {

                                        @Override
                                        public void onStateChanged(int id, TransferState state) {
                                            //Log.d(TAG, "loadingArConfig: Observer: descvp: vp[" + j_inner + "]: id=" + id + " State=" + state);
                                            if (state.equals(TransferState.COMPLETED)) {
                                                descvpFileCHK[j_inner] = true;
                                            }
                                        }

                                        @Override
                                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                            //Do nothing
                                        }

                                        @Override
                                        public void onError(int id, Exception ex) {
                                            Log.e(TAG, "loadingArConfig: Observer: descvp loading failed with Exception:" + ex);
                                            File faileddescvpFile = new File(getApplicationContext().getFilesDir(), observer.getAbsoluteFilePath());
                                            boolean faileddescvpFileIsDeleted = faileddescvpFile.delete();
                                            Log.e(TAG, "loadingArConfig: Observer: faileddescvpFileIsDeleted =" + faileddescvpFileIsDeleted);
                                            errorWhileArConfigLoading = true;
                                        }

                                    });
                                } else {
                                    descvpFileCHK[j] = true;
                                    //Log.d(TAG, "loadingArConfig: descvp: vp[" + j + "]: isNewFileAvailable = FALSE thus using local");
                                }
                            } else {
                                File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp" + j + ".png");
                                if (!descvpFile.exists()) {
                                    ConfigFileCreator.createLocalDescvpFile(getApplicationContext(),
                                            getApplicationContext().getFilesDir(),
                                            "descvp" + j + ".png");
                                }
                                descvpFileCHK[j] = true;
                                //Log.d(TAG, "loadingArConfig: descvp: vp[" + j + "]: vpArIsConfigured = FALSE thus creating local.");
                            }

                        }

                    } catch (Exception e) {
                        Log.e(TAG, "loadingArConfig: Error descvp loading failed with Exception:" + e);
                        return false;
                    }

                    if (errorWhileArConfigLoading) return false;

            /*
            *********************************************************************************************************************
            Loading Vp Marker Images from Remote Storage and writing to local storage.
            *********************************************************************************************************************
            */

                    markervpFileCHK = new Boolean[Constants.maxQtyVps];
                    try {
                        for (int j = 0; j < (Constants.maxQtyVps); j++) {
                            markervpFileCHK[j] = false;
                            final int j_inner = j;
                            if (vpArIsConfigured[j]) {
                                final File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (j) + ".png");
                                if (MymUtils.isNewFileAvailable(s3Client,
                                        ("markervp" + (j) + ".png"),
                                        (markervpRemotePath + "markervp" + (j) + ".png"),
                                        Constants.BUCKET_NAME,
                                        getApplicationContext())) {
                                    //Log.d(TAG, "loadingArConfig: markervp: vp[" + j + "]: isNewFileAvailable = TRUE");
                                    loadingMarkervpFile = true;
                                    final TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (markervpRemotePath + "markervp" + j + ".png"), Constants.BUCKET_NAME, markervpFile);
                                    observer.setTransferListener(new TransferListener() {

                                        @Override
                                        public void onStateChanged(int id, TransferState state) {
                                            //Log.d(TAG, "loadingArConfig: Observer: markervp: vp[" + j_inner + "]: id=" + id + " State=" + state);
                                            if (state.equals(TransferState.COMPLETED)) {
                                                markervpFileCHK[j_inner] = true;
                                            }
                                        }

                                        @Override
                                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                            //Do nothing
                                        }

                                        @Override
                                        public void onError(int id, Exception ex) {
                                            Log.e(TAG, "loadingArConfig: Observer: markervp loading failed with Exception:" + ex);
                                            File failedmarkervpFile = new File(getApplicationContext().getFilesDir(), observer.getAbsoluteFilePath());
                                            boolean failedmarkervpFileIsDeleted = failedmarkervpFile.delete();
                                            Log.e(TAG, "loadingArConfig: Observer: failedmarkervpFileIsDeleted =" + failedmarkervpFileIsDeleted);
                                            errorWhileArConfigLoading = true;
                                        }

                                    });
                                } else {
                                    markervpFileCHK[j] = true;
                                    //Log.d(TAG, "loadingArConfig: markervp: vp[" + j + "]: isNewFileAvailable = FALSE thus using local");
                                }
                            } else {
                                File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (j) + ".png");
                                if (!markervpFile.exists()) {
                                    ConfigFileCreator.createLocalMarkervpFile(getApplicationContext(),
                                            getApplicationContext().getFilesDir(),
                                            "markervp" + (j) + ".png");
                                }
                                markervpFileCHK[j] = true;
                                //Log.d(TAG, "loadingArConfig: markervp: vp[" + j + "]: vpArIsConfigured = FALSE thus creating local.");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadingArConfig: Error markervp loading failed with Exception:" + e);
                        return false;
                    }

                    if (errorWhileArConfigLoading) return false;

            /*
            *********************************************************************************************************************
            Checking if all images are already in the local storage, as network operations take place in background.
            *********************************************************************************************************************
            */

                    //Log.d(TAG, "loadingArConfig: Checking if all images are already in the local storage, as network operations take place in background.");

                    int prod = 0;

                    if (configFromRemoteStorageExistsAndAccessible && ((loadingDescvpFile) || (loadingMarkervpFile))) {
                        //Log.d(TAG, "loadingArConfig: configFromRemoteStorageExistsAndAccessible: Starting the wait....");
                        long startChk2 = System.currentTimeMillis();
                        if ((loadingDescvpFile) || (loadingMarkervpFile)) {
                            do {
                                for (int k = 0; k < (qtyVps); k++) {
                                    ////Log.d(TAG,"descvpFileCHK["+k+"]="+descvpFileCHK[k]+"- markervpFileCHK["+k+"]="+markervpFileCHK[k]);
                                    if (descvpFileCHK[k] && markervpFileCHK[k]) {
                                        if (k == 0) {
                                            prod = Math.abs(1);
                                        } else {
                                            prod *= Math.abs(1);
                                        }
                                    } else {
                                        if (k == 0) {
                                            prod = Math.abs(0);
                                        } else {
                                            prod *= Math.abs(0);
                                        }
                                    }
                                }
                                if (errorWhileArConfigLoading) return false;
                            }
                            while ((prod == 0) && ((System.currentTimeMillis() - startChk2) < 120000));
                        }
                        //Log.d(TAG, "loadingArConfig: configFromRemoteStorageExistsAndAccessible: All files in local storage, now checking if they are OK.");
                    }

                    for (int k = 0; k < (qtyVps); k++) {
                        if (vpDescFileSize[k] == null) return false;
                        if (vpMarkerFileSize[k] == null) return false;
                        try {
                            reloadEnded = true;
                            File descvpFileCHK = new File(getApplicationContext().getFilesDir(), "descvp" + (k) + ".png");
                            File markervpFileCHK = new File(getApplicationContext().getFilesDir(), "markervp" + (k) + ".png");
                            if (!descvpFileCHK.exists() || !(descvpFileCHK.length() == vpDescFileSize[k])) {
                                //Log.d(TAG, "Reloading descvp" + (k) + ".png");
                                File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp" + (k) + ".png");
                                reloadEnded = false;
                                final int k_inner = k;
                                final TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (descvpRemotePath + "descvp" + k + ".png"), Constants.BUCKET_NAME, descvpFile);
                                observer.setTransferListener(new TransferListener() {

                                    @Override
                                    public void onStateChanged(int id, TransferState state) {
                                        //Log.d(TAG, "loadingArConfig: Observer: RELOADING descvp: vp[" + k_inner + "]: id=" + id + " State=" + state);
                                        if (state.equals(TransferState.COMPLETED)) {
                                            reloadEnded = true;
                                        }
                                    }

                                    @Override
                                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                        //Do nothing
                                    }

                                    @Override
                                    public void onError(int id, Exception ex) {
                                        Log.e(TAG, "loadingArConfig: Observer: RELOADING descvp loading failed with Exception:" + ex);
                                        File faileddescvpFile = new File(getApplicationContext().getFilesDir(), observer.getAbsoluteFilePath());
                                        boolean faileddescvpFileIsDeleted = faileddescvpFile.delete();
                                        Log.e(TAG, "loadingArConfig: Observer: RELOADING faileddescvpFileIsDeleted =" + faileddescvpFileIsDeleted);
                                        errorWhileArConfigLoading = true;
                                    }

                                });
                            }
                            long startChk4 = System.currentTimeMillis();
                            do {
                                //waiting
                            }
                            while (!reloadEnded && ((System.currentTimeMillis() - startChk4) < 60000));
                            if (!markervpFileCHK.exists() || !(markervpFileCHK.length() == vpMarkerFileSize[k])) {
                                //Log.d(TAG, "Reloading descvp" + (k) + ".png");
                                File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (k) + ".png");
                                reloadEnded = false;
                                final int k_inner = k;
                                final TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (markervpRemotePath + "markervp" + k + ".png"), Constants.BUCKET_NAME, markervpFile);
                                observer.setTransferListener(new TransferListener() {

                                    @Override
                                    public void onStateChanged(int id, TransferState state) {
                                        //Log.d(TAG, "loadingArConfig: Observer: markervp: vp[" + k_inner + "]: id=" + id + " State=" + state);
                                        if (state.equals(TransferState.COMPLETED)) {
                                            reloadEnded = true;
                                        }
                                    }

                                    @Override
                                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                        //Do nothing
                                    }

                                    @Override
                                    public void onError(int id, Exception ex) {
                                        Log.e(TAG, "loadingArConfig: Observer: markervp loading failed with Exception:" + ex);
                                        File failedmarkervpFile = new File(getApplicationContext().getFilesDir(), observer.getAbsoluteFilePath());
                                        boolean failedmarkervpFileIsDeleted = failedmarkervpFile.delete();
                                        Log.e(TAG, "loadingArConfig: Observer: failedmarkervpFileIsDeleted =" + failedmarkervpFileIsDeleted);
                                        errorWhileArConfigLoading = true;
                                    }

                                });
                                long startChk3 = System.currentTimeMillis();
                                do {
                                    //waiting
                                }
                                while (!reloadEnded && ((System.currentTimeMillis() - startChk3) < 60000));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "loadingArConfig: Image Files Checking Failed:" + e.toString());
                        }
                        if (errorWhileArConfigLoading) return false;
                    }

                    if (errorWhileArConfigLoading) {
                        return false;
                    } else {
                        return true;
                    }
                }
                return true;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                //Log.d(TAG, "loadingArConfig: onPostExecute: result=" + result);
                if (!comingFromConfigActivity) {
                    boolean loadConfigurationFileIsSuccessful = loadConfigurationFile();
                    boolean loadVpsCheckedIsSuccessful = loadVpsChecked();
                    if (loadConfigurationFileIsSuccessful && loadVpsCheckedIsSuccessful) {
                        verifyVpsChecked();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (result) {
                                    isArConfigLoaded = true;
                                    if (configFromRemoteStorageExistsAndAccessible) {
                                        MymUtils.showToastMessage(getApplicationContext(), getString(R.string.start_with_server_connection));
                                    } else {
                                        MymUtils.showToastMessage(getApplicationContext(), getString(R.string.start_with_no_server_connection));
                                    }
                                } else {
                                    isArConfigLoaded = false;
                                    MymUtils.showToastMessage(getApplicationContext(), getString(R.string.loadingarconfigfailed));
                                }
                            }
                        });
                    } else {
                        isArConfigLoaded = false;
                        MymUtils.showToastMessage(getApplicationContext(), getString(R.string.loadingarconfigfailed));
                    }
                    isArConfigLoading = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            (findViewById(R.id.arSwitchText)).setVisibility(View.VISIBLE);
                            (findViewById(R.id.ar_load_progressbar)).setVisibility(View.GONE);
                        }
                    });
                    //Log.d(TAG, "loadingArConfig: loading finalized with success: isArConfigLoading=" + isArConfigLoading);
                } else {
                    boolean loadConfigurationFileIsSuccessful = loadConfigurationFile();
                    boolean loadVpsCheckedIsSuccessful = loadVpsChecked();
                    if (loadConfigurationFileIsSuccessful && loadVpsCheckedIsSuccessful) {
                        verifyVpsChecked();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (result) {
                                    isArConfigLoaded = true;
                                    if (isVpsCheckedInformationLost) {
                                        MymUtils.showToastMessage(getApplicationContext(), getString(R.string.new_config_loaded_vps_chk_lost));
                                    } else {
                                        MymUtils.showToastMessage(getApplicationContext(), getString(R.string.new_config_loaded));
                                    }
                                } else {
                                    isArConfigLoaded = false;
                                    MymUtils.showToastMessage(getApplicationContext(), getString(R.string.loadingarconfigfailed));
                                }
                            }
                        });
                    } else {
                        isArConfigLoaded = false;
                        MymUtils.showToastMessage(getApplicationContext(), getString(R.string.loadingarconfigfailed));
                    }
                    isArConfigLoading = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            (findViewById(R.id.arSwitchText)).setVisibility(View.VISIBLE);
                            (findViewById(R.id.ar_load_progressbar)).setVisibility(View.GONE);
                        }
                    });
                    //Log.d(TAG, "loadingArConfig: loading finalized with success: isArConfigLoading=" + isArConfigLoading);
                }

            }
        }.execute();
    }


    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getLong(LAST_UPDATED_TIME_STRING_KEY);
            }
            //updateUI();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        if (mCurrentLocation == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
            //updateUI();
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        mLocationUpdated = false;
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mLocationUpdated = false;
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
        mLocationUpdated = true;
        //Log.d(TAG, "onLocationChanged: mLastUpdateTime=" + mLastUpdateTime + " mCurrentLocation=" + mCurrentLocation.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                positionCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
            }
        });
        isPositionCertified = true;
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS); //UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.d(TAG, "startLocationUpdates: No permission to access fine location, finishing the app");
            Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_nopermissiontouselocation), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
            toast.show();
            finish();
            return;
        }

    }

    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                positionCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
            }
        });
        mLocationUpdated = false;
        isPositionCertified = false;
    }


    public String[] getLocationToExifStrings(Location location, String photoTakenMillis) {
        String[] locationString = new String[14];
        if (isTimeCertified) {
            locationString[10] = Integer.toString(1);
            locationString[11] = photoTakenMillis;
        } else {
            locationString[10] = Integer.toString(0);
            locationString[11] = photoTakenMillis;
        }
        if (isPositionCertified) {
            locationString[12] = Integer.toString(1);
        } else {
            locationString[12] = Integer.toString(0);
        }
        if (isArSwitchOn) {
            locationString[13] = Integer.toString(1);
        } else {
            locationString[13] = Integer.toString(0);
        }
        try {
            double[] gps = new double[2];
            if (location != null) {
                gps[0] = location.getLatitude();
                gps[1] = location.getLongitude();

                if (gps[0] < 0) {
                    locationString[1] = "S";
                    gps[0] = (-1) * gps[0];
                } else {
                    locationString[1] = "N";
                }
                if (gps[1] < 0) {
                    locationString[3] = "W";
                    gps[1] = (-1) * gps[1];
                } else {
                    locationString[3] = "E";
                }
                long latDegInteger = (long) (gps[0] - (gps[0] % 1));
                long latMinInteger = (long) ((60 * (gps[0] - latDegInteger)) - ((60 * (gps[0] - latDegInteger)) % 1));
                long latSecInteger = (long) (((60 * (gps[0] - latDegInteger)) % 1) * 60 * 1000);
                locationString[0] = "" + latDegInteger + "/1," + latMinInteger + "/1," + latSecInteger + "/1000";

                long lonDegInteger = (long) (gps[1] - (gps[1] % 1));
                long lonMinInteger = (long) ((60 * (gps[1] - lonDegInteger)) - ((60 * (gps[1] - lonDegInteger)) % 1));
                long lonSecInteger = (long) (((60 * (gps[1] - lonDegInteger)) % 1) * 60 * 1000);
                locationString[2] = "" + lonDegInteger + "/1," + lonMinInteger + "/1," + lonSecInteger + "/1000";
                locationString[8] = Double.toString(location.getLatitude());
                locationString[9] = Double.toString(location.getLongitude());
                locationString[4] = Float.toString(location.getAccuracy());
                locationString[5] = mLastUpdateTime.toString();
                locationString[6] = location.getProvider();
                locationString[7] = Double.toString(location.getAltitude());
                //Log.d(TAG, "getLocationToExifStrings: LAT:" + gps[0] + " " + (gps[0] % 1) + " " + locationString[0] + locationString[1] + " LON:" + gps[1] + " " + locationString[2] + locationString[3]);
            } else {
                locationString[0] = "" + 0 + "/1," + 0 + "/1," + 0 + "/1000";
                locationString[1] = "N";
                locationString[2] = "" + 0 + "/1," + 0 + "/1," + 0 + "/1000";
                locationString[3] = "W";
                locationString[4] = "0";
                locationString[5] = "0";
                locationString[6] = "LOCOFF";
                locationString[7] = "0";
                locationString[8] = "0";
                locationString[9] = "0";
            }
            for (int index = 0; index < locationString.length; index++) {
                if (locationString[index] == null) locationString[index] = " ";
                //Log.d(TAG, "getLocationToExifStrings: locationString[index]=" + locationString[index]);
            }

        } catch (Exception e) {
            //Log.d(TAG, "getLocationToExifStrings: failed:" + e.toString());
        }
        return locationString;
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current filter indices.
        savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX, mImageDetectionFilterIndex);
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        if (mLastUpdateTime != null) {
            savedInstanceState.putLong(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        }
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
        //Log.d(TAG, "onStart() ********************");
        if (appStartState.equalsIgnoreCase("firstthisversion") || appStartState.equalsIgnoreCase("firstever")) {
            showCallMyMensorSite();
        }
    }


    public void startAppTour() {
        final android.support.v7.app.AlertDialog.Builder alert = new android.support.v7.app.AlertDialog.Builder(this);
        alert.setIcon(R.drawable.logo_mymensor);
        alert.setTitle(getText(R.string.walkthroughStartTitle));
        alert.setMessage(R.string.walkthroughStart);

        alert.setPositiveButton(R.string.go, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isArSwitchOn) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    final Window window = getWindow();
                    if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                    showWalktroughStep3a();
                } else {
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    final Window window = getWindow();
                    if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                    showWalktroughStep1();
                }

            }
        });

        alert.setNegativeButton(R.string.notnow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                final Window window = getWindow();
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                dialog.dismiss();
            }
        });

        alert.show();

    }

    //                .setPrimaryText(getText(R.string.takeaphoto))

    public void showWalktroughStep1() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.cameraShutterButton))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.walkthroughtakeaphotosecond))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            if (!mymIsRunningOnKitKat) {
                                showWalktroughStep2();
                            } else {
                                showWalktroughStep3();
                            }

                        }
                    }
                })
                .show();
    }

    //.setPrimaryText(getText(R.string.makevideo))

    public void showWalktroughStep2() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.videoCameraShutterButton))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.walkthroughmakevideosecond))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showWalktroughStep3();
                        }
                    }
                })
                .show();
    }

    // .setPrimaryText(getText(R.string.selectvp))

    public void showWalktroughStep3() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.vp_list))
                .setPrimaryText(getText(R.string.walkthroughselectvpsecond))
                .setSecondaryText("")
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showWalktroughStep4();
                        }
                    }
                })
                .show();
    }

    public void showWalktroughStep3a() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.vp_list))
                .setPrimaryText(getText(R.string.walkthrougharautoarchive))
                .setSecondaryText("")
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showWalktroughStep4();
                        }
                    }
                })
                .show();
    }

    public void showWalktroughStep4() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.certificationlayot))
                .setPrimaryText(getText(R.string.walkthroughcertificationstatus))
                .setSecondaryText(getText(R.string.walkthroughcertificationstatussecond))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            if (isArSwitchOn) {
                                showWalktroughStep5a();
                            } else {
                                showWalktroughStep5();
                            }
                        }
                    }
                })
                .show();
    }

    public void showWalktroughStep5() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonShowVpCapturesMainScreen))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.walkthroughshowvpcapsecond))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showWalktroughStep5a();
                        }
                    }
                })
                .show();
    }

    public void showWalktroughStep5a() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonCallWebAppMainScreen))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.walkthroughcallwebappsecond))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showWalktroughStep6();
                        }
                    }
                })
                .show();
    }

    public void showWalktroughStep6() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.arSwitchLinearLayout))
                .setPrimaryText(getText(R.string.walkthrougharonoff))
                .setSecondaryText(getText(R.string.walkthrougharonoffsecond))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            if (!isArSwitchOn) {
                                arSwitch.setChecked(true);
                            }
                            showWalktroughStep7();
                        }
                    }
                })
                .show();
    }

    public void showWalktroughStep7() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.imageViewRadarScan))
                .setPrimaryText(getText(R.string.walkthroughradarscansecond))
                .setSecondaryText("")
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showWalktroughStepLast();
                        }
                    }
                })
                .show();
    }

    public void showWalktroughStepLast() {
        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonShowHelpMainScreen))
                .setPrimaryText(getText(R.string.walkthroughLastStep))
                .setSecondaryText(getText(R.string.walkthroughcheckagainifneeded))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_DISMISSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            // User wants to stop tour
                        }
                    }
                })
                .show();
    }

    protected void returnToInitialScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Log.d(TAG, "returnToInitialScreen");
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                //Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                //Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                final Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    //Log.d(TAG, "returnToInitialScreen - Calling FULLSCREEN");
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                if (videoView.isPlaying()) videoView.stopPlayback();
                videoView.setZOrderOnTop(false);
                videoView.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
                buttonShowHelpMainScreen.setVisibility(View.VISIBLE);
                //mCameraView.setVisibility(View.VISIBLE);
                linearLayoutButtonsOnShowVpCaptures.setVisibility(View.GONE);
                linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.GONE);
                linearLayoutVpArStatus.setVisibility(View.GONE);
                if (isArSwitchOn) {
                    mImageDetectionFilterIndex = 1;
                } else {
                    mImageDetectionFilterIndex = 0;
                }
                isShowingVpPhoto = false;
                vpLocationDesTextView.setVisibility(View.GONE);
                vpIdNumber.setVisibility(View.GONE);
                buttonCallConfig.setVisibility(View.GONE);
                buttonAlphaToggle.setVisibility(View.GONE);
                showPreviousVpCaptureButton.setVisibility(View.GONE);
                showNextVpCaptureButton.setVisibility(View.GONE);
                buttonShowHelpShowVPCapScreen.setVisibility(View.GONE);
                buttonShowHelpDescVPScreen.setVisibility(View.GONE);
                if (buttonStartVideoInVpCaptures.isShown())
                    buttonStartVideoInVpCaptures.setVisibility(View.GONE);
                showVpCapturesButton.setVisibility(View.GONE);

                // Layout showing VP configuration state
                if (linearLayoutConfigCaptureVps.isShown()) {
                    linearLayoutConfigCaptureVps.setVisibility(View.GONE);
                    linearLayoutVpArStatus.setVisibility(View.GONE);
                    if (linearLayoutMarkerId.isShown()) {
                        linearLayoutMarkerId.setVisibility(View.GONE);
                        linearLayoutAmbiguousVp.setVisibility(View.GONE);
                        buttonAmbiguousVpToggle.setVisibility(View.GONE);
                    }
                    if (linearLayoutSuperSingleVp.isShown()) {
                        linearLayoutSuperSingleVp.setVisibility(View.GONE);
                        buttonSuperSingleVpToggle.setVisibility(View.GONE);
                    }
                }

                linearLayoutCallWebAppMainScreen.setVisibility(View.VISIBLE);

                if (!isArSwitchOn) {
                    cameraShutterButton.setVisibility(View.VISIBLE);
                    if (!mymIsRunningOnKitKat) {
                        videoCameraShutterButton.setVisibility(View.VISIBLE);
                    }
                    showVpCapturesMainScreenButton.setVisibility(View.VISIBLE);
                } else {
                    cameraShutterButton.setVisibility(View.GONE);
                    if (!mymIsRunningOnKitKat) {
                        videoCameraShutterButton.setVisibility(View.GONE);
                    }
                }
                vpsListView.setVisibility(View.VISIBLE);
                // TURNING ON RADAR SCAN
                if (isArSwitchOn) {
                    radarScanImageView.setVisibility(View.VISIBLE);
                    radarScanImageView.startAnimation(rotationRadarScan);
                } else {
                    if (radarScanImageView.isShown()) {
                        radarScanImageView.clearAnimation();
                        radarScanImageView.setVisibility(View.GONE);
                    }
                }
                // Turning on control buttons
                if (pendingUploadTransfers > 0) {
                    uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                } else {
                    uploadPendingLinearLayout.setVisibility(View.GONE);
                }
                arSwitchLinearLayout.setVisibility(View.VISIBLE);
                arSwitch.setVisibility(View.VISIBLE);
                positionCertifiedButton.setVisibility(View.VISIBLE);
                timeCertifiedButton.setVisibility(View.VISIBLE);
                connectedToServerButton.setVisibility(View.VISIBLE);
            }
        });
    }

    ;


    @Override
    public void onBackPressed() {
        //Log.d(TAG, "Testando JNI:" + getSecretKeyFromJNI());
        boolean specialBackClick = false;
        if (isShowingVpPhoto) {
            specialBackClick = true;
            returnToInitialScreen();
        }

        if (!isShowingVpPhoto) {
            if ((backPressed + 2000 > System.currentTimeMillis()) && (!specialBackClick)) {
                super.onBackPressed();
            } else {
                if (!specialBackClick) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar mSnackBar = Snackbar.make(mCameraView, getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG);
                            TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                            mainTextView.setTextColor(Color.WHITE);
                            mSnackBar.show();
                        }
                    });
                }
            }

            backPressed = System.currentTimeMillis();
        }

    }


    @Override
    public void recreate() {
        super.recreate();
        //Log.d(TAG, "recreate() CALLED");
    }


    @Override
    protected void onResume() {
        super.onResume();
        //Log.d(TAG, "onResume CALLED");

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        //Log.d(TAG, "onResume SCRRES Display Width (Pixels):" + metrics.widthPixels);
        //Log.d(TAG, "onResume SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
            //Log.d(TAG, "onResume - Calling FULLSCREEN");
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        if (!OpenCVLoader.initDebug()) {
            //Log.d("ERROR", "Unable to load OpenCV");
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        // Use TransferUtility to get all upload transfers.
        pendingUploadTransfers = 0;
        observers = transferUtility.getTransfersWithType(TransferType.UPLOAD);
        TransferListener listener = new UploadListener();
        for (TransferObserver observer : observers) {

            // For each transfer we will will create an entry in
            // transferRecordMaps which will display
            // as a single row in the UI
            //HashMap<String, Object> map = new HashMap<String, Object>();
            //Util.fillMap(map, observer, false);
            //transferRecordMaps.add(map);

            // Sets listeners to in progress transfers
            if (!TransferState.COMPLETED.equals(observer.getState())) {
                observer.setTransferListener(listener);
                pendingUploadTransfers++;
                //Log.d(TAG, "Observer ID:" + observer.getId() + " key:" + observer.getKey() + " state:" + observer.getState() + " %:" + observer.getBytesTransferred());
                transferUtility.resume(observer.getId());
            }
        }
        updatePendingUpload();

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        if (isArSwitchOn) setVpsChecked();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                    Snackbar mSnackBar = Snackbar.make(vpsListView.getRootView(), getString(R.string.imagecapready), Snackbar.LENGTH_LONG);
                    TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                    mainTextView.setTextColor(Color.WHITE);
                    mSnackBar.show();
                }

            }
        });
        returnToInitialScreen();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //Log.d(TAG, "onRestart CALLED");
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        //Log.d(TAG, "onRestart SCRRES Display Width (Pixels):" + metrics.widthPixels);
        //Log.d(TAG, "onRestart SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
            //Log.d(TAG, "onRestart - Calling FULLSCREEN");
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        if (isArSwitchOn) setVpsChecked();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                }

            }
        });
        returnToInitialScreen();
    }

    @Override
    protected void onPause() {
        //Log.d(TAG, "onPause CALLED");
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        if (observers != null && !observers.isEmpty()) {
            for (TransferObserver observer : observers) {
                observer.cleanTransferListener();
            }
        }
        if (buttonStartVideoInVpCaptures.isShown())
            buttonStartVideoInVpCaptures.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        //Log.d(TAG, "onStop CALLED");
        mGoogleApiClient.disconnect();
        saveVpsChecked(false);
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    @Override
    protected void onDestroy() {
        //Log.d(TAG, "onDestroy CALLED");
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        // Dispose of native resources.
        disposeFilters(mImageDetectionFilters);
        try {
            this.unregisterReceiver(receiver);
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, "Error while unregistering receiver: " + iae);
        }

        super.onDestroy();
    }

    private void disposeFilters(Filter[] filters) {
        if (filters != null) {
            for (Filter filter : filters) {
                filter.dispose();
            }
        }
    }


    // The OpenCV loader callback.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    //Log.d(TAG, "OpenCV loaded successfully");
                    mCameraMatrix = MymUtils.getCameraMatrix(cameraWidthInPixels, Constants.cameraHeigthInPixels);
                    mCameraView.enableView();
                    //mCameraView.enableFpsMeter();

                    if (!waitingUntilMultipleImageTrackingIsSet) {
                        setMultipleImageTrackingConfiguration();
                    }


                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void setSingleImageTrackingConfiguration(int vpIndex) {
        waitingUntilSingleImageTrackingIsSet = true;
        idTrackingIsSet = false;
        markerBufferSingle = new ArrayList<Mat>();
        try {
            File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (vpIndex) + ".png");
            Mat tmpMarker = Imgcodecs.imread(markervpFile.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
            markerBufferSingle.add(tmpMarker);
        } catch (Exception e) {
            Log.e(TAG, "setSingleImageTrackingConfiguration(int vpIndex): markerImageFileContents failed:" + e.toString());
        }
        ARFilter trackFilter = null;
        //Log.d(TAG, "setSingleImageTrackingConfiguration: markerBufferSingle.toArray().length=" + markerBufferSingle.toArray().length);
        try {
            trackFilter = new ImageDetectionFilter(
                    ImageCapActivity.this,
                    markerBufferSingle.toArray(),
                    1,
                    mCameraMatrix,
                    Constants.standardMarkerlessMarkerWidth);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load marker: " + e.toString());
        }
        if (trackFilter != null) {
            mImageDetectionFilters = new ARFilter[]{
                    new NoneARFilter(),
                    trackFilter
            };
            singleImageTrackingIsSet = true;
            waitingUntilSingleImageTrackingIsSet = false;
            multipleImageTrackingIsSet = false;
            millisWhenSingleImageTrackingWasSet = System.currentTimeMillis();
        }
    }


    private void setMultipleImageTrackingConfiguration() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        waitingUntilMultipleImageTrackingIsSet = true;
                        //Log.d(TAG, "onPreExecute(): setMultipleImageTrackingConfiguration IN BACKGROUND - Lighting Waiting Circle");
                        //Log.d(TAG, "waitingUntilMultipleImageTrackingIsSet=" + waitingUntilMultipleImageTrackingIsSet);
                        //Log.d(TAG, "multipleImageTrackingIsSet=" + multipleImageTrackingIsSet);
                        //Log.d(TAG, "waitingUntilSingleImageTrackingIsSet=" + waitingUntilSingleImageTrackingIsSet);
                        //Log.d(TAG, "singleImageTrackingIsSet=" + singleImageTrackingIsSet);
                        //Log.d(TAG, "isHudOn=" + isHudOn);
                        mProgress.setVisibility(View.VISIBLE);
                        mProgress.startAnimation(rotationMProgress);
                    }
                });

            }

            @Override
            protected Void doInBackground(Void... params) {
                //mBgr = new Mat();
                markerBuffer = new ArrayList<Mat>();
                for (int i = 1; i < (qtyVps); i++) {
                    try {
                        File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + i + ".png");
                        Mat tmpMarker = Imgcodecs.imread(markervpFile.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                        markerBuffer.add(tmpMarker);
                    } catch (Exception e) {
                        Log.e(TAG, "setMultipleImageTrackingConfiguration(): markerImageFileContents failed:" + e.toString());
                    }
                }
                ARFilter trackFilter = null;
                //Log.d(TAG, "setMultipleImageTrackingConfiguration: markerBuffer.toArray().length=" + markerBuffer.toArray().length);
                try {
                    trackFilter = new ImageDetectionFilter(
                            ImageCapActivity.this,
                            markerBuffer.toArray(),
                            (qtyVps - 1),
                            mCameraMatrix,
                            Constants.standardMarkerlessMarkerWidth);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to load marker: " + e.toString());
                }
                if (trackFilter != null) {
                    mImageDetectionFilters = new ARFilter[]{
                            new NoneARFilter(),
                            trackFilter
                    };
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                //Log.d(TAG, "FINISHING setMultipleImageTrackingConfiguration IN BACKGROUND");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Log.d(TAG, "FINISHING setMultipleImageTrackingConfiguration IN BACKGROUND - Turning off Waiting Circle");
                        mProgress.clearAnimation();
                        mProgress.setVisibility(View.GONE);
                        //Log.d(TAG, "FINISHING setMultipleImageTrackingConfiguration IN BACKGROUND - mProgress.isShown():" + mProgress.isShown());
                        // TURNING ON RADAR SCAN
                        if ((!radarScanImageView.isShown()) && (isArSwitchOn) && (!isShowingVpPhoto)) {
                            radarScanImageView.setVisibility(View.VISIBLE);
                            radarScanImageView.startAnimation(rotationRadarScan);
                        }
                        mImageDetectionFilterIndex = 1;
                        waitingUntilMultipleImageTrackingIsSet = false;
                        singleImageTrackingIsSet = false;
                        idTrackingIsSet = false;
                        multipleImageTrackingIsSet = true;
                        isHudOn = 1;

                    }
                });
                //Log.d(TAG, "onPostExecute: waitingUntilMultipleImageTrackingIsSet=" + waitingUntilMultipleImageTrackingIsSet);
                //Log.d(TAG, "multipleImageTrackingIsSet=" + multipleImageTrackingIsSet);
                //Log.d(TAG, "waitingUntilSingleImageTrackingIsSet=" + waitingUntilSingleImageTrackingIsSet);
                //Log.d(TAG, "singleImageTrackingIsSet=" + singleImageTrackingIsSet);
                //Log.d(TAG, "isHudOn=" + isHudOn);
            }
        }.execute();
    }


    private void setIdTrackingConfiguration() {
        singleImageTrackingIsSet = false;
        ARFilter trackFilter = null;
        try {
            trackFilter = new IdMarkerDetectionFilter(
                    ImageCapActivity.this,
                    1,
                    mCameraMatrix,
                    Constants.idMarkerStdSize);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load marker: " + e.toString());
        }
        if (trackFilter != null) {
            mImageDetectionFilters = new ARFilter[]{
                    new NoneARFilter(),
                    trackFilter
            };
            if (mImageDetectionFilterIndex == 1) {
                idTrackingIsSet = true;
                //idTrackingIsSetMillis = System.currentTimeMillis();
            } else {
                idTrackingIsSet = false;
            }
        }
    }


    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    @TargetApi(21)
    private boolean prepareVideoRecorder(String videoFileName) {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setOutputFile(videoFileName);
        try {
            mMediaRecorder.prepare();
            mCameraView.setRecorder(mMediaRecorder);
            mMediaRecorder.start();
            capturingManualVideo = true;
            videoRecorderPrepared = true;
        } catch (IllegalStateException e) {
            //Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            //Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }


    @Override
    public void onCameraViewStarted(final int width,
                                    final int height) {
        //Log.d(TAG, "onCameraViewStarted CALLED width:" + width + " height:" + height);
    }


    @Override
    public void onCameraViewStopped() {
        //Log.d(TAG, "onCameraViewStopped CALLED");
    }

    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        if (mymIsRunningOnFlippedDisplay) {
            Core.flip(rgba, rgba, -1);
        }
        if (isArSwitchOn) {
            verifyVpsChecked();
            setVpsChecked();
        }

        if (!isArSwitchOn) {
            if (!vpIsManuallySelected) vpTrackedInPose = 0;
            final int tmpvpfree = vpTrackedInPose;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (radarScanImageView.isShown()) {
                        radarScanImageView.clearAnimation();
                        radarScanImageView.setVisibility(View.GONE);
                    }
                    int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                    int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                    if (tmpvpfree < firstVisiblePosition || tmpvpfree > lastVisiblePosition) {
                        if (firstFrameAfterArSwitchOff) {
                            vpsListView.smoothScrollToPosition(tmpvpfree);
                            firstFrameAfterArSwitchOff = false;
                        }
                        firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                        lastVisiblePosition = vpsListView.getLastVisiblePosition();
                    }
                    int k = firstVisiblePosition - 1;
                    int i = -1;
                    do {
                        k++;
                        i++;
                        if (k == tmpvpfree) {
                            vpsListView.getChildAt(i).setBackgroundColor(Color.argb(255, 0, 175, 239));
                        } else {
                            vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                        }
                    } while (k < lastVisiblePosition);
                }
            });
        }

        // Start of AR OFF Photo

        if ((!isArSwitchOn) && (askForManualPhoto)) {
            //Log.d(TAG, "Requesting manual photo");
            takePhoto(rgba);
        }

        // End of AR OFF Photo

        // Start of AR OFF Video Recorder

        if ((!isArSwitchOn) && (askForManualVideo) || (!isArSwitchOn) && (capturingManualVideo)) {
            if (askForManualVideo) {
                //Log.d(TAG, "A manual video was requested");
                askForManualVideo = false;
                videoCaptureStartmillis = System.currentTimeMillis();
                long momentoLong = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
                photoTakenTimeMillis[vpTrackedInPose] = momentoLong;
                String momento = String.valueOf(momentoLong);
                videoFileName = vpNumber[vpTrackedInPose] + "_v_" + momento + ".mp4";
                videoThumbnailFileName = vpNumber[vpTrackedInPose] + "_t_" + momento + ".jpg";
                videoFileNameLong = getApplicationContext().getFilesDir() + "/" + videoFileName;
                videoThumbnailFileNameLong = getApplicationContext().getFilesDir() + "/" + videoThumbnailFileName;
                if (!capturingManualVideo) prepareVideoRecorder(videoFileNameLong);
            }
            if (videoRecorderPrepared) {
                if (((System.currentTimeMillis() - videoCaptureStartmillis) < Constants.shortVideoLength) && (!stopManualVideo)) {
                    ////Log.d(TAG, "Waiting for video recording to end:" + (System.currentTimeMillis() - videoCaptureStartmillis));
                } else {
                    if (capturingManualVideo) {
                        stopManualVideo = false;
                        capturingManualVideo = false;
                        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                        float volume = actualVolume / maxVolume;
                        if (videoRecordStopedSoundIDLoaded) {
                            soundPool.play(videoRecordStopedSoundID, volume, volume, 1, 0, 1f);
                            //Log.d(TAG, "Video STOP: Duartion limit exceeded Played sound");
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recText.clearAnimation();
                                videoCameraShutterButton.setVisibility(View.VISIBLE);
                                videoCameraShutterStopButton.setVisibility(View.GONE);
                                videoRecorderTimeLayout.setVisibility(View.GONE);
                            }
                        });
                        mMediaRecorder.stop();
                        mCameraView.setRecorder(null);
                        videoRecorderPrepared = false;
                        videoRecorderChronometer.stop();
                        releaseMediaRecorder();
                        captureVideo();
                    }
                }
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
            }

        }

        // End of AR OFF Video Recorder


        // Start of Photos with AR

        if ((mImageDetectionFilters != null) && (isArSwitchOn)) {

            if ((idTrackingIsSet) && (validMarkerFound) && (vpIsSuperSingle[vpTrackedInPose])) {
                mImageDetectionFilters[mImageDetectionFilterIndex].apply(rgba, isHudOn, 0, vpXCameraRotation[vpTrackedInPose], vpYCameraRotation[vpTrackedInPose], vpZCameraRotation[vpTrackedInPose], vpXCameraDistance[vpTrackedInPose], vpYCameraDistance[vpTrackedInPose], vpZCameraDistance[vpTrackedInPose]);
            } else {
                mImageDetectionFilters[mImageDetectionFilterIndex].apply(rgba, isHudOn, 0, 0, 0, 0, 0, 0, 0);
            }

            if (mImageDetectionFilters[mImageDetectionFilterIndex].getPose() != null) {
                trackingValues = trackingValues.setTrackingValues(mImageDetectionFilters[mImageDetectionFilterIndex].getPose());
                if (!singleImageTrackingIsSet) {
                    vpTrackedInPose = trackingValues.getVpNumberTrackedInPose();
                }
                if (idTrackingIsSet) {
                    int markerIdInPose = trackingValues.getVpNumberTrackedInPose();
                    validMarkerFound = false;
                    ////Log.d(TAG, "idTrackingIsSet: markerIdInPose=" + markerIdInPose);
                    for (int j = 0; j < Constants.validIdMarkersForMyMensor.length; j++) {
                        if (Constants.validIdMarkersForMyMensor[j] == markerIdInPose) {
                            for (int k = 1; k < (qtyVps); k++) {
                                ////Log.d(TAG, "idTrackingIsSet: vpSuperMarkerId[" + k + "]=" + vpSuperMarkerId[k]);
                                if (vpSuperMarkerId[k] == markerIdInPose) {
                                    vpTrackedInPose = k;
                                    validMarkerFound = true;
                                    //Log.d(TAG, "idTrackingIsSet: vpTrackedInPose=" + vpTrackedInPose);
                                }
                            }
                        }
                    }
                    if ((!validMarkerFound) && (!waitingUntilMultipleImageTrackingIsSet))
                        setMultipleImageTrackingConfiguration();
                }

                if ((vpTrackedInPose > 0) && (vpTrackedInPose < (Constants.maxQtyVps))) {
                    final int tmpvp = vpTrackedInPose;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // TURNING OFF RADAR SCAN
                            radarScanImageView.clearAnimation();
                            radarScanImageView.setVisibility(View.GONE);
                            if (vpChecked[vpTrackedInPose]) {
                                vpCheckedView.setVisibility(View.VISIBLE);
                            } else {
                                if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                            }
                            int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                            int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                            if (tmpvp < firstVisiblePosition || tmpvp > lastVisiblePosition) {
                                vpsListView.smoothScrollToPosition(tmpvp);
                                firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                                lastVisiblePosition = vpsListView.getLastVisiblePosition();
                            }
                            int k = firstVisiblePosition - 1;
                            int i = -1;
                            do {
                                k++;
                                i++;
                                if (k == tmpvp) {
                                    vpsListView.getChildAt(i).setBackgroundColor(Color.argb(255, 0, 175, 239));
                                } else {
                                    vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                                }
                            } while (k < lastVisiblePosition);
                        }
                    });

                    // If it is a NORMAL VP DETECTED, then set Single Image Tracking until capture, to speed up things.

                    if ((!vpIsAmbiguous[vpTrackedInPose]) ||
                            ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated) && (!vpIsSuperSingle[vpTrackedInPose])) ||
                            (waitingToCaptureVpAfterDisambiguationProcedureSuccessful)) {
                        if (!vpChecked[vpTrackedInPose]) {
                            if (!singleImageTrackingIsSet) {
                                if (!waitingUntilSingleImageTrackingIsSet) {
                                    if (vpArIsConfigured[vpTrackedInPose])
                                        setSingleImageTrackingConfiguration(vpTrackedInPose);
                                }

                            }
                        }
                    }

                    // If it is a AMBIGUOUS VP DETECTED AND NOT SUPER then set Id tracking to disambiguate.

                    if (((vpIsAmbiguous[vpTrackedInPose]) && (!idTrackingIsSet) && (!waitingToCaptureVpAfterDisambiguationProcedureSuccessful)) || (doubleCheckingProcedureStarted)) {
                        //Log.d(TAG, "MULTIIMAGE: AMBIGUOUS VP DETECTED then set Id tracking to disambiguate");
                        mImageDetectionFilterIndex = 1;
                        setIdTrackingConfiguration();
                        singleImageTrackingIsSet = false;
                        vpIsDisambiguated = false;
                        waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
                        if (doubleCheckingProcedureStarted) {
                            doubleCheckingProcedureStarted = false;
                            doubleCheckingProcedureFinalized = true;
                        }
                    }

                    if (idTrackingIsSet) {
                        //Log.d(TAG, "idTrackingIsSet: validMarkerFound=" + validMarkerFound);
                        if (validMarkerFound) {
                            if (!vpIsSuperSingle[vpTrackedInPose]) {
                                vpIsDisambiguated = true;
                                waitingToCaptureVpAfterDisambiguationProcedureSuccessful = true;
                                //Log.d(TAG, "Disambiguation SUCCESFULL: waiting for vp capture: vpTrackedInPose=" + vpTrackedInPose);
                                setSingleImageTrackingConfiguration(vpTrackedInPose);
                            } else {
                                checkPositionToTarget(trackingValues, rgba);
                            }
                        }
                    }

                    if (singleImageTrackingIsSet) {
                        if (((!vpIsAmbiguous[vpTrackedInPose]) ||
                                ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated)) ||
                                (waitingToCaptureVpAfterDisambiguationProcedureSuccessful)) && (!vpIsSuperSingle[vpTrackedInPose])) {
                            if (!vpChecked[vpTrackedInPose]) {
                                if (!waitingUntilMultipleImageTrackingIsSet)
                                    checkPositionToTarget(trackingValues, rgba);
                            }
                        }

                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                            int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                            int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                            int k = firstVisiblePosition - 1;
                            int i = -1;
                            do {
                                k++;
                                i++;
                                vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                            } while (k < lastVisiblePosition);

                        }
                    });
                    if (isHudOn == 0) isHudOn = 1;
                    ////Log.d(TAG, "INVALID VP TRACKED IN POSE");
                }

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                        int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                        int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                        int k = firstVisiblePosition - 1;
                        int i = -1;
                        do {
                            k++;
                            i++;
                            vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                        } while (k < lastVisiblePosition);
                        if ((!radarScanImageView.isShown()) && (isArSwitchOn) && (!isShowingVpPhoto)) {
                            radarScanImageView.setVisibility(View.VISIBLE);
                            radarScanImageView.startAnimation(rotationRadarScan);
                        }
                    }
                });
                /*
                //Log.d(TAG,
                        "Tracking LOST!!!! (singleImageTrackingIsSet="+singleImageTrackingIsSet+
                        ") (waitingToCaptureVpAfterDisambiguationProcedureSuccessful="+waitingToCaptureVpAfterDisambiguationProcedureSuccessful+")");
                        */
                if ((singleImageTrackingIsSet)
                        && ((!waitingToCaptureVpAfterDisambiguationProcedureSuccessful) ||
                        (System.currentTimeMillis() - millisWhenSingleImageTrackingWasSet > 500))) {
                    if (!isShowingVpPhoto) {
                        singleImageTrackingIsSet = false;
                        if (!waitingUntilMultipleImageTrackingIsSet)
                            setMultipleImageTrackingConfiguration();
                    }
                }

                if ((idTrackingIsSet) && (vpIsSuperSingle[vpTrackedInPose])) {
                    validMarkerFound = false;
                    if ((!waitingUntilMultipleImageTrackingIsSet)) {
                        setMultipleImageTrackingConfiguration();
                    }
                }


                if ((isHudOn == 0) && (!isShowingVpPhoto)) isHudOn = 1;
            }
        }
        return rgba;
    }


    @TargetApi(21)
    private void captureVideo() {
        final String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        String[] fileInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.equalsIgnoreCase(videoFileName);
            }
        });

        // Preparing UI for user decision upon capture acceptance
        if ((!vpPhotoAccepted) && (!vpPhotoRejected)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setVisibility(View.GONE);
                    vpsListView.setVisibility(View.GONE);
                    uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                    arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                    arSwitch.setVisibility(View.INVISIBLE);
                    positionCertifiedButton.setVisibility(View.INVISIBLE);
                    timeCertifiedButton.setVisibility(View.INVISIBLE);
                    connectedToServerButton.setVisibility(View.INVISIBLE);
                    cameraShutterButton.setVisibility(View.INVISIBLE);
                    if (!mymIsRunningOnKitKat) {
                        videoCameraShutterButton.setVisibility(View.INVISIBLE);
                    }
                    showVpCapturesMainScreenButton.setVisibility(View.GONE);
                    linearLayoutCallWebAppMainScreen.setVisibility(View.GONE);
                    buttonShowHelpMainScreen.setVisibility(View.GONE);
                    if (radarScanImageView.isShown()) {
                        radarScanImageView.clearAnimation();
                        radarScanImageView.setVisibility(View.GONE);
                    }
                    videoView.setVisibility(View.GONE);
                    Uri videoFileTMP = Uri.fromFile(new File(getApplicationContext().getFilesDir(), videoFileName));
                    //Log.d(TAG, "media PATH:" + videoFileTMP.getPath());
                    boolean fileNotFound = true;
                    do {
                        try {
                            videoView.setVideoURI(videoFileTMP);
                            fileNotFound = false;
                        } catch (Exception e) {
                            fileNotFound = true;
                        }
                        //Log.d(TAG, "Trying Media:" + fileNotFound);
                    } while (fileNotFound);
                    videoView.setZOrderOnTop(true);
                    videoView.setVisibility(View.VISIBLE);
                    videoView.setMediaController(mMediaController);
                    videoView.start();
                    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            videoView.setZOrderOnTop(false);
                            videoView.setVisibility(View.GONE);
                            acceptVpPhotoButton.setVisibility(View.VISIBLE);
                            rejectVpPhotoButton.setVisibility(View.VISIBLE);
                            buttonRemarkVpPhoto.setVisibility(View.VISIBLE);
                            buttonReplayVpVideo.setVisibility(View.VISIBLE);
                            //Log.d(TAG, "Turned on VIDEO Decision Buttons!!!! captureVideo 1:vpphta:" + vpPhotoAccepted + "vpphtr:" + vpPhotoRejected);
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);

                            //Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                            //Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                            final Window window = getWindow();
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                //Log.d(TAG, "captureVideo Acceptance - Calling FULLSCREEN");
                                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                            }
                        }
                    });
                }
            });
        }

        do {
            // Waiting for user response
            if (vpVideoTobeReplayed) {
                vpVideoTobeReplayed = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        videoView.setVisibility(View.VISIBLE);
                        videoView.start();
                        videoView.setZOrderOnTop(true);
                        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                videoView.setZOrderOnTop(false);
                                videoView.setVisibility(View.GONE);
                                acceptVpPhotoButton.setVisibility(View.VISIBLE);
                                rejectVpPhotoButton.setVisibility(View.VISIBLE);
                                buttonRemarkVpPhoto.setVisibility(View.VISIBLE);
                                buttonReplayVpVideo.setVisibility(View.VISIBLE);
                                //Log.d(TAG, "Turned on VIDEO Decision Buttons!!!! captureVideo 2");
                                DisplayMetrics metrics = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                                //Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                                //Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                                final Window window = getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                    //Log.d(TAG, "captureVideo Acceptance - Calling FULLSCREEN");
                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                }
                            }
                        });
                    }
                });
            }
            if (vpPhotoTobeRemarked) {
                vpPhotoTobeRemarked = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder alert = new AlertDialog.Builder(ImageCapActivity.this);

                        alert.setTitle(R.string.remark);
                        alert.setMessage(R.string.sizelimit100);

                        // Set an EditText view to get user input
                        final EditText input = new EditText(ImageCapActivity.this);

                        alert.setView(input);

                        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                vpPhotoRemark = input.getText().toString();
                                DisplayMetrics metrics = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                                //Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                                //Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                                final Window window = getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                    //Log.d(TAG, "captureVideo Acceptance - Calling FULLSCREEN");
                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                }
                            }
                        });

                        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                DisplayMetrics metrics = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                                //Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                                //Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                                final Window window = getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                    //Log.d(TAG, "captureVideo Acceptance - Calling FULLSCREEN");
                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                }
                                // Canceled.
                            }
                        });

                        alert.show();
                    }
                });
            }
        } while ((!vpPhotoAccepted) && (!vpPhotoRejected));
        vpVideoTobeReplayed = false;
        vpPhotoTobeRemarked = false;
        //Log.d(TAG, "takePhoto: LOOP ENDED: vpPhotoAccepted:" + vpPhotoAccepted + " vpPhotoRejected:" + vpPhotoRejected);

        if (vpPhotoAccepted) {
            //Log.d(TAG, "AROFF Video: vpPhotoAccepted!!!!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoView.setVisibility(View.GONE);
                    acceptVpPhotoButton.setVisibility(View.GONE);
                    rejectVpPhotoButton.setVisibility(View.GONE);
                    buttonRemarkVpPhoto.setVisibility(View.GONE);
                    buttonReplayVpVideo.setVisibility(View.GONE);
                    vpsListView.setVisibility(View.VISIBLE);
                    buttonShowHelpMainScreen.setVisibility(View.VISIBLE);
                    // TURNING ON RADAR SCAN
                    if (isArSwitchOn) {
                        radarScanImageView.setVisibility(View.VISIBLE);
                        radarScanImageView.startAnimation(rotationRadarScan);

                    }
                    if (!isArSwitchOn) {
                        cameraShutterButton.setVisibility(View.VISIBLE);
                        if (!mymIsRunningOnKitKat) {
                            videoCameraShutterButton.setVisibility(View.VISIBLE);
                        }
                        showVpCapturesMainScreenButton.setVisibility(View.VISIBLE);
                    }
                    linearLayoutCallWebAppMainScreen.setVisibility(View.VISIBLE);
                    if (pendingUploadTransfers > 0)
                        uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                    arSwitchLinearLayout.setVisibility(View.VISIBLE);
                    arSwitch.setVisibility(View.VISIBLE);
                    positionCertifiedButton.setVisibility(View.VISIBLE);
                    timeCertifiedButton.setVisibility(View.VISIBLE);
                    connectedToServerButton.setVisibility(View.VISIBLE);
                }
            });
            String vpPhotoRemark1000 = null;
            if (vpPhotoRemark != null) {
                vpPhotoRemark1000 = Uri.encode(this.vpPhotoRemark.substring(0, Math.min(this.vpPhotoRemark.length(), 1000)), "@#&=*+-_.,:!?()/~'%");
                vpPhotoRemark = null;
            }
            try {
                if (fileInDirectory != null) {
                    File videoFile = new File(getApplicationContext().getFilesDir(), videoFileName);
                    File pictureVideoThumbnailFile = new File(videoThumbnailFileNameLong);
                    Bitmap videoThumbnailHdBitmap = null;
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    try {
                        mediaMetadataRetriever.setDataSource(videoFileNameLong);
                        videoThumbnailHdBitmap = mediaMetadataRetriever.getFrameAtTime();
                    } catch (IllegalArgumentException iae) {
                        Log.e(TAG, "MediaMetadataRetriever exception: " + iae.toString());
                    } finally {
                        mediaMetadataRetriever.release();
                    }
                    if (videoThumbnailHdBitmap != null) {
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureVideoThumbnailFile);
                            videoThumbnailHdBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                            fos.close();
                        } catch (Exception e) {
                            Log.e(TAG, "videoThumbnailHdBitmap saving to videoThumbnailFileNameLong failed:" + e.toString());
                        }
                    } else {
                        try {
                            InputStream fis = MymUtils.getLocalFile("mymensoremptytn.png", getApplicationContext());
                            if (!(fis == null)) {
                                videoThumbnailHdBitmap = BitmapFactory.decodeStream(fis);
                                fis.close();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "videoThumbnailHdBitmap contents from vpLocationDescImageFile0 failed:" + e.toString());
                        }
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureVideoThumbnailFile);
                            videoThumbnailHdBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                            fos.close();
                        } catch (Exception e) {
                            Log.e(TAG, "videoThumbnailHdBitmap saving to videoThumbnailFileNameLong from descvp0 failed:" + e.toString());
                        }
                    }
                    //Log.d(TAG, "pictureFile.getName()=" + pictureVideoThumbnailFile.getName());
                    //Log.d(TAG, "pictureVideoThumbnailFile.getPath()=" + pictureVideoThumbnailFile.getPath());
                    ObjectMetadata thumbnailMetadata = new ObjectMetadata();
                    Map<String, String> userThumbMetadata = new HashMap<String, String>();
                    userThumbMetadata.put("vp", "" + (vpTrackedInPose));
                    userThumbMetadata.put("mymensoraccount", mymensorAccount);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    thumbnailMetadata.setUserMetadata(userThumbMetadata);
                    //uploading the objects
                    TransferObserver thumbObserver = MymUtils.storeRemoteFile(
                            transferUtility,
                            Constants.capturesFolder + "/" + mymensorAccount + "/" + videoThumbnailFileName,
                            Constants.BUCKET_NAME,
                            pictureVideoThumbnailFile,
                            thumbnailMetadata);
                    thumbObserver.setTransferListener(new UploadListener());
                    pendingUploadTransfers++;
                    updatePendingUpload();

                    //Log.d(TAG, "videoFile.getName()=" + videoFile.getName());
                    //Log.d(TAG, "videoFile.getPath()=" + videoFile.getPath());
                    String fileSha256Hash = MymUtils.getFileHash(videoFile);
                    locPhotoToExif = getLocationToExifStrings(mCurrentLocation, Long.toString(photoTakenTimeMillis[vpTrackedInPose]));
                    ObjectMetadata myObjectMetadata = new ObjectMetadata();
                    //create a map to store user metadata
                    Map<String, String> userMetadata = new HashMap<String, String>();
                    userMetadata.put("loclatitude", locPhotoToExif[8]);
                    userMetadata.put("loclongitude", locPhotoToExif[9]);
                    userMetadata.put("vp", "" + (vpTrackedInPose));
                    userMetadata.put("mymensoraccount", mymensorAccount);
                    userMetadata.put("origmymacc", origMymAcc);
                    userMetadata.put("deviceid", deviceId);
                    userMetadata.put("clitype", Constants.CLIENT_SOFTWARE_TYPE);
                    userMetadata.put("locprecisioninm", locPhotoToExif[4]);
                    userMetadata.put("localtitude", locPhotoToExif[7]);
                    userMetadata.put("locmillis", locPhotoToExif[5]);
                    userMetadata.put("locmethod", locPhotoToExif[6]);
                    userMetadata.put("loccertified", locPhotoToExif[12]);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                    sdf.setTimeZone(TimeZone.getDefault());
                    String formattedDateTime = sdf.format(photoTakenTimeMillis[vpTrackedInPose]);
                    userMetadata.put("datetime", formattedDateTime);
                    userMetadata.put("phototakenmillis", locPhotoToExif[11]);
                    userMetadata.put("timecertified", locPhotoToExif[10]);
                    userMetadata.put("isarswitchOn", locPhotoToExif[13]);
                    userMetadata.put("sha-256", fileSha256Hash);
                    userMetadata.put("remark", vpPhotoRemark1000);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = MymUtils.storeRemoteFileLazy(
                            transferUtility,
                            Constants.capturesFolder + "/" + mymensorAccount + "/" + videoFileName,
                            Constants.BUCKET_NAME,
                            videoFile,
                            myObjectMetadata);
                    try {
                        observer.setTransferListener(new UploadListener());
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while setting Transfer Listener: " + e);
                    }
                    pendingUploadTransfers++;
                    updatePendingUpload();
                    vpPhotoAccepted = false;
                    if (observer != null) {
                        //Log.d(TAG, "takePhoto: AWS s3 Observer: " + observer.getState().toString());
                        //Log.d(TAG, "takePhoto: AWS s3 Observer: " + observer.getAbsoluteFilePath());
                        //Log.d(TAG, "takePhoto: AWS s3 Observer: " + observer.getBucket());
                        //Log.d(TAG, "takePhoto: AWS s3 Observer: " + observer.getKey());
                    } else {
                        //Log.d(TAG, "Failure to save video to remote storage: videoFile.exists()==false");
                        vpChecked[vpTrackedInPose] = false;
                        if (isArSwitchOn) {
                            setVpsChecked();
                            saveVpsChecked(false);
                        }
                    }
                } else {
                    //Log.d(TAG, "Failure to save video to remote storage: videoFile.exists()==false");
                    vpChecked[vpTrackedInPose] = false;
                    if (isArSwitchOn) {
                        setVpsChecked();
                        saveVpsChecked(false);
                    }
                }
            } catch (AmazonServiceException ase) {
                Log.e(TAG, "Failure to save video : AmazonServiceException: Error when writing captured image to Remote Storage:" + ase.toString());
                isConnectedToServer = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isConnectedToServer) {
                            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                        } else {
                            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failure to save video to remote storage:" + e.toString());
                vpChecked[vpTrackedInPose] = false;
                if (isArSwitchOn) {
                    setVpsChecked();
                    saveVpsChecked(false);
                }
                //waitingToCaptureVpAfterDisambiguationProcedureSuccessful =true;
                e.printStackTrace();
            }
        }

        if (vpPhotoRejected) {
            //Log.d(TAG, "AROFF Video: vpPhotoRejected!!!!");
            File videoFile = new File(getApplicationContext().getFilesDir(), videoFileName);
            try {
                if (!videoFile.delete()) {
                    //Log.d(TAG, "Rejected video could not be deleted!!!!!!!");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while deletion rejected video:" + e.toString());
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoView.setVisibility(View.GONE);
                    //mCameraView.setZOrderOnTop(true);
                    acceptVpPhotoButton.setVisibility(View.GONE);
                    rejectVpPhotoButton.setVisibility(View.GONE);
                    buttonRemarkVpPhoto.setVisibility(View.GONE);
                    buttonReplayVpVideo.setVisibility(View.GONE);
                    vpsListView.setVisibility(View.VISIBLE);
                    buttonShowHelpMainScreen.setVisibility(View.VISIBLE);
                    // TURNING ON RADAR SCAN
                    if (isArSwitchOn) {
                        radarScanImageView.setVisibility(View.VISIBLE);
                        radarScanImageView.startAnimation(rotationRadarScan);

                    }
                    if (!isArSwitchOn) {
                        cameraShutterButton.setVisibility(View.VISIBLE);
                        if (!mymIsRunningOnKitKat) {
                            videoCameraShutterButton.setVisibility(View.VISIBLE);
                        }
                        showVpCapturesMainScreenButton.setVisibility(View.VISIBLE);
                    }
                    linearLayoutCallWebAppMainScreen.setVisibility(View.VISIBLE);
                    if (pendingUploadTransfers > 0)
                        uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                    arSwitchLinearLayout.setVisibility(View.VISIBLE);
                    arSwitch.setVisibility(View.VISIBLE);
                    positionCertifiedButton.setVisibility(View.VISIBLE);
                    timeCertifiedButton.setVisibility(View.VISIBLE);
                    connectedToServerButton.setVisibility(View.VISIBLE);
                }
            });
            vpChecked[vpTrackedInPose] = false;
            if (isArSwitchOn) {
                setVpsChecked();
                saveVpsChecked(false);
            }
            lastVpPhotoRejected = true;
            vpPhotoRejected = false;
            vpPhotoRequestInProgress = false;
            //Log.d(TAG, "takePhoto: vpPhotoRejected >>>>> calling setMarkerlessTrackingConfiguration");
            //Log.d(TAG, "takePhoto: vpPhotoRejected: vpPhotoRequestInProgress = " + vpPhotoRequestInProgress);
            if ((!waitingUntilMultipleImageTrackingIsSet) && (isArSwitchOn)) {
                setMultipleImageTrackingConfiguration();
            }
        }
    }


    private void checkPositionToTarget(TrackingValues trackingValues, final Mat rgba) {

        if (!vpIsSuperSingle[vpTrackedInPose]) {
            inPosition = ((Math.abs(trackingValues.getX() - 0) <= tolerancePosition) &&
                    (Math.abs(trackingValues.getY() - 0) <= tolerancePosition) &&
                    (Math.abs(trackingValues.getZ() - vpZCameraDistance[vpTrackedInPose]) <= tolerancePosition));
            inRotation = ((Math.abs(trackingValues.getEAX() - 0) <= toleranceRotation) &&
                    (Math.abs(trackingValues.getEAY() - 0) <= toleranceRotation) &&
                    (Math.abs(trackingValues.getEAZ() - 0) <= toleranceRotation));
        } else {

            inPosition = ((Math.abs(trackingValues.getXid() - vpXCameraDistance[vpTrackedInPose]) <= (Constants.tolerancePositionSuper)) &&
                    (Math.abs(trackingValues.getYid() - vpYCameraDistance[vpTrackedInPose]) <= (Constants.tolerancePositionSuper)) &&
                    (Math.abs(trackingValues.getZ() - vpZCameraDistance[vpTrackedInPose]) <= (Constants.tolerancePositionSuper)));
            inRotation = ((Math.abs(trackingValues.getEAX() - vpXCameraRotation[vpTrackedInPose]) <= (Constants.toleranceRotationSuper)) &&
                    (Math.abs(trackingValues.getEAY() - vpYCameraRotation[vpTrackedInPose]) <= (Constants.toleranceRotationSuper)) &&
                    (Math.abs(trackingValues.getEAZ() - vpZCameraRotation[vpTrackedInPose]) <= (Constants.toleranceRotationSuper)));
        }

        //Log.d(TAG, "TST inPos=" + inPosition + " inRot=" + inRotation + " wtUtlMultpl=" + waitingUntilMultipleImageTrackingIsSet + " vpPhReq=" + vpPhotoRequestInProgress + " Score=" + inPosRotScore);
        if ((inPosition) && (inRotation) && (!waitingUntilMultipleImageTrackingIsSet) && (!vpPhotoRequestInProgress)) {
            inPosRotScore++;
            if (vpIsSuperSingle[vpTrackedInPose]) {
                //Log.d(TAG, "TST IN delta POS=" + Math.abs(trackingValues.getXid() - vpXCameraDistance[vpTrackedInPose])
                //        + " deltaY=" + Math.abs(trackingValues.getYid() - vpYCameraDistance[vpTrackedInPose])
                //        + " deltaZ=" + Math.abs(trackingValues.getZ() - vpZCameraDistance[vpTrackedInPose]));
                //Log.d(TAG, "TST IN delta ROT=" + Math.abs(trackingValues.getEAX() - vpXCameraRotation[vpTrackedInPose])
                //        + " deltaRY=" + Math.abs(trackingValues.getEAY() - vpYCameraRotation[vpTrackedInPose])
                //        + " deltaRZ=" + Math.abs(trackingValues.getEAZ() - vpZCameraRotation[vpTrackedInPose]));
            }
            if ((vpIsAmbiguous[vpTrackedInPose]) && (!doubleCheckingProcedureStarted) && (!doubleCheckingProcedureFinalized)) {
                mImageDetectionFilterIndex = 1;
                setIdTrackingConfiguration();
                doubleCheckingProcedureStarted = true;
                inPosRotScore = 0;
            }

            if ((!vpIsAmbiguous[vpTrackedInPose]) || ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated) && (doubleCheckingProcedureFinalized)) || vpIsSuperSingle[vpTrackedInPose]) {
                if (!waitingUntilMultipleImageTrackingIsSet) {
                    if (isHudOn == 1) {
                        if (inPosRotScore > 1) {
                            isHudOn = 0;
                        }
                    } else {
                        //Log.d(TAG, "Calling takePhoto: doubleCheckingProcedureFinalized=" + doubleCheckingProcedureFinalized);
                        inPosRotScore = 0;
                        if (!vpChecked[vpTrackedInPose]) takePhoto(rgba);
                    }
                }
            }
        } else {
            inPosRotScore = 0;
        }

    }

    private void takePhoto(Mat rgba) {
        Bitmap bitmapImage = null;
        long momentoLong = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
        photoTakenTimeMillis[vpTrackedInPose] = momentoLong;
        if (askForManualPhoto) askForManualPhoto = false;
        final String momento = String.valueOf(momentoLong);
        String pictureFileName;
        pictureFileName = vpNumber[vpTrackedInPose] + "_p_" + momento + ".jpg";
        File pictureFile = new File(getApplicationContext().getFilesDir(), pictureFileName);

        //Log.d(TAG, "takePhoto: a new camera frame image is delivered " + momento);
        //Log.d(TAG, "takePhoto: pictureFileName including account: " + pictureFileName);
        if (isArSwitchOn) {
            if ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated))
                waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
            if (doubleCheckingProcedureFinalized) {
                doubleCheckingProcedureStarted = false;
                doubleCheckingProcedureFinalized = false;
            }
        }
        if (rgba != null) {
            bitmapImage = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgba, bitmapImage);
            final int width = bitmapImage.getWidth();
            final int height = bitmapImage.getHeight();
            //Log.d(TAG, "takePhoto: Camera frame width: " + width + " height: " + height);
        }
        if (bitmapImage != null) {
            // Turning tracking OFF
            mImageDetectionFilterIndex = 0;
            // Playing photo capture sound
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            float volume = actualVolume / maxVolume;
            // Is the sound loaded already?
            if (camShutterSoundIDLoaded) {
                soundPool.play(camShutterSoundID, volume, volume, 1, 0, 1f);
                //Log.d(TAG, "takePhoto: Camera Shutter Played sound");
            }
            // Preparing UI for user decision upon capture acceptance
            if ((!vpPhotoAccepted) && (!vpPhotoRejected)) {
                final Bitmap tmpBitmapImage = bitmapImage;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(tmpBitmapImage);
                        imageView.resetZoom();
                        imageView.setVisibility(View.VISIBLE);
                        if (imageView.getImageAlpha() == 128) imageView.setImageAlpha(255);
                        acceptVpPhotoButton.setVisibility(View.VISIBLE);
                        rejectVpPhotoButton.setVisibility(View.VISIBLE);
                        buttonRemarkVpPhoto.setVisibility(View.VISIBLE);
                        vpsListView.setVisibility(View.GONE);
                        uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                        arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                        arSwitch.setVisibility(View.INVISIBLE);
                        positionCertifiedButton.setVisibility(View.INVISIBLE);
                        timeCertifiedButton.setVisibility(View.INVISIBLE);
                        connectedToServerButton.setVisibility(View.INVISIBLE);
                        cameraShutterButton.setVisibility(View.INVISIBLE);
                        if (!mymIsRunningOnKitKat) {
                            videoCameraShutterButton.setVisibility(View.INVISIBLE);
                        }
                        showVpCapturesMainScreenButton.setVisibility(View.GONE);
                        linearLayoutCallWebAppMainScreen.setVisibility(View.GONE);
                        buttonShowHelpMainScreen.setVisibility(View.GONE);
                        if (radarScanImageView.isShown()) {
                            radarScanImageView.clearAnimation();
                            radarScanImageView.setVisibility(View.GONE);
                        }
                    }
                });
            }

            do {
                // Waiting for user response
                if (vpPhotoTobeRemarked) {
                    vpPhotoTobeRemarked = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder alert = new AlertDialog.Builder(ImageCapActivity.this);

                            alert.setTitle(R.string.remark);
                            alert.setMessage(R.string.sizelimit100);

                            // Set an EditText view to get user input
                            final EditText input = new EditText(ImageCapActivity.this);

                            alert.setView(input);

                            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    vpPhotoRemark = input.getText().toString();
                                }
                            });

                            alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Canceled.
                                }
                            });

                            alert.show();
                        }
                    });
                }
            } while ((!vpPhotoAccepted) && (!vpPhotoRejected));

            //Log.d(TAG, "takePhoto: LOOP ENDED: vpPhotoAccepted:" + vpPhotoAccepted + " vpPhotoRejected:" + vpPhotoRejected);

            if (vpPhotoAccepted) {
                //Log.d(TAG, "takePhoto: vpPhotoAccepted!!!!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setVisibility(View.GONE);
                        acceptVpPhotoButton.setVisibility(View.GONE);
                        rejectVpPhotoButton.setVisibility(View.GONE);
                        buttonRemarkVpPhoto.setVisibility(View.GONE);
                        vpsListView.setVisibility(View.VISIBLE);
                        vpChecked[vpTrackedInPose] = true;
                        locPhotoToExif = getLocationToExifStrings(mCurrentLocation, momento);
                        if (pendingUploadTransfers > 0)
                            uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                        arSwitchLinearLayout.setVisibility(View.VISIBLE);
                        arSwitch.setVisibility(View.VISIBLE);
                        positionCertifiedButton.setVisibility(View.VISIBLE);
                        timeCertifiedButton.setVisibility(View.VISIBLE);
                        connectedToServerButton.setVisibility(View.VISIBLE);
                        buttonShowHelpMainScreen.setVisibility(View.VISIBLE);
                        if (!isArSwitchOn) {
                            cameraShutterButton.setVisibility(View.VISIBLE);
                            if (!mymIsRunningOnKitKat) {
                                videoCameraShutterButton.setVisibility(View.VISIBLE);
                            }
                            showVpCapturesMainScreenButton.setVisibility(View.VISIBLE);
                        }
                        linearLayoutCallWebAppMainScreen.setVisibility(View.VISIBLE);
                    }
                });
                if (isArSwitchOn) {
                    setVpsChecked();
                    saveVpsChecked(false);
                }
                String vpPhotoRemark1000 = null;
                try {
                    if (vpPhotoRemark != null) {
                        vpPhotoRemark1000 = Uri.encode(this.vpPhotoRemark.substring(0, Math.min(this.vpPhotoRemark.length(), 1000)), "@#&=*+-_.,:!?()/~'%");
                        vpPhotoRemark = null;
                    }
                } catch (Exception e) {
                    vpPhotoRemark = null;
                }

                try {
                    //pictureFile.renameTo(new File(getApplicationContext().getFilesDir(), pictureFileName));
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    bitmapImage.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.close();
                    ExifInterface locPhotoTags = new ExifInterface(pictureFile.getAbsolutePath());
                    locPhotoTags.setAttribute("GPSLatitude", locPhotoToExif[0]); //LocLatitude
                    locPhotoTags.setAttribute("GPSLatitudeRef", locPhotoToExif[1]);
                    locPhotoTags.setAttribute("GPSLongitude", locPhotoToExif[2]); //LocLongitude
                    locPhotoTags.setAttribute("GPSLongitudeRef", locPhotoToExif[3]);
                    locPhotoTags.setAttribute("GPSAltitude", locPhotoToExif[4]); //LocPrecisioninm
                    locPhotoTags.setAttribute("DateTime", locPhotoToExif[5]); //LocMillis
                    locPhotoTags.setAttribute("GPSProcessingMethod", locPhotoToExif[6]); //LocMethod
                    locPhotoTags.setAttribute("GPSAltitudeRef", locPhotoToExif[10]); //IsTimeCertified
                    locPhotoTags.setAttribute("GPSDateStamp", locPhotoToExif[11]); //photoTakenTimeMillis
                    locPhotoTags.setAttribute("Make", locPhotoToExif[12]); //IsPositionCertified
                    locPhotoTags.setAttribute("Model", locPhotoToExif[13]); //IsArSwitchOn
                    locPhotoTags.saveAttributes();
                    String fileSha256Hash = MymUtils.getFileHash(pictureFile);
                    ObjectMetadata myObjectMetadata = new ObjectMetadata();
                    //create a map to store user metadata
                    Map<String, String> userMetadata = new HashMap<String, String>();
                    userMetadata.put("loclatitude", locPhotoToExif[8]);
                    userMetadata.put("loclongitude", locPhotoToExif[9]);
                    userMetadata.put("vp", "" + (vpTrackedInPose));
                    userMetadata.put("mymensoraccount", mymensorAccount);
                    userMetadata.put("origmymacc", origMymAcc);
                    userMetadata.put("deviceid", deviceId);
                    userMetadata.put("clitype", Constants.CLIENT_SOFTWARE_TYPE);
                    userMetadata.put("locprecisioninm", locPhotoToExif[4]);
                    userMetadata.put("localtitude", locPhotoToExif[7]);
                    userMetadata.put("locmillis", locPhotoToExif[5]);
                    userMetadata.put("locmethod", locPhotoToExif[6]);
                    userMetadata.put("loccertified", locPhotoToExif[12]);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                    sdf.setTimeZone(TimeZone.getDefault());
                    String formattedDateTime = sdf.format(photoTakenTimeMillis[vpTrackedInPose]);
                    userMetadata.put("datetime", formattedDateTime);
                    userMetadata.put("phototakenmillis", locPhotoToExif[11]);
                    userMetadata.put("timecertified", locPhotoToExif[10]);
                    userMetadata.put("isarswitchOn", locPhotoToExif[13]);
                    userMetadata.put("sha-256", fileSha256Hash);
                    userMetadata.put("remark", vpPhotoRemark1000);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = MymUtils.storeRemoteFile(
                            transferUtility,
                            Constants.capturesFolder + "/" + mymensorAccount + "/" + pictureFileName,
                            Constants.BUCKET_NAME,
                            pictureFile,
                            myObjectMetadata);

                    observer.setTransferListener(new UploadListener());
                    pendingUploadTransfers++;
                    updatePendingUpload();

                    if ((singleImageTrackingIsSet) && (isArSwitchOn)) {
                        //Log.d(TAG, "takePhoto: vpPhotoAccepted >>>>> calling setMarkerlessTrackingConfiguration");
                        if (!waitingUntilMultipleImageTrackingIsSet) {
                            setMultipleImageTrackingConfiguration();
                        }
                        singleImageTrackingIsSet = false;
                        vpIsDisambiguated = false;
                    }
                } catch (AmazonServiceException ase) {
                    Log.e(TAG, "takePhoto: AmazonServiceException: Error when writing captured image to Remote Storage:" + ase.toString());
                    isConnectedToServer = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isConnectedToServer) {
                                connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                            } else {
                                connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "takePhoto: Error when writing captured image to Remote Storage:" + e.toString());
                    vpChecked[vpTrackedInPose] = false;
                    if (isArSwitchOn) {
                        setVpsChecked();
                        saveVpsChecked(false);
                    }
                }
                vpPhotoAccepted = false;
                vpPhotoRequestInProgress = false;
                //Log.d(TAG, "takePhoto: vpPhotoAccepted: vpPhotoRequestInProgress = " + vpPhotoRequestInProgress);
                if (isArSwitchOn) isHudOn = 1;
                if ((!waitingUntilMultipleImageTrackingIsSet) && (isArSwitchOn)) {
                    setMultipleImageTrackingConfiguration();
                }
            }

            if (vpPhotoRejected) {
                //Log.d(TAG, "takePhoto: vpPhotoRejected!!!!");
                try {
                    if (pictureFile.delete()) {
                        //Log.d(TAG, "takePhoto: vpPhotoRejected >>>>> " + pictureFile.getName() + " deleted successfully");
                    }
                    ;

                } catch (Exception e) {

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (imageView.getImageAlpha() == 128) imageView.setImageAlpha(128);
                        imageView.setVisibility(View.GONE);
                        acceptVpPhotoButton.setVisibility(View.GONE);
                        rejectVpPhotoButton.setVisibility(View.GONE);
                        buttonRemarkVpPhoto.setVisibility(View.GONE);
                        vpsListView.setVisibility(View.VISIBLE);
                        buttonShowHelpMainScreen.setVisibility(View.VISIBLE);
                        // TURNING ON RADAR SCAN
                        if (isArSwitchOn) {
                            radarScanImageView.setVisibility(View.VISIBLE);
                            radarScanImageView.startAnimation(rotationRadarScan);

                        }
                        if (!isArSwitchOn) {
                            cameraShutterButton.setVisibility(View.VISIBLE);
                            if (!mymIsRunningOnKitKat) {
                                videoCameraShutterButton.setVisibility(View.VISIBLE);
                            }
                            showVpCapturesMainScreenButton.setVisibility(View.VISIBLE);
                        }
                        linearLayoutCallWebAppMainScreen.setVisibility(View.VISIBLE);
                        if (pendingUploadTransfers > 0)
                            uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                        arSwitchLinearLayout.setVisibility(View.VISIBLE);
                        arSwitch.setVisibility(View.VISIBLE);
                        positionCertifiedButton.setVisibility(View.VISIBLE);
                        timeCertifiedButton.setVisibility(View.VISIBLE);
                        connectedToServerButton.setVisibility(View.VISIBLE);
                    }
                });
                if (isArSwitchOn) {
                    isHudOn = 1;
                    if (resultSpecialTrk) {
                        resultSpecialTrk = false;
                        vpIsDisambiguated = false;
                    }
                }
                vpChecked[vpTrackedInPose] = false;
                if (isArSwitchOn) {
                    setVpsChecked();
                    saveVpsChecked(false);
                }
                lastVpPhotoRejected = true;
                vpPhotoRejected = false;
                vpPhotoRequestInProgress = false;
                //Log.d(TAG, "takePhoto: vpPhotoRejected >>>>> calling setMarkerlessTrackingConfiguration");
                //Log.d(TAG, "takePhoto: vpPhotoRejected: vpPhotoRequestInProgress = " + vpPhotoRequestInProgress);
                if ((!waitingUntilMultipleImageTrackingIsSet) && (isArSwitchOn)) {
                    setMultipleImageTrackingConfiguration();
                }
            }
        }
    }


    private void saveVpsChecked(boolean correctiveSave) {
        // Saving vpChecked state.
        if (isArConfigLoaded || correctiveSave) {
            try {
                XmlSerializer xmlSerializer = Xml.newSerializer();
                StringWriter writer = new StringWriter();
                xmlSerializer.setOutput(writer);
                xmlSerializer.startDocument("UTF-8", true);
                xmlSerializer.text("\n");
                xmlSerializer.startTag("", "VpsChecked");
                xmlSerializer.text("\n");
                for (int i = 0; i < (qtyVps); i++) {
                    xmlSerializer.text("\t");
                    xmlSerializer.startTag("", "Vp");
                    xmlSerializer.text("\n");
                    xmlSerializer.text("\t");
                    xmlSerializer.text("\t");
                    xmlSerializer.startTag("", "VpNumber");
                    xmlSerializer.text(Short.toString(vpNumber[i]));
                    xmlSerializer.endTag("", "VpNumber");
                    xmlSerializer.text("\n");
                    xmlSerializer.text("\t");
                    xmlSerializer.text("\t");
                    xmlSerializer.startTag("", "Checked");
                    xmlSerializer.text(Boolean.toString(vpChecked[i]));
                    xmlSerializer.endTag("", "Checked");
                    xmlSerializer.text("\n");
                    xmlSerializer.text("\t");
                    xmlSerializer.text("\t");
                    xmlSerializer.startTag("", "PhotoTakenTimeMillis");
                    xmlSerializer.text(Long.toString(photoTakenTimeMillis[i]));
                    xmlSerializer.endTag("", "PhotoTakenTimeMillis");
                    xmlSerializer.text("\n");
                    xmlSerializer.text("\t");
                    xmlSerializer.endTag("", "Vp");
                    xmlSerializer.text("\n");
                }
                xmlSerializer.endTag("", "VpsChecked");
                xmlSerializer.endDocument();
                String vpsCheckedFileContents = writer.toString();
                File vpsCheckedFile = new File(getApplicationContext().getFilesDir(), Constants.vpsCheckedConfigFileName);
                FileUtils.writeStringToFile(vpsCheckedFile, vpsCheckedFileContents, UTF_8);
                ObjectMetadata myObjectMetadata = new ObjectMetadata();
                //create a map to store user metadata
                Map<String, String> userMetadata = new HashMap<String, String>();
                userMetadata.put("timestamp", MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference).toString());
                userMetadata.put("mymensoraccount", mymensorAccount);
                userMetadata.put("origmymacc", origMymAcc);
                userMetadata.put("deviceid", deviceId);
                myObjectMetadata.setUserMetadata(userMetadata);
                TransferObserver observer = MymUtils.storeRemoteFile(transferUtility, (vpsCheckedRemotePath + Constants.vpsCheckedConfigFileName), Constants.BUCKET_NAME, vpsCheckedFile, myObjectMetadata);
                observer.setTransferListener(new UploadListener());
                pendingUploadTransfers++;
                updatePendingUpload();

            } catch (Exception e) {
                Log.e(TAG, "SaveVpsChecked(): ERROR data saving to Remote Storage:" + e.toString());
            }
        }
    }


    private void setVpsChecked() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < (qtyVps); i++) {
                        //MetaioDebug.log("setVpsChecked: vpChecked["+i+"]="+vpChecked[i]);
                        if (vpsListView != null) {
                            vpsListView.setItemChecked(i, vpChecked[i]);
                        }

                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "setVpsChecked failed:" + e.toString());
        }
    }


    private void clearVpsChecked() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < (qtyVps); i++) {
                        //MetaioDebug.log("setVpsChecked: vpChecked["+i+"]="+vpChecked[i]);
                        if (vpsListView != null) {
                            vpsListView.setItemChecked(i, false);
                        }

                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "setVpsChecked failed:" + e.toString());
        }
    }

    public void startShowDescVPScreenTour() {
        final android.support.v7.app.AlertDialog.Builder alert = new android.support.v7.app.AlertDialog.Builder(this);
        alert.setIcon(R.drawable.logo_mymensor);
        alert.setTitle(getText(R.string.showdescvpscreentour));
        alert.setMessage(R.string.showdescvpscreentourdescription);

        alert.setPositiveButton(R.string.go, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                final Window window = getWindow();
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                showDescVpScreenTourStep1();
            }
        });

        alert.setNegativeButton(R.string.notnow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                final Window window = getWindow();
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                dialog.dismiss();
            }
        });

        alert.show();
    }

    public void showDescVpScreenTourStep1() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.textView1))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showdescvptourpagetitle))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showDescVpScreenTourStep2();
                        }
                    }
                })
                .show();
    }

    public void showDescVpScreenTourStep2() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonCallConfig))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showdescvptourbtncallcfg))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showDescVpScreenTourStep3();
                        }
                    }
                })
                .show();
    }

    public void showDescVpScreenTourStep3() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonAlphaToggle))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showdescvptourbtnalphatgl))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showDescVpScreenTourStep4();
                        }
                    }
                })
                .show();
    }


    public void showDescVpScreenTourStep4() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonShowVpCaptures))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showdescvptourbtnshowvpcaptures))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showDescVpScreenTourStep5();
                        }
                    }
                })
                .show();
    }

    public void showDescVpScreenTourStep5() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.linearLayoutVpArStatus))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showdescvptourarstatus))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            // nada!!!!
                        }
                    }
                })
                .show();
    }


    @Override
    public void onItemClick(AdapterView<?> adapter, View view, final int position, long id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgress.isShown()) {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                }

            }
        });
        vpLocationDescImageFileContents = null;
        lastVpSelectedByUser = position;
        if (!isArSwitchOn) {
            vpTrackedInPose = position;
            vpIsManuallySelected = true;
            vpNumber[vpTrackedInPose] = (short) position;
        } else {
            try {
                InputStream fis = MymUtils.getLocalFile("descvp" + (position) + ".png", getApplicationContext());
                if (!(fis == null)) {
                    vpLocationDescImageFileContents = BitmapFactory.decodeStream(fis);
                    fis.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "vpLocationDescImageFile failed:" + e.toString());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, "Showing vpLocationDescImageFile for VP=" + position + "(vpLocationDescImageFileContents==null)" + (vpLocationDescImageFileContents == null));
                    // VP Location Picture ImageView
                    if (!(vpLocationDescImageFileContents == null)) {
                        imageView.setImageBitmap(vpLocationDescImageFileContents);
                        imageView.setVisibility(View.VISIBLE);
                    }
                    isShowingVpPhoto = true;
                    //Log.d(TAG, "imageView.isShown()=" + imageView.isShown());
                    uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                    arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                    arSwitch.setVisibility(View.INVISIBLE);
                    showVpCapturesMainScreenButton.setVisibility(View.GONE);
                    linearLayoutCallWebAppMainScreen.setVisibility(View.GONE);
                    buttonShowHelpMainScreen.setVisibility(View.GONE);
                    positionCertifiedButton.setVisibility(View.INVISIBLE);
                    timeCertifiedButton.setVisibility(View.INVISIBLE);
                    connectedToServerButton.setVisibility(View.INVISIBLE);
                    // Setting the correct listview set position
                    vpsListView.setItemChecked(position, vpChecked[position]);
                    vpsListView.setVisibility(View.GONE);
                    // Turning off tracking
                    mImageDetectionFilterIndex = 0;
                    // TURNING OFF RADAR SCAN
                    if (radarScanImageView.isShown()) {
                        radarScanImageView.clearAnimation();
                        radarScanImageView.setVisibility(View.GONE);
                    }
                    // Show last captured date and what is the frequency
                    String lastTimeAcquiredAndNextOne = "";
                    String formattedNextDate = "";
                    if (photoTakenTimeMillis[position] > 0) {
                        Date lastDate = new Date(photoTakenTimeMillis[position]);
                        Date nextDate = new Date(vpNextCaptureMillis[position]);
                        SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd-MMM-yyyy HH:mm:ss zz"));
                        sdf.setTimeZone(TimeZone.getDefault());
                        String formattedLastDate = sdf.format(lastDate);
                        formattedNextDate = sdf.format(nextDate);
                        lastTimeAcquiredAndNextOne = vpLocationDesText[lastVpSelectedByUser] + "\n" +
                                getString(R.string.date_vp_touched_free_to_be_acquired) + ": " +
                                formattedNextDate;
                    } else {
                        lastTimeAcquiredAndNextOne = vpLocationDesText[lastVpSelectedByUser] + "\n" +
                                getString(R.string.date_vp_touched_free_to_be_acquired) + ": " +
                                getString(R.string.date_vp_touched_first_acquisition);
                    }
                    // VP Location Description TextView
                    vpLocationDesTextView.setText(lastTimeAcquiredAndNextOne);
                    vpLocationDesTextView.setVisibility(View.VISIBLE);
                    // VP Location # TextView
                    String vpId = Integer.toString(vpNumber[position]);
                    vpId = getString(R.string.vp_name) + vpId;
                    vpIdNumber.setText(vpId);
                    vpIdNumber.setVisibility(View.VISIBLE);
                    //Help
                    buttonShowHelpDescVPScreen.setVisibility(View.VISIBLE);
                    // Activate Location Description Buttons
                    buttonCallConfig.setVisibility(View.VISIBLE);
                    buttonAlphaToggle.setVisibility(View.VISIBLE);
                    if (imageView.getImageAlpha() == 128)
                        buttonAlphaToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                    if (imageView.getImageAlpha() == 255)
                        buttonAlphaToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                    showVpCapturesButton.setVisibility(View.VISIBLE);
                    // Layout showing VP configuration state

                    linearLayoutConfigCaptureVps.setVisibility(View.VISIBLE);
                    linearLayoutVpArStatus.setVisibility(View.VISIBLE);
                    if (vpArIsConfigured[position]) {
                        vpAcquiredStatus.setText(R.string.vpAcquiredStatus);
                    } else {
                        vpAcquiredStatus.setText(R.string.off);
                    }
                    if (vpIsAmbiguous[position]) {
                        linearLayoutMarkerId.setVisibility(View.VISIBLE);
                        idMarkerNumberTextView.setText(Integer.toString(vpSuperMarkerId[position]));
                        linearLayoutAmbiguousVp.setVisibility(View.VISIBLE);
                        buttonAmbiguousVpToggle.setVisibility(View.VISIBLE);
                        buttonAmbiguousVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                        if (vpIsSuperSingle[position]) {
                            linearLayoutSuperSingleVp.setVisibility(View.VISIBLE);
                            buttonSuperSingleVpToggle.setVisibility(View.VISIBLE);
                            buttonSuperSingleVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                        }
                    } else {
                        linearLayoutMarkerId.setVisibility(View.INVISIBLE);
                        linearLayoutAmbiguousVp.setVisibility(View.INVISIBLE);
                        buttonAmbiguousVpToggle.setVisibility(View.INVISIBLE);
                        linearLayoutSuperSingleVp.setVisibility(View.INVISIBLE);
                        buttonSuperSingleVpToggle.setVisibility(View.INVISIBLE);
                    }
                }
            });

        }

    }

    public void onButtonClick(View v) {
        if (v.getId() == R.id.buttonAcceptVpPhoto) {
            vpPhotoAccepted = true;
            //Log.d(TAG, "onButtonClick: vpPhotoAccepted:" + vpPhotoAccepted + " vpPhotoRejected:" + vpPhotoRejected);
        }
        if (v.getId() == R.id.buttonRejectVpPhoto) {
            vpPhotoRejected = true;
            //Log.d(TAG, "onButtonClick: vpPhotoAccepted:" + vpPhotoAccepted + " vpPhotoRejected:" + vpPhotoRejected);
        }
        if (v.getId() == R.id.buttonRemarkVpPhoto) {
            vpPhotoTobeRemarked = true;
            //Log.d(TAG, "onButtonClick: buttonRemarkVpPhoto");
        }
        if (v.getId() == R.id.buttonReplayVpVideo) {
            vpVideoTobeReplayed = true;
            //Log.d(TAG, "onButtonClick: buttonReplayVpVideo");
        }
        if (v.getId() == R.id.buttonShowPreviousVpCapture) {
            mediaSelected++;
            showVpCaptures(lastVpSelectedByUser);
        }
        if (v.getId() == R.id.buttonShowNextVpCapture) {
            mediaSelected--;
            showVpCaptures(lastVpSelectedByUser);
        }

        if (v.getId() == R.id.buttonPositionCertified) {
            //Log.d(TAG, "onButtonClick: buttonPositionCertified");
            if (v.findViewById(R.id.buttonPositionCertified).getBackground() == circularButtonGreen) {
                Snackbar mSnackBar = Snackbar.make(v, getText(R.string.mediacapwithposcert), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
            if (v.findViewById(R.id.buttonPositionCertified).getBackground() == circularButtonRed) {
                Snackbar mSnackBar = Snackbar.make(v, getText(R.string.mediacapwithposnotcert), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
            if (v.findViewById(R.id.buttonPositionCertified).getBackground() == circularButtonGray) {
                Snackbar mSnackBar = Snackbar.make(v, getText(R.string.mediacapwithposcertunk), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
        }

        if (v.getId() == R.id.buttonTimeCertified) {
            //Log.d(TAG, "onButtonClick: buttonTimeCertified");
            if (v.findViewById(R.id.buttonTimeCertified).getBackground() == circularButtonGreen) {
                Snackbar mSnackBar = Snackbar.make(v, getText(R.string.mediacapwithtimecert), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
            if (v.findViewById(R.id.buttonTimeCertified).getBackground() == circularButtonRed) {
                Snackbar mSnackBar = Snackbar.make(v, getText(R.string.mediacapwithtimenotcert), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
            if (v.findViewById(R.id.buttonTimeCertified).getBackground() == circularButtonGray) {
                Snackbar mSnackBar = Snackbar.make(v, getText(R.string.mediacapwithtimecertunk), Snackbar.LENGTH_LONG);
                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                mainTextView.setTextColor(Color.WHITE);
                mSnackBar.show();
            }
        }

    }


    private void deleteLocalShownCapture(int vpSelected, final View view) {
        //Log.d(TAG, "deleteLocalShownCapture: vpSelected=" + vpSelected + " lastVpSelectedByUser=" + lastVpSelectedByUser);
        final int vpToList = vpSelected;
        final String vpMediaFileName;
        final String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        String[] capsInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(vpToList + "_");
            }
        });
        int numOfEntries = 0;
        try {
            if (!(capsInDirectory == null)) {
                vpMediaFileName = capsInDirectory[mediaSelected];
                //Log.d(TAG, "deleteLocalShownCapture: vpMediaFileName=" + path + "/" + vpMediaFileName);
                File fileToBeDeleted = new File(path + "/" + vpMediaFileName);
                if (fileToBeDeleted.delete()) {
                    //Log.d(TAG, "deleteLocalShownCapture: vpMediaFileName=" + path + "/" + vpMediaFileName + " succesfully deleted from local storage.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String message = getString(R.string.local_file_deleted);
                            Snackbar mSnackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
                            TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                            mainTextView.setTextColor(Color.WHITE);
                            mSnackBar.show();
                        }
                    });
                }
                ;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while deleting captures:" + e.toString());
        }

    }


    private void getRemoteFileMetadata(final String filename) {

        new AsyncTask<Void, Void, ObjectMetadata>() {
            @Override
            protected void onPreExecute() {
                //Log.d(TAG, "getRemoteFileMetadata: onPreExecute");
            }

            @Override
            protected ObjectMetadata doInBackground(Void... params) {
                int retries = 4;
                boolean nthtry = false;
                try {
                    do {
                        final ObjectMetadata objMetadata = s3Amazon.getObjectMetadata(Constants.BUCKET_NAME, filename);
                        try {
                            if (objMetadata.getContentLength() > 5) nthtry = true;
                        } catch (Exception ex) {
                            nthtry = false;
                        }
                        if (nthtry) {
                            //Log.d(TAG, "Request to s3Amazon.getObjectMetadata succeeded");
                            return objMetadata;
                        } else {
                            //Log.d(TAG, "Request to s3Amazon.getObjectMetadata failed or object does not exist");
                        }
                    } while (retries-- > 0);
                } catch (AmazonServiceException ase) {
                    //Log.d(TAG, "AmazonServiceException=" + ase.toString());
                    return null;
                } catch (AmazonClientException ace) {
                    //Log.d(TAG, "AmazonClientException=" + ace.toString());
                    return null;
                } catch (Exception e) {
                    //Log.d(TAG, "Exception=" + e.toString());
                    return null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(final ObjectMetadata objectMetadata) {
                //Log.d(TAG, "getRemoteFileMetadata: onPostExecute");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (objectMetadata != null) {
                            Map<String, String> userMetadata = new HashMap<String, String>();
                            userMetadata = objectMetadata.getUserMetadata();
                            //Log.d(TAG, "userMetadata=" + userMetadata.toString());
                            //Log.d(TAG, "Location=LocCertified=" + userMetadata.get("loccertified") + " Time=TimeCertified=" + userMetadata.get("timecertified"));
                            if (userMetadata.get("loccertified").equalsIgnoreCase("1")) {
                                //IsPositionCertified
                                buttonPositionCertified.setVisibility(View.VISIBLE);
                                buttonPositionCertified.setBackground(circularButtonGreen);
                            } else {
                                buttonPositionCertified.setVisibility(View.VISIBLE);
                                buttonPositionCertified.setBackground(circularButtonRed);
                            }
                            if (userMetadata.get("timecertified").equalsIgnoreCase("1")) {
                                //IsTimeCertified
                                buttonTimeCertified.setVisibility(View.VISIBLE);
                                buttonTimeCertified.setBackground(circularButtonGreen);
                            } else {
                                buttonTimeCertified.setVisibility(View.VISIBLE);
                                buttonTimeCertified.setBackground(circularButtonRed);
                            }
                        } else {
                            buttonPositionCertified.setVisibility(View.VISIBLE);
                            buttonPositionCertified.setBackground(circularButtonGray);
                            buttonTimeCertified.setVisibility(View.VISIBLE);
                            buttonTimeCertified.setBackground(circularButtonGray);
                        }
                    }
                });
            }
        }.execute();
    }


    public void startShowVpCapturesTour() {
        final android.support.v7.app.AlertDialog.Builder alert = new android.support.v7.app.AlertDialog.Builder(this);
        alert.setIcon(R.drawable.logo_mymensor);
        alert.setTitle(getText(R.string.showvpcapturestour));
        alert.setMessage(R.string.showvpcapturestourdescription);

        alert.setPositiveButton(R.string.go, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                final Window window = getWindow();
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                showVpCapturesTourStep1();
            }
        });

        alert.setNegativeButton(R.string.notnow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                final Window window = getWindow();
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                dialog.dismiss();
            }
        });

        alert.show();
    }

    public void showVpCapturesTourStep1() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.textView1))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptourpagetitle))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showVpCapturesTourStep2();
                        }
                    }
                })
                .show();
    }

    public void showVpCapturesTourStep2() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonShowNextVpCapture))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptourfwdbutton))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showVpCapturesTourStep3();
                        }
                    }
                })
                .show();
    }

    public void showVpCapturesTourStep3() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.deleteLocalMediaButton))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptourdeletebutton))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showVpCapturesTourStep4();
                        }
                    }
                })
                .show();
    }

    public void showVpCapturesTourStep4() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.linearLayoutshareMediaButton2))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptoursharecontentbutton))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showVpCapturesTourStep5();
                        }
                    }
                })
                .show();
    }

    public void showVpCapturesTourStep5() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.linearLayoutshareMediaButton))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptoursharelinkbutton))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showVpCapturesTourStep6();
                        }
                    }
                })
                .show();
    }

    public void showVpCapturesTourStep6() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonDownloadPDFOnShowVpCaptures))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptourpdfbutton))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showVpCapturesTourStep7();
                        }
                    }
                })
                .show();
    }

    public void showVpCapturesTourStep7() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonPositionCertified))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptourposcert))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            showVpCapturesTourStep8();
                        }
                    }
                })
                .show();
    }

    public void showVpCapturesTourStep8() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.buttonTimeCertified))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptourtimecert))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            if (!mymIsRunningOnKitKat) {
                                if (linearLayoutConfigCaptureVpsHRZ.isShown()) {
                                    showVpCapturesTourStep9();
                                }
                            } else {
                                // nada!!!!
                            }

                        }
                    }
                })
                .show();
    }

    public void showVpCapturesTourStep9() {

        new MaterialTapTargetPrompt.Builder(this)
                .setTarget(findViewById(R.id.linearLayoutConfigCaptureVpsHRZ))
                .setPrimaryText("")
                .setSecondaryText(getText(R.string.showvpcaptourarsetup))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                            // nada!!!!
                        }
                    }
                })
                .show();
    }

    private void showVpCaptures(int vpSelected) {
        final Bitmap showVpPhotoImageFileContents;
        final Bitmap showVpVideoThumbImageFileContents;
        //Log.d(TAG, "showVpCaptures: vpSelected=" + vpSelected + " lastVpSelectedByUser=" + lastVpSelectedByUser);
        final int position = vpSelected;
        final int vpToList = vpSelected;
        final String vpMediaFileName;
        final String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        String[] capsInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(vpToList + "_p") || filename.startsWith(vpToList + "_v");
            }
        });
        int numOfEntries = 0;
        try {
            if (capsInDirectory.length > 0) {
                numOfEntries = capsInDirectory.length;
                if (mediaSelected == -1) mediaSelected = numOfEntries - 1;
                if (mediaSelected < 0) mediaSelected = 0;
                if (mediaSelected > (numOfEntries - 1)) mediaSelected = 0;
                //Log.d(TAG, "SHOWVPCAPTURES: vpSelected=" + vpSelected + " lastVpSelectedByUser=" + lastVpSelectedByUser + " mediaSelected=" + mediaSelected);
                vpMediaFileName = capsInDirectory[mediaSelected];
                //Log.d(TAG, "SHOWVPCAPTURES: vpMediaFileName=" + vpMediaFileName);
                showingMediaFileName = vpMediaFileName;
                //Log.d(TAG, "showVpCaptures: vpMediaFileName=" + vpMediaFileName);
                StringBuilder sb = new StringBuilder(vpMediaFileName);
                final String millisMoment = sb.substring(vpMediaFileName.length() - 17, vpMediaFileName.length() - 4);
                final String mediaType = sb.substring(vpMediaFileName.length() - 19, vpMediaFileName.length() - 18);
                showingMediaType = mediaType;
                if (mediaType.equalsIgnoreCase("p")) {
                    // When the item is a photo
                    final InputStream fiscaps = MymUtils.getLocalFile(vpMediaFileName, getApplicationContext());
                    showVpPhotoImageFileContents = BitmapFactory.decodeStream(fiscaps);
                    try {
                        fiscaps.close();
                    } catch (Exception fiscapse) {
                        Log.e(TAG, "showVpCaptures exception when closing file: " + fiscapse);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!(showVpPhotoImageFileContents == null)) {
                                linearLayoutButtonsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                try {
                                    ExifInterface tags = new ExifInterface(path + "/" + vpMediaFileName);
                                    //Log.d(TAG, "Location=Make=" + tags.getAttribute("Make") + " Time=GPSAltitudeRef=" + tags.getAttribute("GPSAltitudeRef"));
                                    if (tags.getAttribute("Make").equalsIgnoreCase("1")) {
                                        //IsPositionCertified
                                        buttonPositionCertified.setVisibility(View.VISIBLE);
                                        buttonPositionCertified.setBackground(circularButtonGreen);
                                    } else {
                                        buttonPositionCertified.setVisibility(View.VISIBLE);
                                        buttonPositionCertified.setBackground(circularButtonRed);
                                    }
                                    if (tags.getAttribute("GPSAltitudeRef").equalsIgnoreCase("1")) {
                                        //IsTimeCertified
                                        buttonTimeCertified.setVisibility(View.VISIBLE);
                                        buttonTimeCertified.setBackground(circularButtonGreen);
                                    } else {
                                        buttonTimeCertified.setVisibility(View.VISIBLE);
                                        buttonTimeCertified.setBackground(circularButtonRed);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Problem with Exif tags or drawable setting:" + e.toString());
                                }
                                videoView.setVisibility(View.GONE);
                                if (buttonStartVideoInVpCaptures.isShown()) {
                                    buttonStartVideoInVpCaptures.setVisibility(View.GONE);
                                }
                                imageView.setVisibility(View.VISIBLE);
                                imageView.setImageBitmap(showVpPhotoImageFileContents);
                                imageView.resetZoom();
                                if (imageView.getImageAlpha() == 128) imageView.setImageAlpha(255);
                                String lastTimeAcquired = "";
                                Date lastDate = new Date(Long.parseLong(millisMoment));
                                SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd-MMM-yyyy HH:mm:ss zz"));
                                sdf.setTimeZone(TimeZone.getDefault());
                                String formattedLastDate = sdf.format(lastDate);
                                lastTimeAcquired = getString(R.string.date_vp_capture_shown) + ": " + formattedLastDate;
                                if (isArConfigLoaded) {
                                    String vpId = Integer.toString(vpNumber[position]);
                                    vpId = getString(R.string.vp_name) + vpId;
                                    vpIdNumber.setText(vpId);
                                    vpLocationDesTextView.setText(vpLocationDesText[lastVpSelectedByUser] + "\n" + lastTimeAcquired);
                                } else {
                                    String vpId = Integer.toString(lastVpSelectedByUser);
                                    vpId = getString(R.string.vp_name) + vpId;
                                    vpIdNumber.setText(vpId);
                                    vpLocationDesTextView.setText("VP#" + lastVpSelectedByUser + "\n" + lastTimeAcquired);
                                }
                                vpIdNumber.setVisibility(View.VISIBLE);
                                vpLocationDesTextView.setVisibility(View.VISIBLE);
                                // Layout showing VP configuration state
                                if (isArConfigLoaded) {
                                    if (!mymIsRunningOnKitKat) {
                                        linearLayoutConfigCaptureVpsHRZ.setVisibility(View.VISIBLE);
                                        linearLayoutVpArStatusHRZ.setVisibility(View.VISIBLE);
                                        if (vpArIsConfigured[position]) {
                                            vpAcquiredStatusHRZ.setText(R.string.vpAcquiredStatus);
                                        } else {
                                            vpAcquiredStatusHRZ.setText(R.string.off);
                                        }
                                        if (vpIsAmbiguous[position]) {
                                            linearLayoutMarkerIdHRZ.setVisibility(View.VISIBLE);
                                            idMarkerNumberTextViewHRZ.setText(Integer.toString(vpSuperMarkerId[position]));
                                            buttonAmbiguousVpToggleHRZ.setVisibility(View.VISIBLE);
                                            buttonAmbiguousVpToggleHRZ.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                                            if (vpIsSuperSingle[position]) {
                                                buttonSuperSingleVpToggleHRZ.setVisibility(View.VISIBLE);
                                                buttonSuperSingleVpToggleHRZ.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                                            }
                                        } else {
                                            linearLayoutMarkerIdHRZ.setVisibility(View.INVISIBLE);
                                            buttonAmbiguousVpToggleHRZ.setVisibility(View.INVISIBLE);
                                            buttonSuperSingleVpToggleHRZ.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                } else {
                                    linearLayoutConfigCaptureVpsHRZ.setVisibility(View.GONE);
                                }
                                if (appStartState.equalsIgnoreCase("firstthisversion") || appStartState.equalsIgnoreCase("firstever")) {
                                    startShowVpCapturesTour();
                                }
                            }
                        }
                    });
                } else {
                    if (mediaType.equalsIgnoreCase("v")) {
                        // when the item is a video.
                        try {
                            getRemoteFileMetadata(Constants.capturesFolder + "/" + mymensorAccount + "/" + vpMediaFileName);
                        } catch (Exception e) {
                            Log.e(TAG, "Problem Remote files Metadata:" + e.toString());
                        }
                        String lastTimeAcquired = "";
                        Date lastDate = new Date(Long.parseLong(millisMoment));
                        SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd-MMM-yyyy HH:mm:ss zz"));
                        sdf.setTimeZone(TimeZone.getDefault());
                        String formattedLastDate = sdf.format(lastDate);
                        lastTimeAcquired = getString(R.string.date_vp_capture_shown) + ": " + formattedLastDate;
                        String tempTextView = "";
                        if (isArConfigLoaded) {
                            tempTextView = vpLocationDesText[lastVpSelectedByUser] + "\n" + lastTimeAcquired;
                        } else {
                            tempTextView = "VP#" + lastVpSelectedByUser + "\n" + lastTimeAcquired;
                        }
                        final String desTextView = tempTextView;
                        final Uri videoFileTMP = Uri.fromFile(new File(getApplicationContext().getFilesDir(), vpMediaFileName));
                        final InputStream fisthumbs = MymUtils.getLocalFile(vpSelected + "_t_" + millisMoment + ".jpg", getApplicationContext());
                        showVpVideoThumbImageFileContents = BitmapFactory.decodeStream(fisthumbs);
                        try {
                            fisthumbs.close();
                        } catch (Exception fisthumbse) {
                            Log.e(TAG, "showVpCaptures exception when closing file: " + fisthumbse);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                videoView.setVisibility(View.GONE);
                                imageView.setVisibility(View.VISIBLE);
                                imageView.setImageBitmap(showVpVideoThumbImageFileContents);
                                imageView.resetZoom();
                                if (imageView.getImageAlpha() == 128) imageView.setImageAlpha(255);

                                buttonStartVideoInVpCaptures.setVisibility(View.VISIBLE);
                                linearLayoutButtonsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                buttonPositionCertified.setVisibility(View.GONE);
                                buttonTimeCertified.setVisibility(View.GONE);
                                if (radarScanImageView.isShown()) {
                                    radarScanImageView.clearAnimation();
                                    radarScanImageView.setVisibility(View.GONE);
                                }
                                vpLocationDesTextView.setText(desTextView);
                                vpLocationDesTextView.setVisibility(View.VISIBLE);
                                // Layout showing VP configuration state
                                if (isArConfigLoaded) {
                                    if (!mymIsRunningOnKitKat) {
                                        linearLayoutConfigCaptureVpsHRZ.setVisibility(View.VISIBLE);
                                        linearLayoutVpArStatusHRZ.setVisibility(View.VISIBLE);
                                        if (vpArIsConfigured[position]) {
                                            vpAcquiredStatusHRZ.setText(R.string.vpAcquiredStatus);
                                        } else {
                                            vpAcquiredStatusHRZ.setText(R.string.off);
                                        }
                                        if (vpIsAmbiguous[position]) {
                                            linearLayoutMarkerIdHRZ.setVisibility(View.VISIBLE);
                                            idMarkerNumberTextViewHRZ.setText(Integer.toString(vpSuperMarkerId[position]));
                                            buttonAmbiguousVpToggleHRZ.setVisibility(View.VISIBLE);
                                            buttonAmbiguousVpToggleHRZ.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                                            if (vpIsSuperSingle[position]) {
                                                buttonSuperSingleVpToggleHRZ.setVisibility(View.VISIBLE);
                                                buttonSuperSingleVpToggleHRZ.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                                            }
                                        } else {
                                            linearLayoutMarkerIdHRZ.setVisibility(View.INVISIBLE);
                                            buttonAmbiguousVpToggleHRZ.setVisibility(View.INVISIBLE);
                                            buttonSuperSingleVpToggleHRZ.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                } else {
                                    linearLayoutConfigCaptureVpsHRZ.setVisibility(View.GONE);
                                }
                                buttonStartVideoInVpCaptures.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        buttonStartVideoInVpCaptures.setVisibility(View.GONE);
                                        videoView.setVisibility(View.VISIBLE);
                                        videoView.setVideoURI(videoFileTMP);
                                        videoView.setMediaController(mMediaController);
                                        videoView.start();
                                        videoView.setZOrderOnTop(true);
                                        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                            @Override
                                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                                String message = getString(R.string.error_while_playing_video);
                                                Snackbar mSnackBar = Snackbar.make(videoView, message, Snackbar.LENGTH_LONG);
                                                TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                                                mainTextView.setTextColor(Color.WHITE);
                                                mSnackBar.show();
                                                returnToInitialScreen();
                                                return false;
                                            }
                                        });
                                        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                            @Override
                                            public void onCompletion(MediaPlayer mp) {
                                                videoView.setZOrderOnTop(false);
                                                videoView.setVisibility(View.GONE);
                                                //Log.d(TAG, "onCompletion Listener VIDEO showVpCaptures");
                                                buttonStartVideoInVpCaptures.setVisibility(View.VISIBLE);
                                                DisplayMetrics metrics = new DisplayMetrics();
                                                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                                                //Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                                                //Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);
                                                final Window window = getWindow();
                                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                                    //Log.d(TAG, "showVpCaptures - Calling FULLSCREEN");
                                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                                }
                                            }
                                        });
                                    }
                                });
                                if (appStartState.equalsIgnoreCase("firstthisversion") || appStartState.equalsIgnoreCase("firstever")) {
                                    startShowVpCapturesTour();
                                }
                            }
                        });
                    }
                }
            } else {
                //when no item has been acquired to the vp.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = getString(R.string.no_photo_captured_in_this_vp);
                        Snackbar mSnackBar = Snackbar.make(imageView, message, Snackbar.LENGTH_LONG);
                        TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                        mainTextView.setTextColor(Color.WHITE);
                        mSnackBar.show();
                        returnToInitialScreen();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while retrieving captures:" + e.toString());
        }
    }


    private boolean loadConfigurationFile() {
        vpTrackedInPose = 1;
        short[] vpMarkerlessMarkerWidth = new short[qtyVps];
        short[] vpMarkerlessMarkerHeigth = new short[qtyVps];

        //Log.d(TAG, "loadConfigurationFile(): started");

        for (int i = 0; i < (qtyVps); i++) {
            vpFrequencyUnit[i] = "";
            vpFrequencyValue[i] = 0;
        }

        // Load Initialization Values from file
        short vpListOrder = -1;

        try {
            // Getting a file path for vps configuration XML file
            //Log.d(TAG, "Vps Config Local name = " + Constants.vpsConfigFileName);
            File vpsFile = new File(getApplicationContext().getFilesDir(), Constants.vpsConfigFileName);
            InputStream fis = MymUtils.getLocalFile(Constants.vpsConfigFileName, getApplicationContext());
            try {
                XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser myparser = xmlFactoryObject.newPullParser();
                myparser.setInput(fis, null);
                int eventType = myparser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                        //
                    } else if (eventType == XmlPullParser.START_TAG) {
                        if (myparser.getName().equalsIgnoreCase("Parameters")) {
                            //
                        } else if (myparser.getName().equalsIgnoreCase("AssetId")) {
                            eventType = myparser.next();
                            assetId = Short.parseShort(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("FrequencyUnit")) {
                            eventType = myparser.next();
                            frequencyUnit = myparser.getText();
                        } else if (myparser.getName().equalsIgnoreCase("FrequencyValue")) {
                            eventType = myparser.next();
                            frequencyValue = Integer.parseInt(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("QtyVps")) {
                            eventType = myparser.next();
                            qtyVps = Short.parseShort(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("TolerancePosition")) {
                            eventType = myparser.next();
                            tolerancePosition = Float.parseFloat(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("ToleranceRotation")) {
                            eventType = myparser.next();
                            toleranceRotation = Float.parseFloat(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("Vp")) {
                            vpListOrder++;
                            //MetaioDebug.log("VpListOrder: "+vpListOrder);
                        } else if (myparser.getName().equalsIgnoreCase("VpNumber")) {
                            eventType = myparser.next();
                            vpNumber[vpListOrder] = Short.parseShort(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpArIsConfigured")) {
                            eventType = myparser.next();
                            vpArIsConfigured[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpIsVideo")) {
                            eventType = myparser.next();
                            vpIsVideo[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpXCameraDistance")) {
                            eventType = myparser.next();
                            vpXCameraDistance[vpListOrder] = Integer.parseInt(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpYCameraDistance")) {
                            eventType = myparser.next();
                            vpYCameraDistance[vpListOrder] = Integer.parseInt(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpZCameraDistance")) {
                            eventType = myparser.next();
                            vpZCameraDistance[vpListOrder] = Integer.parseInt(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpXCameraRotation")) {
                            eventType = myparser.next();
                            vpXCameraRotation[vpListOrder] = Integer.parseInt(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpYCameraRotation")) {
                            eventType = myparser.next();
                            vpYCameraRotation[vpListOrder] = Integer.parseInt(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpZCameraRotation")) {
                            eventType = myparser.next();
                            vpZCameraRotation[vpListOrder] = Integer.parseInt(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpLocDescription")) {
                            eventType = myparser.next();
                            vpLocationDesText[vpListOrder] = myparser.getText();
                        } else if (myparser.getName().equalsIgnoreCase("VpMarkerlessMarkerWidth")) {
                            eventType = myparser.next();
                            vpMarkerlessMarkerWidth[vpListOrder] = Short.parseShort(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpMarkerlessMarkerHeigth")) {
                            eventType = myparser.next();
                            vpMarkerlessMarkerHeigth[vpListOrder] = Short.parseShort(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpIsAmbiguous")) {
                            eventType = myparser.next();
                            vpIsAmbiguous[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpFlashTorchIsOn")) {
                            eventType = myparser.next();
                            vpFlashTorchIsOn[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpIsSuperSingle")) {
                            eventType = myparser.next();
                            vpIsSuperSingle[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpSuperMarkerId")) {
                            eventType = myparser.next();
                            vpSuperMarkerId[vpListOrder] = Integer.parseInt(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("VpFrequencyUnit")) {
                            eventType = myparser.next();
                            vpFrequencyUnit[vpListOrder] = myparser.getText();
                        } else if (myparser.getName().equalsIgnoreCase("VpFrequencyValue")) {
                            eventType = myparser.next();
                            vpFrequencyValue[vpListOrder] = Long.parseLong(myparser.getText());
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        //MetaioDebug.log("End tag "+myparser.getName());
                    } else if (eventType == XmlPullParser.TEXT) {
                        //MetaioDebug.log("Text "+myparser.getText());
                    }
                    eventType = myparser.next();
                }
                try {
                    fis.close();
                } catch (Exception fise) {
                    Log.e(TAG, "showVpCaptures exception when closing file: " + fise);
                }
            } finally {
                //Log.d(TAG, "Vps Config Local file = " + vpsFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Vps data loading failed:" + e.toString());
            return false;
        }


        for (int i = 0; i < (qtyVps); i++) {
            ////Log.d(TAG, "vpNumber[" + i + "]=" + vpNumber[i]);
            vpChecked[i] = false;
            if (frequencyUnit == null) frequencyUnit = "millis";
            if (!(vpFrequencyUnit[i] == null)) {
                if (vpFrequencyUnit[i].equalsIgnoreCase("")) vpFrequencyUnit[i] = frequencyUnit;
            } else {
                vpFrequencyUnit[i] = frequencyUnit;
            }

            if (vpFrequencyUnit[i] == null) {
                vpFrequencyUnit[i] = frequencyUnit;
            }

            if (vpFrequencyValue[i] == 0) {
                vpFrequencyValue[i] = frequencyValue;
            }

            if (vpIsAmbiguous[i] && (vpSuperMarkerId[i] == 0)) {
                vpIsAmbiguous[i] = false;
                vpIsSuperSingle[i] = false;
            }
        }

        //Log.d(TAG, "loadConfigurationFile(): ended");
        return true;
    }

    private void verifyVpsChecked() {
        boolean change = false;
        long presentMillis = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
        long presentHour = presentMillis / (1000 * 60 * 60);
        long presentDay = presentMillis / (1000 * 60 * 60 * 24);
        long presentWeek = presentDay / 7;
        long presentMonth = presentWeek / (52 / 12);

        for (int i = 0; i < (qtyVps); i++) {
            //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
            if (vpChecked[i]) {
                if (vpFrequencyUnit[i].equalsIgnoreCase("millis")) {
                    //MetaioDebug.log("Present Millis since Epoch: "+presentMillis);
                    if ((presentMillis - (photoTakenTimeMillis[i])) > (vpFrequencyValue[i])) {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Millis since Epoch: "+(photoTakenTimeMillis[i]));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (vpFrequencyUnit[i].equalsIgnoreCase("hour")) {
                    //MetaioDebug.log("Present Hour since Epoch: "+presentHour);
                    if ((presentHour - (photoTakenTimeMillis[i] / (1000 * 60 * 60))) > (vpFrequencyValue[i])) {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Hour since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60)));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (vpFrequencyUnit[i].equalsIgnoreCase("day")) {
                    //MetaioDebug.log("Present Day since Epoch: "+presentDay);
                    if ((presentDay - (photoTakenTimeMillis[i] / (1000 * 60 * 60 * 24))) > (vpFrequencyValue[i])) {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Day since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24)));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (vpFrequencyUnit[i].equalsIgnoreCase("week")) {
                    //MetaioDebug.log("Present Week since Epoch: "+presentWeek);
                    if ((presentWeek - (photoTakenTimeMillis[i] / (1000 * 60 * 60 * 24 * 7))) > (vpFrequencyValue[i])) {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Week since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24*7)));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (vpFrequencyUnit[i].equalsIgnoreCase("month")) {
                    //MetaioDebug.log("Present Month since Epoch: "+presentMonth);
                    if ((presentMonth - (photoTakenTimeMillis[i] / (1000 * 60 * 60 * 24 * 7 * (52 / 12)))) > (vpFrequencyValue[i])) {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Month since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24*7*(52/12))));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (change) setVpsChecked();
            }

            if (vpFrequencyUnit[i].equalsIgnoreCase("millis")) {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + vpFrequencyValue[i];
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("hour")) {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i] * 60 * 60 * 1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("day")) {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i] * 24 * 60 * 60 * 1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("week")) {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i] * 7 * 24 * 60 * 60 * 1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("month")) {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i] * (52 / 12) * 7 * 24 * 60 * 60 * 1000);
            }
        }

        //Log.e(TAG,"verifyVpsChecked() Error: "+e);


    }

    private boolean loadVpsChecked() {
        //Log.d(TAG, "loadVpsChecked(): started ");
        int vpListOrder = -1;
        try {
            InputStream fis = MymUtils.getLocalFile(Constants.vpsCheckedConfigFileName, getApplicationContext());
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser myparser = xmlFactoryObject.newPullParser();
            myparser.setInput(fis, null);
            int eventType = myparser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    //
                } else if (eventType == XmlPullParser.START_TAG) {

                    if (myparser.getName().equalsIgnoreCase("Vp")) {
                        vpListOrder++;
                    } else if (myparser.getName().equalsIgnoreCase("VpNumber")) {
                        eventType = myparser.next();
                        vpNumber[vpListOrder] = Short.parseShort(myparser.getText());
                        ////Log.d(TAG, "vpNumber[" + vpListOrder + "]=" + vpNumber[vpListOrder]);
                    } else if (myparser.getName().equalsIgnoreCase("Checked")) {
                        eventType = myparser.next();
                        vpChecked[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        ////Log.d(TAG, "vpChecked[" + vpListOrder + "]=" + vpChecked[vpListOrder]);
                    } else if (myparser.getName().equalsIgnoreCase("PhotoTakenTimeMillis")) {
                        eventType = myparser.next();
                        photoTakenTimeMillis[vpListOrder] = Long.parseLong(myparser.getText());
                        ////Log.d(TAG, "photoTakenTimeMillis[" + vpListOrder + "]=" + photoTakenTimeMillis[vpListOrder]);
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    //
                } else if (eventType == XmlPullParser.TEXT) {
                    //
                }
                eventType = myparser.next();
            }
            try {
                fis.close();
            } catch (Exception fise) {
                Log.e(TAG, "showVpCaptures exception when closing file: " + fise);
            }


            //Log.d(TAG, "loadVpsChecked(): ended ");
        } catch (Exception e) {
            Log.e(TAG, "Checked Vps data loading failed:" + e.getMessage());
            //Log.d(TAG, "Creating a new file.");
            for (short i = 0; i < (qtyVps); i++) {
                vpNumber[i] = i;
                vpChecked[i] = false;
                photoTakenTimeMillis[i] = 0;
            }
            saveVpsChecked(true);
            isVpsCheckedInformationLost = true;
            return true;
        }
        return true;
    }

    private void callTimeServerInBackground() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected Void doInBackground(Void... params) {
                isTimeCertified = false;
                long now = 0;
                Long loopStart = System.currentTimeMillis();
                //Log.d(TAG, "callTimeServerInBackground: Calling SNTP");
                SntpClient sntpClient = new SntpClient();
                do {
                    if (isCancelled()) {
                        Log.i("AsyncTask", "callTimeServerInBackground: cancelled");
                        break;
                    }
                    if (sntpClient.requestTime("pool.ntp.org", 10000)) {
                        sntpTime = sntpClient.getNtpTime();
                        sntpTimeReference = sntpClient.getNtpTimeReference();
                        now = sntpTime + SystemClock.elapsedRealtime() - sntpTimeReference;
                        Log.i("SNTP", "SNTP Present Time =" + now);
                        Log.i("SNTP", "System Present Time =" + System.currentTimeMillis());
                        isTimeCertified = true;
                    }
                    //if (now != 0)
                    //Log.d(TAG, "callTimeServerInBackground: ntp:now=" + now);

                } while ((now == 0) && ((System.currentTimeMillis() - loopStart) < 10000));
                //Log.d(TAG, "callTimeServerInBackground: ending the loop querying pool.ntp.org for 10 seconds max:" + (System.currentTimeMillis() - loopStart) + " millis:" + now);

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (isTimeCertified) {
                    //Log.d(TAG, "callTimeServerInBackground: System.currentTimeMillis() before setTime=" + System.currentTimeMillis());
                    //Log.d(TAG, "callTimeServerInBackground: System.currentTimeMillis() AFTER setTime=" + MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timeCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                            Snackbar mSnackBar = Snackbar.make(timeCertifiedButton.getRootView(), getText(R.string.usingcerttimeistrue), Snackbar.LENGTH_LONG);
                            TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                            mainTextView.setTextColor(Color.WHITE);
                            mSnackBar.show();
                        }
                    });
                } else {
                    sntpTime = 0;
                    sntpTimeReference = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timeCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                            Snackbar mSnackBar = Snackbar.make(timeCertifiedButton.getRootView(), getText(R.string.usingcerttimeisfalse), Snackbar.LENGTH_LONG);
                            TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(android.support.design.R.id.snackbar_text);
                            mainTextView.setTextColor(Color.WHITE);
                            mSnackBar.show();
                        }
                    });
                }
            }
        }.execute();
    }

    private class UploadListener implements TransferListener {

        // Simply updates the UI when notified.
        @Override
        public void onError(int id, Exception e) {
            Log.e(TAG, "Observer: Error during upload: " + id, e);
            updatePendingUpload();
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            //Log.d(TAG, String.format("Observer: onProgressChanged: %d, total: %d, current: %d",id, bytesTotal, bytesCurrent));
            updatePendingUpload();
        }

        @Override
        public void onStateChanged(int id, TransferState newState) {
            //Log.d(TAG, "Observer: onStateChanged: " + id + ", " + newState);
            if (newState.equals(TransferState.COMPLETED)) {
                pendingUploadTransfers--;
                if (pendingUploadTransfers < 0) pendingUploadTransfers = 0;
            }
            updatePendingUpload();
        }
    }

    private void updatePendingUpload() {

        if (pendingUploadTransfers == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (uploadPendingText.isShown()) {
                        uploadPendingText.setText(Integer.toString(pendingUploadTransfers));
                        uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                    }
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                    uploadPendingText.setText(Integer.toString(pendingUploadTransfers));
                }
            });
        }
    }


}
