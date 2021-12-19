package com.enova.spotify.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

import java.awt.*;

@Setter
public class GapComponent implements LayoutableRenderableEntity
{
    @Getter
    private final Rectangle bounds = new Rectangle();

    private Point preferredLocation = new Point();
    private Dimension preferredSize = new Dimension();

    private final int offset;

    public GapComponent(int offset) {
        this.offset = offset;
    }

    public GapComponent() {
        this.offset = 5;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        bounds.setSize(offset, offset);
        bounds.setLocation(preferredLocation.x, preferredLocation.y);
        return new Dimension(offset, offset);
    }
}
