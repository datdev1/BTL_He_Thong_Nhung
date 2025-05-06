package com.b21dccn216.vaxrobot.Model;

import android.util.Log;

public class MapModel {

    // Number of grid box
    public static final int numberGridBox =  1000;
    // Map size : mapShapeSize x mapShapeSize
    public static final float mapShapeSize = 100000f;
    // Size of each grid box in pixel
    public static final int squareSize = (int) mapShapeSize/numberGridBox;

    // Initiate map
    private int[][] map = new int[numberGridBox][numberGridBox];
    // Each grid box size is 10 cm in real life
    public static final float squareSizeCm = 10;


    // Robot Model to save position -> index in map, angle
    private RobotModel robotModel;

    public MapModel() {
        robotModel = new RobotModel(
                (float) numberGridBox /2,    // 500
                (float) numberGridBox /2,        //500
                0,
                squareSize
        );
    }

    public void setRobotAngle(float angle){
        robotModel.setAngle(angle);
    }

    public void updateRobotModel(RobotModel newRobotModel){
        robotModel.setAction(newRobotModel.getAction());
        robotModel.setAngle(newRobotModel.getAngle());
        moveCar(newRobotModel.getDistanceCm(), newRobotModel.getAction());
        processSonicValue(newRobotModel.getSonicValue());
    }

    public void moveCar(float distanceCm,  String action){
//        // TODO: Update map and set index to 1,2,3
        float[] oldPosition = new float[] { robotModel.getFloatX(), robotModel.getFloatY()};

        Log.i("VALIDATE_FLOAT", "Old float position x: " + robotModel.getFloatX() + " Y: " + robotModel.getFloatY());
        float[] newPosition = calculateNewPosition(
                robotModel.getFloatX(), robotModel.getFloatY(),
                robotModel.getAngle(), distanceCm, action);

        // TODO : cache decimal
        robotModel.setFloatX( newPosition[0]);
        robotModel.setFloatY( newPosition[1]);
        Log.i("VALIDATE_FLOAT", "New float position x: " + robotModel.getFloatX() + " Y: " + robotModel.getFloatY());

        drawLine(oldPosition[0], oldPosition[1],
                robotModel.getFloatX(), robotModel.getFloatY(),
                1);
        // call invalidate to update map view
    }

    // Call after call moveCar since FloatX and FloatY has yet re assigned
    public void processSonicValue(SonicValue sonicValue){
        robotModel.setSonicValue(sonicValue);
        // TODO: process sonic value
        // sonic left
        float angle = (robotModel.getAngle() + 270) % 360;
        float[] leftWall = calculateWallPosition(
                robotModel.getFloatX(), robotModel.getFloatY(),
                angle, robotModel.getSonicValue().getLeft());

        drawLine(robotModel.getFloatX(), robotModel.getFloatY(),
                leftWall[0], leftWall[1],2);

        //sonic right
        float rightAngle = (robotModel.getAngle() + 90) % 360;
        float[] rightWall = calculateWallPosition(
                robotModel.getFloatX(), robotModel.getFloatY(),
                rightAngle, robotModel.getSonicValue().getRight());
        drawLine(robotModel.getFloatX(), robotModel.getFloatY(),
                rightWall[0], rightWall[1], 2);

        // sonic front
        float[] frontWall = calculateWallPosition(
                 robotModel.getFloatX(),  robotModel.getFloatY(),
                robotModel.getAngle(), robotModel.getSonicValue().getFront());
        drawLine(
                robotModel.getFloatX(), robotModel.getFloatY(),
                frontWall[0], frontWall[1], 2);
        // call invalidate to update map view
    }



    public void resetMap(){
        for(int i = 0; i<map.length; i++){
            for(int j = 0; j<map[i].length; j++){
                map[i][j] = 0;
            }
        }
        robotModel = new RobotModel(
                numberGridBox/2,    // 500
                numberGridBox/2,        //500
                0,
                squareSize);
    }

    // calculate new position after move distanceCm cm to an angle
    private float[] calculateNewPosition(
            float oldX, float oldY,
            float angleDeg,
            float distanceCm,
            String action) {

        // Convert angle to radians
        float angleRad = (float) Math.toRadians(angleDeg);

        // Calculate delta in cm
        float deltaCmX = (float) (distanceCm * Math.sin(angleRad));
        float deltaCmY = (float) (distanceCm * Math.cos(angleRad));

        // Convert cm to grid index delta
        float deltaCellX =  (deltaCmX / MapModel.squareSizeCm); // 10cm per cell
        float deltaCellY = (deltaCmY / MapModel.squareSizeCm);


        // Y axis usually increases downward in arrays, so invert dy if needed
        float newX = 0, newY = 0;
        if(action.equals("F")){
            newX = oldX + deltaCellX;
            newY = oldY - deltaCellY; // subtract if y=0 is top of map
            return new float[]{newX, newY};
        }else if(action.equals("B")){
            newX = oldX - deltaCellX;
            newY = oldY + deltaCellY; // subtract if y=0 is top of map
            return new float[]{newX, newY};
        }else{
            Log.d("WHEN_ROLL", "action: " + action);
            return new float[]{oldX, oldY};
        }
    }

    // Calculating wall position base on sonic value
    private float[] calculateWallPosition(
            float x, float y,
            float angleDeg,
            float distanceCm) {
        // Convert angle to radians
        float angleRad = (float) Math.toRadians(angleDeg);

        // Calculate delta in cm
        float deltaCmX = (float) (distanceCm * Math.sin(angleRad));
        float deltaCmY = (float) (distanceCm * Math.cos(angleRad));

        // Convert cm to grid index delta
        int deltaCellX =  Math.round(deltaCmX / squareSizeCm); // 10cm per cell
        int deltaCellY =  Math.round(deltaCmY / squareSizeCm);


        // Y axis usually increases downward in arrays, so invert dy if needed
        float newX = 0, newY = 0;
        newX = x + deltaCellX;
        newY = y - deltaCellY; // subtract if y=0 is top of map
        return new float[]{newX, newY};
    }

    // drawLine is a function to mark in map value linePaint:
    // LinePaint = 1 -> robot has last run from
    // LinePaint = 2 -> space which is detect by sonic
    // LinePaint = 3 -> obstacle which is detect by sonic
    private void drawLine(float originX, float originY, float robotX, float robotY, int linePaint) {
        int x0 = (int) originX;
        int y0 = (int) originY;
        int x1 = (int) robotX;
        int y1 = (int) robotY;

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && y0 >= 0 && x0 < map.length && y0 < map[0].length) {
                if(linePaint == 1){
                    map[x0][y0] = linePaint;
                    Log.e("VALIDATE_FLOAT", "x0: " + x0 + " y0: " + y0 + " line: " + linePaint);
                }else{
                    if(map[x0][y0] != 1){
                        map[x0][y0] = linePaint;
//                        Log.e("VALIDATE_FLOAT", "x0: " + x0 + " y0: " + y0 + " line: " + linePaint);
                    }
                }
            }

            if (x0 == x1 && y0 == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }



    /*
        GETTER and SETTER
     */
    public int getNumberGridBox() {
        return numberGridBox;
    }

    public float getMapShapeSize() {
        return mapShapeSize;
    }

    public int getSquareSize() {
        return squareSize;
    }

    public int[][] getMap() {
        return map;
    }

    public void setMap(int[][] map) {
        this.map = map;
    }

    public RobotModel getRobotModel() {
        return robotModel;
    }

    public void setRobotModel(RobotModel robotModel) {
        this.robotModel = robotModel;
    }
}
