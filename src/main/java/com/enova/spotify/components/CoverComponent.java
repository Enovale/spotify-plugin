package com.enova.spotify.components;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

@Setter
public class CoverComponent implements LayoutableRenderableEntity
{
    private final BufferedImage image;

    @Getter
    private final Rectangle bounds = new Rectangle();

    private Point preferredLocation = new Point();
    private Dimension preferredSize = new Dimension();
    private Dimension imageSize;

    public CoverComponent(BufferedImage image) {
        this.image = image;
        imageSize = new Dimension(image.getWidth(), image.getHeight());
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (image == null)
        {
            return null;
        }

        val realSize = new Dimension(Math.max(imageSize.width, preferredSize.width), Math.max(imageSize.height, preferredSize.height));
        graphics.drawImage(image,
                preferredLocation.x + ((preferredSize.width - imageSize.width) / 2),
                preferredLocation.y,
                imageSize.width,
                imageSize.height,
                null);
        bounds.setLocation(preferredLocation);
        bounds.setSize(realSize);
        return realSize;
    }

    @Override
    public void setPreferredSize(Dimension dimension)
    {
        this.preferredSize = dimension;
    }

    public void setImageSize(Dimension dimension)
    {
        this.imageSize = dimension;
    }
}