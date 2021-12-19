package com.enova.spotify.providers;

import com.enova.spotify.PlayingData;
import com.enova.spotify.SpotifyConfig;
import net.runelite.client.config.ConfigManager;

import java.util.concurrent.FutureTask;

public class OSInterface extends ProviderInterface {

    public boolean authenticated = true;

    public OSInterface(SpotifyConfig config, ConfigManager configManager) {
        super(config, configManager);
    }

    @Override
    public boolean attemptAuthentication() {
        return true;
    }

    @Override
    public FutureTask<PlayingData> currentlyPlaying() {
        return null;
    }

    @Override
    public void back()
    {

    }

    @Override
    public boolean togglePause()
    {
        return false;
    }

    @Override
    public void skip()
    {

    }
}
