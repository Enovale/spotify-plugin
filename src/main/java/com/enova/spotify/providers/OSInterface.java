package com.enova.spotify.providers;

import com.enova.spotify.PlayingData;
import com.enova.spotify.Provider;
import com.enova.spotify.SpotifyConfig;
import net.runelite.client.config.ConfigManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.FutureTask;

/*
    TODO: Implement JNI, osascript
    Currently implements `playerctl` on Linux
    Will eventually use JNI to support Windows/Linux without playerctl
 */
public class OSInterface extends ProviderInterface
{
    public static final String DAEMON_KEY = "playerctlDaemon";
    public static final String DAEMON_KEY_PROMPT = DAEMON_KEY + "_prompted";

    private int OsType = 0;

    public OSInterface(SpotifyConfig config, ConfigManager configManager)
    {
        super(config, configManager);

        var os = System.getProperty("os.name");
        if (os.startsWith("Linux")) {
            OsType = 0;
        } else if (os.startsWith("Windows")) {
            OsType = 1;
            // TODO: Check what it actually outputs on MacOS
        } else if (os.startsWith("MacOS")) {
            OsType = 2;
        }
        attemptAuthentication();
    }

    @Override
    public boolean attemptAuthentication()
    {
        if(authenticated)
            return true;

        try {
            switch (OsType) {
                case 0 -> {
                    authenticated = !runCmd("playerctl").isEmpty();

                    if(!authenticated) {
                        JOptionPane.showMessageDialog(null,
                                "Playerctl does not appear to be installed or in your PATH.\n" +
                                        "Please refer to https://github.com/altdesktop/playerctl.",
                                "SpotifyPlugin Configuration", JOptionPane.ERROR_MESSAGE);
                        config.provider(Provider.None);
                        return false;
                    }

                    if(!config.daemonPrompt()) {
                        var choice = JOptionPane.showConfirmDialog(null,
                                "Do you want playerctl's daemon to be autolaunched " +
                                        "in order to improve media detection?\n" +
                                        "(Refer to https://github.com/altdesktop/playerctl for more details)",
                                "SpotifyPlugin Confirmation", JOptionPane.YES_NO_OPTION);
                        config.playerCtlDaemon(choice == 0);
                        config.daemonPrompt(true);
                    }

                    Boolean daemonEnabled = configManager.getConfiguration(SpotifyConfig.GROUP, DAEMON_KEY, Boolean.class);
                    if(daemonEnabled) {
                        runCmd("playerctld", "daemon");
                    }
                }
                default -> {
                    authenticated = false;
                }
            }
        } catch (IOException e) {
            authenticated = false;
        }
        return authenticated;
    }

    @Override
    public FutureTask<PlayingData> currentlyPlaying()
    {
        return new FutureTask<>(() -> {
            try {
                switch (OsType) {
                    case 0 -> {
                        var allData = runCmd("playerctl", "metadata", "--format",
                                "{{status}}\n{{markup_escape(mpris:artUrl)}}\n{{album}}\n{{title}}\n{{position}}\n{{mpris:length}}\nend")
                                .split("\n");
                        if(allData.length  <= 6) {
                            return null;
                        }
                        return new PlayingData(!allData[0].equals("Playing"),
                                Instant.now().getEpochSecond(),
                                allData[1].isEmpty() ? null : ImageIO.read(new URL(allData[1])),
                                allData[2],
                                allData[3],
                                allData[3],
                                allData[4].isEmpty() ? 0 : (int) (Long.parseLong(allData[4]) / 1000),
                                allData[5].isEmpty() ? 0 : (int) (Long.parseLong(allData[5]) / 1000));
                    }
                    default -> {
                        return null;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public void back()
    {
        try {
            runCmd("playerctl", "previous");
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean togglePause()
    {
        try {
            runCmd("playerctl", "play-pause");
            return runCmd("playerctl", "status").equals("Playing");
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void skip()
    {
        try {
            runCmd("playerctl", "next");
        } catch (IOException ignored) {
        }
    }

    private static String runCmd(String... cmd) throws IOException
    {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        String s = null;
        StringBuilder stringBuilder = new StringBuilder();
        while ((s = stdInput.readLine()) != null) stringBuilder.append(s + "\n");
        return stringBuilder.substring(0, Math.max(0, stringBuilder.toString().length() - 1));
    }
}
