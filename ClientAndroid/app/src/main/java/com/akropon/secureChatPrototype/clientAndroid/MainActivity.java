package com.akropon.secureChatPrototype.clientAndroid;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.akropon.clientAndroid.R;
import com.akropon.secureChatPrototype.clientApi.ClientHandler;
import com.akropon.secureChatPrototype.clientApi.ClientImpl;
import com.akropon.secureChatPrototype.clientApi.EventHandler;

public class MainActivity extends AppCompatActivity implements EventHandler {

    private View layout_main;
    private Button button_connect;
    private TextView textView_status;
    private EditText textEdit_host;
    private EditText textEdit_port;

    private View layout_chat;
    private TextView textView_chat;
    private EditText textEdit_msg;
    private Button button_send;

    private ClientImpl client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout_main = findViewById(R.id.layout_main);
        button_connect = findViewById(R.id.button_connect);
        textView_status = findViewById(R.id.textView_status);
        textEdit_host = findViewById(R.id.textEdit_host);
        textEdit_port = findViewById(R.id.textEdit_port);

        layout_chat = findViewById(R.id.layout_chat);
        textView_chat = findViewById(R.id.textView_chat);
        textEdit_msg = findViewById(R.id.textEdit_msg);
        button_send = findViewById(R.id.button_send);

        button_connect.setOnClickListener(v -> onButtonConnectClicked());
        button_send.setOnClickListener(v -> onButtonSendClicked());

        switchToMainLayout();

        ((Button) findViewById(R.id.button_debug)).setOnClickListener(v -> switchToChatLayout());
    }

    // TODO: 2019-05-22 close chanel on close of activity

    private void switchToChatLayout() {
        layout_main.setVisibility(View.GONE);
        layout_chat.setVisibility(View.VISIBLE);
    }

    private void switchToMainLayout() {
        layout_chat.setVisibility(View.GONE);
        layout_main.setVisibility(View.VISIBLE);
    }

    private void onButtonConnectClicked() {
        client = new ClientImpl(this);

        String host = textEdit_host.getText().toString();
        String portStr = textEdit_port.getText().toString();
        int port = Integer.parseInt(portStr);

        client.setHost(host);
        client.setPort(port);
        client.setLogger(System.out);

        client.tryConnectToServer();
    }

    private void onButtonSendClicked() {
        String msg = textEdit_msg.getText().toString();
        if (msg.equals("")) {
            return;
        }
        textEdit_msg.setText("");

        textView_chat.append("You: ");
        textView_chat.append(msg);
        textView_chat.append("\n");

        client.sendMessage(msg);
    }

    @Override
    public void fireSuccessfullyConnectedToServer() {
        runOnUiThread(() -> {
            textView_status.setText("Successfully connected");
            switchToChatLayout();
        });
    }

    @Override
    public void fireFailedConnectionToServer(Exception e) {
        e.printStackTrace();
        runOnUiThread(() -> {
            textView_status.setText("Failed to connect");
            switchToMainLayout();
        });
    }

    @Override
    public void fireConnectionClosed() {
        runOnUiThread(() -> {
            textView_status.setText("Connection closed");
            switchToMainLayout();
        });
    }

    @Override
    public void fireClosingFailed(InterruptedException e) {
        e.printStackTrace();
        runOnUiThread(() -> {
            textView_status.setText("Closing failed.");
            switchToMainLayout();
        });
    }

    @Override
    public void fireMessageReceived(String msg) {
        runOnUiThread(() -> {
            textView_chat.append("Companion: ");
            textView_chat.append(msg);
            textView_chat.append("\n");
        });

    }
}
