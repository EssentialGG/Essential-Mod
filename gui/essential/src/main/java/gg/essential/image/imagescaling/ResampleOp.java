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

import gg.essential.gui.screenshot.downsampling.BufferBackedImage;
import gg.essential.gui.screenshot.downsampling.PixelBuffer;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Based on work from Java Image Util ( http://schmidt.devlib.org/jiu/ )
 * <p>
 * Note that the filter method is not thread safe
 *
 * @author Morten Nobel-Joergensen
 * @author Heinz Doerr
 */
public class ResampleOp extends AdvancedResizeOp {
    private static final ExecutorService service = new ThreadPoolExecutor(100, 100,
        0L, TimeUnit.MILLISECONDS,
        new PriorityBlockingQueue<>(100, Comparator.comparingInt(o -> ((ResampleTask) o).targetWidth)));

    private static final Executor backgroundService = Runnable::run;

    private static Executor getService() {
        if (isBackgroundTask.get()) {
            return backgroundService;
        } else {
            return service;
        }
    }

    public static final ThreadLocal<Boolean> isBackgroundTask = ThreadLocal.withInitial(() -> false);

    private final int MAX_CHANNEL_VALUE = 255;
    private int nrChannels;
    private int srcWidth;
    private int srcHeight;
    private int dstWidth;
    private int dstHeight;
    private SubSamplingData horizontalSubsamplingData;
    private SubSamplingData verticalSubsamplingData;
    private int processedItems;
    private float totalItems;
    private int numberOfThreads = Runtime.getRuntime().availableProcessors();
    private AtomicInteger multipleInvocationLock = new AtomicInteger();
    private ResampleFilter filter = ResampleFilters.getLanczos3Filter();

    public ResampleOp(int destWidth, int destHeight) {
        this(DimensionConstrain.createAbsolutionDimension(destWidth, destHeight));
    }


    public ResampleOp(DimensionConstrain dimensionConstrain) {
        super(dimensionConstrain);
    }

    static SubSamplingData createSubSampling(ResampleFilter filter, int srcSize, int dstSize) {
        float scale = (float) dstSize / (float) srcSize;
        int[] arrN = new int[dstSize];
        int numContributors;
        float[] arrWeight;
        int[] arrPixel;

        final float fwidth = filter.getSamplingRadius();

        float centerOffset = 0.5f / scale;

        if (scale < 1.0f) {
            final float width = fwidth / scale;
            numContributors = (int) (width * 2.0f + 2); // Heinz: added 1 to be save with the ceilling
            arrWeight = new float[dstSize * numContributors];
            arrPixel = new int[dstSize * numContributors];

            final float fNormFac = (float) (1f / (Math.ceil(width) / fwidth));
            //
            for (int i = 0; i < dstSize; i++) {
                final int subindex = i * numContributors;
                float center = i / scale + centerOffset;
                int left = (int) Math.floor(center - width);
                int right = (int) Math.ceil(center + width);
                for (int j = left; j <= right; j++) {
                    float weight;
                    weight = filter.apply((center - j) * fNormFac);

                    if (weight == 0.0f) {
                        continue;
                    }
                    int n;
                    if (j < 0) {
                        n = -j;
                    } else if (j >= srcSize) {
                        n = srcSize - j + srcSize - 1;
                    } else {
                        n = j;
                    }
                    int k = arrN[i];
                    //assert k == j-left:String.format("%s = %s %s", k,j,left);
                    arrN[i]++;
                    if (n < 0 || n >= srcSize) {
                        weight = 0.0f;// Flag that cell should not be used
                    }
                    arrPixel[subindex + k] = n;
                    arrWeight[subindex + k] = weight;
                }
                // normalize the filter's weight's so the sum equals to 1.0, very important for avoiding box type of artifacts
                final int max = arrN[i];
                float tot = 0;
                for (int k = 0; k < max; k++)
                    tot += arrWeight[subindex + k];
                if (tot != 0f) { // 0 should never happen except bug in filter
                    for (int k = 0; k < max; k++)
                        arrWeight[subindex + k] /= tot;
                }
            }
        } else
        // super-sampling
        // Scales from smaller to bigger height
        {
            numContributors = (int) (fwidth * 2.0f + 1);
            arrWeight = new float[dstSize * numContributors];
            arrPixel = new int[dstSize * numContributors];
            //
            for (int i = 0; i < dstSize; i++) {
                final int subindex = i * numContributors;
                float center = i / scale + centerOffset;
                int left = (int) Math.floor(center - fwidth);
                int right = (int) Math.ceil(center + fwidth);
                for (int j = left; j <= right; j++) {
                    float weight = filter.apply(center - j);
                    if (weight == 0.0f) {
                        continue;
                    }
                    int n;
                    if (j < 0) {
                        n = -j;
                    } else if (j >= srcSize) {
                        n = srcSize - j + srcSize - 1;
                    } else {
                        n = j;
                    }
                    int k = arrN[i];
                    arrN[i]++;
                    if (n < 0 || n >= srcSize) {
                        weight = 0.0f;// Flag that cell should not be used
                    }
                    arrPixel[subindex + k] = n;
                    arrWeight[subindex + k] = weight;
                }
                // normalize the filter's weight's so the sum equals to 1.0, very important for avoiding box type of artifacts
                final int max = arrN[i];
                float tot = 0;
                for (int k = 0; k < max; k++)
                    tot += arrWeight[subindex + k];
                assert tot != 0 : "should never happen except bug in filter";
                if (tot != 0f) {
                    for (int k = 0; k < max; k++)
                        arrWeight[subindex + k] /= tot;
                }
            }
        }
        return new SubSamplingData(arrN, arrPixel, arrWeight, numContributors);
    }

    public ResampleFilter getFilter() {
        return filter;
    }

    public void setFilter(ResampleFilter filter) {
        this.filter = filter;
    }

    public PixelBuffer doFilter(PixelBuffer srcImg, int dstWidth, int dstHeight) throws InterruptedException {
        this.dstWidth = dstWidth;
        this.dstHeight = dstHeight;

        if (dstWidth < 3 || dstHeight < 3) {
            throw new RuntimeException("Error doing rescale. Target size was " + dstWidth + "x" + dstHeight + " but must be at least 3x3.");
        }

        assert multipleInvocationLock.incrementAndGet() == 1 : "Multiple concurrent invocations detected";


        this.nrChannels = srcImg.getChannels();
        this.srcWidth = srcImg.getWidth();
        this.srcHeight = srcImg.getHeight();

        byte[][] workPixels = new byte[srcHeight][dstWidth * 4];

        this.processedItems = 0;
        this.totalItems = srcHeight + dstWidth;

        // Pre-calculate  sub-sampling
        horizontalSubsamplingData = createSubSampling(filter, srcWidth, dstWidth);
        verticalSubsamplingData = createSubSampling(filter, srcHeight, dstHeight);


        final PixelBuffer scrImgCopy = srcImg;
        final byte[][] workPixelsCopy = workPixels;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            final int finalI = i;
            CountDownLatch finalLatch1 = latch;
            getService().execute(new ResampleTask(dstWidth, () -> horizontallyFromSrcToWork(scrImgCopy, workPixelsCopy, finalI, numberOfThreads, finalLatch1)));
        }
        latch.await();

        int dstSize = dstHeight * dstWidth * 4;
        ByteBuf outPixels = srcImg.content().alloc().directBuffer(dstSize);
        outPixels.writerIndex(dstSize);
        // --------------------------------------------------
        // Apply filter to sample vertically from Work to Dst
        // --------------------------------------------------
        final ByteBuffer outPixelsCopy = outPixels.nioBuffer();
        latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            final int finalI = i;
            CountDownLatch finalLatch = latch;
            getService().execute(new ResampleTask(dstWidth, () -> verticalFromWorkToDst(workPixelsCopy, outPixelsCopy, finalI, numberOfThreads, finalLatch)));
        }

        latch.await();

        //noinspection UnusedAssignment
        workPixels = null; // free memory

        assert multipleInvocationLock.decrementAndGet() == 0 : "Multiple concurrent invocations detected";

        return new BufferBackedImage(dstWidth, dstHeight, outPixels);
    }


    private void verticalFromWorkToDst(byte[][] workPixels, ByteBuffer outPixels, int start, int delta, CountDownLatch latch) {
        for (int x = start; x < dstWidth; x += delta) {
            final int xLocation = x * 4;
            for (int y = dstHeight - 1; y >= 0; y--) {
                final int yTimesNumContributors = y * verticalSubsamplingData.numContributors;
                final int max = verticalSubsamplingData.arrN[y];
                final int sampleLocation = (y * dstWidth + x) * 4;


                float sample0 = 0.0f;
                float sample1 = 0.0f;
                float sample2 = 0.0f;
                int index = yTimesNumContributors;
                for (int j = max - 1; j >= 0; j--) {
                    int valueLocation = verticalSubsamplingData.arrPixel[index];
                    float arrWeight = verticalSubsamplingData.arrWeight[index];
                    sample0 += (workPixels[valueLocation][xLocation] & 0xff) * arrWeight;
                    sample1 += (workPixels[valueLocation][xLocation + 1] & 0xff) * arrWeight;
                    sample2 += (workPixels[valueLocation][xLocation + 2] & 0xff) * arrWeight;
//                    if (useChannel3) {
//                        sample3 += (workPixels[valueLocation][xLocation + 3] & 0xff) * arrWeight;
//                    }

                    index++;
                }

                outPixels.put(sampleLocation, toByte(sample0));
                outPixels.put(sampleLocation + 1, toByte(sample1));
                outPixels.put(sampleLocation + 2, toByte(sample2));
//                if (useChannel3) {
                outPixels.put(sampleLocation + 3, ((byte) 255));
//                }

            }
            processedItems++;
            if (start == 0) { // only update progress listener from main thread
                setProgress();
            }
        }
        latch.countDown();
    }

    /**
     * Apply filter to sample horizontally from Src to Work
     *
     * @param srcImg
     * @param workPixels
     * @param latch
     */
    private void horizontallyFromSrcToWork(PixelBuffer srcImg, byte[][] workPixels, int start, int delta, CountDownLatch latch) {
        if (nrChannels == 1) {
            latch.countDown();
            return;
        }
        final ByteBuffer srcImgArray = srcImg.getBuffer();


        for (int k = start; k < srcHeight; k = k + delta) {
            int offset = k * srcImg.getWidth() * srcImg.getChannels();

            for (int i = dstWidth - 1; i >= 0; i--) {
                int sampleLocation = i * 4;
                final int max = horizontalSubsamplingData.arrN[i];

                float sample0 = 0.0f;
                float sample1 = 0.0f;
                float sample2 = 0.0f;
                int index = i * horizontalSubsamplingData.numContributors;
                for (int j = max - 1; j >= 0; j--) {
                    float arrWeight = horizontalSubsamplingData.arrWeight[index];
                    int pixelIndex = horizontalSubsamplingData.arrPixel[index] * srcImg.getChannels();

                    sample0 += (srcImgArray.get(offset + pixelIndex) & 0xff) * arrWeight;
                    sample1 += (srcImgArray.get(offset + pixelIndex + 1) & 0xff) * arrWeight;
                    sample2 += (srcImgArray.get(offset + pixelIndex + 2) & 0xff) * arrWeight;
                    index++;
                }

                workPixels[k][sampleLocation] = toByte(sample0);
                workPixels[k][sampleLocation + 1] = toByte(sample1);
                workPixels[k][sampleLocation + 2] = toByte(sample2);
            }
            processedItems++;
            if (start == 0) { // only update progress listener from main thread
                setProgress();
            }
        }
        latch.countDown();
    }


    private byte toByte(float f) {
        if (f < 0) {
            return 0;
        }
        if (f > MAX_CHANNEL_VALUE) {
            return (byte) MAX_CHANNEL_VALUE;
        }
        return (byte) (f + 0.5f); // add 0.5 same as Math.round
    }

    private void setProgress() {
        fireProgressChanged(processedItems / totalItems);
    }

    static class ResampleTask implements Runnable {
        private final int targetWidth;
        private final Runnable operation;

        ResampleTask(int targetWidth, Runnable operation) {
            this.targetWidth = targetWidth;
            this.operation = operation;
        }

        public int getTargetWidth() {
            return targetWidth;
        }

        @Override
        public void run() {
            operation.run();
        }
    }

    static class SubSamplingData {
        private final int[] arrN; // individual - per row or per column - nr of contributions
        private final int[] arrPixel;  // 2Dim: [wid or hei][contrib]
        private final float[] arrWeight; // 2Dim: [wid or hei][contrib]
        private final int numContributors; // the primary index length for the 2Dim arrays : arrPixel and arrWeight

        private SubSamplingData(int[] arrN, int[] arrPixel, float[] arrWeight, int numContributors) {
            this.arrN = arrN;
            this.arrPixel = arrPixel;
            this.arrWeight = arrWeight;
            this.numContributors = numContributors;
        }


    }
}

