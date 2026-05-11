import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateTextures {
    public static void main(String[] args) throws Exception {
        File baseDir = new File("src/main/resources/assets/virtualexplorer/textures");
        
        // 1. GUI
        File guiDir = new File(baseDir, "gui");
        guiDir.mkdirs();
        BufferedImage gui = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gui.createGraphics();
        g.setColor(new Color(198, 198, 198));
        g.fillRect(0, 0, 176, 166);
        g.setColor(Color.WHITE);
        g.drawRect(0, 0, 175, 165);
        g.setColor(new Color(55, 55, 55));
        g.drawRect(1, 1, 173, 163);

        // エネルギーバーの背景枠 (x:12, y:20, w:14, h:50)
        g.setColor(new Color(50, 0, 0));
        g.fillRect(12, 20, 14, 50);
        g.setColor(new Color(55, 55, 55));
        g.drawRect(11, 19, 15, 51);
        
        // スロット背景
        g.setColor(new Color(139, 139, 139));
        int[] ys = {20, 40, 60};
        for (int y : ys) {
            g.fillRect(43, y - 1, 18, 18);
            g.setColor(new Color(55, 55, 55));
            g.drawRect(43, y - 1, 17, 17);
            g.setColor(new Color(139, 139, 139)); // 戻す
        }
        for (int i = 0; i < 7; i++) {
            g.fillRect(79 + i * 18, 39, 18, 18);
            g.setColor(new Color(55, 55, 55));
            g.drawRect(79 + i * 18, 39, 17, 17);
            g.setColor(new Color(139, 139, 139));
        }
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                g.fillRect(7 + l * 18, 83 + i * 18, 18, 18);
                g.setColor(new Color(55, 55, 55));
                g.drawRect(7 + l * 18, 83 + i * 18, 17, 17);
                g.setColor(new Color(139, 139, 139));
            }
        }
        for (int i = 0; i < 9; ++i) {
            g.fillRect(7 + i * 18, 141, 18, 18);
            g.setColor(new Color(55, 55, 55));
            g.drawRect(7 + i * 18, 141, 17, 17);
            g.setColor(new Color(139, 139, 139));
        }
        g.dispose();
        ImageIO.write(gui, "png", new File(guiDir, "virtual_mapping_table.png"));

        // 2. Block
        File blockDir = new File(baseDir, "block");
        blockDir.mkdirs();
        BufferedImage block = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        g = block.createGraphics();
        g.setColor(new Color(100, 70, 40));
        g.fillRect(0, 0, 16, 16);
        g.setColor(new Color(150, 110, 60));
        g.fillRect(2, 2, 12, 12);
        g.setColor(Color.GREEN);
        g.drawLine(4, 4, 11, 11);
        g.drawLine(11, 4, 4, 11);
        g.dispose();
        ImageIO.write(block, "png", new File(blockDir, "virtual_mapping_table.png"));

        // 3. Items
        File itemDir = new File(baseDir, "item");
        itemDir.mkdirs();
        
        BufferedImage item1 = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        g = item1.createGraphics();
        g.setColor(Color.GRAY);
        g.fillOval(2, 2, 12, 12);
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(6, 6, 4, 4);
        g.dispose();
        ImageIO.write(item1, "png", new File(itemDir, "module_underground.png"));

        BufferedImage item2 = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        g = item2.createGraphics();
        g.setColor(new Color(200, 50, 50));
        g.fillOval(2, 2, 12, 12);
        g.setColor(Color.ORANGE);
        g.fillRect(6, 6, 4, 4);
        g.dispose();
        ImageIO.write(item2, "png", new File(itemDir, "module_nether.png"));

        BufferedImage item3 = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        g = item3.createGraphics();
        g.setColor(Color.YELLOW);
        g.fillOval(2, 2, 12, 12);
        g.setColor(Color.CYAN);
        g.fillRect(6, 6, 4, 4);
        g.dispose();
        ImageIO.write(item3, "png", new File(itemDir, "speed_upgrade.png"));

        System.out.println("Textures generated successfully.");
    }
}
