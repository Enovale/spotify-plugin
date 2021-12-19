package com.enova.spotify;

import com.enova.spotify.providers.OSInterface;
import com.enova.spotify.providers.ProviderInterface;
import com.enova.spotify.providers.SpotifyInterface;
import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.concurrent.*;

@Slf4j
@PluginDescriptor(
        name = "Spotify"
)
public class SpotifyPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SpotifyConfig config;

    private final BufferedImage backIcon = ImageUtil.loadImageResource(getClass(), "back.png");
    private final BufferedImage pauseIcon = ImageUtil.loadImageResource(getClass(), "pause.png");
    private final BufferedImage skipIcon = ImageUtil.loadImageResource(getClass(), "skip.png");

    private ProviderInterface provider;

    private SpotifyOverlay overlay;
    private NavigationButton backButton;
    private NavigationButton pauseButton;
    private NavigationButton skipButton;

    private ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private FutureTask<PlayingData> currentApiCall;

    @Override
    protected void startUp()
    {
        switch (config.provider()) {
            case OperatingSystem -> provider = new OSInterface(config, configManager);
            case Spotify -> provider = new SpotifyInterface(config, configManager);
        }

        provider.attemptAuthentication();
        overlay = new SpotifyOverlay();
        overlayManager.add(overlay);

        backButton = NavigationButton.builder()
                .tab(false)
                .tooltip("Previous Song")
                .icon(backIcon)
                .onClick(provider::back)
                .priority(-3)
                .build();
        pauseButton = NavigationButton.builder()
                .tab(false)
                .tooltip("Toggle Pause")
                .icon(pauseIcon)
                .onClick(provider::togglePause)
                .priority(-2)
                .build();
        skipButton = NavigationButton.builder()
                .tab(false)
                .tooltip("Next Song")
                .icon(skipIcon)
                .onClick(provider::skip)
                .priority(-1)
                .build();

        updateConfig();
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals(SpotifyConfig.GROUP)) {
            updateConfig();
        }
    }

    private void updateConfig() {
        if(config.mediaControls()) {
            clientToolbar.addNavigation(backButton);
            clientToolbar.addNavigation(pauseButton);
            clientToolbar.addNavigation(skipButton);
        } else {
            clientToolbar.removeNavigation(backButton);
            clientToolbar.removeNavigation(pauseButton);
            clientToolbar.removeNavigation(skipButton);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        provider.attemptAuthentication();
    }

    @Subscribe
    public void onClientTick(ClientTick clientTick)
    {
        try {
            if (currentApiCall != null && currentApiCall.isDone()) {
                var playback = currentApiCall.get();
                overlay.setPlayingData(playback);
                if (config.mutingEnabled() && !isMuted()) {
                    if (playback != null) {
                        // Playing something
                        // TODO: I dont know why im checking this but I don't wanna figure it out
                        if (client.getMusicVolume() >= config.loudVolume()) {
                            client.setMusicVolume(config.quietVolume());
                        }
                    } else {
                        // Not playing anything
                        if (client.getMusicVolume() <= config.quietVolume()) {
                            client.setMusicVolume(config.loudVolume());
                        }
                    }
                    currentApiCall = null;
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (currentApiCall != null)
            currentApiCall.cancel(true);
        currentApiCall = provider.currentlyPlaying();
        threadPool.submit(currentApiCall);
    }

    private boolean isMuted()
    {
        if (client.getMusicVolume() == 0)
        {
            if (client.getVar(VarPlayer.MUSIC_VOLUME) == 0)
            {
                return client.getVar(Varbits.MUTED_MUSIC_VOLUME) == 0;
            }
        }

        return false;
    }

    public static boolean promptUser(String message, int type, String acceptMessage, String denyMessage)
    {
        final int result = JOptionPane.showOptionDialog(null,
                message,
                log.getName(), JOptionPane.YES_NO_OPTION, type,
                null, new String[]{acceptMessage, denyMessage}, denyMessage);

        return result == JOptionPane.OK_OPTION;
    }

    public static boolean promptUser(String message, int type)
    {
        return promptUser(message, type, "Ok", "Cancel");
    }

    public static boolean promptUser(String message)
    {
        return promptUser(message, JOptionPane.INFORMATION_MESSAGE);
    }

    @Provides
    SpotifyConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SpotifyConfig.class);
    }
}
