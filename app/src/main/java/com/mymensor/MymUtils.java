package com.mymensor;


import android.content.Context;
import android.content.res.AssetManager;
import android.os.SystemClock;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import org.opencv.core.CvType;
import org.opencv.core.MatOfDouble;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MymUtils {

    private static final String TAG = "MymUtils";

    public static boolean isNewFileAvailable(   AmazonS3Client s3,
                                                String localFileName,
                                                String remoteFileName,
                                                String bucketName,
                                                Context context) {
        File localFile = new File(context.getFilesDir(), localFileName );
        if (!localFile.exists()) { return true; }
        try {
            ObjectMetadata metadata = s3.getObjectMetadata(bucketName,remoteFileName);
            long remoteLastModified = metadata.getLastModified().getTime();
            if (localFile.lastModified()<remoteLastModified) {
                return true;
            }
            else {
                return false;
            }
        } catch (AmazonClientException ace){
            Log.e("MymUtils","isNewFileAvailable: exception: "+ace.toString());
            return false;
        }
    }

    public static TransferObserver storeRemoteFile( TransferUtility transferUtility,
                                                    String fileName,
                                                    String bucketName,
                                                    File file,
                                                    ObjectMetadata objectMetadata){

        TransferObserver observer = transferUtility.upload(
                bucketName,		/* The bucket to upload to */
                fileName,		/* The key for the uploaded object */
                file,				/* The file where the data to upload exists */
                objectMetadata			/* The ObjectMetadata associated with the object*/
        );

        return observer;
    }

    public static TransferObserver getRemoteFile( TransferUtility transferUtility,
                                                  String fileName,
                                                  String bucketName,
                                                  File file) {

        TransferObserver observer = transferUtility.download(bucketName, fileName, file);
        return observer;
    }

    public static InputStream getLocalFile( String fileName, Context context ) {
        try {
            return context.openFileInput(fileName);
        }
        catch ( FileNotFoundException exception ) {
            return null;
        }
    }

    public static Long timeNow (Boolean clockSetSuccess, Long sntpTime, Long sntpTimeReference){

        if (clockSetSuccess){
            Long now;
            now = sntpTime + SystemClock.elapsedRealtime() - sntpTimeReference;
            return now;
        }
        else
        {
            return System.currentTimeMillis();
        }

    }

    public static void extractAllAssets(Context context){

        AssetManager assetManager = context.getAssets();
        String[] assetsList =  null;
        try {
            assetsList = assetManager.list(".");
        } catch(Exception e1) {
            Log.e(TAG, "extractAllAssets: Failed to list assets: " + e1.toString());
        }
        InputStream in = null;
        OutputStream out = null;
        if (assetsList!=null){
            for (String asset : assetsList){
                try {
                    in = assetManager.open(asset);
                    File outFile = new File(context.getFilesDir(), asset);
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                } catch(IOException e) {
                    Log.e(TAG, "extractAllAssets: Failed to copy asset file: " + asset, e);
                }
                finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                }
            }

        }

    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }


    public static MatOfDouble getCameraMatrix(int mWidth, int mHeight) {

        MatOfDouble mCameraMatrix = new MatOfDouble();
        mCameraMatrix.create(3, 3, CvType.CV_64FC1);

        final float fovAspectRatio = Constants.mFOVX/Constants.mFOVY;
        final double diagonalPx = Math.sqrt((Math.pow(mWidth, 2.0)+Math.pow(mWidth / fovAspectRatio, 2.0)));
        final double focalLengthPx = 0.5*diagonalPx/Math.sqrt(Math.pow(Math.tan(0.5 * Constants.mFOVX * Math.PI / 180f), 2.0) +
                        Math.pow(Math.tan(0.5 * Constants.mFOVY * Math.PI / 180f), 2.0));

        mCameraMatrix.put(0, 0, focalLengthPx);
        mCameraMatrix.put(0, 1, 0.0);
        mCameraMatrix.put(0, 2, 0.5 * mWidth);
        mCameraMatrix.put(1, 0, 0.0);
        mCameraMatrix.put(1, 1, focalLengthPx);
        mCameraMatrix.put(1, 2, 0.5 * mHeight);
        mCameraMatrix.put(2, 0, 0.0);
        mCameraMatrix.put(2, 1, 0.0);
        mCameraMatrix.put(2, 2, 1.0);

        return mCameraMatrix;
    }



    public static class IsRemoteServerAvailable {


    }


}