package com.enova.spotify;

import com.enova.spotify.providers.OSInterface;
import com.enova.spotify.providers.SpotifyInterface;
import net.runelite.client.config.*;

@ConfigGroup(SpotifyConfig.GROUP)
public interface SpotifyConfig extends Config
{
	String GROUP = "spotify";

	@ConfigSection(
			name = "Configuration",
			description = "General Configuration",
			position = 0,
			closedByDefault = false
	)
	String generalSection = "configuration";

	@ConfigSection(
			name = "Muting",
			description = "Automatic Game Muting Configuration",
			position = 1,
			closedByDefault = false
	)
	String mutingSection = "muting";

	@ConfigSection(
			name = "Music Providers",
			description = "Select your preferred music player and change their settings.",
			position = 2,
			closedByDefault = true
	)
	String providerSection = "providers";

	@ConfigItem(
			keyName = "mutingEnabled",
			name = "Mute Game When Playing",
			description = "Whether or not to mute the music in-game if you are playing music externally.",
			section = generalSection
	)
	default boolean mutingEnabled()
	{
		return true;
	}

	@ConfigItem(
			keyName = "mediaControls",
			name = "Media Control Buttons",
			description = "Whether or not to add buttons to the side panel to control the music.",
			section = generalSection
	)
	default boolean mediaControls()
	{
		return true;
	}

	@ConfigItem(
			keyName = "overlayEnabled",
			name = "Media Overlay",
			description = "Whether or not to display the currently playing media in an overlay.",
			section = generalSection
	)
	default boolean overlayEnabled()
	{
		return true;
	}

	@ConfigItem(
			keyName = "displayWhenStopped",
			name = "Always Overlay",
			description = "Whether or not the overlay should be displayed when no music is playing.",
			section = generalSection
	)
	default boolean displayWhenStopped()
	{
		return true;
	}

	@ConfigItem(
			keyName = "treatPausedAsStopped",
			name = "Pause to Stop",
			description = "Whether or not the media being paused should be treated like nothing is playing",
			section = generalSection
	)
	default boolean treatPausedAsStopped()
	{
		return false;
	}

	@ConfigItem(
			keyName = "quietVolume",
			name = "Quiet Volume",
			description = "The volume to set when music is playing (0 - 255)",
			section = mutingSection
	)
	@Range(min = 0, max = 255)
	default int quietVolume()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "loudVolume",
			name = "Loud Volume",
			description = "The volume to set when music is NOT playing (0 - 255)",
			section = mutingSection
	)
	@Range(min = 0, max = 255)
	default int loudVolume()
	{
		return 255;
	}

	@ConfigItem(
			keyName = "provider",
			name = "Provider",
			description = "Which service to retrieve music playing status from",
			section = providerSection
	)
	default Provider provider() { return Provider.None; }

	/* Individual Provider Configuration */

	@ConfigItem(
			keyName = SpotifyInterface.TOKEN_KEY,
			name = "Spotify Refresh Token",
			description = "The refresh token used to obtain the access key for Spotify.",
			section = providerSection,
			hidden = true
	)
	default String refreshToken() { return ""; }

	@ConfigItem(
			keyName = SpotifyInterface.TOKEN_KEY,
			name = "Playerctl Daemon",
			description = "Should the playerctl daemon be initialized when OperatingSystem provider is used.",
			section = providerSection
	)
	void refreshToken(String val);

	@ConfigItem(
			keyName = OSInterface.DAEMON_KEY,
			name = "Playerctl Daemon",
			description = "Should the playerctl daemon be initialized when OperatingSystem provider is used.",
			section = providerSection
	)
	default boolean playerCtlDaemon() { return false; }

	@ConfigItem(
			keyName = OSInterface.DAEMON_KEY,
			name = "Playerctl Daemon",
			description = "Should the playerctl daemon be initialized when OperatingSystem provider is used.",
			section = providerSection
	)
	void playerCtlDaemon(boolean val);

	@ConfigItem(
			keyName = OSInterface.DAEMON_KEY_PROMPT,
			name = "Playerctl Daemon Prompt",
			description = "Has the plugin prompted for the user's choice on the daemon",
			hidden = true)
	default boolean daemonPrompt() { return false; }

	@ConfigItem(
			keyName = OSInterface.DAEMON_KEY_PROMPT,
			name = "Playerctl Daemon Prompt",
			description = "Has the plugin prompted for the user's choice on the daemon")
	void daemonPrompt(boolean val);

	@ConfigItem(
			keyName = "firstRun",
			name = "First Run",
			description = "Has the plugin started up once",
			hidden = true)
	default boolean firstRun() { return false; }

	@ConfigItem(
			keyName = "firstRun",
			name = "First Run",
			description = "Has the plugin started up once")
	void firstRun(boolean val);
}
