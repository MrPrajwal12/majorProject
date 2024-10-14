package com.project.intellifit_trainer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MyWebSocketClient {

    private final OkHttpClient client; // Make client final
    private WebSocket webSocket;
    private final WeakReference<StartWorkoutActivity> activity; // Use WeakReference for activity
    private boolean isConnected = false;

    public MyWebSocketClient(StartWorkoutActivity activity) {
        this.client = new OkHttpClient();
        this.activity = new WeakReference<>(activity); // Initialize with WeakReference
    }

    public void start() {
        if (!isConnected) { // Only start if not already connected
            // WebSocket URL and correct port number
            Request request = new Request.Builder()
                    .url("wss://fitness-app-r2af.onrender.com:8765") // Use wss:// if your server has SSL enabled
                    .build();

            this.webSocket = client.newWebSocket(request, new EchoWebSocketListener());
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing connection");
            webSocket = null; // Clear the reference to the webSocket
            isConnected = false; // Update connection status
        }
    }

    public boolean isConnected() {
        return isConnected; // Method to check connection status
    }

    private class EchoWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull okhttp3.Response response) {
            isConnected = true;
            Log.d("WebSocket", "Socket opened successfully");
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
            Log.d("WebSocket", "Received a message from the server.");
            byte[] byteArray = bytes.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            StartWorkoutActivity activityInstance = activity.get(); // Get activity reference
            if (activityInstance != null) {
                activityInstance.runOnUiThread(() -> activityInstance.updateImageView(bitmap));
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            Log.d("WebSocket", "Received a text message: " + text);
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, okhttp3.Response response) {
            isConnected = false;
            Log.e("WebSocket", "Connection failed", t);
            // Attempt to reconnect after a delay
            new android.os.Handler().postDelayed(() -> {
                Log.d("WebSocket", "Attempting to reconnect...");
                start(); // Restart the WebSocket connection
            }, 5000); // Delay in milliseconds before reconnecting
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            isConnected = false;
            Log.d("WebSocket", "Socket closed: " + reason);
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.d("WebSocket", "Socket is closing: " + reason);
            webSocket.close(code, reason);
            isConnected = false;
        }
    }
}
