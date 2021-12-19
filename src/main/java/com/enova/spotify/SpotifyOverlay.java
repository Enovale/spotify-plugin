package com.enova.spotify;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.*;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

import java.awt.*;
import java.time.Duration;

public class SpotifyOverlay extends OverlayPanel
{
    private PlayingData playingData;

    SpotifyOverlay() {
        setResizable(true);
        setMinimumSize(200);
        setPreferredSize(new Dimension(200, 800));
    }

    public void setPlayingData(PlayingData data)
    {
        this.playingData = data;
    }

    @Override
    public Dimension render(final Graphics2D graphics)
    {
        panelComponent.setOrientation(ComponentOrientation.VERTICAL);
        panelComponent.getChildren().add(TitleComponent.builder().text("Currently Playing").build());
        var size = getPreferredSize();
        if (playingData != null) {
            var image = new CoverComponent(playingData.image);
            var imageSize = size.width - 20;
            image.setImageSize(new Dimension(imageSize, imageSize));
            panelComponent.getChildren().add(image);
            panelComponent.getChildren().add(TitleComponent.builder().text(playingData.trackName).build());
            panelComponent.getChildren().add(TitleComponent.builder().text(playingData.albumName).color(Color.gray).build());
            var bar = new ProgressBarComponent();
            bar.setPreferredLocation(new Point(0, 3));
            bar.setMinimum(0);
            bar.setMaximum(playingData.songLength);
            bar.setValue(playingData.progress);
            bar.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
            var duration = Duration.ofMillis(playingData.progress);
            bar.setCenterLabel(playingData.paused ? "Paused" : String.format("%d:%02d", duration.toMinutesPart(), duration.toSecondsPart()));
            panelComponent.getChildren().add(bar);
        } else {
            panelComponent.getChildren().add(TitleComponent.builder().text("Nothing is currently playing.").build());
        }
        return super.render(graphics);
    }
}
