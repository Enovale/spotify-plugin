package com.enova.spotify;

import java.awt.image.BufferedImage;

public class PlayingData {
    public Long timestamp;
    public boolean paused;
    public final BufferedImage image;
    public final String albumName;
    public final String trackName;
    public final String trackId;
    public Integer progress;
    public final Integer songLength;

    public PlayingData(boolean paused,Long timestamp, BufferedImage image, String albumName, String trackName, String trackId, Integer progress, Integer sondLength) {
        this.paused = paused;
        this.timestamp = timestamp;
        this.image = image;
        this.albumName = albumName;
        this.trackName = trackName;
        this.trackId = trackId;
        this.progress = progress;
        this.songLength = sondLength;
    }
}
