package com.enova.spotify.providers;

import com.enova.spotify.PlayingData;
import com.enova.spotify.SpotifyConfig;
import net.runelite.client.config.ConfigManager;

import java.util.concurrent.FutureTask;

public class NoProviderInterface extends ProviderInterface
{
    public NoProviderInterface(SpotifyConfig config, ConfigManager configManager)
    {
        super(config, configManager);
    }

    @Override
    public boolean attemptAuthentication()
    {
        return false;
    }

    @Override
    public FutureTask<PlayingData> currentlyPlaying()
    {
        return new FutureTask<>(() -> null);
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
