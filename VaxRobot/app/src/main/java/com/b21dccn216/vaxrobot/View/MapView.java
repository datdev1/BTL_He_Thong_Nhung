package com.b21dccn216.vaxrobot.View;

import static com.b21dccn216.vaxrobot.Model.MapModel.mapShapeSize;
import static com.b21dccn216.vaxrobot.Model.MapModel.numberGridBox;
import static com.b21dccn216.vaxrobot.Model.MapModel.squareSize;

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

import com.b21dccn216.vaxrobot.Model.MapModel;
import com.b21dccn216.vaxrobot.Model.RobotModel;
import com.b21dccn216.vaxrobot.Model.SonicValue;
import com.b21dccn216.vaxrobot.R;

public class MapView extends View {

    private MapModel mapModel;
    // Image of robot
    private  Bitmap robotBitmap = BitmapFactory
            .decodeResource(getResources(), R.drawable.car);


    // Paints
    private Paint gridPaint;
    private Paint pathPaint;
    private Paint obstaclePaint;
    private Paint spacePaint;
    private Paint borderPaint;


    // Variable for zoom, swipe map, rotate map, pan
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
        // init map Model
        mapModel = new MapModel();
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

        obstaclePaint = new Paint();
        obstaclePaint.setColor(getResources().getColor(R.color.darkTealColor));
        obstaclePaint.setStrokeWidth(5f);

        spacePaint = new Paint();
        spacePaint.setColor(getResources().getColor(R.color.greenColor));
        spacePaint.setStrokeWidth(5f);

        // scale detector for zoom, pan
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setMapModel(MapModel mapModel){
        this.mapModel = mapModel;
        invalidate();
    }

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if(mapModel == null) return;

        float[][] map = mapModel.getMap();
        canvas.save();

        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        // Draw path / map content
        for(int i = 0; i<map.length; i++){
            for(int j = 0; j<map[i].length; j++){
                if(map[i][j] != 0){
                    Log.i("MAP_VALUE", "i: " + i + " j: " + j + " value: " + map[i][j] + "");
                }
                if(map[i][j] == 1){
                    canvas.drawRect(i*squareSize, j*squareSize,
                            i*squareSize + squareSize, j*squareSize + squareSize,
                            pathPaint);
                }else if(map[i][j] >= 2.0f && map[i][j] <= 3.0f){
                    int alpha = (int) ((map[i][j] - 2.0F)*100);
                    spacePaint.setAlpha(alpha);
                    canvas.drawRect(i*squareSize, j*squareSize,
                            i*squareSize + squareSize, j*squareSize + squareSize,
                            spacePaint);
                    spacePaint.setAlpha(100);
                }else if (map[i][j] >= 4.F && map[i][j] <= 5.F){
                    int alpha = (int) ((map[i][j] - 4.0)*100);
                    obstaclePaint.setAlpha(alpha);
                    canvas.drawRect(i*squareSize, j*squareSize,
                            i*squareSize + squareSize, j*squareSize + squareSize,
                            obstaclePaint);
                    obstaclePaint.setAlpha(100);
                }else if(map[i][j] == 7){
                    canvas.drawRect(i*squareSize, j*squareSize,
                            i*squareSize + squareSize, j*squareSize + squareSize,
                            borderPaint);
                }
            }
        }

        drawGrid(canvas);
        drawRobot(canvas);

        canvas.restore();
    }

    private void drawRobot(Canvas canvas){
        RobotModel robotModel = mapModel.getRobotModel();
        if (robotModel != null && robotBitmap != null) {
            int x = (int) robotModel.getXAxis();
            int y = (int) robotModel.getYAxis();
            Log.e("VALIDATE_FLOAT", "x: " + x + " y: " + y + " angle: " + robotModel.getAngle());

            float centerX = x + (squareSize / 2f);
            float centerY = y + (squareSize / 2f);
            robotBitmap = Bitmap.createScaledBitmap(robotBitmap, squareSize, squareSize, false);

            Matrix matrix = new Matrix();
            matrix.postTranslate(-robotBitmap.getWidth() / 2f, -robotBitmap.getHeight() / 2f); // move to origin
            matrix.postRotate(robotModel.getAngle()); // rotate around origin
            matrix.postTranslate(centerX, centerY); // move back to position

            canvas.drawBitmap(robotBitmap, matrix, null);
        }
    }

    private void drawGrid(Canvas canvas){
        int gridSize = (int) (mapShapeSize / numberGridBox); // Grid cell size

        // Draw vertical lines
        for (float x = 0; x <= mapShapeSize; x += gridSize) {
            if(x % squareSize == 0){
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
                canvas.drawText(y + "", -2, y, borderPaint);
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
                break;
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

    public void centerOnRobotPosition() {
        RobotModel robotModel = mapModel.getRobotModel();
        // Ensure scaling is considered
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Center formula
        translateX = (viewWidth / 2f) - (robotModel.getXAxis() * scaleFactor);
        translateY = (viewHeight / 2f) - (robotModel.getYAxis() * scaleFactor);

        invalidate();
    }
    public void centeronPoint(float x, float y) {

        // Ensure scaling is considered
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Center formula
        translateX = (viewWidth / 2f) - (x * scaleFactor);
        translateY = (viewHeight / 2f) - (y * scaleFactor);

        invalidate();
    }

    public void resetMap(){
        mapModel.resetMap();
        invalidate();
    }

    public void moveRobotCar(float distance, String action){
        mapModel.moveCar(distance, action);
        invalidate();
    }

    public void setRobotAngle(float angle){
        mapModel.setRobotAngle(angle);
        invalidate();
    }

    public void processSonicValue(SonicValue s){
        mapModel.processSonicValue(s);
        invalidate();
    }

    public void updateRobotModel(RobotModel robotModel){
        mapModel.updateRobotModel(robotModel);
        invalidate();
    }


}

