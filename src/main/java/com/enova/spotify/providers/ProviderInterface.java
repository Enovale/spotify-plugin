package com.enova.spotify.providers;

import com.enova.spotify.PlayingData;
import com.enova.spotify.SpotifyConfig;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.concurrent.FutureTask;

public abstract class ProviderInterface {
    public BufferedImage providerIcon = ImageUtil.loadImageResource(getClass(), "default.png");
    public boolean authenticated = false;

    protected SpotifyConfig config;
    protected ConfigManager configManager;

    public ProviderInterface(SpotifyConfig config, ConfigManager configManager) {
        this.config = config;
        this.configManager = configManager;
    }

    abstract public boolean attemptAuthentication();

    abstract public FutureTask<PlayingData> currentlyPlaying();

    abstract public void back();
    abstract public boolean togglePause();
    abstract public void skip();
}
