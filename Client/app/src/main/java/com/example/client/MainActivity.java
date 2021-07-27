package com.example.client;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    static final int SocketServerPORT = 8080;

    LinearLayout loginPanel, chatPanel;
    EditText editTextUserName, editTextAddress;
    Button buttonConnect;
    TextView chatMsg, textPort;

    //ChatPanel component
    EditText editTextSay;
    Button buttonSend, buttonDisconnect,buttonSavedChat,buttonClearChat;

    String msgLog = "";

    ChatClientThread chatClientThread = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginPanel = findViewById(R.id.loginpanelId);
        chatPanel = findViewById(R.id.chatpanelId);

        editTextUserName =  findViewById(R.id.username);
        editTextAddress =  findViewById(R.id.address);
        textPort =  findViewById(R.id.port);
        textPort.setText("port: " + SocketServerPORT);
        buttonConnect =  findViewById(R.id.connect);
        chatMsg =  findViewById(R.id.chatmsg);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);


        editTextSay = findViewById(R.id.say);
        buttonSend = findViewById(R.id.send);
        buttonSavedChat= findViewById(R.id.savechatmsg);
        buttonClearChat= findViewById(R.id.deletchatemsg);
        buttonDisconnect =  findViewById(R.id.disconnect);

        buttonSend.setOnClickListener(buttonSendOnClickListener);
        buttonSavedChat.setOnClickListener(buttonSavedChatConversation);
        buttonClearChat.setOnClickListener(buttonClearChatConversation);
        buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);

        verifyStoragePermissions();
    }

    public void verifyStoragePermissions() {
        // Check if we have write permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
    }

    //To save string to a textFile
    File file1;
    private void writeToFile(String fileName, String data) {
//        Long time= System.currentTimeMillis();
//        String timeMill = " "+time.toString();
        File defaultDir = Environment.getExternalStorageDirectory();
         file1 = new File(defaultDir, fileName+".txt");
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file1, false);
            stream.write(data.getBytes());
            stream.close();
            Toast.makeText(MainActivity.this,"Client Chatlog is successfully saved",Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            Toast.makeText(this,e.getMessage().toString(),Toast.LENGTH_LONG).show();
           // Log.d(TAG, e.toString());
        } catch (IOException e) {
            Toast.makeText(this,e.getMessage().toString(),Toast.LENGTH_LONG).show();
           // Log.d(TAG, e.toString());
        }
    }

    //To delete the ChatLog file
    private void deleteChatLogFile(){
        try {
            File file = new File(file1.getPath());
            if(file.exists()) file.delete();
            Toast.makeText(this,"ChatLog is successfully deleted",Toast.LENGTH_LONG).show();
        }catch (Exception e){
            Toast.makeText(this,e.getMessage().toString(),Toast.LENGTH_LONG).show();
        }
    }

    // Server Connect Button working process
    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            String textUserName = editTextUserName.getText().toString();
            String textAddress = editTextAddress.getText().toString();

            if (textUserName.equals("") && textAddress.equals("")) {
                Toast.makeText(MainActivity.this, "Enter User Name and Addresses",
                        Toast.LENGTH_LONG).show();
                return;
            }
            else if (textUserName.equals("")) {
                Toast.makeText(MainActivity.this, "Enter User Name",
                        Toast.LENGTH_LONG).show();
                return;
            }
            else if(textAddress.equals("")) {
                Toast.makeText(MainActivity.this, "Enter Addresses",
                        Toast.LENGTH_LONG).show();
                return;
            }

            //Setting log message To the Client MessagesList TextView
            msgLog = "";
            chatMsg.setText(msgLog);

            //Invisible the Login Screen And Visible the Chat Screen
            loginPanel.setVisibility(View.GONE);
            chatPanel.setVisibility(View.VISIBLE);


            chatClientThread = new ChatClientThread(
                    textUserName, textAddress, SocketServerPORT);
            chatClientThread.start();
            editTextUserName.setText("");
            editTextAddress.setText("");
        }

    };

    //Working process of Send Button
    View.OnClickListener buttonSendOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (editTextSay.getText().toString().equals("")) {
                return;
            }

            if(chatClientThread==null){
                return;
            }

            chatClientThread.sendMsg(editTextSay.getText().toString() + "\n");
            editTextSay.setText("");
        }

    };

    View.OnClickListener buttonSavedChatConversation= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            writeToFile("ClientChatLog",msgLog);
        }
    };

    //Working process of Disconnect Button
    View.OnClickListener buttonDisconnectOnClickListener = v -> {

        Toast.makeText(getApplicationContext(),"Connection is turned off",Toast.LENGTH_LONG).show();
        if(chatClientThread==null){
            return;
        }
        chatClientThread.sendMsg(editTextUserName.getText().toString()+"Server connection is turned off.\n");
        editTextSay.setText("");
        chatClientThread.disconnect();
    };


    View.OnClickListener buttonClearChatConversation= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            deleteChatLogFile();
        }
    };

    //Synchronized the active Client Chat thread
    private class ChatClientThread extends Thread {

        String name;
        String dstAddress;
        int dstPort;

        String msgToSend = "";
        boolean goOut = false;  // Checking Connection

        ChatClientThread(String name, String address, int port) {
            this.name = name;
            dstAddress = address;
            dstPort = port;
        }

        //Established Socket Connection
        @Override
        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;


            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream.writeUTF(name);
                dataOutputStream.flush();

                while (!goOut) {
                    if (dataInputStream.available() > 0) {
                        msgLog += dataInputStream.readUTF();

                        MainActivity.this.runOnUiThread(() -> chatMsg.setText(msgLog));
                    }

                    // Writing Server And Client Chat Message
                    if(!msgToSend.equals("")){
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        msgToSend = "";
                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String errorString = e.toString();
                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, errorString, Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                e.printStackTrace();
                final String errorString = e.toString();
                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, errorString, Toast.LENGTH_LONG).show());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();     // Disconnecting Socket Connection
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                //Closing OutputStream and InputStream connection
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                //Invisible chat Screen and visible login screen
                MainActivity.this.runOnUiThread(() -> {
                    loginPanel.setVisibility(View.VISIBLE);
                    chatPanel.setVisibility(View.GONE);
                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

        private void disconnect(){
            goOut = true;
        }
    }
}