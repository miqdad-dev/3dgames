package com.perspectiveracer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Game surface that renders the pseudo-3D highway and manages the game loop.
 */
@SuppressWarnings("serial")
public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 640;

    private static final int PLAYER_WIDTH = 64;
    private static final int PLAYER_HEIGHT = 118;

    private static final int ENEMY_WIDTH = 58;
    private static final int ENEMY_HEIGHT = 104;

    private static final int ROAD_TOP_WIDTH = (int) (PANEL_WIDTH * 0.45);
    private static final int ROAD_BOTTOM_WIDTH = (int) (PANEL_WIDTH * 0.88);

    private static final int ROAD_TOP_X = (PANEL_WIDTH - ROAD_TOP_WIDTH) / 2;
    private static final int ROAD_BOTTOM_LEFT_X = (PANEL_WIDTH - ROAD_BOTTOM_WIDTH) / 2;
    private static final int ROAD_BOTTOM_RIGHT_X = ROAD_BOTTOM_LEFT_X + ROAD_BOTTOM_WIDTH;

    private static final int FRAME_DELAY_MS = 16;

    private static final double PERSPECTIVE_OFFSET = 240.0;

    private final Timer timer;
    private final Random random = new Random();

    private final List<HighwayCar> incomingCars = new ArrayList<>();
    private final List<RoadStripe> roadStripes = new ArrayList<>();

    private int playerLane = 1;
    private double playerX;
    private double targetLaneX;
    private int playerBaselineY;
    private boolean playerPositionInitialised;

    private boolean paused;
    private boolean gameOver;

    private int score;
    private int level = 1;
    private double speed = 5.0;
    private int spawnCooldown;
    private int framesSinceSpawn;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(20, 24, 44));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);

        timer = new Timer(FRAME_DELAY_MS, this);

        playerBaselineY = PANEL_HEIGHT - 80;
        calculateLaneCenters();
        updatePlayerTargets();
    }

    public void start() {
        resetGame();
        timer.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void calculateLaneCenters() {
        // Lane centers are evaluated dynamically through the perspective helpers, but we
        // still initialise the player's anchor position here to keep the flow explicit.
        updatePlayerTargets();
    }

    private void resetGame() {
        incomingCars.clear();
        roadStripes.clear();
        playerLane = 1;
        playerPositionInitialised = false;
        paused = false;
        gameOver = false;
        score = 0;
        level = 1;
        speed = 5.0;
        spawnCooldown = 50;
        framesSinceSpawn = spawnCooldown;
        initialiseStripes();
        updatePlayerTargets();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (paused || gameOver) {
            repaint();
            return;
        }

        updatePlayer();
        updateCars();
        updateStripes();
        spawnCars();
        checkForCollisions();
        repaint();
    }

    private void updatePlayer() {
        if (paused || gameOver) {
            return;
        }

        double easing = 0.2;
        playerX += (targetLaneX - playerX) * easing;
    }

    private void updateCars() {
        Iterator<HighwayCar> iterator = incomingCars.iterator();
        while (iterator.hasNext()) {
            HighwayCar car = iterator.next();
            car.move(speed);
            double screenY = projectedY(car.getBaselineY());
            if (screenY - getProjectedCarHeight(ENEMY_HEIGHT, 1.0) > PANEL_HEIGHT + 40) {
                iterator.remove();
                score++;
                if (score % 10 == 0) {
                    level++;
                    speed += 1.2;
                    spawnCooldown = Math.max(20, spawnCooldown - 5);
                }
            }
        }
        framesSinceSpawn++;
    }

    private void updateStripes() {
        double stripeSpeed = speed * 1.1;
        for (RoadStripe stripe : roadStripes) {
            stripe.move(stripeSpeed);
            if (stripe.getBaselineY() > PANEL_HEIGHT + 80) {
                stripe.wrap(stripe.getBaselineY() - (PANEL_HEIGHT + 200));
            }
        }
    }

    private void spawnCars() {
        if (framesSinceSpawn < spawnCooldown) {
            return;
        }

        int lane = random.nextInt(3);
        if (!incomingCars.isEmpty()) {
            HighwayCar last = incomingCars.get(incomingCars.size() - 1);
            if (Math.abs(last.getLane() - lane) == 0 && last.getBaselineY() < ENEMY_HEIGHT * 1.6) {
                lane = (lane + 1 + random.nextInt(2)) % 3;
            }
        }

        double spawnY = -ENEMY_HEIGHT - random.nextInt(160);
        incomingCars.add(new HighwayCar(lane, spawnY));
        framesSinceSpawn = 0;
    }

    private void checkForCollisions() {
        Rectangle playerBounds = createPlayerBounds();
        for (HighwayCar car : incomingCars) {
            Rectangle enemyBounds = createEnemyBounds(car);
            if (playerBounds.intersects(enemyBounds)) {
                gameOver = true;
                paused = true;
                break;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackdrop(g2d);
        drawRoad(g2d);
        drawStripes(g2d);
        drawPlayer(g2d);
        drawEnemies(g2d);
        drawHud(g2d);
        drawStatus(g2d);

        g2d.dispose();
    }

    private void drawBackdrop(Graphics2D g2d) {
        GradientPaint dusk = new GradientPaint(0, 0, new Color(23, 32, 72), 0, PANEL_HEIGHT,
                new Color(10, 12, 24));
        g2d.setPaint(dusk);
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(new Color(255, 197, 110, 180));
        g2d.fillOval(PANEL_WIDTH / 2 - 70, 20, 140, 140);

        g2d.setColor(new Color(16, 26, 52));
        int mountainBaseY = 200;
        int[] leftX = {0, 110, 260};
        int[] leftY = {mountainBaseY, 60, mountainBaseY};
        g2d.fillPolygon(leftX, leftY, leftX.length);

        int[] rightX = {PANEL_WIDTH, PANEL_WIDTH - 140, PANEL_WIDTH - 280};
        int[] rightY = {mountainBaseY, 80, mountainBaseY};
        g2d.fillPolygon(rightX, rightY, rightX.length);
    }

    private void drawRoad(Graphics2D g2d) {
        java.awt.Polygon road = new java.awt.Polygon(
                new int[] {ROAD_TOP_X, ROAD_TOP_X + ROAD_TOP_WIDTH, ROAD_BOTTOM_RIGHT_X, ROAD_BOTTOM_LEFT_X},
                new int[] {0, 0, PANEL_HEIGHT, PANEL_HEIGHT},
                4);
        LinearGradientPaint asphaltPaint = new LinearGradientPaint(
                0, 0, 0, PANEL_HEIGHT,
                new float[] {0f, 1f},
                new Color[] {new Color(30, 30, 34), new Color(18, 18, 22)});
        g2d.setPaint(asphaltPaint);
        g2d.fillPolygon(road);

        g2d.setStroke(new BasicStroke(6f));
        g2d.setColor(new Color(210, 196, 138));
        g2d.drawLine(ROAD_TOP_X, 0, ROAD_BOTTOM_LEFT_X, PANEL_HEIGHT);
        g2d.drawLine(ROAD_TOP_X + ROAD_TOP_WIDTH, 0, ROAD_BOTTOM_RIGHT_X, PANEL_HEIGHT);

        drawShoulders(g2d);
    }

    private void drawShoulders(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.setColor(new Color(154, 114, 68));
        double segmentLength = 120.0;
        for (double baseline = -segmentLength; baseline < PANEL_HEIGHT + segmentLength; baseline += segmentLength) {
            double nextBaseline = baseline + segmentLength;
            double depthTop = depthForBaseline(baseline);
            double depthBottom = depthForBaseline(nextBaseline);

            int leftTop = (int) Math.round(roadEdgeLeft(depthTop));
            int leftBottom = (int) Math.round(roadEdgeLeft(depthBottom));
            int rightTop = (int) Math.round(roadEdgeRight(depthTop));
            int rightBottom = (int) Math.round(roadEdgeRight(depthBottom));

            int topY = (int) Math.round(projectedY(baseline));
            int bottomY = (int) Math.round(projectedY(nextBaseline));

            g2d.drawLine(leftTop, topY, leftBottom, bottomY);
            g2d.drawLine(rightTop, topY, rightBottom, bottomY);
        }
    }

    private void drawStripes(Graphics2D g2d) {
        g2d.setColor(new Color(233, 220, 156));
        for (RoadStripe stripe : roadStripes) {
            int[] xPoints = new int[4];
            int[] yPoints = new int[4];

            double topDepth = depthForBaseline(stripe.getBaselineY());
            double bottomDepth = depthForBaseline(stripe.getBaselineY() + stripe.getLength());

            double topCenter = roadCenter(topDepth);
            double bottomCenter = roadCenter(bottomDepth);

            double halfTop = getStripeHalfWidth(topDepth);
            double halfBottom = getStripeHalfWidth(bottomDepth);

            int topY = (int) Math.round(projectedY(stripe.getBaselineY()));
            int bottomY = (int) Math.round(projectedY(stripe.getBaselineY() + stripe.getLength()));

            xPoints[0] = (int) Math.round(topCenter - halfTop);
            yPoints[0] = topY;
            xPoints[1] = (int) Math.round(topCenter + halfTop);
            yPoints[1] = topY;
            xPoints[2] = (int) Math.round(bottomCenter + halfBottom);
            yPoints[2] = bottomY;
            xPoints[3] = (int) Math.round(bottomCenter - halfBottom);
            yPoints[3] = bottomY;

            g2d.fillPolygon(xPoints, yPoints, 4);
        }
    }

    private void drawPlayer(Graphics2D g2d) {
        Rectangle bounds = createPlayerBounds();
        double depth = depthForBaseline(playerBaselineY);
        double lean = targetLaneX - playerX;
        drawCar(g2d, bounds, new Color(208, 93, 76), new Color(240, 240, 240), depth, lean);
    }

    private void drawEnemies(Graphics2D g2d) {
        for (HighwayCar car : incomingCars) {
            Rectangle bounds = createEnemyBounds(car);
            double depth = depthForBaseline(car.getBaselineY());
            drawCar(g2d, bounds, new Color(87, 141, 177), new Color(26, 33, 46, 160), depth, 0);
        }
    }

    private void drawHud(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Level: " + level, 20, 55);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.drawString("Speed: " + String.format("%.1f", speed), 20, 78);
        g2d.drawString("Use \u2190 and \u2192 to switch lanes", 20, PANEL_HEIGHT - 50);
        g2d.drawString("Press P to pause/resume", 20, PANEL_HEIGHT - 30);
    }

    private void drawStatus(Graphics2D g2d) {
        if (paused || gameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 36));
            String message = gameOver ? "Collision!" : "Paused";
            int textWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (PANEL_WIDTH - textWidth) / 2, PANEL_HEIGHT / 2 - 20);

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
            String info = gameOver ? "Press Enter to restart" : "Press P to continue";
            int infoWidth = g2d.getFontMetrics().stringWidth(info);
            g2d.drawString(info, (PANEL_WIDTH - infoWidth) / 2, PANEL_HEIGHT / 2 + 20);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            movePlayer(-1);
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            movePlayer(1);
        } else if (e.getKeyCode() == KeyEvent.VK_P) {
            if (!gameOver) {
                paused = !paused;
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (gameOver) {
                resetGame();
            }
        }
    }

    private void movePlayer(int direction) {
        if (paused && !gameOver) {
            return;
        }
        if (gameOver) {
            return;
        }
        int newLane = playerLane + direction;
        if (newLane >= 0 && newLane < 3) {
            playerLane = newLane;
            updatePlayerTargets();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used
    }

    private void updatePlayerTargets() {
        double depth = depthForBaseline(playerBaselineY);
        targetLaneX = laneCenterAtDepth(playerLane, depth);
        if (!playerPositionInitialised || Double.isNaN(playerX)) {
            playerX = targetLaneX;
            playerPositionInitialised = true;
        }
    }

    private Rectangle createPlayerBounds() {
        double depth = depthForBaseline(playerBaselineY);
        return createCarBounds(playerX, PLAYER_WIDTH, PLAYER_HEIGHT, depth, playerBaselineY);
    }

    private Rectangle createEnemyBounds(HighwayCar car) {
        double depth = depthForBaseline(car.getBaselineY());
        double laneCenter = laneCenterAtDepth(car.getLane(), depth);
        return createCarBounds(laneCenter, ENEMY_WIDTH, ENEMY_HEIGHT, depth, car.getBaselineY());
    }

    private Rectangle createCarBounds(double centerX, double baseWidth, double baseHeight,
            double depth, double baselineY) {
        int width = (int) Math.round(getProjectedCarWidth(baseWidth, depth));
        int height = (int) Math.round(getProjectedCarHeight(baseHeight, depth));
        int x = (int) Math.round(centerX - width / 2.0);
        int y = (int) Math.round(projectedY(baselineY) - height);
        return new Rectangle(x, y, width, height);
    }

    private double depthForBaseline(double baselineY) {
        return clamp(projectedY(baselineY) / PANEL_HEIGHT, 0.0, 1.0);
    }

    private double projectedY(double baselineY) {
        double depth = clamp((baselineY + PERSPECTIVE_OFFSET) / (PANEL_HEIGHT + PERSPECTIVE_OFFSET), 0.0, 1.0);
        double curved = Math.pow(depth, 1.25);
        return curved * PANEL_HEIGHT;
    }

    private double laneCenterAtDepth(int lane, double depth) {
        double leftEdge = roadEdgeLeft(depth);
        double rightEdge = roadEdgeRight(depth);
        double laneWidth = (rightEdge - leftEdge) / 3.0;
        return leftEdge + laneWidth * (lane + 0.5);
    }

    private double roadCenter(double depth) {
        return (roadEdgeLeft(depth) + roadEdgeRight(depth)) / 2.0;
    }

    private double roadEdgeLeft(double depth) {
        return ROAD_TOP_X + (ROAD_BOTTOM_LEFT_X - ROAD_TOP_X) * depth;
    }

    private double roadEdgeRight(double depth) {
        return ROAD_TOP_X + ROAD_TOP_WIDTH + (ROAD_BOTTOM_RIGHT_X - (ROAD_TOP_X + ROAD_TOP_WIDTH)) * depth;
    }

    private double getProjectedCarWidth(double baseWidth, double depth) {
        double minScale = 0.35;
        double scale = minScale + (1.0 - minScale) * depth;
        return baseWidth * scale;
    }

    private double getProjectedCarHeight(double baseHeight, double depth) {
        double minScale = 0.38;
        double scale = minScale + (1.0 - minScale) * depth;
        return baseHeight * scale;
    }

    private double getStripeHalfWidth(double depth) {
        double roadWidth = roadEdgeRight(depth) - roadEdgeLeft(depth);
        return Math.max(4, roadWidth * 0.025);
    }

    private void drawCar(Graphics2D g2d, Rectangle bounds, Color bodyColor, Color canopyColor,
            double depth, double lean) {
        int arc = (int) Math.max(14, Math.min(bounds.width, bounds.height) * 0.25);

        g2d.setColor(bodyColor);
        g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);

        Paint sheen = new LinearGradientPaint(
                bounds.x, bounds.y,
                bounds.x, bounds.y + bounds.height,
                new float[] {0f, 1f},
                new Color[] {
                        bodyColor.brighter(),
                        bodyColor.darker()
                });
        g2d.setPaint(sheen);
        g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);

        int canopyHeight = (int) Math.round(bounds.height * 0.45);
        int canopyWidth = (int) Math.round(bounds.width * 0.66);
        int canopyX = bounds.x + (bounds.width - canopyWidth) / 2 + (int) Math.round(lean * 0.05);
        int canopyY = bounds.y + (int) Math.round(bounds.height * 0.22);

        g2d.setColor(canopyColor);
        g2d.fillRoundRect(canopyX, canopyY, canopyWidth, canopyHeight, arc / 2, arc / 2);

        int lightWidth = (int) Math.max(6, bounds.width * 0.2);
        int lightHeight = (int) Math.max(5, bounds.height * 0.08);
        int lightY = bounds.y + bounds.height - lightHeight - 4;
        g2d.setColor(new Color(248, 222, 109));
        g2d.fillRoundRect(bounds.x + 6, lightY, lightWidth, lightHeight, 6, 6);
        g2d.fillRoundRect(bounds.x + bounds.width - lightWidth - 6, lightY, lightWidth, lightHeight, 6, 6);

        int tailWidth = (int) Math.max(6, bounds.width * 0.18);
        int tailHeight = (int) Math.max(5, bounds.height * 0.07);
        int tailY = bounds.y + 6;
        g2d.setColor(new Color(220, 72, 72, (int) Math.min(255, 120 + depth * 80)));
        g2d.fillRoundRect(bounds.x + 6, tailY, tailWidth, tailHeight, 6, 6);
        g2d.fillRoundRect(bounds.x + bounds.width - tailWidth - 6, tailY, tailWidth, tailHeight, 6, 6);
    }

    private void initialiseStripes() {
        double spacing = PANEL_HEIGHT / 10.0;
        double length = 90;
        for (int i = 0; i < 12; i++) {
            double startY = i * spacing - length;
            roadStripes.add(new RoadStripe(startY, length));
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class HighwayCar {
        private final int lane;
        private double baselineY;

        HighwayCar(int lane, double startY) {
            this.lane = lane;
            this.baselineY = startY;
        }

        int getLane() {
            return lane;
        }

        double getBaselineY() {
            return baselineY;
        }

        void move(double distance) {
            baselineY += distance;
        }
    }

    private static final class RoadStripe {
        private double baselineY;
        private final double length;

        RoadStripe(double baselineY, double length) {
            this.baselineY = baselineY;
            this.length = length;
        }

        double getBaselineY() {
            return baselineY;
        }

        double getLength() {
            return length;
        }

        void move(double distance) {
            baselineY += distance;
        }

        void wrap(double offset) {
            baselineY = offset;
        }
    }
}
