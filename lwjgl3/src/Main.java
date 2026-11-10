import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.flappyrehab.FlappyRehabGame;

public class Main {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Flappy Rehab");
        config.setWindowedMode(1200, 674);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new FlappyRehabGame(), config);
    }
}
