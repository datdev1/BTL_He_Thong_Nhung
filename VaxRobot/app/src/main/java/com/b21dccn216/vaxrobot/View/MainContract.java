package com.b21dccn216.vaxrobot.View;

public interface MainContract {
    interface View {
        void showConnectionSuccess(String device);
        void showConnectionFailed();
        void showDisconnected();
        void showMessage(String message);
        void showError(String error);
    }
    interface Presenter {
        void connectToDevice(String deviceAddress, String name);
        void sendCommand(String command);
        void disconnect();
    }

}
