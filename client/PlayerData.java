import java.awt.Color;

public class PlayerData {
    public String name;
    public int    x, y;
    public String direction;
    public Color  color;
    public int    animTimer;
    public boolean moving;

    public PlayerData(String name, Color color, int x, int y, String direction) {
        this.name = name; this.color = color;
        this.x = x; this.y = y;
        this.direction = direction;
        this.animTimer = 0;
        this.moving = false;
    }

    public void update(int nx, int ny, String dir) {
        moving    = (nx != x || ny != y);
        x = nx; y = ny; direction = dir;
        if (moving) animTimer++;
        else        animTimer = 0;
    }
}
