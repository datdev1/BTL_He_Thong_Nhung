package com.b21dccn216.vaxrobot.View;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;

import com.b21dccn216.vaxrobot.Model.AxisModel;
import com.b21dccn216.vaxrobot.R;

public class MapView extends View {

    private final int numberGridBox =  1000;
    private final float mapShapeSize = 100000f;
    private final int squareSize = (int) mapShapeSize/numberGridBox;

    private int[][] map = new int[numberGridBox][numberGridBox];
    private AxisModel robotPosition;



    private Paint robotPaint;

    private Paint gridPaint = new Paint();
    private Paint pathPaint;
    private Paint borderPaint;

//    private PointF robotPosition;

    private float scaleFactor = 0.5f;
    private float maxScale = 0.3f;
    private float minScale = 5f;

    private float translateX = 0f, translateY = 0f;
    private float lastTouchX, lastTouchY;
    private int activePointerId = -1;
    private ScaleGestureDetector scaleDetector;

    public MapView(Context context) {
        super(context);
        init(context);
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        robotPaint = new Paint();
        robotPaint.setColor(Color.RED);
        robotPaint.setStyle(Paint.Style.FILL);

        pathPaint = new Paint();
        pathPaint.setColor(Color.BLUE);
        pathPaint.setStrokeWidth(5f);

        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setAntiAlias(true);


        borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(10f);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        robotPosition = new AxisModel(numberGridBox/2, numberGridBox/2, squareSize);
//        translateX = robotPosition.getXAxis();
//        translateY = robotPosition.getYAxis();

    }

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.drawColor(R.color.beige);
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        // Draw the box
        for(int i = 0; i<map.length; i++){
            for(int j = 0; j<map[i].length; j++){
                if(map[i][j] == 1){
                    canvas.drawRect(i*squareSize, j*squareSize,
                            i*squareSize + squareSize, j*squareSize + squareSize,  pathPaint);
                }
            }
        }

        // Draw the robot
        if (robotPosition != null) {
            canvas.drawRect(robotPosition.getXAxis(), robotPosition.getYAxis(),
                    robotPosition.getXAxis() + squareSize,robotPosition.getYAxis() + squareSize,
                    robotPaint);
        }


        drawGrid(canvas);

        canvas.restore();
    }

    private void drawGrid(Canvas canvas){
        int gridSize = (int) (mapShapeSize / numberGridBox); // Grid cell size

        // Draw vertical lines
        for (float x = 0; x <= mapShapeSize; x += gridSize) {
            if(x == 0 || x == mapShapeSize){
                canvas.drawLine(x, 0, x, mapShapeSize, borderPaint);
                continue;
            }
            canvas.drawLine(x, 0, x, mapShapeSize, gridPaint);
        }
        // Draw horizontal lines
        for (float y = 0; y <= mapShapeSize; y += gridSize) {
            if(y == 0 || y == mapShapeSize){
                canvas.drawLine(0, y, mapShapeSize, y, borderPaint);
                continue;
            }
            canvas.drawLine(0, y, mapShapeSize, y, gridPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        scaleDetector.onTouchEvent(event);
        final int action = event.getAction();

        switch (action){
            case MotionEvent.ACTION_DOWN: {
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                activePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!scaleDetector.isInProgress()) {
                    final int pointerIndex = event.findPointerIndex(activePointerId);
                    final float x = event.getX(pointerIndex);
                    final float y = event.getY(pointerIndex);

                    // Calculate pan distance
                    final float dx = x - lastTouchX;
                    final float dy = y - lastTouchY;

                    translateX += dx;
                    translateY += dy;
                    float maxTranslateX = 0;
                    float maxTranslateY = 0;
                    float minTranslateX = -mapShapeSize * scaleFactor + getWidth();
                    float minTranslateY = -mapShapeSize * scaleFactor + getHeight();

                    translateX = Math.max(minTranslateX, Math.min(maxTranslateX, translateX));
                    translateY = Math.max(minTranslateY, Math.min(maxTranslateY, translateY));

                    invalidate();

                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = -1;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newPointerIndex);
                    lastTouchY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    // Update robot position
    public void updateRobotPosition(int x, int y) {
        int clampedX = Math.max(0, Math.min(numberGridBox - 1, robotPosition.getX() + x));
        int clampedY = Math.max(0, Math.min(numberGridBox - 1, robotPosition.getY() + y));

        robotPosition.setX(clampedX);
        robotPosition.setY(clampedY);

        map[robotPosition.getX()][robotPosition.getY()] = 1;
        // center to robot position
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Center formula
        translateX = (viewWidth / 2f) - (robotPosition.getXAxis() * scaleFactor);
        translateY = (viewHeight / 2f) - (robotPosition.getYAxis() * scaleFactor);


        invalidate();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
//            scaleFactor *= detector.getScaleFactor();
//            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f));
//            invalidate();
//            return true;



            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            float prevScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(maxScale, Math.min(scaleFactor, minScale));

            // Adjust translation so zoom keeps the focus point in place
            translateX += (focusX - translateX) * (1 - scaleFactor / prevScale);
            translateY += (focusY - translateY) * (1 - scaleFactor / prevScale);
            invalidate();
            return  true;
        }
    }

    public void centerOnPoint(float x, float y) {
        // Ensure scaling is considered
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Center formula
        translateX = (viewWidth / 2f) - (x * scaleFactor);
        translateY = (viewHeight / 2f) - (y * scaleFactor);

        invalidate();
    }

}

