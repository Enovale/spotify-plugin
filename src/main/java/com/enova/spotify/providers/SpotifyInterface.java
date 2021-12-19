package com.enova.spotify.providers;

import com.enova.spotify.PlayingData;
import com.enova.spotify.SpotifyConfig;
import com.enova.spotify.SpotifyPlugin;
import com.jogamp.common.net.Uri;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.LinkBrowser;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

public class SpotifyInterface extends ProviderInterface
{
    public static String CLIENT_ID = "6b1cc33a3c7d460797ce9166b2fd413f";
    public static String CLIENT_SECRET = "52455d8500d1400b99223cdd15a3221a";
    public static String REDIRECT_URI = "http://localhost/copyThisUrlEntirely";
    private final String TOKEN_KEY = "refresh_token";

    public boolean authenticated = false;

    private PlayingData cachedPlaybackData;

    private final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRedirectUri(SpotifyHttpManager.makeUri(REDIRECT_URI))
            .build();
    private final AuthorizationCodeUriRequest uriRequest = spotifyApi.authorizationCodeUri()
            .state("Dunno what to put here lmao")
            .scope("user-read-playback-state,user-modify-playback-state,user-read-currently-playing")
            .build();

    public SpotifyInterface(SpotifyConfig config, ConfigManager configManager)
    {
        super(config, configManager);
        var savedRefreshToken = configManager.getConfiguration(SpotifyConfig.GROUP, TOKEN_KEY);
        if (savedRefreshToken != null) {
            try {
                spotifyApi.setRefreshToken(savedRefreshToken);
                var credentials = spotifyApi.authorizationCodeRefresh().refresh_token(savedRefreshToken).build().execute();
                if (credentials.getAccessToken() == null) {
                    return;
                }
                spotifyApi.setAccessToken(credentials.getAccessToken());
                authenticated = true;
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public void promptUser()
    {
        LinkBrowser.browse(uriRequest.execute().toString());
    }

    public boolean exchangeCode(Uri responseUrl)
    {
        try {
            var query = responseUrl.query.decode();
            var parsedCode = query.split("&")[0].split("=")[1];
            final AuthorizationCodeRequest request = spotifyApi.authorizationCode(parsedCode).build();
            final AuthorizationCodeCredentials credentials = request.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());
            configManager.setConfiguration(SpotifyConfig.GROUP, TOKEN_KEY, credentials.getRefreshToken());
            authenticated = true;
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean attemptAuthentication()
    {
        while (!authenticated) {
            promptUser();
            // Ask the user for the url they were sent to
            var input = JOptionPane.showInputDialog("Please paste the URL that spotify redirected you to after authenticating");
            if (input == null || input.isEmpty()) {
                return false;
            }
            var response = filterResponseUrl(input);
            try {
                if (exchangeCode(Uri.cast(response))) {
                    return true;
                }
            } catch (URISyntaxException e) {
                if (!SpotifyPlugin.promptUser("The pasted URI could not be exchanged. Please make sure to copy the whole url." +
                        " Do you need the OAuth page to be reopened?", JOptionPane.ERROR_MESSAGE, "Yes", "No"))
                {
                    continue;
                }
            }
        }

        return false;
    }

    private String filterResponseUrl(String inputUrl)
    {
        // Assume the user just pasted the code
        if (!inputUrl.startsWith(REDIRECT_URI)) {
            return REDIRECT_URI + "?code=" + inputUrl + "&";
        } else {
            return inputUrl;
        }
    }

    @Override
    public FutureTask<PlayingData> currentlyPlaying()
    {
        return new FutureTask<>(() -> {
            try {
                var currentPlayback = spotifyApi.getInformationAboutUsersCurrentPlayback().additionalTypes("track,episode").build().execute();
                if (currentPlayback == null || currentPlayback.getItem() == null || currentPlayback.getItem().getId() == null) {
                    return null;
                }
                if (cachedPlaybackData != null && currentPlayback.getItem().getId().equals(cachedPlaybackData.trackId)) {
                    cachedPlaybackData.paused = !currentPlayback.getIs_playing();
                    cachedPlaybackData.timestamp = currentPlayback.getTimestamp();
                    cachedPlaybackData.progress = currentPlayback.getProgress_ms();
                    return cachedPlaybackData;
                }
                String trackName;
                String albumName;
                String artUrl;
                switch (currentPlayback.getCurrentlyPlayingType()) {
                    case TRACK -> {
                        var track = spotifyApi.getTrack(currentPlayback.getItem().getId()).build().execute();
                        trackName = track.getName();
                        var album = track.getAlbum();
                        albumName = album.getName();
                        artUrl = album.getImages()[0].getUrl();
                    }
                    case EPISODE -> {
                        var track = spotifyApi.getEpisode(currentPlayback.getItem().getId()).build().execute();
                        trackName = track.getName();
                        var show = track.getShow();
                        albumName = show.getName();
                        artUrl = show.getImages()[0].getUrl();
                    }
                    default -> {
                        return null;
                    }
                }
                var result = new PlayingData(
                        !currentPlayback.getIs_playing(),
                        currentPlayback.getTimestamp(),
                        ImageIO.read(new URL(artUrl)),
                        albumName,
                        trackName,
                        currentPlayback.getItem().getId(),
                        currentPlayback.getProgress_ms(),
                        currentPlayback.getItem().getDurationMs());
                cachedPlaybackData = result;
                return result;
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    @Override
    public void back()
    {
        try {
            spotifyApi.skipUsersPlaybackToPreviousTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean togglePause()
    {
        try {
            Boolean playing;
            if (cachedPlaybackData != null) {
                playing = !cachedPlaybackData.paused;
            } else {
                playing = !currentlyPlaying().get().paused;
            }

            if (playing) {
                spotifyApi.pauseUsersPlayback().build().execute();
            } else {
                spotifyApi.startResumeUsersPlayback().build().execute();
            }
            return !playing;
        } catch (ForbiddenException e) {
            return cachedPlaybackData != null && !cachedPlaybackData.paused;
        } catch (IOException | SpotifyWebApiException | ParseException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void skip()
    {
        try {
            spotifyApi.skipUsersPlaybackToNextTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
        }
    }
}