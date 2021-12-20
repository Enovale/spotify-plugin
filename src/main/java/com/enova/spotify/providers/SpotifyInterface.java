package com.enova.spotify.providers;

import com.enova.spotify.PlayingData;
import com.enova.spotify.Provider;
import com.enova.spotify.SpotifyConfig;
import com.enova.spotify.SpotifyPlugin;
import com.jogamp.common.net.Uri;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class SpotifyInterface extends ProviderInterface
{
    public final static String TOKEN_KEY = "refresh_token";

    private PlayingData cachedPlaybackData;

    private SpotifyApi spotifyApi;
    private AuthorizationCodeUriRequest uriRequest;

    public SpotifyInterface(SpotifyConfig config, ConfigManager configManager)
    {
        super(config, configManager);
        providerIcon = ImageUtil.loadImageResource(getClass(), "spotify.png");
    }

    public void promptUser()
    {
        LinkBrowser.browse(uriRequest.execute().toString());
    }

    public boolean exchangeCode(Uri responseUrl) throws IOException, ParseException, SpotifyWebApiException
    {
        try {
            var query = responseUrl.query.decode();
            var parsedCode = query.split("&")[0].split("=")[1];
            final AuthorizationCodeRequest request = spotifyApi.authorizationCode(parsedCode).build();
            final AuthorizationCodeCredentials credentials = request.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());
            config.refreshToken(credentials.getRefreshToken());
            authenticated = true;
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw e;
        }
    }

    @Override
    public boolean attemptAuthentication()
    {
        if (!ensureConfigured())
            return false;

        var prompt = true;
        while (!authenticated) {
            var savedRefreshToken = config.refreshToken();
            if (savedRefreshToken != null && !savedRefreshToken.isEmpty()) {
                try {
                    spotifyApi.setRefreshToken(savedRefreshToken);
                    var credentials = spotifyApi.authorizationCodeRefresh().refresh_token(savedRefreshToken).build().execute();
                    if (credentials.getAccessToken() != null) {
                        spotifyApi.setAccessToken(credentials.getAccessToken());
                        authenticated = true;
                        return true;
                    }
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    e.printStackTrace();
                }
            }

            if (prompt)
                promptUser();
            // Ask the user for the url they were sent to
            var input = JOptionPane.showInputDialog("Please paste the URL that spotify redirected you to after authenticating");
            if (input == null || input.isEmpty()) {
                return false;
            }
            var response = filterResponseUrl(input);
            try {
                if (exchangeCode(Uri.cast(response))) {
                    authenticated = true;
                    return true;
                }
            } catch (URISyntaxException | IOException | ParseException | SpotifyWebApiException e) {
                if (!SpotifyPlugin.promptUser(
                        "The pasted URI could not be exchanged because: " + e.getMessage() +
                                ". Please make sure to copy the whole url." +
                                " Do you need the OAuth page to be reopened?", JOptionPane.ERROR_MESSAGE,
                        "Yes", "No"))
                {
                    prompt = false;
                }
            }
        }

        return false;
    }

    private boolean ensureConfigured()
    {
        if(spotifyApi != null)
            return true;

        if (!config.clientId().isEmpty() && !config.clientSecret().isEmpty() && !config.redirectUrl().isEmpty()) {
            createApi();
            return true;
        }

        final int result = JOptionPane.showConfirmDialog(null,
                "Spotify integration will not work unless you create an app on the\n" +
                        "developer dashboard. Would you like to be redirected to the app creation page?\n" +
                        "(Make sure your app has a redirect URL set.)",
                "Spotify Integration", JOptionPane.YES_NO_CANCEL_OPTION);
        switch (result) {
            case JOptionPane.YES_OPTION -> LinkBrowser.browse("https://developer.spotify.com/dashboard/applications");
            case JOptionPane.CANCEL_OPTION -> {
                config.provider(Provider.None);
                return false;
            }
        }

        var inputId = JOptionPane.showInputDialog("Please paste the client ID provided on the dashboard.");
        if (inputId == null)
            return false;
        config.clientId(inputId);
        var inputSecret = JOptionPane.showInputDialog("Please paste the client Secret provided on the dashboard.");
        if (inputSecret == null)
            return false;
        config.clientSecret(inputSecret);
        var inputUrl = JOptionPane.showInputDialog("Please ensure that your app has a redirect url and then paste it here.\n" +
                "(Put in something like 'https://localhost/blahblah')");
        if (inputUrl == null)
            return false;
        config.redirectUrl(inputUrl);

        createApi();
        return true;
    }

    private void createApi()
    {
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(config.clientId())
                .setClientSecret(config.clientSecret())
                .setRedirectUri(SpotifyHttpManager.makeUri(config.redirectUrl()))
                .build();
        uriRequest = spotifyApi.authorizationCodeUri()
                .redirect_uri(SpotifyHttpManager.makeUri(config.redirectUrl()))
                .state("Dunno what to put here lmao")
                .scope("user-read-playback-state,user-modify-playback-state,user-read-currently-playing")
                .build();
    }

    private String filterResponseUrl(String inputUrl)
    {
        // Assume the user just pasted the code
        if (!inputUrl.startsWith(config.redirectUrl())) {
            return config.redirectUrl() + "?code=" + inputUrl + "&";
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