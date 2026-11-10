
package com.flappyrehab;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

public class FlappyRehabGame extends ApplicationAdapter {

    private static final float WORLD_W = 1200f;
    private static final float WORLD_H = 674f;

    private static final float PLAYER_X = 220f;
    private static final float PLAYER_R = 17f;
    private static final float PIPE_W = 78f;

    private static final float PHASE_1_SPEED = 150f;
    private static final float PHASE_1_GAP = 230f;
    private static final float PHASE_1_INTERVAL = 2.8f;

    private static final float PHASE_2_SPEED = 210f;
    private static final float PHASE_2_GAP = 190f;
    private static final float PHASE_2_INTERVAL = 2.1f;

    private static final float PHASE_3_SPEED = 270f;
    private static final float PHASE_3_GAP = 160f;
    private static final float PHASE_3_INTERVAL = 1.6f;

    private ShapeRenderer shapes;
    private OrthographicCamera camera;
    private Viewport viewport;

    private SpriteBatch batch;
    private BitmapFont font;

    private SerialDistanceInput serialInput;

    private final Array<PipePair> pipes = new Array<>();

    private float playerY;
    private float verticalVelocity;

    private float pipeTimer;
    private float worldTime;

    private float score;
    private float bestScore;

    private float distanceCm = 25f;
    private float filteredDistance = 25f;
    private float targetDistance = WORLD_H / 2f;

    private float distanceMin = 8f;
    private float distanceMax = 30f;

    private float speed = PHASE_1_SPEED;
    private float gap = PHASE_1_GAP;
    private float spawnInterval = PHASE_1_INTERVAL;

    private int phase = 1;

    // 0 = Menu
    // 1 = Jogando
    // 2 = Fim de jogo
    // 3 = Pausado
    private int state = 0;

    private boolean sensorConnected;

    private String sensorStatus =
            "Teclado: W/S ou setas | Arduino: configure COM3";

    private String lastDistanceText = "--";

    @Override
    public void create() {

        shapes = new ShapeRenderer();

        batch = new SpriteBatch();
        font = new BitmapFont();

        camera = new OrthographicCamera();

        viewport = new FitViewport(WORLD_W, WORLD_H, camera);
        viewport.apply();

        camera.position.set(WORLD_W / 2f, WORLD_H / 2f, 0);
        camera.update();

        String port = System.getProperty("serial.port", "").trim();

        if (!port.isEmpty()) {

            serialInput = new SerialDistanceInput(port);
            serialInput.start();

            sensorStatus = "Conectando ao Arduino em " + port + "...";

        } else {

            sensorStatus =
                    "Modo teste: W/S ou setas | Arduino: COM3";
        }

        resetGame();
    }

    private void resetGame() {

        pipes.clear();

        playerY = WORLD_H * 0.52f;
        verticalVelocity = 0;

        worldTime = 0;
        score = 0;

        phase = 1;

        speed = PHASE_1_SPEED;
        gap = PHASE_1_GAP;
        spawnInterval = PHASE_1_INTERVAL;

        pipeTimer = PHASE_1_INTERVAL;

        addPipe(WORLD_W + 100f);
    }

    private void addPipe(float x) {

        float margin = 105f;

        float center = MathUtils.random(
                margin + gap / 2f,
                WORLD_H - margin - gap / 2f
        );

        pipes.add(new PipePair(x, center, gap));
    }

    @Override
    public void render() {

        float dt = Math.min(
                Gdx.graphics.getDeltaTime(),
                1f / 30f
        );

        updateSensor();
        update(dt);
        draw();
    }

    // Recebe as informações do Arduino
    private void updateSensor() {

        if (serialInput != null) {

            sensorConnected = serialInput.isConnected();

            if (sensorConnected) {

                sensorStatus =
                        "Arduino conectado: " + serialInput.getPortName();

                double d = serialInput.getDistanceCm();

                if (Double.isFinite(d) && d >= 2 && d <= 400) {

                    distanceCm = (float) d;

                    lastDistanceText = String.format(
                            java.util.Locale.US,
                            "%.1f cm",
                            distanceCm
                    );
                }

            } else {

                sensorStatus = serialInput.getStatus();
            }
        }

        filteredDistance +=
                (distanceCm - filteredDistance) * 0.22f;
    }

    private void update(float dt) {

        if (state != 1) {

            if (state == 0 &&
                    Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {

                startGame();
            }

            if (state == 2 &&
                    (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                     Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {

                startGame();
            }

            if (state == 3 &&
                    (Gdx.input.isKeyJustPressed(Input.Keys.P) ||
                     Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) {

                state = 1;
            }

            if (state == 3 &&
                    Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {

                startGame();
            }

            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {

            state = 3;
            return;
        }

        worldTime += dt;

        updatePhase();

        // NOVO: se a mão não estiver dentro da faixa,
        // o personagem retorna ao centro da tela.
        boolean handDetected =
                distanceCm >= distanceMin &&
                distanceCm <= distanceMax;

        if (handDetected) {

            float normalized = MathUtils.clamp(
                    (filteredDistance - distanceMin) /
                            (distanceMax - distanceMin),
                    0f,
                    1f
            );

            targetDistance = MathUtils.lerp(
                    90f,
                    WORLD_H - 90f,
                    normalized
            );

        } else {

            targetDistance = WORLD_H / 2f;
        }

        // Controle pelo teclado para testes
        float keyboard = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.UP) ||
                Gdx.input.isKeyPressed(Input.Keys.W)) {

            keyboard += 1f;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) ||
                Gdx.input.isKeyPressed(Input.Keys.S)) {

            keyboard -= 1f;
        }

        if (keyboard != 0f) {

            verticalVelocity = keyboard * 300f;
            playerY += verticalVelocity * dt;

        } else {

            // Retorno suave ao centro ou acompanhamento da mão
            playerY = MathUtils.lerp(
                    playerY,
                    targetDistance,
                    Math.min(1f, dt * 3.4f)
            );
        }

        playerY = MathUtils.clamp(
                playerY,
                PLAYER_R + 12f,
                WORLD_H - PLAYER_R - 58f
        );

        pipeTimer -= dt;

        if (pipeTimer <= 0) {

            addPipe(WORLD_W + 30f);

            pipeTimer = spawnInterval;
        }

        for (int i = pipes.size - 1; i >= 0; i--) {

            PipePair p = pipes.get(i);

            p.x -= speed * dt;

            if (!p.scored && p.x + PIPE_W < PLAYER_X) {

                p.scored = true;
                score += 1;

                bestScore = Math.max(bestScore, score);
            }

            if (p.x + PIPE_W < -20) {

                pipes.removeIndex(i);
                continue;
            }

            if (collides(p)) {

                state = 2;
                break;
            }
        }

        if (playerY <= PLAYER_R + 12f ||
                playerY >= WORLD_H - PLAYER_R - 58f) {

            state = 2;
        }
    }

    private void updatePhase() {

        int newPhase;

        if (score < 5) {
            newPhase = 1;
        } else if (score < 12) {
            newPhase = 2;
        } else {
            newPhase = 3;
        }

        if (newPhase != phase) {

            phase = newPhase;

            if (phase == 1) {

                speed = PHASE_1_SPEED;
                gap = PHASE_1_GAP;
                spawnInterval = PHASE_1_INTERVAL;

            } else if (phase == 2) {

                speed = PHASE_2_SPEED;
                gap = PHASE_2_GAP;
                spawnInterval = PHASE_2_INTERVAL;

            } else {

                speed = PHASE_3_SPEED;
                gap = PHASE_3_GAP;
                spawnInterval = PHASE_3_INTERVAL;
            }

            pipeTimer = spawnInterval;
        }
    }

    private boolean collides(PipePair p) {

        if (PLAYER_X + PLAYER_R < p.x ||
                PLAYER_X - PLAYER_R > p.x + PIPE_W) {

            return false;
        }

        float gapBottom = p.center - p.gap / 2f;
        float gapTop = p.center + p.gap / 2f;

        return playerY - PLAYER_R < gapBottom ||
                playerY + PLAYER_R > gapTop;
    }

    private void startGame() {

        resetGame();
        state = 1;
    }

    private void draw() {

        if (phase == 1) {

            ScreenUtils.clear(0.40f, 0.78f, 0.94f, 1f);

        } else if (phase == 2) {

            ScreenUtils.clear(0.20f, 0.10f, 0.35f, 1f);

        } else {

            ScreenUtils.clear(0.12f, 0.035f, 0.06f, 1f);
        }

        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawBackground();

        if (phase == 1) {

            shapes.setColor(0.30f, 0.68f, 0.25f, 1f);
            shapes.rect(0, 0, WORLD_W, 42);

            shapes.setColor(0.75f, 0.55f, 0.28f, 1f);
            shapes.rect(0, 0, WORLD_W, 12);

        } else if (phase == 2) {

            shapes.setColor(0.35f, 0.20f, 0.55f, 1f);
            shapes.rect(0, 0, WORLD_W, 42);

            shapes.setColor(0.65f, 0.35f, 0.85f, 1f);
            shapes.rect(0, 0, WORLD_W, 10);

        } else {

            shapes.setColor(0.30f, 0.08f, 0.12f, 1f);
            shapes.rect(0, 0, WORLD_W, 42);

            shapes.setColor(0.95f, 0.15f, 0.18f, 1f);
            shapes.rect(0, 0, WORLD_W, 10);
        }

        for (PipePair p : pipes) {
            drawPipe(p);
        }

        // Personagem
        shapes.setColor(0.98f, 0.76f, 0.18f, 1f);
        shapes.circle(PLAYER_X, playerY, PLAYER_R);

        shapes.setColor(0.98f, 0.91f, 0.63f, 1f);
        shapes.circle(PLAYER_X + 3, playerY + 3, 10);

        shapes.setColor(Color.WHITE);
        shapes.circle(PLAYER_X + 8, playerY + 6, 5);

        shapes.setColor(Color.NAVY);
        shapes.circle(PLAYER_X + 10, playerY + 6, 2.3f);

        shapes.setColor(0.95f, 0.42f, 0.18f, 1f);

        shapes.triangle(
                PLAYER_X + 14, playerY,
                PLAYER_X + 28, playerY - 4,
                PLAYER_X + 14, playerY - 7
        );

        shapes.setColor(0.96f, 0.53f, 0.20f, 1f);

        shapes.ellipse(
                PLAYER_X - 13,
                playerY - 8,
                15,
                8
        );

        shapes.setColor(0.05f, 0.13f, 0.20f, 0.75f);
        shapes.rect(20, WORLD_H - 92, 390, 70);

        shapes.end();

        drawTextOverlay();
    }

    private void drawBackground() {

        if (phase == 1) {

            shapes.setColor(0.88f, 0.96f, 1f, 1f);

            drawCloud(140, 555);
            drawCloud(520, 600);
            drawCloud(930, 540);

            shapes.setColor(0.48f, 0.78f, 0.48f, 1f);

            shapes.circle(100, 70, 130);
            shapes.circle(390, 65, 150);
            shapes.circle(830, 55, 175);
            shapes.circle(1110, 70, 130);

        } else if (phase == 2) {

            shapes.setColor(0.55f, 0.35f, 0.85f, 1f);

            shapes.circle(950, 540, 55);
            shapes.circle(950, 540, 40);

            shapes.setColor(0.35f, 0.20f, 0.55f, 1f);

            shapes.triangle(50, 42, 240, 360, 430, 42);
            shapes.triangle(600, 42, 800, 300, 1000, 42);

            shapes.setColor(0.75f, 0.55f, 1f, 1f);

            shapes.circle(170, 480, 5);
            shapes.circle(420, 550, 4);
            shapes.circle(700, 460, 6);
            shapes.circle(1080, 420, 5);

        } else {

            shapes.setColor(0.35f, 0.07f, 0.12f, 1f);

            shapes.rect(0, 42, WORLD_W, 180);

            shapes.setColor(0.75f, 0.08f, 0.12f, 1f);

            shapes.rect(80, 220, 12, 280);
            shapes.rect(340, 220, 12, 350);
            shapes.rect(700, 220, 12, 260);
            shapes.rect(1050, 220, 12, 330);

            shapes.setColor(1f, 0.15f, 0.12f, 1f);

            shapes.circle(180, 520, 5);
            shapes.circle(480, 450, 5);
            shapes.circle(800, 550, 5);
            shapes.circle(1120, 470, 5);
        }
    }

    private void drawCloud(float x, float y) {

        shapes.circle(x, y, 20);
        shapes.circle(x + 23, y + 8, 26);
        shapes.circle(x + 49, y, 19);

        shapes.rect(x, y - 15, 49, 25);
    }

    private void drawPipe(PipePair p) {

        float bottom = p.center - p.gap / 2f;
        float top = p.center + p.gap / 2f;

        if (phase == 1) {

            shapes.setColor(0.08f, 0.63f, 0.28f, 1f);

            shapes.rect(p.x, 42, PIPE_W, bottom - 42);
            shapes.rect(p.x, top, PIPE_W, WORLD_H - top - 42);

            shapes.setColor(0.15f, 0.78f, 0.34f, 1f);

            shapes.rect(p.x - 7, bottom - 20, PIPE_W + 14, 20);
            shapes.rect(p.x - 7, top, PIPE_W + 14, 20);

            shapes.setColor(0.04f, 0.45f, 0.18f, 1f);

            shapes.rect(p.x + 8, 42, 7, Math.max(0, bottom - 42));
            shapes.rect(p.x + 8, top + 20, 7,
                    Math.max(0, WORLD_H - top - 62));

        } else if (phase == 2) {

            shapes.setColor(0.42f, 0.16f, 0.75f, 1f);

            shapes.rect(p.x + 8, 42, PIPE_W - 16, bottom - 42);
            shapes.rect(p.x + 8, top, PIPE_W - 16, WORLD_H - top - 42);

            shapes.setColor(0.72f, 0.40f, 1f, 1f);

            shapes.rect(p.x, bottom - 16, PIPE_W, 16);
            shapes.circle(p.x + PIPE_W / 2f, bottom - 16, 12);

            shapes.rect(p.x, top, PIPE_W, 16);
            shapes.circle(p.x + PIPE_W / 2f, top + 16, 12);

            shapes.setColor(0.90f, 0.72f, 1f, 1f);

            shapes.rect(p.x + 22, 42, 5, Math.max(0, bottom - 42));
            shapes.rect(p.x + 22, top + 16, 5,
                    Math.max(0, WORLD_H - top - 58));

        } else {

            shapes.setColor(0.35f, 0.04f, 0.08f, 1f);

            shapes.rect(p.x + 8, 42, PIPE_W - 16, bottom - 42);
            shapes.rect(p.x + 8, top, PIPE_W - 16, WORLD_H - top - 42);

            shapes.setColor(0.65f, 0.10f, 0.15f, 1f);

            shapes.rect(p.x, 42, 12, bottom - 42);
            shapes.rect(p.x + PIPE_W - 12, 42, 12, bottom - 42);

            shapes.rect(p.x, top, 12, WORLD_H - top - 42);
            shapes.rect(p.x + PIPE_W - 12, top, 12, WORLD_H - top - 42);

            shapes.setColor(0.95f, 0.15f, 0.12f, 1f);

            shapes.triangle(
                    p.x - 8, bottom - 18,
                    p.x + PIPE_W / 2f, bottom + 8,
                    p.x + PIPE_W + 8, bottom - 18
            );

            shapes.triangle(
                    p.x - 8, top + 18,
                    p.x + PIPE_W / 2f, top - 8,
                    p.x + PIPE_W + 8, top + 18
            );

            shapes.setColor(1f, 0.55f, 0.10f, 1f);

            for (float y = 75; y < bottom - 30; y += 65) {
                shapes.rect(p.x + 25, y, 28, 5);
            }

            for (float y = top + 40; y < WORLD_H - 70; y += 65) {
                shapes.rect(p.x + 25, y, 28, 5);
            }
        }
    }

    private void drawTextOverlay() {

        if (font == null) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.setColor(Color.WHITE);

        font.draw(
                batch,
                "SCORE: " + (int) score +
                        "     FASE: " + phase,
                34,
                WORLD_H - 38
        );

        font.draw(
                batch,
                sensorStatus,
                34,
                WORLD_H - 60
        );

        font.draw(
                batch,
                "Distancia: " + lastDistanceText +
                        " | Melhor: " + (int) bestScore,
                34,
                WORLD_H - 80
        );

        if (state == 0) {

            drawCenterText("FLAPPY REHAB", WORLD_H * 0.62f, 2.0f);
            drawCenterText("ENTER - Iniciar", WORLD_H * 0.52f, 1.1f);
            drawCenterText(
                    "Afaste a mao: sobe | Aproxime: desce",
                    WORLD_H * 0.44f,
                    0.85f
            );

        } else if (state == 2) {

            drawCenterText("FIM DE JOGO", WORLD_H * 0.60f, 1.7f);
            drawCenterText(
                    "Pontuacao: " + (int) score,
                    WORLD_H * 0.51f,
                    1.0f
            );
            drawCenterText(
                    "ENTER ou ESPACO - Jogar novamente",
                    WORLD_H * 0.43f,
                    0.9f
            );

        } else if (state == 3) {

            drawCenterText("PAUSADO", WORLD_H * 0.58f, 1.7f);
            drawCenterText(
                    "P ou ESC - Continuar",
                    WORLD_H * 0.48f,
                    1.0f
            );
            drawCenterText(
                    "ENTER - Reiniciar",
                    WORLD_H * 0.40f,
                    0.9f
            );
        }

        batch.end();
    }

    private void drawCenterText(String text, float y, float scale) {

        font.getData().setScale(scale);

        GlyphLayout layout = new GlyphLayout(font, text);

        font.draw(
                batch,
                layout,
                (WORLD_W - layout.width) / 2f,
                y
        );

        font.getData().setScale(1f);
    }

    @Override
    public void resize(int width, int height) {

        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {

        if (serialInput != null) {
            serialInput.close();
        }

        if (shapes != null) {
            shapes.dispose();
        }

        if (font != null) {
            font.dispose();
        }

        if (batch != null) {
            batch.dispose();
        }
    }

    private static class PipePair {

        float x;

        final float center;
        final float gap;

        boolean scored;

        PipePair(float x, float center, float gap) {

            this.x = x;
            this.center = center;
            this.gap = gap;
        }
    }
}
