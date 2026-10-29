import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Original geometric icon. Run with Java 25 from the repository root. No game textures. */
public class GenerateIcon {
    private static void polygon(Graphics2D g, int color, int... coordinates) {
        Path2D path = new Path2D.Double();
        path.moveTo(coordinates[0], coordinates[1]);
        for (int i = 2; i < coordinates.length; i += 2) path.lineTo(coordinates[i], coordinates[i + 1]);
        path.closePath();
        g.setColor(new Color(color, true));
        g.fill(path);
    }
    private static void line(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }
    public static void main(String[] args) throws Exception {
        var image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x142331));
        g.fillRoundRect(0, 0, 256, 256, 48, 48);
        polygon(g, 0xFF2D5260, 128, 51, 196, 89, 128, 128, 60, 89);
        polygon(g, 0xFF203D4B, 60, 89, 128, 128, 128, 209, 60, 169);
        polygon(g, 0xFF274A56, 128, 128, 196, 89, 196, 169, 128, 209);
        g.setColor(new Color(0x73F59A));
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[][] edges = {{128,51,196,89},{196,89,196,169},{196,169,128,209},
                {128,209,60,169},{60,169,60,89},{60,89,128,51},
                {60,89,128,128},{196,89,128,128},{128,128,128,209}};
        for (var e : edges) line(g, e[0], e[1], e[2], e[3]);
        g.setColor(new Color(0xFFD166));
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        line(g, 208, 35, 208, 61);
        line(g, 195, 48, 221, 48);
        g.setColor(new Color(0x63D9FF));
        line(g, 35, 123, 35, 139);
        line(g, 27, 131, 43, 131);
        g.dispose();
        Path output = Path.of("src/main/resources/assets/show_interaction/icon.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
    }
}
