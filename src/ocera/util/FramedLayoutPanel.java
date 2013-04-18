/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: May 22, 2003
 * Time: 12:01:52 PM
 * To change this template use Options | File Templates.
 */
package ocera.util;

import javax.swing.*;
import java.awt.*;

/**
 * Frames used layout. <br>
 * Still supports just GridBackLayout
 */
public class FramedLayoutPanel extends JPanel
{
    public Color frameColor = null;
    /**
     * @param frameColor Setting frameColor to null disables framing behaviour.
     */
    public void setFrameColor(Color frameColor) { this.frameColor = frameColor; }
    public Color getFrameColor() {return frameColor;}

    public void paint(Graphics g)
    {
        super.paint(g);
        if(getFrameColor() == null) return;

        LayoutManager manager = getLayout();
        if(manager != null && manager instanceof GridBagLayout) {
            GridBagLayout layout = (GridBagLayout)manager;
            Point p = layout.getLayoutOrigin();
            int[][] sizes = layout.getLayoutDimensions();
            int[] colWidths = sizes[0];
            int[] rowHeights = sizes[1];
            int width = 0, height = 0;
            for(int i=0; i<colWidths.length; i++) width += colWidths[i];
            for(int i=0; i<rowHeights.length; i++) height += rowHeights[i];

            g.setColor(getFrameColor());
            g.drawRect(p.x, p.y, p.x + width-1, p.y + height-1);
        }
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if(getFrameColor() == null) return;
        LayoutManager manager = getLayout();
        if(manager != null && manager instanceof GridBagLayout) {
            GridBagLayout layout = (GridBagLayout)manager;
            g.setColor(getFrameColor());
            Point p = layout.getLayoutOrigin();
            int[][] sizes = layout.getLayoutDimensions();
            int[] colWidths = sizes[0];
            int[] rowHeights = sizes[1];
            int width, height;
            int xpos = p.x;
            int ypos;
            for(int x=0; x<colWidths.length; x++) {
                ypos = p.y;
                width = colWidths[x];
                for(int y=0; y<rowHeights.length; y++) {
                    height = rowHeights[y];
                    g.drawRect(xpos, ypos, width-1, height-1);
                    g.drawRect(xpos+1, ypos+1, width-3, height-3);
                    ypos += height;
                }
                xpos += width;
            }
        }
    }
}
