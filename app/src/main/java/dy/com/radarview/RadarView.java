package dy.com.radarview;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import dy.com.rippleviewtest.R;

/**
 * Created by duyuan797 on on 16/9/27.
 */
public class RadarView extends TextView {
    private static final String TAG = RadarView.class.getSimpleName();

    private static final int DEFAULT_RIPPLE_COLOR = Color.parseColor("#ffffff");
    private static final int DEFAULT_RIPPLE_NUM = 4;
    /**
     * 水波纹动画由外至内
     */
    public static final int MODE_IN = 1;
    /**
     * 水波纹动画由内至外
     */
    public static final int MODE_OUT = 2;

    /**
     * 默认的波纹直径的最小值
     */
    private static final int MINE_SIZE = 900;

    /**
     * 动画中波纹的颜色
     */
    private int mRippleColor;
    /**
     * 动画中波纹的数量
     */
    private int mRippleNum;

    /**
     * 波纹动画效果是否正在进行
     */
    private boolean isAnimationRunning = false;

    private int currentProgress = 0;

    private int rotateDegree = 0;

    /**
     * 动画执行的时间
     */
    private int mTotalTime = 100 * 1000;

    private int mode = MODE_OUT;

    private int mPeriod = 20;
    private int mCenterX;
    private int mCenterY;
    private int mRadius;
    private Paint mPaint;
    private ObjectAnimator mAnimator;

    private Bitmap mScanBitmap;
    /**
     * 水波纹动画执行一次后,开始扫描动画
     */
    private int initProgress;

    public RadarView(Context context) {
        this(context, null);
    }

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RadarView);
        mRippleColor = typedArray.getColor(R.styleable.RadarView_rippleColor, DEFAULT_RIPPLE_COLOR);
        mRippleNum = typedArray.getInt(R.styleable.RadarView_rippleNum, DEFAULT_RIPPLE_NUM);

        initPaint();
        initAnimation();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mRippleColor);

        mScanBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_radar_scan)
                .copy(Bitmap.Config.ARGB_8888, true);
    }

    private void initAnimation() {
        mAnimator = ObjectAnimator.ofInt(this, "currentProgress", 0, 100);
        //循环次数,无线循环
        mAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mAnimator.setRepeatMode(ObjectAnimator.RESTART);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setEvaluator(mProgressEvaluator);
        mAnimator.setDuration(mTotalTime);

        startRippleAnimation();
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void startRippleAnimation() {
        if (!isAnimationRunning) {
            mAnimator.start();
            isAnimationRunning = true;
        }
    }

    public void stopRippleAnimation() {
        if (isAnimationRunning) {
            mAnimator.end();
            isAnimationRunning = false;
        }
    }

    public boolean isRippleAnimationRunning() {
        return isAnimationRunning;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
        this.invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int resultWidth = 0;
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (modeWidth == MeasureSpec.EXACTLY) {
            resultWidth = sizeWidth;
        } else {
            resultWidth = MINE_SIZE;
            if (modeWidth == MeasureSpec.AT_MOST) {
                resultWidth = Math.min(resultWidth, sizeWidth);
            }
        }

        int resultHeight = 0;
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (modeHeight == MeasureSpec.EXACTLY) {
            resultHeight = sizeHeight;
        } else {
            resultHeight = MINE_SIZE;
            if (modeHeight == MeasureSpec.AT_MOST) {
                resultHeight = Math.min(resultHeight, sizeHeight);
            }
        }

        mCenterX = resultWidth / 2;
        mCenterY = resultHeight / 2;
        mRadius = Math.max(resultWidth, resultHeight) / 2;

        mRadius = mScanBitmap.getWidth() / 2;

        Log.d(TAG, "ripple out view radius = "
                + mRadius
                + "; width ="
                + resultWidth
                + "; height = "
                + resultHeight);

        setMeasuredDimension(resultWidth, resultHeight);
    }

    @Override public void onDraw(Canvas canvas) {
        int progress = 0;
        for (int i = 0; i < mRippleNum; i++) {
            progress = (currentProgress + i * 100 / (mRippleNum)) % 100;
            if (mode == 1) progress = 100 - progress;
            int alpha = 255 - 255 * (progress) / 100 - 50;
            if (alpha < 10) {
                alpha = 10;
            }
            mPaint.setAlpha(alpha);
            canvas.drawCircle(mCenterX, mCenterY, mRadius * progress / 100, mPaint);
        }

        mPaint.setAlpha(255);
        if (progress == 99) {
            initProgress = progress;
        }
        /**
         * 如果水波纹动画执行完1次,开始执行旋转动画
         */
        if (initProgress == 99) {
            canvas.save();
            canvas.rotate(rotateDegree++, mCenterX, mCenterY);
            canvas.drawBitmap(mScanBitmap, mCenterX - mScanBitmap.getWidth() / 2,
                    mCenterY - mScanBitmap.getHeight() / 2, mPaint);
            canvas.restore();
        }
        if (rotateDegree == 360) {
            rotateDegree = 0;
        }

        /**
         * 绘制中心按钮
         */
        Bitmap centerBitmap =
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_radar_center);
        canvas.drawBitmap(centerBitmap, mCenterX - centerBitmap.getWidth() / 2,
                mCenterY - centerBitmap.getHeight() / 2, mPaint);

        super.onDraw(canvas);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isRippleAnimationRunning()) stopRippleAnimation();
    }

    /**
     * 自定义估值器
     */
    private TypeEvaluator mProgressEvaluator = new TypeEvaluator() {

        @Override public Object evaluate(float fraction, Object startValue, Object endValue) {
            fraction = (fraction * mTotalTime / mPeriod) % 100;
            return fraction;
        }
    };
}

