package audio;

import engine.Core;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Provides global audio control for background music and sound effects.
 * This class manages BGM playback, SFX playback, volume levels,
 * and the global mute state.
 */
public class AudioManager {

    /** Application logger. */
    private static Logger logger;

    /** Clip for the current background music. */
    private static Clip bgmClip;

    /** Mutex for background music playback management. */
    private static final Object BGM_MUTEX = new Object();

    /** Mutex for sound effect playback management. */
    private static final Object SFX_MUTEX = new Object();

    /** Clips indexed by their playback IDs. */
    private static final Map<Integer, Clip> sfxClips = new HashMap<>();

    /** Next sound effect playback ID. */
    private static int nextSFXId = 0;

    /** Background music volume, from 0 to 100; 50 preserves the original gain. */
    private static int bgmVolume = 50;

    /** Sound effect volume, from 0 to 100; 50 preserves the original gain. */
    private static int sfxVolume = 50;

    /** Global mute state, visible across audio control threads. */
    private static volatile boolean muted = false;

    static {
        initialize();
    }

    /**
     * Initializes the audio system and required internal resources.
     */
    private static void initialize() {
        logger = Core.getLogger();
        bgmClip = null;
        logger.info("Audio system initialized.");
    }

    /**
     * Plays the specified background music in a continuous loop.
     * Replaces the current BGM after the new resource is loaded.
     * Playback failures are logged.
     * Use 16-bit PCM WAV resources for compatibility.
     *
     * @param path the resource path relative to the classpath root,
     *             without a leading slash or the res directory prefix
     */
    public static void playBGM(String path) {
        AudioInputStream stream = null;
        Clip newClip = null;
        boolean started = false;

        try {
            if (path == null || path.trim().isEmpty())
                throw new IllegalArgumentException("BGM path must not be empty.");

            URL resource = AudioManager.class.getClassLoader().getResource(path);

            if (resource == null)
                throw new IllegalArgumentException("BGM resource not found: " + path);

            stream = AudioSystem.getAudioInputStream(resource);

            newClip = AudioSystem.getClip();
            newClip.open(stream);
            synchronized (BGM_MUTEX) {
                stopBGM();

                newClip.setFramePosition(0);
                applyVolume(newClip, bgmVolume);
                newClip.loop(Clip.LOOP_CONTINUOUSLY);

                bgmClip = newClip;
                started = true;
            }

            logger.info("Playing BGM: " + path);

        } catch (Exception e) {
            logger.warning(
                    "Failed to play BGM: " + path + " / " + e);

        } finally {
            if (!started && newClip != null)
                newClip.close();

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    logger.warning("Failed to close BGM input stream: " + e);
                }
            }
        }
    }

    /**
     * Stops the currently playing background music.
     */
    public static void stopBGM() {
        synchronized (BGM_MUTEX) {
            if (bgmClip == null) {
                logger.info("No BGM is loaded.");
                return;
            }

            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;

            logger.info("BGM stopped.");
        }
    }

    /**
     * Pauses the currently playing background music.
     */
    public static void pauseBGM() {
        synchronized (BGM_MUTEX) {
            if (bgmClip == null || !bgmClip.isOpen()) {
                logger.warning("Cannot pause BGM: no BGM is loaded.");
                return;
            }

            if (!bgmClip.isRunning()) {
                logger.info("BGM is already paused.");
                return;
            }

            bgmClip.stop();
            logger.info("BGM paused.");
        }
    }

    /**
     * Resumes the previously paused background music.
     */
    public static void resumeBGM() {
        synchronized (BGM_MUTEX) {
            if (bgmClip == null || !bgmClip.isOpen()) {
                logger.warning("Cannot resume BGM: no BGM is loaded.");
                return;
            }

            if (bgmClip.isRunning()) {
                logger.info("BGM is already playing.");
                return;
            }

            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            logger.info("BGM resumed.");
        }
    }

    /**
     * Plays the specified sound effect once.
     * Multiple sound effects can play simultaneously.
     * Each playback receives a separate ID.
     * Finished clips are automatically removed and closed.
     * Playback failures are logged.
     * Use 16-bit PCM WAV resources for compatibility.
     *
     * @param path the resource path relative to the classpath root,
     *             without a leading slash or the res directory prefix
     * @return the playback ID, or -1 if playback fails
     */
    public static int playSFX(String path) {
        AudioInputStream stream = null;
        Clip newClip = null;
        int id = -1;
        boolean registered = false;

        try {
            if (path == null || path.trim().isEmpty())
                throw new IllegalArgumentException("SFX path must not be empty.");

            URL resource = AudioManager.class.getClassLoader().getResource(path);

            if (resource == null)
                throw new IllegalArgumentException("SFX resource not found: " + path);

            stream = AudioSystem.getAudioInputStream(resource);
            newClip = AudioSystem.getClip();
            newClip.open(stream);

            /* for thread-safe */
            synchronized (SFX_MUTEX) {
                id = nextSFXId++;
                final int tid = id;
                newClip.addLineListener(new LineListener() {
                    @Override
                    public void update(LineEvent event) {
                        if (event.getType() != LineEvent.Type.STOP) return;

                        Clip finishedClip;

                        synchronized (SFX_MUTEX) {
                            finishedClip = sfxClips.remove(tid);
                        }

                        if (finishedClip != null) {
                            finishedClip.close();
                            logger.info("SFX finished. ID: " + tid);
                        }
                    }
                });

                sfxClips.put(id, newClip);
                registered = true;
                applyVolume(newClip, sfxVolume);
                newClip.start();
            }

            logger.info("Playing SFX: " + path + " / ID: " + id);
            return id;
        } catch (Exception e) {
            if (registered) {
                stopSFX(id);
            } else if (newClip != null) {
                newClip.close();
            }
            logger.warning("Failed to play SFX: " + path + " / " + e);
            return -1;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    logger.warning("Failed to close SFX input stream: " + e);
                }
            }
        }
    }

    /**
     * Stops the specified sound effect and releases its audio resources.
     * If the ID is unknown or already completed, logs a warning
     * and returns without stopping any other sound effect.
     *
     * @param id the playback ID returned by playSFX
     */
    public static void stopSFX(int id) {
        Clip clip;

        synchronized (SFX_MUTEX) {
            clip = sfxClips.remove(id);
        }

        if (clip == null) {
            logger.warning("No active SFX found. ID: " + id);
            return;
        }

        clip.stop();
        clip.close();

        logger.info("SFX stopped. ID: " + id);
    }

    /**
     * Sets the background music volume.
     * Invalid volume values are logged and ignored.
     *
     * @param vol the volume level, from 0 to 100
     */
    public static void setBGMVolume(int vol) {
        if (vol < 0 || vol > 100) {
            logger.warning("Volume must be an integer between 0 and 100.");
            return;
        }

        synchronized (BGM_MUTEX) {
            bgmVolume = vol;
            applyVolume(bgmClip, vol);
        }

        logger.info("BGM volume set to " + vol);
    }

    /**
     * Sets the sound effect volume.
     * Invalid volume values are logged and ignored.
     *
     * @param vol the volume level, from 0 to 100
     */
    public static void setSFXVolume(int vol) {
        if (vol < 0 || vol > 100) {
            logger.warning("Volume must be an integer between 0 and 100.");
            return;
        }

        synchronized (SFX_MUTEX) {
            sfxVolume = vol;
            for (Clip clip : sfxClips.values())
                applyVolume(clip, vol);
        }

        logger.info("SFX volume set to " + vol);
    }

    /**
     * Returns the current background music volume.
     *
     * @return the BGM volume level, from 0 to 100
     */
    public static int getBGMVolume() {
        synchronized (BGM_MUTEX) {
            return bgmVolume;
        }
    }

    /**
     * Returns the current sound effect volume.
     *
     * @return the SFX volume level, from 0 to 100
     */
    public static int getSFXVolume() {
        synchronized (SFX_MUTEX) {
            return sfxVolume;
        }
    }

    /**
     * Applies the specified volume and global mute setting to an open clip.
     * Muting uses the minimum supported gain.
     *
     * @param clip the clip to update
     * @param vol the volume level, from 0 to 100
     */
    private static void applyVolume(Clip clip, int vol) {
        if (clip == null || !clip.isOpen()) return;

        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            logger.warning("Volume control is not supported.");
            return;
        }

        if (muted) vol = 0;

        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

        float db = vol == 0 ? gain.getMinimum() : (float) (20.0 * Math.log10(vol / 50.0));

        db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
        gain.setValue(db);
    }

    /**
     * Updates the global mute state without changing stored volume levels.
     *
     * @param value true to mute audio, false to restore audio
     */
    public static synchronized void setMuted(boolean value) {
        muted = value;

        synchronized (BGM_MUTEX) {
            applyVolume(bgmClip, bgmVolume);
        }

        synchronized (SFX_MUTEX) {
            for (Clip clip : sfxClips.values()) {
                applyVolume(clip, sfxVolume);
            }
        }

        logger.info(value ? "Audio muted." : "Audio unmuted.");
    }

    /**
     * Returns the global mute setting.
     *
     * @return true if muting is enabled, otherwise false
     */
    public static boolean isMuted() {
        return muted;
    }
}
