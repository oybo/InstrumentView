package com.example.text.com.myapp;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘
 * Created by ouyangbo on 2017-03-01.
 */
public class InstrumentView extends View {

    private int mRadius; // 扇形半径
    private float mSelectorWitdh; // 渐变区域扇形长度
    private float mTxtSize; // 数字大小
    private int mStartAngle = 180; // 起始角度
    private int mSweepAngle = 180; // 绘制角度
    private int mMin = 50; // 最小值
    private int mMax = 210; // 最大值
    private int mSection = 8; // 值域（mMax-mMin）等分份数
    private int mPortion = 10; // 一个mSection等分份数
    private int mVelocity = mMin; // 实时速度
    private int mStrokeWidth; // 画笔宽度
    private int mLength1; // 长刻度的相对圆弧的长度
    private int mLength2; // 刻度读数顶部的相对圆弧的长度
    private int mPLRadius; // 指针长半径
    private int mPointerColor; // 指针颜色
    private Map<String, String> mDividerValues; // 设置的显示分割值
    private Path mPath;
    private int mPadding;
    private float mCenterX, mCenterY; // 圆心坐标
    private Paint mPaint;
    private RectF mRectFArc;
    private RectF mRectFInnerArc;
    private RectF mShaderRectFArc;
    private Rect mRectText;
    private String[] mTexts;
    private int[] mColors;

    public InstrumentView(Context context) {
        this(context, null);
    }

    public InstrumentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InstrumentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.InstrumentView);
        if(a != null) {
            mTxtSize = a.getDimension(R.styleable.InstrumentView_iv_txt_size, sp2px(16));
            mSelectorWitdh = (int) a.getDimension(R.styleable.InstrumentView_iv_selector_width, dp2px(40));
            mPointerColor = a.getColor(R.styleable.InstrumentView_iv_pointer_color, Color.parseColor("#B1181D"));
        }

        init();
    }

    private void init() {
        mStrokeWidth = dp2px(3);
        mLength1 = dp2px(8) + mStrokeWidth;
        mLength2 = mLength1 + dp2px(4);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mPath = new Path();
        mRectFArc = new RectF();
        mRectFInnerArc = new RectF();
        mShaderRectFArc = new RectF();
        mRectText = new Rect();

        mTexts = new String[mSection + 1]; // 需要显示mSection + 1个刻度读数
        for (int i = 0; i < mTexts.length; i++) {
            int n = (mMax - mMin) / mSection;
            mTexts[i] = String.valueOf(mMin + i * n);
        }

        mColors = new int[]{ContextCompat.getColor(getContext(), R.color.color_green),
                ContextCompat.getColor(getContext(), R.color.color_yellow),
                ContextCompat.getColor(getContext(), R.color.color_red)};
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mPadding = Math.max(
                Math.max(getPaddingLeft(), getPaddingTop()),
                Math.max(getPaddingRight(), getPaddingBottom())
        );
        setPadding(mPadding, mPadding, mPadding, mPadding);

        int width = resolveSize(dp2px(260), widthMeasureSpec);
        if(mRadius == 0) {
            mRadius = (width - mPadding * 2 - mStrokeWidth * 2) / 2;
        }

        // 由起始角度确定的高度
        float[] point1 = getCoordinatePoint(mRadius, mStartAngle);
        // 由结束角度确定的高度
        float[] point2 = getCoordinatePoint(mRadius, mStartAngle + mSweepAngle);
        int height = (int) Math.max(point1[1] + mRadius + mStrokeWidth * 2,
                point2[1] + mRadius + mStrokeWidth * 2);
        setMeasuredDimension(width, height + getPaddingTop() + getPaddingBottom());

        mCenterX = mCenterY = getMeasuredWidth() / 2f;
        mRectFArc.set(
                getPaddingLeft() + mStrokeWidth,
                getPaddingTop() + mStrokeWidth,
                getMeasuredWidth() - getPaddingRight() - mStrokeWidth,
                getMeasuredWidth() - getPaddingBottom() - mStrokeWidth
        );

        mPaint.setTextSize(sp2px(16));
        mPaint.getTextBounds("0", 0, "0".length(), mRectText);

        mRectFInnerArc.set(
                getPaddingLeft() + mStrokeWidth + mSelectorWitdh,
                getPaddingTop() + mStrokeWidth + mSelectorWitdh,
                getMeasuredWidth() - getPaddingRight() - mStrokeWidth - mSelectorWitdh,
                getMeasuredWidth() - getPaddingBottom() - mStrokeWidth - mSelectorWitdh
        );

        mPLRadius = mRadius - dp2px(15);

        mShaderRectFArc.set(
                (getPaddingLeft() + mStrokeWidth +  + mSelectorWitdh / 2),
                (getPaddingTop() + mStrokeWidth  + mSelectorWitdh / 2),
                getMeasuredWidth() - getPaddingRight() - mStrokeWidth - mSelectorWitdh / 2,
                getMeasuredWidth() - getPaddingBottom() - mStrokeWidth - mSelectorWitdh / 2
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /**
         * 中间渐变色区域
         */
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mSelectorWitdh);
        mPaint.setShader(generateSweepGradient());
        canvas.drawArc(mShaderRectFArc, mStartAngle - 2, mSweepAngle + 5, false, mPaint);
        mPaint.setShader(null);

        /**
         * 画外圆弧
         */
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_light));
        canvas.drawArc(mRectFArc, mStartAngle - 2, mSweepAngle + 5, false, mPaint);

        /**
         * 画内圆弧
         */
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(dp2px(5));
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_light));
        canvas.drawArc(mRectFInnerArc, mStartAngle - 2, mSweepAngle + 5, false, mPaint);

        mPaint.setStrokeWidth(mStrokeWidth);
        /**
         * 画长刻度
         * 画好起始角度的一条刻度后通过canvas绕着原点旋转来画剩下的长刻度
         */
        double cos = Math.cos(Math.toRadians(mStartAngle - 180));
        double sin = Math.sin(Math.toRadians(mStartAngle - 180));
        float x0 = (float) (mPadding + mStrokeWidth + mRadius * (1 - cos));
        float y0 = (float) (mPadding + mStrokeWidth + mRadius * (1 - sin));
        float x1 = (float) (mPadding + mStrokeWidth + mRadius - (mRadius - mLength1) * cos);
        float y1 = (float) (mPadding + mStrokeWidth + mRadius - (mRadius - mLength1) * sin);

        canvas.save();
        canvas.drawLine(x0, y0, x1, y1, mPaint);
        float angle = mSweepAngle * 1f / mSection;
        for (int i = 0; i < mSection; i++) {
            canvas.rotate(angle, mCenterX, mCenterY);
            canvas.drawLine(x0, y0, x1, y1, mPaint);
        }
        canvas.restore();

        /**
         * 画短刻度
         * 同样采用canvas的旋转原理
         */
        canvas.save();
        mPaint.setStrokeWidth(mStrokeWidth / 2f);
        float x2 = (float) (mPadding + mStrokeWidth + mRadius - (mRadius - 2 * mLength1 / 3f) * cos);
        float y2 = (float) (mPadding + mStrokeWidth + mRadius - (mRadius - 2 * mLength1 / 3f) * sin);
        canvas.drawLine(x0, y0, x2, y2, mPaint);
        angle = mSweepAngle * 1f / (mSection * mPortion);
        for (int i = 1; i < mSection * mPortion; i++) {
            canvas.rotate(angle, mCenterX, mCenterY);

            /**  绘制默认线条  */
            if (mDividerValues != null) {
                int n = (mMax - mMin) / mSection / mPortion;
                String value = String.valueOf(mMin + i * n);
                if (mDividerValues.containsKey(value)) {
                    canvas.drawLine(x0, y0, x2 + (mSelectorWitdh - mStrokeWidth - dp2px(5)), y2, mPaint);
                }
            }

            if (i % mPortion == 0) { // 避免与长刻度画重合
                continue;
            }
            // 绘制短刻度
            canvas.drawLine(x0, y0, x2, y2, mPaint);
        }
        canvas.restore();

        /**
         * 画长刻度读数
         */
        mPaint.setTextSize(mTxtSize);
        mPaint.setStyle(Paint.Style.FILL);
        float α;
        float[] p;
        angle = mSweepAngle * 1f / mSection;
        for (int i = 0; i <= mSection; i++) {
            α = mStartAngle + angle * i;
            p = getCoordinatePoint(mRadius - mLength2, α);
            if (α % 360 > 135 && α % 360 < 225) {
                mPaint.setTextAlign(Paint.Align.LEFT);
            } else if ((α % 360 >= 0 && α % 360 < 45) || (α % 360 > 315 && α % 360 <= 360)) {
                mPaint.setTextAlign(Paint.Align.RIGHT);
            } else {
                mPaint.setTextAlign(Paint.Align.CENTER);
            }
            int tt = (mStrokeWidth );
            int txtH = mRectText.height();
            if (i <= 1 || i >= mSection - 1) {
                canvas.drawText(mTexts[i], p[0], p[1] + txtH / 2 - tt, mPaint);
            } else if (i == 3) {
                canvas.drawText(mTexts[i], p[0] + txtH / 2 - tt, p[1] + txtH - tt, mPaint);
            } else if (i == mSection - 3) {
                canvas.drawText(mTexts[i], p[0] - txtH / 2 + tt, p[1] + txtH - tt, mPaint);
            } else {
                canvas.drawText(mTexts[i], p[0], p[1] + txtH - tt, mPaint);
            }
        }

        /**
         * 画指针
         */
        mPaint.setColor(mPointerColor);
        float θ = mStartAngle + mSweepAngle * (mVelocity - mMin) / (mMax - mMin); // 指针与水平线夹角
        int d = dp2px(5); // 指针由两个等腰三角形构成，d为共底边长的一半
        mPath.reset();
        float[] p1 = getCoordinatePoint(d, θ - 90);
        mPath.moveTo(p1[0], p1[1]);
        float[] p2 = getCoordinatePoint(mPLRadius, θ);
        mPath.lineTo(p2[0], p2[1]);
        float[] p3 = getCoordinatePoint(d, θ + 90);
        mPath.lineTo(p3[0], p3[1]);
        mPath.close();
        canvas.drawPath(mPath, mPaint);

        /**
         * 画中心两个圆
         */
        canvas.drawCircle(mCenterX, mCenterY, dp2px(10), mPaint);
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(mCenterX, mCenterY, dp2px(5), mPaint);
    }

    /**
     * 设置的显示分割值
     * @param scales
     */
    public void setDividerValues(int... scales) {
        mDividerValues = new HashMap<>();
        for (int i = 0; i < scales.length; i++) {
            mDividerValues.put(String.valueOf(scales[i]), "");
        }
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics());
    }

    private int sp2px(int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                Resources.getSystem().getDisplayMetrics());
    }

    public float[] getCoordinatePoint(int radius, float angle) {
        float[] point = new float[2];

        double arcAngle = Math.toRadians(angle); //将角度转换为弧度
        if (angle < 90) {
            point[0] = (float) (mCenterX + Math.cos(arcAngle) * radius);
            point[1] = (float) (mCenterY + Math.sin(arcAngle) * radius);
        } else if (angle == 90) {
            point[0] = mCenterX;
            point[1] = mCenterY + radius;
        } else if (angle > 90 && angle < 180) {
            arcAngle = Math.PI * (180 - angle) / 180.0;
            point[0] = (float) (mCenterX - Math.cos(arcAngle) * radius);
            point[1] = (float) (mCenterY + Math.sin(arcAngle) * radius);
        } else if (angle == 180) {
            point[0] = mCenterX - radius;
            point[1] = mCenterY;
        } else if (angle > 180 && angle < 270) {
            arcAngle = Math.PI * (angle - 180) / 180.0;
            point[0] = (float) (mCenterX - Math.cos(arcAngle) * radius);
            point[1] = (float) (mCenterY - Math.sin(arcAngle) * radius);
        } else if (angle == 270) {
            point[0] = mCenterX;
            point[1] = mCenterY - radius;
        } else {
            arcAngle = Math.PI * (360 - angle) / 180.0;
            point[0] = (float) (mCenterX + Math.cos(arcAngle) * radius);
            point[1] = (float) (mCenterY - Math.sin(arcAngle) * radius);
        }

        return point;
    }

    private SweepGradient generateSweepGradient() {
        SweepGradient sweepGradient = new SweepGradient(mCenterX, mCenterY,
                mColors,
                new float[]{0, 120 / 360f, mSweepAngle / 360f}
        );

        Matrix matrix = new Matrix();
        matrix.setRotate(mStartAngle * 0.75f, mCenterX, mCenterY);
        sweepGradient.setLocalMatrix(matrix);

        return sweepGradient;
    }

    public int getVelocity() {
        return mVelocity;
    }

    public void setVelocity(int velocity) {
        if (mVelocity == velocity || velocity < mMin || velocity > mMax) {
            return;
        }

        mVelocity = velocity;
        postInvalidate();
    }
}
