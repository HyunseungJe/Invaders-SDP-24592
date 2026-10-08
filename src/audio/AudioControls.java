package audio;

import engine.Core;

import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Persists audio settings for background music and sound effects.
 * Saved settings are kept after the game exits.
 * Settings are stored with java.util.prefs.Preferences in the
 * user's preference storage, not in a project file.
 */
public class AudioControls {

    /** Application logger. */
    private static final Logger logger = Core.getLogger();

    /** Preference node storing the audio settings for this package. */
    private static final Preferences prefs = Preferences.userNodeForPackage(AudioControls.class);

    /** Preference key for the background music volume. */
    private static final String BGM_VOLUME_KEY = "bgmVolume";

    /** Preference key for the sound effect volume. */
    private static final String SFX_VOLUME_KEY = "sfxVolume";

    /** Preference key for the global mute state. */
    private static final String MUTED_KEY = "muted";

    /** Volume used when no valid volume has been saved. */
    private static final int DEFAULT_VOLUME = 50;

    /**
     * Saves the background music volume.
     * Invalid volume values are logged and ignored.
     *
     * @param vol the volume level, from 0 to 100
     */
    public static void saveBGMVolume(int vol) {
        if (vol < 0 || vol > 100) {
            logger.warning("Volume must be an integer between 0 and 100.");
            return;
        }

        prefs.putInt(BGM_VOLUME_KEY, vol);
        logger.info("Saved BGM volume " + vol);
    }

    /**
     * Saves the sound effect volume.
     * Invalid volume values are logged and ignored.
     *
     * @param vol the volume level, from 0 to 100
     */
    public static void saveSFXVolume(int vol) {
        if (vol < 0 || vol > 100) {
            logger.warning("Volume must be an integer between 0 and 100.");
            return;
        }

        prefs.putInt(SFX_VOLUME_KEY, vol);
        logger.info("Saved SFX volume " + vol);
    }

    /**
     * Saves the global mute state.
     *
     * @param muted true if audio is muted, otherwise false
     */
    public static void saveMuted(boolean muted) {
        prefs.putBoolean(MUTED_KEY, muted);
        logger.info("Saved mute state " + muted);
    }

    /**
     * Loads the saved background music volume.
     * Returns the default volume if nothing is saved.
     * Invalid saved values are logged and replaced with the default volume.
     *
     * @return the BGM volume level, from 0 to 100
     */
    public static int loadBGMVolume() {
        int vol = prefs.getInt(BGM_VOLUME_KEY, DEFAULT_VOLUME);

        if (vol < 0 || vol > 100) {
            logger.warning("Saved BGM volume " + vol + " is invalid. Using " + DEFAULT_VOLUME + ".");
            return DEFAULT_VOLUME;
        }

        return vol;
    }

    /**
     * Loads the saved sound effect volume.
     * Returns the default volume if nothing is saved.
     * Invalid saved values are logged and replaced with the default volume.
     *
     * @return the SFX volume level, from 0 to 100
     */
    public static int loadSFXVolume() {
        int vol = prefs.getInt(SFX_VOLUME_KEY, DEFAULT_VOLUME);

        if (vol < 0 || vol > 100) {
            logger.warning("Saved SFX volume " + vol + " is invalid. Using " + DEFAULT_VOLUME + ".");
            return DEFAULT_VOLUME;
        }

        return vol;
    }

    /**
     * Loads the saved global mute state.
     * Returns false if nothing is saved or the saved value is not true or false.
     *
     * @return true if audio is muted, otherwise false
     */
    public static boolean loadMuted() {
        return prefs.getBoolean(MUTED_KEY, false);
    }
}
