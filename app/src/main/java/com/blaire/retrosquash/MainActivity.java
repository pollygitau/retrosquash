package com.blaire.retrosquash;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.media.SoundPool;
import android.media.AudioManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //sound
    private SoundPool soundPool;
    int  sample1;
    int  sample2;
    int  sample3;
    int  sample4;

    //screen
    Display display;
    int screenWidth;
    int screenHeight;

    //Game Objects
    int racketWidth;
    int racketHeight;
    Point racketPosition;
    int ballWidth;
    Point ballPosition;

    //racket movement
    boolean racketIsMovingRight;
    boolean racketIsMovingLeft;

    //ball movements
    boolean ballIsMovingLeft;
    boolean ballIsMovingRight;
    boolean ballIsMovingUp;
    boolean ballIsMovingDown;

    //stats
    int score;
    int lives;
    int fps;
    long lastFrameTime;

    Canvas canvas;
    SquashCourtView courtView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        courtView = new SquashCourtView(this);
        setContentView(courtView);
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        sample1 = soundPool.load(this, R.raw.sample1,0);
        sample2 = soundPool.load(this, R.raw.sample2,0);
        sample3 = soundPool.load(this, R.raw.sample3,0);
        sample4 = soundPool.load(this, R.raw.sample4,0);

        //display
        display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        //game object positions
        racketPosition = new Point();
        racketPosition.x = screenWidth/2;
        racketPosition.y = screenHeight -70;
        racketWidth = screenWidth/8;
        racketHeight = 10;

        //ball
        ballWidth = screenWidth/35; //arbitrary division
        ballPosition = new Point();
        ballPosition.x = screenWidth/2;
        ballPosition.y = 1 + ballWidth;

        lives = 3;
        score = 0;
    }

    @Override
    protected void onResume(){
        super.onResume();
        courtView.resume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        courtView.pause();
    }

    class SquashCourtView extends SurfaceView implements Runnable{
        Thread ourThread;
        SurfaceHolder ourHolder;
        volatile boolean playingSquash;
        Paint paint;
        public SquashCourtView(Context context){
            super(context);
            ourHolder = getHolder();
            paint = new Paint();
            ballIsMovingDown = true;

            //send ball
            Random randomNumber = new Random();
            int ballDirection = randomNumber.nextInt(3);
            switch (ballDirection){
                case 0:
                    ballIsMovingLeft =  false;
                    ballIsMovingRight = false;
                    break;
                case 1:
                    ballIsMovingLeft =true;
                    ballIsMovingRight = false;
                    break;
                case 2:
                    ballIsMovingLeft = false;
                    ballIsMovingRight = false;
                    break;
            }
        }

        @Override
        public void run(){
            while(playingSquash){
                updateLogic();
                drawCourt();
                controlFPS();
            }
        }

        public void updateLogic(){
            //racket movement
            if(racketIsMovingRight){
                if(racketPosition.x + (racketWidth/2)<screenWidth){
                    racketPosition.x += 20;
                }
            }
            if (racketIsMovingLeft){
                if(racketPosition.x -(racketWidth/2)>0){
                    racketPosition.x -=20;
                }
            }

            //Detect collisions
            //right
            if(ballPosition.x + ballWidth>screenWidth){
                ballIsMovingLeft = true;
                ballIsMovingRight = false;
                soundPool.play(sample1, 1,1,0,0,1);
            }
            //left
            if(ballPosition.x < 0 ){
                ballIsMovingRight = true;
                ballIsMovingLeft = false;
                soundPool.play(sample1,1,1,0,0,1);
            }
            //bottom
            if(ballPosition.y > screenHeight - ballWidth){
                lives -= 1;
                if(lives  == 0){
                    lives = 3;
                    score = 0;
                    soundPool.play(sample4,1,1,0,0,1);
                }
                ballPosition.y = 1+ ballWidth;
                Random ran  = new Random();
                int direction  = ran.nextInt(3);
                        switch (direction){
                            case 0:
                                ballIsMovingLeft = false;
                                ballIsMovingRight = false;
                                break;
                            case 1:
                                ballIsMovingLeft = true;
                                ballIsMovingRight = false;
                                break;
                            case 2:
                                ballIsMovingLeft = false;
                                ballIsMovingRight = true;
                                break;
                        }
            }

            //top
            if(ballPosition.y <= 0){
                ballIsMovingDown = true;
                ballIsMovingUp = false;
                ballPosition.y = 1;
                soundPool.play(sample2,1,1,0,0,1);
            }

            //move
            if(ballIsMovingDown){
                ballPosition.y += 6;
            }

            if(ballIsMovingUp){
                ballPosition.y -= 10;
            }

            if(ballIsMovingLeft){
                ballPosition.x -= 12;
            }

            if(ballIsMovingRight){
                ballPosition.x +=12;
            }

            //has ball  hit   the racket
            if(ballPosition.y + ballWidth >+ (racketPosition.y - racketHeight/2)){
                int halfRacket = racketWidth/2;
                if(ballPosition.x + ballWidth > (racketPosition.x - halfRacket) &&
                        ballPosition.x -ballWidth <(racketPosition.x + racketHeight)){
                    //rebound ball
                    soundPool.play(sample3,1,1,0,0,1);
                    score++;
                    ballIsMovingUp = true;
                    ballIsMovingDown = false;

                    //go up lefty or righty
                    if(ballPosition.x> racketPosition.x){
                        ballIsMovingRight = true;
                        ballIsMovingLeft = false;
                    }else{
                        ballIsMovingRight = false;
                        ballIsMovingLeft = true;
                    }
                }
            }
        }

        public  void drawCourt(){
            if(!ourHolder.getSurface().isValid()){
                return;
            }
            canvas = ourHolder.lockCanvas();
            canvas.drawColor(Color.BLACK);

            paint.setColor(Color.WHITE);
            paint.setTextSize(45);
            String title = "Score: " +  score + " Lives: " + " fps " + fps;
            canvas.drawText(title,40,40,paint);

            //racket
            int left = racketPosition.x -(racketWidth/2);
            int top = racketPosition.y -(racketHeight/2);
            int right = racketPosition.x + (racketWidth/2);
            int bottom = racketPosition.y + (racketWidth/2);
            canvas.drawRect(left,top,right,bottom,paint);

            canvas.drawCircle(ballPosition.x,ballPosition.y,ballWidth,paint);

            ourHolder.unlockCanvasAndPost(canvas);

            //ball
        }



        private void controlFPS(){
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 15- timeThisFrame;
            if(timeThisFrame > 0){
                fps = (int)(1000/timeThisFrame);
            }
            if(timeToSleep > 0){
                try{
                    Thread.sleep(timeToSleep);
                }catch(InterruptedException e){

                }
            }
            lastFrameTime = System.currentTimeMillis();
        }

        public void pause(){
            playingSquash = false;
            try {
                ourThread.join();
            }catch (InterruptedException e){
            }
        }

        public void resume(){
            playingSquash=true;
            ourThread = new Thread(this);
            ourThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent){
            switch(motionEvent.getAction() & MotionEvent.ACTION_MASK){
                case MotionEvent.ACTION_DOWN:
                    if(motionEvent.getX() >= screenWidth/2){
                        racketIsMovingRight = true;
                        racketIsMovingLeft = false;
                    }else{
                        racketIsMovingLeft = true;
                        racketIsMovingRight = false;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    racketIsMovingRight = false;
                    racketIsMovingLeft = false;
                    break;
            }
            return true;
        }

    }
}

