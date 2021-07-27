package com.example.server;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static final int SocketServerPORT = 8080;

    TextView infoIp, infoPort, chatMsg;
    EditText serverText;
    Spinner spUsers;
    ArrayAdapter<ChatClient> spUsersAdapter;
    Button btnSentToIndividual, btnSentToAll,saveServerChat,clientServerChat;

    String msgLog = "";

    List<ChatClient> userList;

    ServerSocket serverSocket;

    //declare this variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoIp =  (TextView) findViewById(R.id.infoip);
        infoPort =  (TextView) findViewById(R.id.infoport);
        chatMsg =  (TextView) findViewById(R.id.chatmsg);
        serverText= (EditText) findViewById(R.id.servertextid);


        spUsers = (Spinner) findViewById(R.id.spusers);
        userList = new ArrayList<ChatClient>();
        spUsersAdapter = new ArrayAdapter<ChatClient>(
                MainActivity.this, android.R.layout.simple_spinner_item, userList);
        spUsersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUsers.setAdapter(spUsersAdapter);


        btnSentToIndividual = (Button)findViewById(R.id.sentToIndividualUser);
        btnSentToIndividual.setOnClickListener(btnSentToOnClickListener);

        btnSentToAll= (Button) findViewById(R.id.sentToAllUser);
        btnSentToAll.setOnClickListener(btnSentToAllOnClickListener);

        saveServerChat= (Button) findViewById(R.id.savedtextid);
        saveServerChat.setOnClickListener(btnSaveServerChat);

        clientServerChat= (Button) findViewById(R.id.cleartextid);
        clientServerChat.setOnClickListener(btnClearServerChat);

        verifyStoragePermissions();

        infoIp.setText(getIpAddress());   // Getting client Ip Address

        ChatServerThread chatServerThread = new ChatServerThread();
        chatServerThread.start();
    }

    //Sending the server message to the all active client
    View.OnClickListener btnSentToOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChatClient client = (ChatClient)spUsers.getSelectedItem();
            if(client != null){
                String serverMsg = "Server: "+serverText.getText().toString()+"\n";
                client.chatThread.sendMsg(serverMsg);
                msgLog += serverMsg;
                chatMsg.setText(msgLog);
                serverText.setText("");

            }else{
                Toast.makeText(MainActivity.this, "No user connected", Toast.LENGTH_LONG).show();
            }
        }
    };

    View.OnClickListener btnSentToAllOnClickListener= new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(!serverText.getText().toString().equals("")) {
                String serverMsg = "Server: " + serverText.getText().toString() + "\n";
                msgLog += serverMsg;
                chatMsg.setText(msgLog);
                serverText.setText("");
                broadcastMsg(serverMsg);
            }
            else{
                Toast.makeText(MainActivity.this,"Chat is empty.",Toast.LENGTH_LONG).show();
            }
        }
    };

    //Save Server Text Box Chat
    View.OnClickListener btnSaveServerChat= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            writeToFile("ServerChatLog",msgLog);
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();       //Disconnecting Socket Connection
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //Clear Server Text Box Chat
    View.OnClickListener btnClearServerChat= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            deleteServerFile("SeverChatLog");
        }
    };

    //Create and call this function from onCreate(). It is needed for granting storage permission.
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
    File file11;

    //To save Server chat
    private void writeToFile(String fileName, String data) {
        Long time= System.currentTimeMillis();
        String timeMill = " "+time.toString();
        File defaultDir = Environment.getExternalStorageDirectory();
        file11 = new File(defaultDir, fileName+".txt");
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file11, false);
            stream.write(data.getBytes());
            stream.close();
            Toast.makeText(MainActivity.this,"Server Chatlog is successfully saved",Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            Log.d("Error", e.toString());
        } catch (IOException e) {
            Log.d("Error", e.toString());
        }
    }

    private void deleteServerFile(String fileName){
        try {
            File file = new File(file11.getPath());
            if(file.exists()) file.delete();
            Toast.makeText(this,"Chat log is successfully deleted",Toast.LENGTH_LONG).show();
        }catch (Exception e){
            Toast.makeText(this,e.getMessage().toString(),Toast.LENGTH_LONG).show();
        }
    }


    //Synchronizing the Server ChatList
    private class ChatServerThread extends Thread {
        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                MainActivity.this.runOnUiThread(() -> {
                    infoPort.setText("I'm waiting here: " + serverSocket.getLocalPort());
                });

                while (true) {
                    socket = serverSocket.accept();     // Established Client Socket Connection
                    ChatClient client = new ChatClient();
                    userList.add(client);       //Connecting client
                    ConnectThread connectThread = new ConnectThread(client, socket);
                    connectThread.start();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spUsersAdapter.notifyDataSetChanged();
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();  //Disconnecting Socket Connection
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    private class ConnectThread extends Thread {

        Socket socket;
        ChatClient connectedClient;
        String msgToSend = "";

        ConnectThread(ChatClient client, Socket socket){
            connectedClient = client;
            this.socket= socket;
            client.socket = socket;
            client.chatThread = this;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                String clientname = dataInputStream.readUTF();

                connectedClient.name = clientname;

                //Storing client information
                msgLog += connectedClient.name + " connected@" +
                        connectedClient.socket.getInetAddress() +
                        ":" + connectedClient.socket.getPort() + "\n";
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        chatMsg.setText(msgLog);
                    }
                });

                //Writing data to the inputStream
                dataOutputStream.writeUTF("Sever: Welcome " + clientname + "\n");
                dataOutputStream.flush();

                broadcastMsg("Server: "+ clientname + " joined our chat.\n");

                while (true) {
                    if (dataInputStream.available() > 0) {
                        String newMsg = dataInputStream.readUTF();


                        msgLog += clientname + ": " + newMsg;
                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMsg.setText(msgLog);
                            }  //updating the chat text
                        });

                        broadcastMsg(clientname + ": " + newMsg);
                    }

                    if(!msgToSend.equals("")){
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        msgToSend = "";
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                // Removing Client from the Server connection
                userList.remove(connectedClient);

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        spUsersAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this,
                                connectedClient.name + " removed.", Toast.LENGTH_LONG).show();

                        msgLog += "-- " + connectedClient.name + " leaved\n";
                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMsg.setText(msgLog);
                            }
                        });

                        broadcastMsg("-- " + connectedClient.name + " leaved\n");
                    }
                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

    }

    private void broadcastMsg(String msg){
        for(int i=0; i<userList.size(); i++){
            userList.get(i).chatThread.sendMsg(msg);
            msgLog += "- send to " + userList.get(i).name + "\n";
        }
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                chatMsg.setText(msgLog);
            }
        });
    }



    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }
                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    class ChatClient {
        String name;
        Socket socket;
        ConnectThread chatThread;

        @Override
        public String toString() {
            return name + ": " + socket.getInetAddress().getHostAddress();
        }
    }
}