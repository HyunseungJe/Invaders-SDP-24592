package engine;

import currency.PlayerAssetsState;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Logger;

import engine.DrawManager.SpriteType;

/**
 * Manages files used in the application.
 */
public final class FileManager {

	/** Singleton instance of the class. */
	private static FileManager instance;
	/** Application logger. */
	private static Logger logger;
	/** Max number of high scores. */
	private static final int MAX_SCORES = 7;

	/**
	 * private constructor.
	 */
	private FileManager() {
		logger = Core.getLogger();
	}

	/**
	 * Returns shared instance of FileManager.
	 *
	 * @return Shared instance of FileManager.
	 */
	protected static FileManager getInstance() {
		if (instance == null)
			instance = new FileManager();
		return instance;
	}

	/**
	 * Resolves a file path relative to the application's code/JAR execution directory.
	 *
	 * @param filename file name to resolve
	 * @return Path pointing to the file in the application storage directory
	 */
	static Path resolveStoragePath(final String filename) {
		try {
			Path codePath = Paths.get(FileManager.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI());

			Path parent = codePath.getParent();
			if (parent != null) {
				return parent.resolve(filename);
			}
		} catch (Exception e) {
			if (logger != null) {
				logger.warning("Failed to resolve application directory: " + e.getMessage());
			}
		}
		return Paths.get(filename);
	}

	/**
	 * Loads sprites from disk.
	 *
	 * @param spriteMap
	 *            Mapping of sprite type and empty boolean matrix that will
	 *            contain the image.
	 * @throws IOException
	 *             In case of loading problems.
	 */
	public void loadSprite(final Map<SpriteType, boolean[][]> spriteMap)
			throws IOException {
		InputStream inputStream = null;

		try {
			inputStream = DrawManager.class.getClassLoader()
					.getResourceAsStream("graphics");
			char c;

			// Sprite loading.
			for (Map.Entry<SpriteType, boolean[][]> sprite : spriteMap
					.entrySet()) {
				for (int i = 0; i < sprite.getValue().length; i++)
					for (int j = 0; j < sprite.getValue()[i].length; j++) {
						do
							c = (char) inputStream.read();
						while (c != '0' && c != '1');

						sprite.getValue()[i][j] = c == '1';
					}
				logger.fine("Sprite " + sprite.getKey() + " loaded.");
			}
			if (inputStream != null)
				inputStream.close();
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
	}

	/**
	 * Loads a font of a given size.
	 *
	 * @param size
	 *            Point size of the font.
	 * @return New font.
	 * @throws IOException
	 *             In case of loading problems.
	 * @throws FontFormatException
	 *             In case of incorrect font format.
	 */
	public Font loadFont(final float size) throws IOException,
			FontFormatException {
		InputStream inputStream = null;
		Font font;

		try {
			// Font loading.
			inputStream = FileManager.class.getClassLoader()
					.getResourceAsStream("font.ttf");
			font = Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(
					size);
		} finally {
			if (inputStream != null)
				inputStream.close();
		}

		return font;
	}

	/**
	 * Returns the application default scores if there is no user high scores
	 * file.
	 *
	 * @return Default high scores.
	 * @throws IOException
	 *             In case of loading problems.
	 */
	private List<Score> loadDefaultHighScores() throws IOException {
		List<Score> highScores = new ArrayList<Score>();
		InputStream inputStream = null;
		BufferedReader reader = null;

		try {
			inputStream = FileManager.class.getClassLoader()
					.getResourceAsStream("scores");
			reader = new BufferedReader(new InputStreamReader(inputStream));

			Score highScore = null;
			String name = reader.readLine();
			String score = reader.readLine();

			while ((name != null) && (score != null)) {
				highScore = new Score(name, Integer.parseInt(score));
				highScores.add(highScore);
				name = reader.readLine();
				score = reader.readLine();
			}
		} finally {
			if (inputStream != null)
				inputStream.close();
		}

		return highScores;
	}

	/**
	 * Loads high scores from file, and returns a sorted list of pairs score -
	 * value.
	 *
	 * @return Sorted list of scores - players.
	 * @throws IOException
	 *             In case of loading problems.
	 */
	public List<Score> loadHighScores() throws IOException {

		List<Score> highScores = new ArrayList<Score>();
		InputStream inputStream = null;
		BufferedReader bufferedReader = null;

		try {
			File scoresFile = resolveStoragePath("scores").toFile();
			inputStream = new FileInputStream(scoresFile);
			bufferedReader = new BufferedReader(new InputStreamReader(
					inputStream, Charset.forName("UTF-8")));

			logger.info("Loading user high scores.");

			Score highScore = null;
			String name = bufferedReader.readLine();
			String score = bufferedReader.readLine();

			while ((name != null) && (score != null)) {
				highScore = new Score(name, Integer.parseInt(score));
				highScores.add(highScore);
				name = bufferedReader.readLine();
				score = bufferedReader.readLine();
			}

		} catch (FileNotFoundException e) {
			// loads default if there's no user scores.
			logger.info("Loading default high scores.");
			highScores = loadDefaultHighScores();
		} finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}

		Collections.sort(highScores);
		return highScores;
	}

	/**
	 * Saves user high scores to disk.
	 *
	 * @param highScores
	 *            High scores to save.
	 * @throws IOException
	 *             In case of loading problems.
	 */
	public void saveHighScores(final List<Score> highScores)
			throws IOException {
		OutputStream outputStream = null;
		BufferedWriter bufferedWriter = null;

		try {
			File scoresFile = resolveStoragePath("scores").toFile();

			if (!scoresFile.exists())
				scoresFile.createNewFile();

			outputStream = new FileOutputStream(scoresFile);
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(
					outputStream, Charset.forName("UTF-8")));

			logger.info("Saving user high scores.");

			// Saves 7 or fewer scores.
			int savedCount = 0;
			for (Score score : highScores) {
				if (savedCount >= MAX_SCORES)
					break;
				bufferedWriter.write(score.getName());
				bufferedWriter.newLine();
				bufferedWriter.write(Integer.toString(score.getScore()));
				bufferedWriter.newLine();
				savedCount++;
			}

		} finally {
			if (bufferedWriter != null)
				bufferedWriter.close();
		}
	}

	public static final String ASSETS_FILENAME = "player_assets.properties";
	public static final String DEFAULT_ASSETS_SAVE_PATH = ASSETS_FILENAME;
	private static final String FORMAT_VERSION = "1";

	private Path assetsSavePath = resolveStoragePath(ASSETS_FILENAME);

	/**
	 * Sets a custom save path for player assets (used for isolated testing).
	 *
	 * @param path custom destination path
	 */
	public void setAssetsSavePath(final Path path) {
		if (path == null) {
			throw new IllegalArgumentException("Save path cannot be null.");
		}
		this.assetsSavePath = path;
	}

	/**
	 * Resets the asset save path back to the default production location.
	 */
	public void resetAssetsSavePath() {
		this.assetsSavePath = resolveStoragePath(ASSETS_FILENAME);
	}

	public Path getAssetsSavePath() {
		return this.assetsSavePath;
	}

	/**
	 * Saves the player asset state to the configured save path.
	 */
	public void savePlayerAssets(final PlayerAssetsState state) throws IOException {
		savePlayerAssets(state, this.assetsSavePath);
	}

	/**
	 * Saves the player asset state using atomic replacement to preserve existing saves upon failure.
	 *
	 * @param state      the player assets state to save
	 * @param targetPath destination file path
	 * @throws IOException if saving fails
	 */
	public void savePlayerAssets(final PlayerAssetsState state, final Path targetPath) throws IOException {
		if (state == null) {
			throw new IllegalArgumentException("PlayerAssetsState cannot be null.");
		}
		Path target = (targetPath != null) ? targetPath : this.assetsSavePath;

		Properties properties = new Properties();
		properties.setProperty("version", FORMAT_VERSION);
		properties.setProperty("balance", String.valueOf(state.getBalance()));

		// Lossless URL encoding to preserve commas, colons, semicolons, and whitespace
		StringBuilder encodedUnlocks = new StringBuilder();
		for (String id : state.getUnlockedContentIds()) {
			if (encodedUnlocks.length() > 0) {
				encodedUnlocks.append(',');
			}
			encodedUnlocks.append(URLEncoder.encode(id, StandardCharsets.UTF_8.name()));
		}
		properties.setProperty("unlockedContentIds", encodedUnlocks.toString());

		StringBuilder encodedItems = new StringBuilder();
		for (Map.Entry<String, Integer> entry : state.getItemQuantities().entrySet()) {
			if (encodedItems.length() > 0) {
				encodedItems.append(',');
			}
			String encodedId = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name());
			encodedItems.append(encodedId).append(':').append(entry.getValue());
		}
		properties.setProperty("itemQuantities", encodedItems.toString());

		Path parent = target.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		// Write to temporary file first so failed saves do not truncate or corrupt the valid save
		Path tempFile = Files.createTempFile(parent != null ? parent : Paths.get("."), "player_assets_", ".tmp");
		try {
			try (OutputStream os = Files.newOutputStream(tempFile)) {
				properties.store(os, "Player Assets Save Data");
			}

			try {
				Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				// Abort without falling back to non-atomic replacement to guarantee the destination file remains intact
				throw new IOException("Atomic replacement is not supported on this filesystem; aborting save to preserve existing file.", e);
			}
			logger.info("Successfully saved player assets state to " + target);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	/**
	 * Loads the player asset state from the configured save path.
	 */
	public PlayerAssetsState loadPlayerAssets() throws IOException {
		return loadPlayerAssets(this.assetsSavePath);
	}

	/**
	 * Loads the player asset state from a specific path with strict validation.
	 *
	 * @param sourcePath file path to load from
	 * @return the loaded PlayerAssetsState, or default empty state if file does not exist
	 * @throws IOException if the file exists but is corrupted, empty, or malformed
	 */
	public PlayerAssetsState loadPlayerAssets(final Path sourcePath) throws IOException {
		Path target = (sourcePath != null) ? sourcePath : this.assetsSavePath;

		// Only a genuinely missing file produces the default state
		if (!Files.exists(target)) {
			logger.info("Save file not found at " + target + ". Returning default initial state.");
			return new PlayerAssetsState(0, Collections.<String>emptySet(),
					Collections.<String, Integer>emptyMap());
		}

		Properties properties = new Properties();
		try (InputStream is = Files.newInputStream(target)) {
			properties.load(is);
		} catch (IllegalArgumentException e) {
			throw new IOException("Malformed properties formatting in save file: " + e.getMessage(), e);
		}

		// 1. Validate format version
		String version = properties.getProperty("version");
		if (version == null) {
			throw new IOException("Missing required property: 'version'");
		}
		if (!FORMAT_VERSION.equals(version.trim())) {
			throw new IOException("Unsupported format version: '" + version + "'");
		}

		// 2. Validate balance
		String balanceStr = properties.getProperty("balance");
		if (balanceStr == null) {
			throw new IOException("Missing required property: 'balance'");
		}
		int balance;
		try {
			balance = Integer.parseInt(balanceStr.trim());
		} catch (NumberFormatException e) {
			throw new IOException("Malformed numeric balance: '" + balanceStr + "'", e);
		}
		if (balance < 0) {
			throw new IOException("Balance cannot be negative: " + balance);
		}

		// 3. Validate unlockedContentIds
		String unlocksRaw = properties.getProperty("unlockedContentIds");
		if (unlocksRaw == null) {
			throw new IOException("Missing required property: 'unlockedContentIds'");
		}
		Set<String> unlockedIds = new HashSet<>();
		if (!unlocksRaw.isEmpty()) {
			String[] tokens = unlocksRaw.split(",", -1);
			for (String token : tokens) {
				if (token.isEmpty()) {
					throw new IOException("Malformed unlockedContentIds: contains empty identifier token");
				}
				String decodedId;
				try {
					decodedId = URLDecoder.decode(token, StandardCharsets.UTF_8.name());
				} catch (IllegalArgumentException e) {
					throw new IOException("Malformed URL-encoded unlocked content ID: '" + token + "'", e);
				}
				if (decodedId.trim().isEmpty()) {
					throw new IOException("Malformed unlocked content ID: cannot be blank");
				}
				if (!unlockedIds.add(decodedId)) {
					throw new IOException("Duplicate unlocked content ID found: " + decodedId);
				}
			}
		}

		// 4. Validate itemQuantities
		String itemsRaw = properties.getProperty("itemQuantities");
		if (itemsRaw == null) {
			throw new IOException("Missing required property: 'itemQuantities'");
		}
		Map<String, Integer> itemQuantities = new HashMap<>();
		if (!itemsRaw.isEmpty()) {
			String[] entries = itemsRaw.split(",", -1);
			for (String entry : entries) {
				if (entry.isEmpty()) {
					throw new IOException("Malformed itemQuantities: contains empty item entry");
				}
				int colonIdx = entry.indexOf(':');
				if (colonIdx <= 0 || colonIdx == entry.length() - 1) {
					throw new IOException("Malformed item entry (expected encodedId:quantity): '" + entry + "'");
				}
				String rawId = entry.substring(0, colonIdx);
				String rawQty = entry.substring(colonIdx + 1);

				String decodedId;
				try {
					decodedId = URLDecoder.decode(rawId, StandardCharsets.UTF_8.name());
				} catch (IllegalArgumentException e) {
					throw new IOException("Malformed URL-encoded item ID: '" + rawId + "' in entry '" + entry + "'", e);
				}
				if (decodedId.trim().isEmpty()) {
					throw new IOException("Malformed item ID: cannot be blank in entry '" + entry + "'");
				}
				int quantity;
				try {
					quantity = Integer.parseInt(rawQty.trim());
				} catch (NumberFormatException e) {
					throw new IOException("Malformed numeric item quantity in entry: '" + entry + "'", e);
				}
				if (quantity <= 0) {
					throw new IOException("Item quantity must be greater than 0: " + quantity);
				}
				if (itemQuantities.put(decodedId, quantity) != null) {
					throw new IOException("Duplicate item ID found in itemQuantities: " + decodedId);
				}
			}
		}

		try {
			return new PlayerAssetsState(balance, unlockedIds, itemQuantities);
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid asset state invariant: " + e.getMessage(), e);
		}
	}
}
