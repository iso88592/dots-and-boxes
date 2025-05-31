package hu.mocman.dotsandboxes;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GameGif {

    private List<BufferedImage> images = new ArrayList<>();
    private BufferedImage backdrop;
    public byte[] gifData;
    public String id;

    public GameGif(String c1, String c2, int boardSize, int size) {
        int ic1 = c1.indexOf('_')+1;
        int ic2 = c2.indexOf('_')+1;
        id="" + c1.charAt(0) + c1.charAt(ic1) + c2.charAt(0) + c2.charAt(ic2)  + boardSize + size;
        backdrop = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = backdrop.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Classic Console Neue", Font.PLAIN, 20));
        graphics.drawString("%s vs %s".formatted(c1, c2), 0, 15);
        graphics.drawString("Board: %d, round %d ".formatted(boardSize, size), 0, 30);
    }

    public void addFrame(GameState gameState) {
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.createGraphics();
        graphics.drawImage(backdrop, 0, 0, null);
        gameState.draw((Graphics2D) graphics);
        images.add(image);
    }

    public void finalizeGif() {
        try {
            BufferedImage first = images.get(0);

            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ImageOutputStream output = new MemoryCacheImageOutputStream(byteOutput);

            GifSequenceWriter writer = new GifSequenceWriter(output, first.getType(), 10, true);

            for (BufferedImage image : images) {
                writer.writeToSequence(image);
            }

            for (int i = 0; i < 2; i++)
                writer.writeToSequence(images.getLast());

            writer.close();
            output.close();
            gifData = byteOutput.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
