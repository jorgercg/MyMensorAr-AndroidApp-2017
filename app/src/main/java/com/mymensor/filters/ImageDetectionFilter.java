package com.mymensor.filters;

import java.io.IOException;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgcodecs.Imgcodecs;

import android.content.Context;

public final class ImageDetectionFilter implements ARFilter {

    // The address of the native object.
    private long mSelfAddr;

    // An adaptor that provides the camera's projection matrix.
    private final MatOfDouble mCameraCalibration;

    static {
        // Load the native library if it is not already loaded.
        System.loadLibrary("MyMensor");
    }

    public ImageDetectionFilter(final Context context,
                                final int referenceImageResourceID,
                                final MatOfDouble cameraCalibration,
                                final double realSize)
            throws IOException {
        final Mat referenceImageBGR = Utils.loadResource(context,
                referenceImageResourceID,
                Imgcodecs.CV_LOAD_IMAGE_COLOR);
        mSelfAddr = newSelf(referenceImageBGR.getNativeObjAddr(), realSize);
        mCameraCalibration = cameraCalibration;
    }

    @Override
    public void dispose() {
        deleteSelf(mSelfAddr);
        mSelfAddr = 0;
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
    }

    @Override
    public float[] getPose() {
        return getPose(mSelfAddr);
    }

    @Override
    public void apply(final Mat src) {
        final Mat projection = mCameraCalibration;
        apply(mSelfAddr, src.getNativeObjAddr(), projection.getNativeObjAddr());
    }

    private static native long newSelf(long referenceImageBGRAddr, double realSize);
    private static native void deleteSelf(long selfAddr);
    private static native float[] getPose(long selfAddr);
    private static native void apply(long selfAddr, long srcAddr, long projectionAddr);
}