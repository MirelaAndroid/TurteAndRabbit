/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbit.turtle.mikw.turteandrabbit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class TurtlRabbitWatchFace extends CanvasWatchFaceService {

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create("sans-serif-light", Typeface.NORMAL);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        Bitmap mBackgroundBitmap;
        Bitmap mGrayBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;
        Bitmap mGrayBackgroundScaledBitmap;

        Paint mBackgroundPaint;
        Paint mBackgrundPaintHours;
        Paint mHandPaint;
        Paint mTextPaint;
        boolean mAmbient;
        Time mTime;
        boolean rabbit = true;

        Bitmap rabbit1, rabbit1r, rabbit2, rabbit2r, rabbit3, rabbit4;

        int cx, cy;
        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(TurtlRabbitWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = TurtlRabbitWatchFace.this.getResources();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inDither = true;

            mBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg, options);
            mGrayBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg_bw, options);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

            mBackgrundPaintHours = new Paint();
            mBackgrundPaintHours.setColor(resources.getColor(R.color.analog_hands));

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.analog_hands));

            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.BUTT);

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();

            rabbit1 = BitmapFactory.decodeResource(getResources(), R.drawable.k1);
            rabbit2 = BitmapFactory.decodeResource(getResources(), R.drawable.k2);
            rabbit3 = BitmapFactory.decodeResource(getResources(), R.drawable.k3);
            rabbit4 = BitmapFactory.decodeResource(getResources(), R.drawable.k4);
            rabbit1r = BitmapFactory.decodeResource(getResources(), R.drawable.k1r);
            rabbit2r = BitmapFactory.decodeResource(getResources(), R.drawable.k2r);

            cx = rabbit1.getWidth() / 2;
            cy = rabbit1.getHeight() / 2;
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            if (mAmbient)
                canvas.drawColor(getResources().getColor(R.color.analog_background_ambient));
            else
                canvas.drawColor(getResources().getColor(R.color.analog_background));

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true);
            }

            if (mGrayBackgroundScaledBitmap == null
                    || mGrayBackgroundScaledBitmap.getWidth() != width
                    || mGrayBackgroundScaledBitmap.getHeight() != height) {
                mGrayBackgroundScaledBitmap = Bitmap.createScaledBitmap(mGrayBackgroundBitmap,
                        width, height, true);
            }

            if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundScaledBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            }

            mHandPaint.setStrokeWidth(getResources().getDimension(R.dimen.analog_hand_stroke));

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float r1 = centerX - 120;
            float r2sec = centerX - 45;
            float r2min = centerX - 95;
            float r2hr = centerX - 105;

//          SECONDS
            if (!mAmbient) {

                Bitmap bmp = rabbit3;

                float secXr1 = (float) Math.sin(secRot) * r1;
                float secYr1 = (float) -Math.cos(secRot) * r1;
                float secXr2 = (float) Math.sin(secRot) * r2sec;
                float secYr2 = (float) -Math.cos(secRot) * r2sec;

                if (secXr1 >= 0 && secYr1 <= 0) {
                    if (rabbit) {
                        bmp = rabbit1;
                        rabbit = false;
                    } else {
                        bmp = rabbit2;
                        rabbit = true;
                    }

                }

                if (secXr1 > 0 && secYr1 > 0) {
                    if (secXr1 == 40.00) {

                        bmp = rabbit4;
                    } else {
                        if (rabbit) {
                            bmp = rabbit1r;
                            rabbit = false;
                        } else {
                            bmp = rabbit2r;
                            rabbit = true;
                        }
                    }
                }


                if (secXr1 < 0 && secYr1 > 0) {
                    if (rabbit) {
                        bmp = rabbit1r;
                        rabbit = false;
                    } else {
                        bmp = rabbit2r;
                        rabbit = true;
                    }
                }

                if (secXr1 < 0 && secYr1 < 0) {
                    if (secXr1 == -40.00) {

                        bmp = rabbit3;
                    } else {
                        if (rabbit) {
                            bmp = rabbit1;
                            rabbit = false;
                        } else {
                            bmp = rabbit2;
                            rabbit = true;
                        }
                    }
                }

                canvas.drawBitmap(bmp, centerX + secXr2 - cx, centerY + secYr2 - cy, null);

            }

//          MINUTES
            float minXr2 = (float) Math.sin(minRot) * r2min;
            float minYr2 = (float) -Math.cos(minRot) * r2min;

            mHandPaint.setStyle(Paint.Style.FILL);
            if (!mAmbient) {
                mHandPaint.setColor(getResources().getColor(R.color.hour));
                Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.z1r);

                int cx = bmp.getWidth() / 2;
                int cy = bmp.getHeight() / 2;
                canvas.drawBitmap(bmp, centerX + minXr2 - cx, centerY + minYr2 - cy, null);
            }
            mHandPaint.setStyle(Paint.Style.FILL);
            if (!mAmbient) {
                mHandPaint.setColor(getResources().getColor(R.color.hour));


//          HOURS
                float hrXr1 = (float) Math.sin(hrRot) * r1;
                float hrYr1 = (float) -Math.cos(hrRot) * r1;
                float hrXr2 = (float) Math.sin(hrRot) * r2hr;
                float hrYr2 = (float) -Math.cos(hrRot) * r2hr;
                canvas.drawLine(centerX + hrXr1, centerY + hrYr1, centerX + hrXr2, centerY + hrYr2, mHandPaint);

            }
            // display time in digital
            String text = String.format("%d:%02d", mTime.hour, mTime.minute);
            if (!mAmbient) {
                mTextPaint.setTextSize(25);
            } else {
                mTextPaint.setTextSize(35);

            }

            float textYShift = centerY;
            if (!getPeekCardPosition().isEmpty()) {
                textYShift = centerY;
            }

            if (!mAmbient) {
                canvas.drawText(text,
                        computeXOffset(text, mTextPaint, bounds),
                        textYShift + 10,
                        mTextPaint);
            } else {
                canvas.drawText(text,
                        computeXOffset(text, mTextPaint, bounds),
                        textYShift + 120,
                        mTextPaint);
            }
        }

        private float computeXOffset(String text, Paint paint, Rect watchBounds) {
            float centerX = watchBounds.exactCenterX();
            float timeLength = paint.measureText(text);
            return centerX - (timeLength / 2.0f);
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            TurtlRabbitWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            TurtlRabbitWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<TurtlRabbitWatchFace.Engine> mWeakReference;

        public EngineHandler(Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            TurtlRabbitWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
