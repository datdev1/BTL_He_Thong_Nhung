package com.b21dccn216.vaxrobot.View;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;

import com.b21dccn216.vaxrobot.Model.RobotModel;
import com.b21dccn216.vaxrobot.R;

import java.util.Objects;
import java.util.logging.Logger;

public class MapView extends View {

    // Number of grid box
    private final int numberGridBox =  1000;
    // Map size : mapShapeSize x mapShapeSize
    private final float mapShapeSize = 100000f;
    // Each grid box size is 10 cm in real life
    private final float squareSizeCm = 10;
    // Size of each grid box in pixel
    private final int squareSize = (int) mapShapeSize/numberGridBox;

    // Initiate map
    private int[][] map = new int[numberGridBox][numberGridBox];
    // Robot Model to save position -> index in map, angle
    private RobotModel robotPosition;
    // Image of robot
    private  Bitmap robotBitmap = BitmapFactory
            .decodeResource(getResources(), R.drawable.car);


    // Paint to draw
    // grid
    private Paint gridPaint;
    // path
    private Paint pathPaint;
    // border
    private Paint borderPaint;


    // Variable for zoom, swipe map, rotate map
    private float scaleFactor = 0.5f;
    private float maxScale = 0.2f;
    private float minScale = 4f;

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
        // Init paint
        pathPaint = new Paint();
        pathPaint.setColor(getResources().getColor(R.color.tealColor));
        pathPaint.setStrokeWidth(5f);
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setAntiAlias(true);
        borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(10f);

        //
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        // Initial, robot is in map[500][500]
        robotPosition = new RobotModel(
                numberGridBox/2,    // 500
                numberGridBox/2,        //500
                0,                      // distance not use yet, we can use this to make robot move
                0,                       // angle -> 0 is up
                squareSize              //
        );

    }

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        // Draw path
        for(int i = 0; i<map.length; i++){
            for(int j = 0; j<map[i].length; j++){
                if(map[i][j] == 1){
                    canvas.drawRect(i*squareSize, j*squareSize,
                            i*squareSize + squareSize, j*squareSize + squareSize,
                            pathPaint);
                }
            }
        }

        drawGrid(canvas);

        // Draw the robot
        if (robotPosition != null && robotBitmap != null) {
            float x = robotPosition.getXAxis();
            float y = robotPosition.getYAxis();

            float centerX = x + (squareSize / 2f);
            float centerY = y + (squareSize / 2f);
            robotBitmap = Bitmap.createScaledBitmap(robotBitmap, squareSize, squareSize, false);

            Matrix matrix = new Matrix();
            matrix.postTranslate(-robotBitmap.getWidth() / 2f, -robotBitmap.getHeight() / 2f); // move to origin
            matrix.postRotate(robotPosition.getAngle()); // rotate around origin
            matrix.postTranslate(centerX, centerY); // move back to position

            canvas.drawBitmap(robotBitmap, matrix, null);
//            canvas.drawBitmap(robotBitmap, x, y, null);
        }

        canvas.restore();
    }

    private void drawGrid(Canvas canvas){
        // Draw the box
        int gridSize = (int) (mapShapeSize / numberGridBox); // Grid cell size

        // Draw vertical lines
        for (float x = 0; x <= mapShapeSize; x += gridSize) {
            if(x % squareSize == 0){
                String xTitle = x/squareSize + "";
                canvas.drawText(x + "", x, -2, borderPaint);
            }
            if(x == 0 ){
                canvas.drawLine(x, 0, x, mapShapeSize, borderPaint);
                continue;
            }else if(x == mapShapeSize){
                canvas.drawLine(x, 0, x, mapShapeSize, borderPaint);
            }
            canvas.drawLine(x, 0, x, mapShapeSize, gridPaint);
        }
        // Draw horizontal lines
        for (float y = 0; y <= mapShapeSize; y += gridSize) {
            if(y % squareSize == 0){
                String yTitle = y/squareSize + "";
                canvas.drawText(yTitle + "", -2, y, borderPaint);
            }
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

    public void setRobotAngle(float angle){
        if(angle > 360){
            angle = angle % 360;
        }else if(angle < 0){
            angle = 360 + (angle % 360);
        }
        this.robotPosition.setAngle(angle);

        Log.d("MapView", "Set angle: " + angle);
        invalidate();
    }

    public float getRobotAngle(){return robotPosition.getAngle();}


    // Update robot position
    public void updateRobotPosition(String action, float distance) {

        double radians = Math.toRadians(robotPosition.getAngle());
        float xDistance = (float) (distance * Math.sin(radians));
        float yDistance = (float) (distance * Math.cos(radians));
        Log.d("MapView", "radians: getAngle = " + robotPosition.getAngle() + ", sin = " + Math.sin(radians) +
                "cos = " + Math.cos(radians));
        int y = robotPosition.getY() +   (int) (yDistance / squareSizeCm);
        int x = robotPosition.getX() +  (int) (xDistance / squareSizeCm);

        if(action.equals("UP")){
            x = robotPosition.getX() +  (int) (xDistance / squareSizeCm);
            y = robotPosition.getY() -   (int) (yDistance / squareSizeCm);
        }else if(action.equals("DOWN")){
            x = robotPosition.getX() -  (int) (xDistance / squareSizeCm);
            y = robotPosition.getY() +   (int) (yDistance / squareSizeCm);
        }



        Log.d("MapView", "Update: x = " + x + ", y = " + y);

//        x, y passed
        int clampedX = Math.max(0, Math.min(numberGridBox - 1, x));
        int clampedY = Math.max(0, Math.min(numberGridBox - 1,  y));

        robotPosition.setX(clampedX);
        robotPosition.setY(clampedY);
        // center to robot position
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Center formula
        translateX = (viewWidth / 2f) - (robotPosition.getXAxis() * scaleFactor);
        translateY = (viewHeight / 2f) - (robotPosition.getYAxis() * scaleFactor);

        // TODO: Update map and set index to 1,2,3
        map[robotPosition.getX()][robotPosition.getY()] = 1;
        invalidate();
    }
}

