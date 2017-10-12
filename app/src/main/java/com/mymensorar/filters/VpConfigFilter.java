package com.mymensorar.filters;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

import java.io.IOException;

public final class VpConfigFilter implements ARFilter {

    // The address of the native object.
    private long mSelfAddr;

    // An adaptor that provides the camera's projection matrix.
    private final MatOfDouble mCameraCalibration;

    static {
        // Load the native library if it is not already loaded.
        System.loadLibrary("MyMensor");
    }

    public VpConfigFilter(final Context context,
                          final Mat referenceImageBGR,
                          final MatOfDouble cameraCalibration,
                          final double realSize)
            throws IOException {
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
    public void apply(final Mat src, final int IsHudOn, final int isSingleImage, final int rotx, final int roty, final int rotz, final int translx, final int transly, final int translz) {
        final Mat projection = mCameraCalibration;
        apply(mSelfAddr, src.getNativeObjAddr(), projection.getNativeObjAddr());
    }

    private static native long newSelf(long referenceImageBGRAddr, double realSize);

    private static native void deleteSelf(long selfAddr);

    private static native float[] getPose(long selfAddr);

    private static native void apply(long selfAddr, long srcAddr, long projectionAddr);
}