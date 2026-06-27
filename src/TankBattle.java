import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Tank Battle
 *
 * Debug: run Main.java in IDE (or run this class's main method directly).
 * Package: use build.bat / build.ps1 in project root to generate TankBattle.jar,
 *          then run with java -jar TankBattle.jar.
 *
 * Controls:
 *   W A S D / Arrow keys : Move
 *   Space                : Fire
 *   P                    : Pause / Resume
 *   R                    : Restart
 *   ESC                  : Quit
 */
public class TankBattle extends JPanel {

    // ===== Constants =====
    static final int CELL = 32;            // pixel size per grid cell
    static final int COLS = 15;            // map columns
    static final int ROWS = 15;            // map rows
    static final int HUD = 44;             // top info bar height
    static final int W = COLS * CELL;      // game area width
    static final int H = ROWS * CELL + HUD;// total height

    // Direction
    enum Dir { UP, DOWN, LEFT, RIGHT }

    // Map tile types
    enum Tile { EMPTY, BRICK, STEEL, BASE }

    // ===== Game Objects =====
    static final int TANK = 28;            // tank size
    static final int BULLET = 6;           // bullet size

    class Tank {
        int x, y;
        Dir dir;
        int speed;
        boolean enemy;
        long lastShot;     // last fire time (ms)
        int cooldown;      // fire cooldown (ms)
        long aiTurn;       // AI next turn time
        boolean moving;
        Tank(int x, int y, Dir dir, boolean enemy) {
            this.x = x; this.y = y; this.dir = dir;
            this.speed = enemy ? 2 : 3;
            this.enemy = enemy;
            this.cooldown = enemy ? 900 : 350;
        }
        Rectangle rect() { return new Rectangle(x, y, TANK, TANK); }
        void fire() {
            long now = System.currentTimeMillis();
            if (now - lastShot < cooldown) return;
            lastShot = now;
            int bx = x + TANK/2 - BULLET/2;
            int by = y + TANK/2 - BULLET/2;
            switch (dir) {
                case UP:    by = y - BULLET; break;
                case DOWN:  by = y + TANK; break;
                case LEFT:  bx = x - BULLET; break;
                case RIGHT: bx = x + TANK; break;
            }
            bullets.add(new Bullet(bx, by, dir, enemy));
        }
    }

    class Bullet {
        int x, y;
        Dir dir;
        int speed;
        boolean enemy;
        boolean dead;
        Bullet(int x, int y, Dir dir, boolean enemy) {
            this.x = x; this.y = y; this.dir = dir;
            this.speed = 6;
            this.enemy = enemy;
        }
        Rectangle rect() { return new Rectangle(x, y, BULLET, BULLET); }
        void step() {
            switch (dir) {
                case UP:    y -= speed; break;
                case DOWN:  y += speed; break;
                case LEFT:  x -= speed; break;
                case RIGHT: x += speed; break;
            }
        }
    }

    class Explosion {
        int x, y;
        long born;
        Explosion(int x, int y) { this.x = x; this.y = y; born = System.currentTimeMillis(); }
        boolean done() { return System.currentTimeMillis() - born > 280; }
    }

    // ===== Game State =====
    Tile[][] map = new Tile[ROWS][COLS];
    List<Tank> tanks = new ArrayList<>();
    List<Bullet> bullets = new ArrayList<>();
    List<Explosion> explosions = new ArrayList<>();

    Tank player;
    int score = 0;
    int lives = 3;
    int enemiesRemaining = 12;     // remaining enemies in this level
    int enemiesOnField = 0;
    int maxOnField = 4;
    boolean gameOver = false;
    boolean win = false;
    boolean paused = false;
    long lastSpawn;

    // Input
    final Set<Integer> keys = new HashSet<>();

    // Random seed for map generation (fixed level for debugging)
    final Random rnd = new Random(42);

    TankBattle() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setupKeyBindings();
        reset();
        // Main loop ~60fps
        new javax.swing.Timer(16, e -> tick()).start();
    }

    /** Bind keys via InputMap/ActionMap, responds as long as the window has focus. */
    void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        // Move + fire keys: add to set on press, remove on release
        int[] moveKeys = {
            KeyEvent.VK_W, KeyEvent.VK_UP,
            KeyEvent.VK_S, KeyEvent.VK_DOWN,
            KeyEvent.VK_A, KeyEvent.VK_LEFT,
            KeyEvent.VK_D, KeyEvent.VK_RIGHT,
            KeyEvent.VK_SPACE,
        };
        for (int code : moveKeys) {
            String pressName = "press." + code;
            String releaseName = "release." + code;
            im.put(KeyStroke.getKeyStroke(code, 0, false), pressName);
            im.put(KeyStroke.getKeyStroke(code, 0, true), releaseName);
            am.put(pressName, new AbstractAction() {
                public void actionPerformed(ActionEvent e) { keys.add(code); }
            });
            am.put(releaseName, new AbstractAction() {
                public void actionPerformed(ActionEvent e) { keys.remove(code); }
            });
        }
        // Single-trigger function keys
        bindAction(im, am, KeyEvent.VK_P, false, e -> paused = !paused);
        bindAction(im, am, KeyEvent.VK_R, false, e -> reset());
        bindAction(im, am, KeyEvent.VK_ESCAPE, false, e -> System.exit(0));
    }

    void bindAction(InputMap im, ActionMap am, int code, boolean onRelease, java.util.function.Consumer<ActionEvent> action) {
        String name = "action." + code;
        im.put(KeyStroke.getKeyStroke(code, 0, onRelease), name);
        am.put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { action.accept(e); }
        });
    }

    // ===== Init / Reset =====
    void reset() {
        tanks.clear();
        bullets.clear();
        explosions.clear();
        score = 0;
        lives = 3;
        enemiesRemaining = 12;
        enemiesOnField = 0;
        gameOver = false;
        win = false;
        paused = false;
        buildMap();
        // Player spawns at bottom center
        player = new Tank((COLS/2)*CELL + (CELL-TANK)/2, (ROWS-2)*CELL + (CELL-TANK)/2, Dir.UP, false);
        tanks.add(player);
        lastSpawn = 0;
    }

    void buildMap() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                map[r][c] = Tile.EMPTY;

        // Outer steel walls
        for (int i = 0; i < COLS; i++) { map[0][i] = Tile.STEEL; map[ROWS-1][i] = Tile.STEEL; }
        for (int i = 0; i < ROWS; i++) { map[i][0] = Tile.STEEL; map[i][COLS-1] = Tile.STEEL; }

        // Base (eagle) placed at bottom center
        int baseR = ROWS - 2, baseC = COLS / 2;
        map[baseR][baseC] = Tile.BASE;
        // Brick walls protecting the base
        int[][] guard = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1}};
        for (int[] d : guard) {
            int r = baseR + d[0], c = baseC + d[1];
            if (map[r][c] == Tile.EMPTY) map[r][c] = Tile.BRICK;
        }

        // Randomly scatter brick and steel walls
        for (int r = 2; r < ROWS - 2; r++) {
            for (int c = 2; c < COLS - 2; c++) {
                if (map[r][c] != Tile.EMPTY) continue;
                // Avoid player spawn area and enemy spawn areas
                if (r >= ROWS - 3 && Math.abs(c - COLS/2) <= 1) continue;
                if (r <= 2 && (c <= 2 || c >= COLS - 3)) continue;
                double v = rnd.nextDouble();
                if (v < 0.18) map[r][c] = Tile.BRICK;
                else if (v < 0.24) map[r][c] = Tile.STEEL;
            }
        }
        // Add symmetric steel walls in center as cover
        map[ROWS/2][COLS/2] = Tile.STEEL;
        map[ROWS/2][COLS/2 - 1] = Tile.BRICK;
        map[ROWS/2][COLS/2 + 1] = Tile.BRICK;
    }

    // ===== Main Loop =====
    void tick() {
        if (!paused && !gameOver && !win) {
            handleInput();
            updateTanks();
            updateBullets();
            spawnEnemies();
            checkWin();
        }
        explosions.removeIf(Explosion::done);
        repaint();
    }

    void handleInput() {
        boolean up = keys.contains(KeyEvent.VK_W) || keys.contains(KeyEvent.VK_UP);
        boolean down = keys.contains(KeyEvent.VK_S) || keys.contains(KeyEvent.VK_DOWN);
        boolean left = keys.contains(KeyEvent.VK_A) || keys.contains(KeyEvent.VK_LEFT);
        boolean right = keys.contains(KeyEvent.VK_D) || keys.contains(KeyEvent.VK_RIGHT);
        if (up)    { player.dir = Dir.UP;    player.moving = true; }
        else if (down)  { player.dir = Dir.DOWN;  player.moving = true; }
        else if (left)  { player.dir = Dir.LEFT;  player.moving = true; }
        else if (right) { player.dir = Dir.RIGHT; player.moving = true; }
        else player.moving = false;
        if (keys.contains(KeyEvent.VK_SPACE)) player.fire();
    }

    void updateTanks() {
        for (Tank t : tanks) {
            if (t == player) {
                if (player.moving) tryMove(player);
            } else {
                aiControl(t);
            }
        }
    }

    void aiControl(Tank t) {
        long now = System.currentTimeMillis();
        if (now > t.aiTurn) {
            // 30% chance to move toward player, otherwise random
            if (rnd.nextDouble() < 0.3) {
                int dx = player.x - t.x;
                int dy = player.y - t.y;
                if (Math.abs(dx) > Math.abs(dy)) t.dir = dx > 0 ? Dir.RIGHT : Dir.LEFT;
                else t.dir = dy > 0 ? Dir.DOWN : Dir.UP;
            } else {
                Dir[] dirs = Dir.values();
                t.dir = dirs[rnd.nextInt(dirs.length)];
            }
            t.aiTurn = now + 600 + rnd.nextInt(900);
        }
        if (!tryMove(t)) {
            // Turn immediately on hitting a wall
            t.aiTurn = now;
        }
        // Random fire
        if (rnd.nextDouble() < 0.04) t.fire();
    }

    boolean tryMove(Tank t) {
        int nx = t.x, ny = t.y;
        switch (t.dir) {
            case UP:    ny -= t.speed; break;
            case DOWN:  ny += t.speed; break;
            case LEFT:  nx -= t.speed; break;
            case RIGHT: nx += t.speed; break;
        }
        Rectangle nr = new Rectangle(nx, ny, TANK, TANK);
        // Bounds
        if (nx < CELL || ny < CELL || nx + TANK > (COLS-1)*CELL || ny + TANK > (ROWS-1)*CELL)
            return false;
        // Walls
        if (hitsWall(nr)) return false;
        // Other tanks
        for (Tank o : tanks) {
            if (o == t) continue;
            if (nr.intersects(o.rect())) return false;
        }
        t.x = nx; t.y = ny;
        return true;
    }

    boolean hitsWall(Rectangle r) {
        int c0 = r.x / CELL, c1 = (r.x + r.width - 1) / CELL;
        int r0 = r.y / CELL, r1 = (r.y + r.height - 1) / CELL;
        for (int rr = r0; rr <= r1; rr++)
            for (int cc = c0; cc <= c1; cc++) {
                if (rr < 0 || rr >= ROWS || cc < 0 || cc >= COLS) return true;
                Tile t = map[rr][cc];
                if (t == Tile.BRICK || t == Tile.STEEL || t == Tile.BASE) return true;
            }
        return false;
    }

    void updateBullets() {
        List<Tank> toRemove = new ArrayList<>();
        for (Bullet b : bullets) {
            b.step();
            Rectangle br = b.rect();
            // Out of bounds
            if (b.x < 0 || b.y < 0 || b.x > W || b.y > H) { b.dead = true; continue; }
            // Hit wall
            int c = b.x / CELL, r = b.y / CELL;
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                Tile t = map[r][c];
                if (t == Tile.BRICK) {
                    map[r][c] = Tile.EMPTY;
                    b.dead = true;
                    explosions.add(new Explosion(c*CELL+CELL/2, r*CELL+CELL/2));
                    continue;
                } else if (t == Tile.STEEL) {
                    b.dead = true;
                    continue;
                } else if (t == Tile.BASE) {
                    map[r][c] = Tile.EMPTY;
                    b.dead = true;
                    gameOver = true;
                    explosions.add(new Explosion(c*CELL+CELL/2, r*CELL+CELL/2));
                    continue;
                }
            }
            // Hit tank
            for (Tank tk : tanks) {
                if (tk.enemy == b.enemy) continue; // same faction, no damage
                if (toRemove.contains(tk)) continue; // already marked for removal
                if (br.intersects(tk.rect())) {
                    b.dead = true;
                    explosions.add(new Explosion(tk.x + TANK/2, tk.y + TANK/2));
                    if (tk.enemy) {
                        toRemove.add(tk);
                        enemiesOnField--;
                        enemiesRemaining--;
                        score += 100;
                    } else {
                        // Player hit
                        lives--;
                        if (lives <= 0) { gameOver = true; }
                        else respawnPlayer();
                    }
                    break;
                }
            }
        }
        tanks.removeAll(toRemove);
        bullets.removeIf(b -> b.dead);
    }

    void respawnPlayer() {
        player.x = (COLS/2)*CELL + (CELL-TANK)/2;
        player.y = (ROWS-2)*CELL + (CELL-TANK)/2;
        player.dir = Dir.UP;
    }

    void spawnEnemies() {
        long now = System.currentTimeMillis();
        if (enemiesOnField < maxOnField && enemiesRemaining > enemiesOnField
                && now - lastSpawn > 1500) {
            // Three spawn points: top-left, top-center, top-right
            int[] xs = { 1*CELL + (CELL-TANK)/2, (COLS/2)*CELL + (CELL-TANK)/2, (COLS-2)*CELL + (CELL-TANK)/2 };
            int spawnIdx = rnd.nextInt(xs.length);
            int sx = xs[spawnIdx], sy = 1*CELL + (CELL-TANK)/2;
            Rectangle sr = new Rectangle(sx, sy, TANK, TANK);
            boolean blocked = false;
            for (Tank o : tanks) if (sr.intersects(o.rect())) { blocked = true; break; }
            if (!blocked) {
                Tank e = new Tank(sx, sy, Dir.DOWN, true);
                e.aiTurn = now + 500;
                tanks.add(e);
                enemiesOnField++;
                lastSpawn = now;
            }
        }
    }

    void checkWin() {
        if (enemiesRemaining <= 0 && enemiesOnField <= 0) win = true;
    }

    // ===== Rendering =====
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Background
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(0, 0, W, H);

        // Map
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Tile t = map[r][c];
                int x = c * CELL, y = r * CELL;
                if (t == Tile.BRICK) {
                    g2.setColor(new Color(180, 90, 40));
                    g2.fillRect(x, y, CELL, CELL);
                    g2.setColor(new Color(120, 60, 20));
                    for (int i = 0; i < CELL; i += 8) {
                        g2.drawLine(x, y + i, x + CELL, y + i);
                    }
                } else if (t == Tile.STEEL) {
                    g2.setColor(new Color(160, 160, 180));
                    g2.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
                    g2.setColor(new Color(90, 90, 110));
                    g2.drawRect(x + 1, y + 1, CELL - 2, CELL - 2);
                } else if (t == Tile.BASE) {
                    g2.setColor(new Color(220, 200, 60));
                    g2.fillRect(x + 4, y + 4, CELL - 8, CELL - 8);
                    g2.setColor(Color.BLACK);
                    g2.drawString("E", x + CELL/2 - 4, y + CELL/2 + 4);
                }
            }
        }

        // Tanks
        for (Tank t : tanks) drawTank(g2, t);

        // Bullets
        g2.setColor(Color.WHITE);
        for (Bullet b : bullets) g2.fillRect(b.x, b.y, BULLET, BULLET);

        // Explosions
        for (Explosion e : explosions) {
            long age = System.currentTimeMillis() - e.born;
            int radius = (int)(age / 10) + 4;
            g2.setColor(new Color(255, 160, 0, 200));
            g2.fillOval(e.x - radius, e.y - radius, radius * 2, radius * 2);
        }

        // HUD
        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(0, ROWS * CELL, W, HUD);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        int hy = ROWS * CELL + 28;
        g2.drawString("Score: " + score, 10, hy);
        g2.drawString("Lives: " + lives, 110, hy);
        g2.drawString("Enemies: " + enemiesRemaining, 200, hy);
        g2.drawString(paused ? "[PAUSED]" : (gameOver ? "[GAME OVER]" : (win ? "[WIN]" : "[PLAYING]")),
                340, hy);
        g2.drawString("WASD/Arrows:Move  Space:Fire  P:Pause  R:Restart  ESC:Quit", 10, hy + 14);

        // Overlay
        if (gameOver || win) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, W, H);
            g2.setColor(win ? new Color(120, 220, 120) : Color.RED);
            g2.setFont(new Font("Consolas", Font.BOLD, 40));
            String msg = win ? "VICTORY!" : "GAME OVER";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.PLAIN, 16));
            g2.drawString("Press R to restart, ESC to quit.", (W - 220) / 2, H / 2 + 36);
        }
    }

    void drawTank(Graphics2D g2, Tank t) {
        Color body = t.enemy ? new Color(200, 60, 60) : new Color(60, 200, 90);
        Color dark = t.enemy ? new Color(120, 30, 30) : new Color(20, 120, 50);
        g2.setColor(body);
        g2.fillRect(t.x, t.y, TANK, TANK);
        g2.setColor(dark);
        g2.drawRect(t.x, t.y, TANK, TANK);
        // Tracks
        g2.setColor(Color.DARK_GRAY);
        switch (t.dir) {
            case UP: case DOWN:
                g2.fillRect(t.x, t.y + 2, 5, TANK - 4);
                g2.fillRect(t.x + TANK - 5, t.y + 2, 5, TANK - 4);
                break;
            case LEFT: case RIGHT:
                g2.fillRect(t.x + 2, t.y, TANK - 4, 5);
                g2.fillRect(t.x + 2, t.y + TANK - 5, TANK - 4, 5);
                break;
        }
        // Turret
        g2.setColor(dark);
        g2.fillOval(t.x + TANK/2 - 6, t.y + TANK/2 - 6, 12, 12);
        // Barrel
        g2.setColor(Color.BLACK);
        int cx = t.x + TANK/2, cy = t.y + TANK/2;
        switch (t.dir) {
            case UP:    g2.fillRect(cx - 1, t.y - 4, 2, TANK/2 + 4); break;
            case DOWN:  g2.fillRect(cx - 1, cy, 2, TANK/2 + 4); break;
            case LEFT:  g2.fillRect(t.x - 4, cy - 1, TANK/2 + 4, 2); break;
            case RIGHT: g2.fillRect(cx, cy - 1, TANK/2 + 4, 2); break;
        }
    }

    // ===== Entry Point =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tank Battle");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            TankBattle game = new TankBattle();
            frame.setContentPane(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
