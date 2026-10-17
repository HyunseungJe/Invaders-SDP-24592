package engine;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import audio.AudioControls;
import screen.AudioControlScreen;
import screen.GameScreen;
import screen.HighScoreScreen;
import screen.InfiniteGameScreen;
import screen.ScoreScreen;
import screen.Screen;
import screen.TitleScreen;
import campaign.CampaignStageData;
import campaign.CampaignManager;
import screen.SettingsScreen;
import screen.ControlsSettingsScreen;
import screen.GameplaySettingsScreen;
import shop.ShopController;

/**
 * Implements core game logic.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public final class Core {

	/** Width of current screen. */
	private static final int WIDTH = 448;
	/** Height of current screen. */
	private static final int HEIGHT = 520;
	/** Max fps of current screen. */
	private static final int FPS = 60;

	/** Max lives. */
	private static final int MAX_LIVES = 3;
	/** Levels between extra life. */
	private static final int EXTRA_LIFE_FRECUENCY = 3;
	
	/** Frame to draw the screen on. */
	private static Frame frame;
	/** Screen currently shown. */
	private static Screen currentScreen;
	/** Player assets shared across levels and new games in this session. */
	private static PlayerAssets playerAssets;
	/** Shop controller shared for this application session. */
	private static ShopController shopController;
	/** Application logger. */
	private static final Logger LOGGER = Logger.getLogger(Core.class
			.getSimpleName());
	/** Logger handler for printing to disk. */
	private static Handler fileHandler;
	/** Logger handler for printing to console. */
	private static ConsoleHandler consoleHandler;


	/**
	 * Test implementation.
	 * 
	 * @param args
	 *            Program args, ignored.
	 */
	public static void main(final String[] args) {
		try {
			LOGGER.setUseParentHandlers(false);

			fileHandler = new FileHandler("log");
			fileHandler.setFormatter(new MinimalFormatter());

			consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(new MinimalFormatter());

			LOGGER.addHandler(fileHandler);
			LOGGER.addHandler(consoleHandler);
			LOGGER.setLevel(Level.ALL);

		} catch (Exception e) {
			// TODO handle exception
			e.printStackTrace();
		}
		AudioControls.applySettings();

		frame = new Frame(WIDTH, HEIGHT);
		DrawManager.getInstance().setFrame(frame);
		int width = frame.getWidth();
		int height = frame.getHeight();

		playerAssets = new PlayerAssets();
		if (!initializeShopWithRetry()) {
			frame.dispose();
			if (fileHandler != null) {
				fileHandler.close();
			}
			return;
		}
		GameState gameState;

		int returnCode = 1;
		do {
			gameState = new GameState(1, 0, MAX_LIVES, 0, 0);

			switch (returnCode) {
			case 1:
				// Main menu.
				currentScreen = new TitleScreen(width, height, FPS);
				LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
						+ " title screen at " + FPS + " fps.");
				returnCode = frame.setScreen(currentScreen);
				LOGGER.info("Closing title screen.");
				break;
			case 2:
				// Game & score.
				do {
					// One extra live every few levels.
					boolean bonusLife = gameState.getLevel()
							% EXTRA_LIFE_FRECUENCY == 0
							&& gameState.getLivesRemaining() < MAX_LIVES;
					
					currentScreen = new GameScreen(gameState,
							applyDifficulty(CampaignStageData
									.getStageSettings(gameState.getLevel())),
							bonusLife, width, height, FPS);
					LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
							+ " game screen at " + FPS + " fps.");
					frame.setScreen(currentScreen);
					LOGGER.info("Closing game screen.");

					boolean stageCleared = ((GameScreen) currentScreen).isStageCleared();
					gameState = ((GameScreen) currentScreen).getGameState();

					if (!stageCleared) {
						LOGGER.info("Stage " + gameState.getLevel()
								+ " failed. Not advancing.");
						break;
					}

                    CampaignManager.updateMaxCampaignStage(
                            Math.min(gameState.getLevel() + 1,
                                    CampaignStageData.getTotalStages())
                    );
                    LOGGER.info("Highest Stage is: " + CampaignManager.getMaxCampaignStage());

                    gameState = new GameState(gameState.getLevel() + 1,
							gameState.getScore(),
							gameState.getLivesRemaining(),
							gameState.getBulletsShot(),
							gameState.getShipsDestroyed());

				} while (gameState.getLivesRemaining() > 0
						&& gameState.getLevel() <= CampaignStageData.getTotalStages());

				LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
						+ " score screen at " + FPS + " fps, with a score of "
						+ gameState.getScore() + ", "
						+ gameState.getLivesRemaining() + " lives remaining, "
						+ gameState.getBulletsShot() + " bullets shot and "
						+ gameState.getShipsDestroyed() + " ships destroyed.");
				currentScreen = new ScoreScreen(width, height, FPS, gameState);
				returnCode = frame.setScreen(currentScreen);
				LOGGER.info("Closing score screen.");
				break;
			case 3:
				// High scores.
				currentScreen = new HighScoreScreen(width, height, FPS);
				LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
						+ " high score screen at " + FPS + " fps.");
				returnCode = frame.setScreen(currentScreen);
				LOGGER.info("Closing high score screen.");
				break;
			case 4:
				currentScreen = new SettingsScreen(width, height, FPS);
				returnCode = frame.setScreen(currentScreen);
				break;
			case 5:
				currentScreen = new AudioControlScreen(width, height, FPS);
				returnCode = frame.setScreen(currentScreen);
				break;
			case 6:
				currentScreen = new ControlsSettingsScreen(width, height, FPS);
				returnCode = frame.setScreen(currentScreen);
				break;
			case 7:
				currentScreen = new GameplaySettingsScreen(width, height, FPS);
				returnCode = frame.setScreen(currentScreen);
				break;
			case 8:
				// Infinite mode & score.
				currentScreen = new InfiniteGameScreen(gameState, width, height,
						FPS);
				LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
						+ " infinite game screen at " + FPS + " fps.");
				frame.setScreen(currentScreen);
				LOGGER.info("Closing infinite game screen.");

				gameState = ((InfiniteGameScreen) currentScreen).getGameState();

				LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
						+ " score screen at " + FPS + " fps, with a score of "
						+ gameState.getScore() + ", "
						+ gameState.getBulletsShot() + " bullets shot and "
						+ gameState.getShipsDestroyed() + " ships destroyed.");
				// Infinite mode score screen, Space plays infinite mode again.
				currentScreen = new ScoreScreen(width, height, FPS, gameState,
						true);
				returnCode = frame.setScreen(currentScreen);
				LOGGER.info("Closing score screen.");
				break;
			}

		} while (returnCode != 0);

		fileHandler.flush();
		fileHandler.close();
		System.exit(0);
	}

	/** Loads shared assets before creating the session's shop controller. */
	static ShopController initializeShop(final PlayerAssets assets,
			final FileManager files) throws IOException {
		return initializeShop(assets, files, files.loadPlayerAssets());
	}

	static ShopController initializeShop(final PlayerAssets assets,
			final FileManager files, final PlayerAssetsState savedAssets) throws IOException {
		if (!assets.restore(savedAssets)) {
			throw new IOException("Loaded assets could not be restored.");
		}
		return new ShopController(assets, files);
	}

	private static boolean initializeShopWithRetry() {
		while (true) {
			try {
				shopController = initializeShop(playerAssets, getFileManager());
				return true;
			} catch (IOException exception) {
				LOGGER.warning("Player assets could not be loaded: " + exception);
			} catch (SecurityException exception) {
				LOGGER.warning("Player assets access denied: " + exception);
			}
			int choice = JOptionPane.showOptionDialog(frame,
					"Your saved progress could not be loaded.\n"
					+ "Your save has not been changed. Retry or quit?",
					"Unable to load progress", JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null,
					new String[] { "Retry", "Quit" }, "Retry");
			if (choice != 0) {
				return false;
			}
		}
	}

	private static GameSettings applyDifficulty(
			final GameSettings stageSettings) {
		double multiplier = GameplayConfig.getMultiplier();
		int movementInterval = Math.max(1,
				(int) Math.round(stageSettings.getBaseSpeed() * multiplier));
		int shootingInterval = Math.max(250,
				(int) Math.round(
						stageSettings.getShootingFrecuency() * multiplier));

		return new GameSettings(stageSettings.getFormationWidth(),
				stageSettings.getFormationHeight(), movementInterval,
				shootingInterval);
	}

	/**
	 * Constructor, not called.
	 */
	private Core() {

	}

	/**
	 * Controls access to the logger.
	 * 
	 * @return Application logger.
	 */
	public static Logger getLogger() {
		return LOGGER;
	}

	/**
	 * Controls access to the drawing manager.
	 * 
	 * @return Application draw manager.
	 */
	public static DrawManager getDrawManager() {
		return DrawManager.getInstance();
	}

	/**
	 * Controls access to the input manager.
	 * 
	 * @return Application input manager.
	 */
	public static InputManager getInputManager() {
		return InputManager.getInstance();
	}

	/**
	 * Controls access to the file manager.
	 * 
	 * @return Application file manager.
	 */
	public static FileManager getFileManager() {
		return FileManager.getInstance();
	}

	/**
	 * Controls creation of new cooldowns.
	 * 
	 * @param milliseconds
	 *            Duration of the cooldown.
	 * @return A new cooldown.
	 */
	public static Cooldown getCooldown(final int milliseconds) {
		return new Cooldown(milliseconds);
	}

	/**
	 * Controls creation of new cooldowns with variance.
	 * 
	 * @param milliseconds
	 *            Duration of the cooldown.
	 * @param variance
	 *            Variation in the cooldown duration.
	 * @return A new cooldown with variance.
	 */
	public static Cooldown getVariableCooldown(final int milliseconds,
			final int variance) {
		return new Cooldown(milliseconds, variance);
	}
}