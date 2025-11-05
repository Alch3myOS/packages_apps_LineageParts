/*
 * SPDX-FileCopyrightText: 2010 The Android Open Source Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2024-2025 Lunaris AOSP
 * SPDX-FileCopyrightText: 2026 Alch3myOS
 * SPDX-License-Identifier: Apache-2.0
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
        private final int PIPE_SPACING = 600;
        
        // Difficulty variables (no longer final)
        private int pipeGap = 400;
        private float pipeSpeed = 8f;
        
        private enum Difficulty { INITIATE, ADEPT, MASTER }
        private Difficulty currentDifficulty = Difficulty.ADEPT;

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
            textPaint.setTextAlign(Paint.Align.CENTER);
            
            prefs = context.getSharedPreferences("AscensionGame", Context.MODE_PRIVATE);

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

        private void applyDifficulty() {
            switch (currentDifficulty) {
                case INITIATE:
                    pipeGap = 500; // Wide gap
                    pipeSpeed = 6f; // Slower pillars
                    break;
                case ADEPT:
                    pipeGap = 400; // Standard Flappy gap
                    pipeSpeed = 8f; // Standard speed
                    break;
                case MASTER:
                    pipeGap = 320; // Brutally tight gap
                    pipeSpeed = 11f; // Fast pillars
                    break;
            }
        }
        
        private void initGame() {
            applyDifficulty();
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
            int maxHeight = screenHeight - pipeGap - 200;
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
                pipe.topHeight + pipeGap,
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

            // Background: Deep Magical Void
            canvas.drawColor(0xFF0F0F1A);
            
            if (screenWidth == 0 || screenHeight == 0) return;
            
            // Draw Obstacles (Crystalline Pillars)
            for (Pipe pipe : pipes) {
                paint.setColor(0x4400E5FF);
                canvas.drawRect(pipe.x, 0, pipe.x + PIPE_WIDTH, pipe.topHeight, paint);
                canvas.drawRect(pipe.x, pipe.topHeight + pipeGap, pipe.x + PIPE_WIDTH, screenHeight, paint);
                
                paint.setColor(0xFF00E5FF);
                canvas.drawRect(pipe.x, 0, pipe.x + 5, pipe.topHeight, paint);
                canvas.drawRect(pipe.x + PIPE_WIDTH - 5, 0, pipe.x + PIPE_WIDTH, pipe.topHeight, paint);
                canvas.drawRect(pipe.x, pipe.topHeight + pipeGap, pipe.x + 5, screenHeight, paint);
                canvas.drawRect(pipe.x + PIPE_WIDTH - 5, pipe.topHeight + pipeGap, pipe.x + PIPE_WIDTH, screenHeight, paint);
                canvas.drawRect(pipe.x, pipe.topHeight - 10, pipe.x + PIPE_WIDTH, pipe.topHeight, paint);
                canvas.drawRect(pipe.x, pipe.topHeight + pipeGap, pipe.x + PIPE_WIDTH, pipe.topHeight + pipeGap + 10, paint);
            }
            
            // Draw Avatar (Glowing Purple Orb)
            paint.setColor(0x44B388FF); 
            canvas.drawCircle(birdX + BIRD_SIZE / 2, birdY + BIRD_SIZE / 2, BIRD_SIZE / 2 + 15, paint);
            paint.setColor(0xFFD0BCFF);
            canvas.drawCircle(birdX + BIRD_SIZE / 2, birdY + BIRD_SIZE / 2, BIRD_SIZE / 2 - 5, paint);
            
            textPaint.setTextSize(80);
            textPaint.setColor(Color.WHITE);
            canvas.drawText("Resonance: " + score, screenWidth / 2f, 100, textPaint);
            
            textPaint.setTextSize(50);
            canvas.drawText("Peak: " + highScore, screenWidth / 2f, 180, textPaint);
            
            // Overlay Menus
            if (!isGameRunning) {
                paint.setColor(0xDD000000); 
                canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
                
                if (isGameOver) {
                    textPaint.setTextSize(90);
                    textPaint.setColor(Color.WHITE);
                    canvas.drawText("Transmutation Failed", screenWidth / 2f, screenHeight / 2f - 150, textPaint);
                    textPaint.setTextSize(60);
                    canvas.drawText("Final Resonance: " + score, screenWidth / 2f, screenHeight / 2f - 50, textPaint);
                } else {
                    textPaint.setTextSize(100);
                    textPaint.setColor(Color.WHITE);
                    canvas.drawText("Alch3my Ascension", screenWidth / 2f, screenHeight / 2f - 100, textPaint);
                }
                
                // Start Instruction
                textPaint.setTextSize(50);
                textPaint.setColor(Color.LTGRAY);
                canvas.drawText(isGameOver ? "Tap top half to Reinitialize" : "Tap top half to Ascend", 
                               screenWidth / 2f, screenHeight / 2f + 50, textPaint);
                               
                // Difficulty Matrix UI
                textPaint.setTextSize(40);
                
                paint.setColor(currentDifficulty == Difficulty.INITIATE ? 0xFF00E5FF : 0x88FFFFFF);
                canvas.drawText("Initiate", screenWidth / 4f, screenHeight / 2f + 200, textPaint);
                
                paint.setColor(currentDifficulty == Difficulty.ADEPT ? 0xFF00E5FF : 0x88FFFFFF);
                canvas.drawText("Adept", screenWidth / 2f, screenHeight / 2f + 200, textPaint);
                
                paint.setColor(currentDifficulty == Difficulty.MASTER ? 0xFF00E5FF : 0x88FFFFFF);
                canvas.drawText("Master", 3 * screenWidth / 4f, screenHeight / 2f + 200, textPaint);
            }
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (!isGameRunning) {
                    // Check if click is in the lower half (Difficulty Selection)
                    if (event.getY() > screenHeight / 2f + 100) {
                        if (event.getX() < screenWidth / 3f) {
                            currentDifficulty = Difficulty.INITIATE;
                        } else if (event.getX() < 2 * screenWidth / 3f) {
                            currentDifficulty = Difficulty.ADEPT;
                        } else {
                            currentDifficulty = Difficulty.MASTER;
                        }
                        invalidate(); // Force a redraw to show the newly highlighted difficulty
                    } else {
                        // Click is in the upper half -> Start the game
                        initGame();
                        isGameRunning = true;
                    }
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