/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package gg.essential.image.imagescaling;


import gg.essential.gui.screenshot.downsampling.PixelBuffer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Nobel-Joergensen
 */
public abstract class AdvancedResizeOp {
    private final DimensionConstrain dimensionConstrain;
    private List<ProgressListener> listeners = new ArrayList<ProgressListener>();
    private UnsharpenMask unsharpenMask = UnsharpenMask.None;
    public AdvancedResizeOp(DimensionConstrain dimensionConstrain) {
        this.dimensionConstrain = dimensionConstrain;
    }

    protected void fireProgressChanged(float fraction) {
        for (ProgressListener progressListener : listeners) {
            progressListener.notifyProgress(fraction);
        }
    }

    public final PixelBuffer filter(PixelBuffer src) throws InterruptedException {
        Dimension dstDimension = dimensionConstrain.getDimension(new Dimension(src.getWidth(), src.getHeight()));
        int dstWidth = dstDimension.width;
        int dstHeight = dstDimension.height;


        return doFilter(src, dstWidth, dstHeight);
    }

    protected abstract PixelBuffer doFilter(PixelBuffer src, int dstWidth, int dstHeight) throws InterruptedException;

    public static enum UnsharpenMask {
        None(0),
        Soft(0.15f),
        Normal(0.3f),
        VerySharp(0.45f),
        Oversharpened(0.60f);
        private final float factor;

        UnsharpenMask(float factor) {
            this.factor = factor;
        }
    }
}
