package com.mymensorar.filters;

import org.opencv.core.Mat;

public class NoneFilter implements Filter {

    @Override
    public void dispose() {
        // Do nothing at all.
    }

    @Override
    public void apply(final Mat src, final int IsHudOn, final int isSingleImage, final int rotx, final int roty, final int rotz, final int translx, final int transly, final int translz) {
        // Do nothing.
    }
}
