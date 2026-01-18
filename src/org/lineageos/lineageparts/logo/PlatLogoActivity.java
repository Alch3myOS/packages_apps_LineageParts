/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.lineageparts.logo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class PlatLogoActivity extends Activity {
    
    private GameView gameView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        gameView = new GameView(this);
        setContentView(gameView);
        
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }
    
    class GameView extends View {
        
        private Handler handler;
        private Runnable runnable;
        
        private Paint paint;
        private Paint textPaint;
        
        private float birdY;
        private float birdX;
        private float birdVelocity;
        private final float GRAVITY = 0.8f;
        private final float JUMP_STRENGTH = -15f;
        private final int BIRD_SIZE = 60;
        
        private ArrayList<Pipe> pipes;
        private final int PIPE_WIDTH = 120;
        private final int PIPE_GAP = 400;
        private final int PIPE_SPACING = 600;
        private float pipeSpeed = 8f;
        
        private boolean isGameRunning = false;
        private boolean isGameOver = false;
        private int score = 0;
        private int highScore = 0;
        
        private Random random;
        private SharedPreferences prefs;
        
        private int screenWidth;
        private int screenHeight;
        
        public GameView(Context context) {
            super(context);
            
            handler = new Handler();
            random = new Random();
            pipes = new ArrayList<>();
            
            paint = new Paint();
            paint.setAntiAlias(true);
            
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(80);
            textPaint.setTextAlign(Paint.Align.CENTER);
            
            prefs = context.getSharedPreferences("FlappyBirdGame", Context.MODE_PRIVATE);
            highScore = prefs.getInt("highScore", 0);
            
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (isGameRunning) {
                        update();
                        invalidate();
                    }
                    handler.postDelayed(this, 16);
                }
            };
        }
        
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            screenWidth = w;
            screenHeight = h;
            initGame();
        }
        
        private void initGame() {
            birdX = screenWidth / 4;
            birdY = screenHeight / 2;
            birdVelocity = 0;
            score = 0;
            isGameOver = false;
            pipes.clear();
            
            for (int i = 0; i < 3; i++) {
                addPipe(screenWidth + i * PIPE_SPACING);
            }
        }
        
        private void addPipe(float x) {
            int minHeight = 200;
            int maxHeight = screenHeight - PIPE_GAP - 200;
            int topHeight = random.nextInt(maxHeight - minHeight) + minHeight;
            pipes.add(new Pipe(x, topHeight));
        }
        
        private void update() {
            if (!isGameRunning || isGameOver) return;
            
            birdVelocity += GRAVITY;
            birdY += birdVelocity;
            
            for (int i = pipes.size() - 1; i >= 0; i--) {
                Pipe pipe = pipes.get(i);
                pipe.x -= pipeSpeed;
                
                if (!pipe.scored && pipe.x + PIPE_WIDTH < birdX) {
                    pipe.scored = true;
                    score++;
                    if (score > highScore) {
                        highScore = score;
                        prefs.edit().putInt("highScore", highScore).apply();
                    }
                }
                
                if (pipe.x + PIPE_WIDTH < 0) {
                    pipes.remove(i);
                    addPipe(pipes.get(pipes.size() - 1).x + PIPE_SPACING);
                }
                
                if (checkCollision(pipe)) {
                    gameOver();
                }
            }
            
            if (birdY > screenHeight - BIRD_SIZE || birdY < 0) {
                gameOver();
            }
        }
        
        private boolean checkCollision(Pipe pipe) {
            Rect birdRect = new Rect(
                (int)birdX, 
                (int)birdY, 
                (int)(birdX + BIRD_SIZE), 
                (int)(birdY + BIRD_SIZE)
            );
            
            Rect topPipeRect = new Rect(
                (int)pipe.x, 
                0, 
                (int)(pipe.x + PIPE_WIDTH), 
                pipe.topHeight
            );
            
            Rect bottomPipeRect = new Rect(
                (int)pipe.x, 
                pipe.topHeight + PIPE_GAP, 
                (int)(pipe.x + PIPE_WIDTH), 
                screenHeight
            );
            
            return Rect.intersects(birdRect, topPipeRect) || 
                   Rect.intersects(birdRect, bottomPipeRect);
        }
        
        private void gameOver() {
            isGameOver = true;
            isGameRunning = false;
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            canvas.drawColor(0xFF87CEEB);
            
            if (screenWidth == 0 || screenHeight == 0) return;
            
            paint.setColor(0xFF228B22);
            for (Pipe pipe : pipes) {
                canvas.drawRect(pipe.x, 0, pipe.x + PIPE_WIDTH, pipe.topHeight, paint);
                canvas.drawRect(pipe.x, pipe.topHeight + PIPE_GAP, 
                              pipe.x + PIPE_WIDTH, screenHeight, paint);
                
                paint.setColor(0xFF32CD32);
                canvas.drawRect(pipe.x, 0, pipe.x + 15, pipe.topHeight, paint);
                canvas.drawRect(pipe.x, pipe.topHeight + PIPE_GAP, 
                              pipe.x + 15, screenHeight, paint);
                paint.setColor(0xFF228B22);
            }
            
            paint.setColor(0xFFFFD700);
            canvas.drawCircle(birdX + BIRD_SIZE / 2, birdY + BIRD_SIZE / 2, 
                            BIRD_SIZE / 2, paint);
            
            paint.setColor(Color.BLACK);
            canvas.drawCircle(birdX + BIRD_SIZE / 2 + 10, birdY + BIRD_SIZE / 2 - 5, 
                            8, paint);
            
            textPaint.setTextSize(80);
            canvas.drawText("Score: " + score, screenWidth / 2, 100, textPaint);
            
            textPaint.setTextSize(50);
            canvas.drawText("Best: " + highScore, screenWidth / 2, 180, textPaint);
            
            if (!isGameRunning) {
                paint.setColor(0xAA000000);
                canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
                
                textPaint.setTextSize(100);
                if (isGameOver) {
                    canvas.drawText("Game Over!", screenWidth / 2, screenHeight / 2 - 100, textPaint);
                    textPaint.setTextSize(60);
                    canvas.drawText("Final Score: " + score, screenWidth / 2, screenHeight / 2, textPaint);
                } else {
                    canvas.drawText("Flappy Bird", screenWidth / 2, screenHeight / 2 - 100, textPaint);
                }
                
                textPaint.setTextSize(50);
                canvas.drawText("Tap to " + (isGameOver ? "Restart" : "Start"), 
                              screenWidth / 2, screenHeight / 2 + 100, textPaint);
            }
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!isGameRunning) {
                    initGame();
                    isGameRunning = true;
                } else if (!isGameOver) {
                    birdVelocity = JUMP_STRENGTH;
                }
            }
            return true;
        }
        
        public void resume() {
            handler.post(runnable);
        }
        
        public void pause() {
            handler.removeCallbacks(runnable);
        }
        
        class Pipe {
            float x;
            int topHeight;
            boolean scored;
            
            Pipe(float x, int topHeight) {
                this.x = x;
                this.topHeight = topHeight;
                this.scored = false;
            }
        }
    }
}
