package com.enova.spotify;

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
			section = "configuration"
	)
	default boolean mutingEnabled()
	{
		return true;
	}

	@ConfigItem(
			keyName = "quietVolume",
			name = "Quiet Volume",
			description = "The volume to set when music is playing (0 - 255).",
			section = "muting"
	)
	@Range(min = 0, max = 255)
	default int quietVolume()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "loudVolume",
			name = "Loud Volume",
			description = "The volume to set when music is NOT playing (0 - 255).",
			section = "muting"
	)
	@Range(min = 0, max = 255)
	default int loudVolume()
	{
		return 255;
	}

	@ConfigItem(
			keyName = "mediaControls",
			name = "Media Control Buttons",
			description = "Whether or not to add buttons to the side panel to control the music.",
			section = "configuration"
	)
	default boolean mediaControls()
	{
		return true;
	}

	@ConfigItem(
			keyName = "provider",
			name = "Music Provider",
			description = "Which service to retrieve music playing status from",
			section = providerSection
	)
	default Provider provider() { return Provider.Spotify; }
}
