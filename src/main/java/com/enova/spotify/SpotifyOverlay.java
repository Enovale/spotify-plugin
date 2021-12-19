package com.enova.spotify;

import com.enova.spotify.components.CoverComponent;
import com.enova.spotify.components.GapComponent;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.*;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Duration;

public class SpotifyOverlay extends OverlayPanel
{
    private final SpotifyConfig config;

    private PlayingData playingData;
    private BufferedImage providerIcon;
    private final BufferedImage unknownArtImage = ImageUtil.loadImageResource(getClass(), "unknown.png");

    SpotifyOverlay(SpotifyConfig config) {
        this.config = config;
        setResizable(true);
        setMinimumSize(100);
    }

    public void setPlayingData(PlayingData data, BufferedImage providerIcon)
    {
        this.playingData = data;
        this.providerIcon = providerIcon;
    }

    @Override
    public Dimension render(final Graphics2D graphics)
    {
        if(!config.displayWhenStopped() && playingData == null)
            return new Dimension();

        panelComponent.setOrientation(ComponentOrientation.VERTICAL);
        panelComponent.setGap(new Point(0, 2));

        var titleBarText = "Currently Playing";

        // Indicate to the user what provider is selected
        if(/*playingData != null && */ providerIcon != null) {
            var imageX = ComponentConstants.STANDARD_BORDER + 6;
            var size = Math.min(30, (getPreferredSize().width / 2) - (graphics.getFontMetrics().stringWidth(titleBarText) / 2) - imageX);
            if(size > 0) {
                graphics.drawImage(providerIcon,
                        imageX,
                        (ComponentConstants.STANDARD_BORDER / 2) + ((30 - size) / 2),
                        size, size, null);
            }
        }
        panelComponent.getChildren().add(new GapComponent(2));
        panelComponent.getChildren().add(TitleComponent.builder().text(titleBarText).build());
        panelComponent.getChildren().add(new GapComponent(5));
        var size = getPreferredSize();
        if (playingData != null) {
            var image = new CoverComponent(playingData.image != null ? playingData.image : unknownArtImage);
            var imageSize = size.width - 20;
            image.setImageSize(new Dimension(imageSize, imageSize));
            panelComponent.getChildren().add(image);
            panelComponent.getChildren().add(TitleComponent.builder()
                    // TODO: Please just...
                    //.text(safeTrim(playingData.trackName, characterLimit))
                    .text(safeTrim(playingData.trackName, slowAwfulCalculateCharacterLimit(playingData.trackName, size.width, graphics.getFontMetrics())))
                    .build());
            panelComponent.getChildren().add(TitleComponent.builder()
                    //.text(safeTrim(playingData.albumName, characterLimit))
                    .text(safeTrim(playingData.albumName, slowAwfulCalculateCharacterLimit(playingData.albumName, size.width, graphics.getFontMetrics())))
                    .color(Color.gray)
                    .build());
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
            panelComponent.getChildren().add(TitleComponent.builder().text("Stopped").build());
        }
        return super.render(graphics);
    }

    private int slowAwfulCalculateCharacterLimit(String str, int maxWidth, FontMetrics metrics) {
        for (int i = 0; i < str.length(); i++) {
            if(metrics.stringWidth(str.substring(0, str.length() - i)) < maxWidth) {
                return str.length() - i;
            }
        }

        return 0;
    }

    private String safeTrim(String str, int endIndex) {
        if(endIndex < str.length())
            return str.substring(0, Math.min(str.length(), endIndex - 3)) + "...";
        else
            return str;
    }
}
