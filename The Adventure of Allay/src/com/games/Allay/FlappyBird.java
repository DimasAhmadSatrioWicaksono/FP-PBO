package com.games.Allay;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    // Images
    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    // Bird class
    int birdX = boardWidth / 8;
    int birdY = boardWidth / 2;
    int birdWidth = 54;
    int birdHeight = 44;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    // Pipe class
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;  // Scaled by 1/6
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    // Game logic
    Bird bird;
    int velocityX = -4; // Move pipes to the left speed (simulates bird moving right)
    int velocityY = 0; // Move bird up/down speed
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipeTimer;
    boolean gameOver = false;
    double score = 0;

    // Sound clip
    Clip clingSound;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        // Load images
        backgroundImg = new ImageIcon(getClass().getResource("./minecraftbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("./Allay.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();

        // Load sound
        loadSound();

        // Initialize bird and pipes
        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();

        // Timer for placing pipes
        placePipeTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        placePipeTimer.start();

        // Game loop timer
        gameLoop = new Timer(1000 / 60, this); // 60 FPS
        gameLoop.start();
    }

    // Load sound
    public void loadSound() {
        try {
            // Get sound file URL
            URL soundURL = getClass().getResource("/com/games/Allay/resources/cling.wav");

            // If the file is not found, print an error message
            if (soundURL == null) {
                System.out.println("Sound file not found!");
                return;
            }

            // Open input stream for the sound file
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);

            // Get Clip to play the sound
            clingSound = AudioSystem.getClip();
            clingSound.open(audioIn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Play the cling sound
    public void playClingSound() {
        if (clingSound != null) {
            clingSound.setFramePosition(0); // Rewind to the start of the sound
            clingSound.start(); // Play sound
        }
    }

    // Place pipes randomly
    void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    // Drawing components (background, bird, pipes, score)
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // Draw background
        g.drawImage(backgroundImg, 0, 0, this.boardWidth, this.boardHeight, null);

        // Draw bird
        g.drawImage(birdImg, bird.x, bird.y, bird.width, bird.height, null);

        // Draw pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // Draw score
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf((int) score), 10, 35);
        } else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }
    }

    // Move bird and pipes
    public void move() {
        // Move bird
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0); // Keep bird within the board bounds

        // Move pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                score += 0.5; // Add score when passing both pipes
                pipe.passed = true;
                playClingSound(); // Play sound when bird passes the pipe
            }

            if (collision(bird, pipe)) {
                gameOver = true;
            }
        }

        // Check if bird falls off the screen
        if (bird.y > boardHeight) {
            gameOver = true;
        }
    }

    // Check collision between bird and pipe
    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&   // a's top left corner doesn't reach b's top right corner
               a.x + a.width > b.x &&   // a's top right corner passes b's top left corner
               a.y < b.y + b.height &&  // a's top left corner doesn't reach b's bottom left corner
               a.y + a.height > b.y;    // a's bottom left corner passes b's top left corner
    }

    @Override
    public void actionPerformed(ActionEvent e) { // Game loop called every x milliseconds
        move();
        repaint();
        if (gameOver) {
            placePipeTimer.stop();
            gameLoop.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = -9;

            if (gameOver) {
                bird.y = birdY;
                velocityY = 0;
                pipes.clear();
                gameOver = false;
                score = 0;
                gameLoop.start();
                placePipeTimer.start();
            }
        }
    }

    // Not needed
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}