package com.example.server;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Server_Splash_Screen_Activity extends AppCompatActivity {

    ImageView imageView;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                );

        setContentView(R.layout.activity_server__splash__screen_);

        imageView= (ImageView) findViewById(R.id.splashImageViewId);
        textView= (TextView) findViewById(R.id.splashTextViewId);

        Animation animation= AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade);
        imageView.startAnimation(animation);
        textView.startAnimation(animation);

        final Thread thread= new Thread(){

            @Override
            public void run() {
                try {
                    sleep(3000);
                    Intent intent= new Intent(Server_Splash_Screen_Activity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

//        Thread thread= new Thread(new Runnable() {
//            @Override
//            public void run() {
//               doWork();
//               startActivity();
//
//
//            }
//        });
//
//        thread.start();
//    }

//    public void doWork(){
//        try {
//            Thread.sleep(4000);
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

//    public void startActivity(){
//        Intent intent= new Intent(Server_Splash_Screen_Activity.this, MainActivity.class);
//        startActivity(intent);
//        finish();
//    }
}
