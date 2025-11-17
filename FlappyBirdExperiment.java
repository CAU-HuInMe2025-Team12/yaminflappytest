
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class FlappyBirdExperiment {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Flappy Bird HCI Experiment - Team 12");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GamePanel panel = new GamePanel();
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.requestFocusInWindow();
        });
    }
}

class Condition {

    String name;
    double jumpPower;
    int pipeDistance;
    int holeSize;

    public Condition(String name, double jumpPower, int pipeDistance, int holeSize) {
        this.name = name;
        this.jumpPower = jumpPower;
        this.pipeDistance = pipeDistance;
        this.holeSize = holeSize;
    }
}

enum GameState {
    MENU, RUNNING, GAME_OVER
}

class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener {

    public static final int WIDTH = 400;
    public static final int HEIGHT = 700;

    private String participantId = null;
    private int trialIndex = 1;

    private List<Condition> conditions = new ArrayList<>();
    private int currentConditionIndex = 0;
    private Condition currentCondition;

    private double JUMP_POWER;
    private int PIPE_DISTANCE;
    private int HOLE_SIZE;

    private double gravity = 0.5;
    private double maxFallSpeed = 15;

    private int birdX = WIDTH / 4;
    private double birdY = HEIGHT / 2.0;
    private double birdVelocity = 0.0;
    private int birdRadius = 14;

    private static final int PIPE_WIDTH = 70;
    private List<Pipe> pipes = new ArrayList<>();
    private Random random = new Random();

    private javax.swing.Timer timer;
    private boolean running = false;
    private boolean gameOver = false;
    private long trialStartTime = 0L;
    private long survivalTime = 0L;
    private int score = 0;

    private GameState state = GameState.MENU;

    private Rectangle btnStart;
    private Rectangle btnTryAgain;
    private Rectangle btnNewGame;
    private Rectangle btnExit;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(160, 216, 239)); // light sky-like
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        initConditions();
        currentCondition = conditions.get(0);
        applyCurrentCondition();

        timer = new javax.swing.Timer(16, this); // ~60 FPS

        int btnWidth = 140;
        int btnHeight = 40;
        int centerX = WIDTH / 2 - btnWidth / 2;
        int baseY = HEIGHT - 260;
        int gapY = 55;

        btnStart = new Rectangle(centerX, baseY, btnWidth, btnHeight);
        btnTryAgain = new Rectangle(centerX, baseY + gapY, btnWidth, btnHeight);
        btnNewGame = new Rectangle(centerX, baseY + 2 * gapY, btnWidth, btnHeight);
        btnExit = new Rectangle(centerX, baseY + 3 * gapY, btnWidth, btnHeight);

        askNewParticipantId();
    }

    private void initConditions() {
        double[] jumpLevels = {8.0, 10.0, 12.0};  // j0, j1, j2
        int[] gapLevels = {220, 260};           // g0, g1  (horizontal distance)
        int[] holeLevels = {160, 200};           // h0, h1  (hole size)

        for (int j = 0; j < jumpLevels.length; j++) {
            for (int g = 0; g < gapLevels.length; g++) {
                for (int h = 0; h < holeLevels.length; h++) {
                    String name = "j" + j + "_g" + g + "_h" + h;
                    conditions.add(new Condition(name, jumpLevels[j], gapLevels[g], holeLevels[h]));
                }
            }
        }
    }

    private void applyCurrentCondition() {
        this.JUMP_POWER = currentCondition.jumpPower;
        this.PIPE_DISTANCE = currentCondition.pipeDistance;
        this.HOLE_SIZE = currentCondition.holeSize;
    }

    private void askNewParticipantId() {
        String id = JOptionPane.showInputDialog(
                this,
                "Participant ID를 입력하세요 (예: P01):",
                "New Participant",
                JOptionPane.QUESTION_MESSAGE
        );
        if (id == null || id.trim().isEmpty()) {
            id = "Unknown";
        }
        participantId = id.trim();
        trialIndex = 1;
        currentConditionIndex = 0;
        currentCondition = conditions.get(currentConditionIndex);
        applyCurrentCondition();
        resetGameWithoutStarting();
        state = GameState.MENU;
        repaint();
    }

    private void resetGameWithoutStarting() {
        gameOver = false;
        running = false;
        pipes.clear();
        birdY = HEIGHT / 2.0;
        birdVelocity = 0.0;
        survivalTime = 0L;
        score = 0;
    }

    public void onStartButton() {
        if (state == GameState.RUNNING) {
            return;
        }
        startTrialWithCurrentCondition();
    }

    public void onTryAgainButton() {
        if (state == GameState.RUNNING) {
            JOptionPane.showMessageDialog(this, "게임이 진행 중입니다. 먼저 Game Over가 되도록 하세요.");
            return;
        }
        if (currentConditionIndex >= conditions.size() - 1) {
            JOptionPane.showMessageDialog(this,
                    "이 참가자는 모든 condition(12개)을 완료했습니다.\nNew Game 버튼으로 다음 참가자로 넘어가세요.");
            return;
        }
        currentConditionIndex++;
        currentCondition = conditions.get(currentConditionIndex);
        applyCurrentCondition();
        startTrialWithCurrentCondition();
    }

    public void onNewGameButton() {
        if (state == GameState.RUNNING) {
            JOptionPane.showMessageDialog(this, "게임이 진행 중입니다. 먼저 Game Over가 되도록 하세요.");
            return;
        }
        askNewParticipantId();
    }

    public void onExitButton() {
        System.exit(0);
    }

    private void startTrialWithCurrentCondition() {
        resetGameWithoutStarting();

        int startX = WIDTH + 200;
        for (int i = 0; i < 3; i++) {
            int x = startX + i * PIPE_DISTANCE;
            pipes.add(createRandomPipe(x));
        }

        gameOver = false;
        running = true;
        survivalTime = 0L;
        trialStartTime = System.currentTimeMillis();
        state = GameState.RUNNING;

        System.out.println("=========================================");
        System.out.println("Participant: " + participantId);
        System.out.println("Trial " + trialIndex + " START");
        System.out.println("Condition   : " + currentCondition.name);
        System.out.println("Jump Power  : " + JUMP_POWER);
        System.out.println("Pipe Dist   : " + PIPE_DISTANCE);
        System.out.println("Hole Size   : " + HOLE_SIZE);
        System.out.println("=========================================");

        timer.start();
    }

    private Pipe createRandomPipe(int x) {
        int minGapY = 100;
        int maxGapY = HEIGHT - 200 - HOLE_SIZE;
        if (maxGapY < minGapY) {
            maxGapY = minGapY + 10;
        }
        int gapY = minGapY + random.nextInt(Math.max(1, maxGapY - minGapY));
        return new Pipe(x, gapY, PIPE_WIDTH, HOLE_SIZE);
    }

    private void endTrial() {
        if (!gameOver && running) {
            survivalTime = System.currentTimeMillis() - trialStartTime;
            System.out.println("Trial " + trialIndex + " END");
            System.out.println("Survival Time (ms): " + survivalTime);
            System.out.println("Survival Time (s) : " + (survivalTime / 1000.0));
            System.out.println("Condition         : " + currentCondition.name);
            System.out.println("=========================================");

            logTrialToCSV();
            trialIndex++;
        }
        gameOver = true;
        running = false;
        timer.stop();
        state = GameState.GAME_OVER;
        repaint();
    }

    private void logTrialToCSV() {
        File file = new File("results.csv");
        boolean writeHeader = !file.exists() || file.length() == 0;

        try (FileWriter fw = new FileWriter(file, true)) {
            if (writeHeader) {
                fw.write("participantId,trialIndex,conditionName,jumpPower,pipeDistance,holeSize,survivalTimeMs\n");
            }
            fw.write(
                    participantId + ","
                    + trialIndex + ","
                    + currentCondition.name + ","
                    + JUMP_POWER + ","
                    + PIPE_DISTANCE + ","
                    + HOLE_SIZE + ","
                    + survivalTime + "\n"
            );
        } catch (IOException e) {
            System.err.println("Error writing CSV: " + e.getMessage());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) {
            return;
        }

        birdVelocity += gravity;
        if (birdVelocity > maxFallSpeed) {
            birdVelocity = maxFallSpeed;
        }
        birdY += birdVelocity;

        updatePipes();
        checkCollisions();
        updateScore();
        repaint();
    }

    private void updatePipes() {
        for (Pipe pipe : pipes) {
            pipe.x -= 3;
        }

        if (!pipes.isEmpty() && pipes.get(0).x + PIPE_WIDTH < 0) {
            pipes.remove(0);
        }

        if (!pipes.isEmpty()) {
            Pipe last = pipes.get(pipes.size() - 1);
            if (last.x + PIPE_WIDTH + PIPE_DISTANCE < WIDTH) {
                pipes.add(createRandomPipe(last.x + PIPE_DISTANCE));
            }
        } else {
            pipes.add(createRandomPipe(WIDTH + 100));
        }
    }

    private void checkCollisions() {
        int birdCenterX = birdX;
        int birdCenterY = (int) birdY;

        if (birdCenterY - birdRadius <= 0 || birdCenterY + birdRadius >= HEIGHT - 50) {
            endTrial();
            return;
        }

        for (Pipe pipe : pipes) {
            int pipeLeft = pipe.x;
            int pipeRight = pipe.x + pipe.width;

            if (birdCenterX + birdRadius > pipeLeft && birdCenterX - birdRadius < pipeRight) {
                int topPipeBottom = pipe.gapY;
                int bottomPipeTop = pipe.gapY + pipe.holeSize;

                if (birdCenterY - birdRadius < topPipeBottom || birdCenterY + birdRadius > bottomPipeTop) {
                    endTrial();
                    return;
                }
            }
        }
    }

    private void updateScore() {
        for (Pipe pipe : pipes) {
            int pipeCenter = pipe.x + pipe.width / 2;
            if (!pipe.passed && birdX > pipeCenter) {
                pipe.passed = true;
                score++;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(160, 216, 239));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(new Color(210, 238, 252));
        g2.fillRect(0, HEIGHT - 160, WIDTH, 40);
        g2.setColor(new Color(190, 230, 245));
        g2.fillRect(0, HEIGHT - 140, WIDTH, 40);

        g2.setColor(new Color(222, 184, 135));
        g2.fillRect(0, HEIGHT - 100, WIDTH, 100);

        g2.setColor(new Color(70, 170, 70));
        for (Pipe pipe : pipes) {

            g2.fillRoundRect(pipe.x, 0, pipe.width, pipe.gapY, 8, 8);

            g2.fillRoundRect(pipe.x, pipe.gapY + pipe.holeSize,
                    pipe.width, HEIGHT - (pipe.gapY + pipe.holeSize) - 100, 8, 8);

            g2.setColor(new Color(40, 130, 40));
            g2.drawRoundRect(pipe.x, 0, pipe.width, pipe.gapY, 8, 8);
            g2.drawRoundRect(pipe.x, pipe.gapY + pipe.holeSize,
                    pipe.width, HEIGHT - (pipe.gapY + pipe.holeSize) - 100, 8, 8);
            g2.setColor(new Color(70, 170, 70));
        }
        g2.setColor(Color.YELLOW);
        g2.fillOval(birdX - birdRadius, (int) birdY - birdRadius, birdRadius * 2, birdRadius * 2);
        g2.setColor(Color.BLACK);
        g2.drawOval(birdX - birdRadius, (int) birdY - birdRadius, birdRadius * 2, birdRadius * 2);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        String scoreText = String.valueOf(score);
        int sw = g2.getFontMetrics().stringWidth(scoreText);
        g2.drawString(scoreText, WIDTH / 2 - sw / 2, 60);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.setColor(Color.BLACK);
        g2.drawString("Participant: " + participantId, 10, 20);
        g2.drawString("Cond: " + currentCondition.name
                + " (" + (currentConditionIndex + 1) + "/" + conditions.size() + ")",
                10, 36);

        if (state == GameState.MENU) {
            drawTitleScreen(g2);
        } else if (state == GameState.GAME_OVER) {
            drawGameOverScreen(g2);
        } else {
            // Running: show condition parameters at bottom
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString(
                    "Jump: " + JUMP_POWER
                    + "  Dist: " + PIPE_DISTANCE
                    + "  Hole: " + HOLE_SIZE,
                    10, HEIGHT - 10
            );
        }

        g2.dispose();
    }

    private void drawTitleScreen(Graphics2D g2) {

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 42));
        String title = "FlappyBird";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (WIDTH - fm.stringWidth(title)) / 2;
        int ty = HEIGHT / 3;
        g2.drawString(title, tx, ty);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String sub = "Click START to begin";
        int sx = (WIDTH - g2.getFontMetrics().stringWidth(sub)) / 2;
        g2.drawString(sub, sx, ty + 30);

        drawButton(g2, btnStart, "Start");
        drawButton(g2, btnNewGame, "New Game");
        drawButton(g2, btnExit, "Exit");
    }

    private void drawGameOverScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(Color.ORANGE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 38));
        String text = "GAME OVER";
        FontMetrics fm = g2.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        int y = HEIGHT / 3;
        g2.drawString(text, x, y);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String sub = "Last Survival: " + String.format("%.2f", survivalTime / 1000.0) + " s";
        int sx = (WIDTH - g2.getFontMetrics().stringWidth(sub)) / 2;
        g2.drawString(sub, sx, y + 30);

        drawButton(g2, btnTryAgain, "Try Again");
        drawButton(g2, btnNewGame, "New Game");
        drawButton(g2, btnExit, "Exit");
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String label) {
        g2.setColor(new Color(70, 200, 110));
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 18, 18);
        g2.setColor(new Color(40, 120, 70));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 18, 18);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int ty = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(label, tx, ty);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_SPACE) {
            if (state == GameState.RUNNING && !gameOver) {
                birdVelocity = -JUMP_POWER;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();

        if (state == GameState.MENU) {
            if (btnStart.contains(p)) {
                onStartButton();
            } else if (btnNewGame.contains(p)) {
                onNewGameButton();
            } else if (btnExit.contains(p)) {
                onExitButton();
            }
        } else if (state == GameState.GAME_OVER) {
            if (btnTryAgain.contains(p)) {
                onTryAgainButton();
            } else if (btnNewGame.contains(p)) {
                onNewGameButton();
            } else if (btnExit.contains(p)) {
                onExitButton();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}

class Pipe {

    int x;
    int gapY;
    int width;
    int holeSize;
    boolean passed = false;

    public Pipe(int x, int gapY, int width, int holeSize) {
        this.x = x;
        this.gapY = gapY;
        this.width = width;
        this.holeSize = holeSize;
    }
}
