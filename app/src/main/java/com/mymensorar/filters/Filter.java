package com.mymensorar.filters;

import org.opencv.core.Mat;

public interface Filter {
    public abstract void dispose();
    public abstract void apply(final Mat src, final int isHudOn, final int isSingleImage);
}
