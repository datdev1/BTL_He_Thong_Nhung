package com.b21dccn216.vaxrobot.Model;


/*
x, y is index of robot in matrix
angle is the angle between robot and y-axis
 */
public class RobotModel {
    private int x;
    private int y;
    private int distance;
    private float angle;

    private int squareSize;

    public RobotModel(int x, int y, int squareSize) {
        this.x = x;
        this.y = y;
        this.squareSize = squareSize;
    }

    public RobotModel(int x, int y, int distance, float angle, int squareSize) {
        this.x = x;
        this.y = y;
        this.distance = distance;
        this.angle = angle;
        this.squareSize = squareSize;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public int getXAxis(){
        return x*squareSize;
    }

    public int getYAxis(){
        return y*squareSize;
    }





}
