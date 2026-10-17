package engine;

import entity.item.ChameleonHull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import screen.Screen;
import currency.PlayerAssetsState;
import currency.ShopCatalog.Category;
import shop.ShopController.SaveStatus;
import shop.ShopProduct;
import entity.Entity;
import entity.Ship;

/**
 * Manages screen drawing.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class DrawManager {

	/** Singleton instance of the class. */
	private static DrawManager instance;
	/** Current frame. */
	private static Frame frame;
	/** FileManager instance. */
	private static FileManager fileManager;
	/** Application logger. */
	private static Logger logger;
	/** Graphics context. */
	private static Graphics graphics;
	/** Buffer Graphics. */
	private static Graphics backBufferGraphics;
	/** Buffer image. */
	private static BufferedImage backBuffer;
	/** Normal sized font. */
	private static Font fontRegular;
	/** Normal sized font properties. */
	private static FontMetrics fontRegularMetrics;
	/** Big sized font. */
	private static Font fontBig;
	/** Big sized font properties. */
	private static FontMetrics fontBigMetrics;

	/** Sprite types mapped to their images. */
	private static Map<SpriteType, boolean[][]> spriteMap;

	/** Sprite types. */
	public static enum SpriteType {
		/** Player ship. */
		Ship,
		/** Destroyed player ship. */
		ShipDestroyed,
		/** Player bullet. */
		Bullet,
		/** Enemy bullet. */
		EnemyBullet,
		/** First enemy ship - first form. */
		EnemyShipA1,
		/** First enemy ship - second form. */
		EnemyShipA2,
		/** Second enemy ship - first form. */
		EnemyShipB1,
		/** Second enemy ship - second form. */
		EnemyShipB2,
		/** Third enemy ship - first form. */
		EnemyShipC1,
		/** Third enemy ship - second form. */
		EnemyShipC2,
		/** Bonus ship. */
		EnemyShipSpecial,
		/** Destroyed enemy ship. */
		Explosion
	};

	/**
	 * Private constructor.
	 */
	private DrawManager() {
		fileManager = Core.getFileManager();
		logger = Core.getLogger();
		logger.info("Started loading resources.");

		try {
			spriteMap = new LinkedHashMap<SpriteType, boolean[][]>();

			spriteMap.put(SpriteType.Ship, new boolean[13][8]);
			spriteMap.put(SpriteType.ShipDestroyed, new boolean[13][8]);
			spriteMap.put(SpriteType.Bullet, new boolean[3][5]);
			spriteMap.put(SpriteType.EnemyBullet, new boolean[3][5]);
			spriteMap.put(SpriteType.EnemyShipA1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipA2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipB1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipB2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipC1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipC2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipSpecial, new boolean[16][7]);
			spriteMap.put(SpriteType.Explosion, new boolean[13][7]);

			fileManager.loadSprite(spriteMap);
			logger.info("Finished loading the sprites.");

			// Font loading.
			fontRegular = fileManager.loadFont(14f);
			fontBig = fileManager.loadFont(24f);
			logger.info("Finished loading the fonts.");

		} catch (IOException e) {
			logger.warning("Loading failed.");
		} catch (FontFormatException e) {
			logger.warning("Font formating failed.");
		}
	}

	/**
	 * Returns shared instance of DrawManager.
	 * 
	 * @return Shared instance of DrawManager.
	 */
	protected static DrawManager getInstance() {
		if (instance == null)
			instance = new DrawManager();
		return instance;
	}

	/**
	 * Sets the frame to draw the image on.
	 * 
	 * @param currentFrame
	 *            Frame to draw on.
	 */
	public void setFrame(final Frame currentFrame) {
		frame = currentFrame;
	}

	/**
	 * First part of the drawing process. Initialices buffers, draws the
	 * background and prepares the images.
	 * 
	 * @param screen
	 *            Screen to draw in.
	 */
	public void initDrawing(final Screen screen) {
		backBuffer = new BufferedImage(screen.getWidth(), screen.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		graphics = frame.getGraphics();
		backBufferGraphics = backBuffer.getGraphics();

		backBufferGraphics.setColor(Color.BLACK);
		backBufferGraphics
				.fillRect(0, 0, screen.getWidth(), screen.getHeight());
		screen.drawBackground(backBufferGraphics);

		fontRegularMetrics = backBufferGraphics.getFontMetrics(fontRegular);
		fontBigMetrics = backBufferGraphics.getFontMetrics(fontBig);

		// drawBorders(screen);
		// drawGrid(screen);
	}

	/**
	 * Draws the completed drawing on screen.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 */
	public void completeDrawing(final Screen screen) {
		graphics.drawImage(backBuffer, frame.getInsets().left,
				frame.getInsets().top, frame);
	}

	/**
	 * Draws an entity, using the apropiate image.
	 * 
	 * @param entity
	 *            Entity to be drawn.
	 * @param positionX
	 *            Coordinates for the left side of the image.
	 * @param positionY
	 *            Coordinates for the upper side of the image.
	 */
	public void drawEntity(final Entity entity, final int positionX,
			final int positionY) {
		boolean[][] image = spriteMap.get(entity.getSpriteType());

		if (entity instanceof ChameleonHull) {
			image = ChameleonHull.getStarSprite();
		}

		if (entity instanceof Ship
				&& ((Ship) entity).isChameleonHullActive()) {
			image = ChameleonHull.getBallSprite();
		}

		backBufferGraphics.setColor(entity.getColor());
		for (int i = 0; i < image.length; i++)
			for (int j = 0; j < image[i].length; j++)
				if (image[i][j])
					backBufferGraphics.drawRect(positionX + i * 2, positionY
							+ j * 2, 1, 1);
	}

	/**
	 * For debugging purpouses, draws the canvas borders.
	 * 
	 * @param screen
	 *            Screen to draw in.
	 */
	@SuppressWarnings("unused")
	private void drawBorders(final Screen screen) {
		backBufferGraphics.setColor(Color.GREEN);
		backBufferGraphics.drawLine(0, 0, screen.getWidth() - 1, 0);
		backBufferGraphics.drawLine(0, 0, 0, screen.getHeight() - 1);
		backBufferGraphics.drawLine(screen.getWidth() - 1, 0,
				screen.getWidth() - 1, screen.getHeight() - 1);
		backBufferGraphics.drawLine(0, screen.getHeight() - 1,
				screen.getWidth() - 1, screen.getHeight() - 1);
	}

	/**
	 * For debugging purpouses, draws a grid over the canvas.
	 * 
	 * @param screen
	 *            Screen to draw in.
	 */
	@SuppressWarnings("unused")
	private void drawGrid(final Screen screen) {
		backBufferGraphics.setColor(Color.DARK_GRAY);
		for (int i = 0; i < screen.getHeight() - 1; i += 2)
			backBufferGraphics.drawLine(0, i, screen.getWidth() - 1, i);
		for (int j = 0; j < screen.getWidth() - 1; j += 2)
			backBufferGraphics.drawLine(j, 0, j, screen.getHeight() - 1);
	}

	/**
	 * Draws current score on screen.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param score
	 *            Current score.
	 */
	public void drawScore(final Screen screen, final int score) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		String scoreString = String.format("%04d", score);
		String scoreLabel = "SCORE ";

		int scoreX = screen.getWidth() - 60;
		int labelWidth = backBufferGraphics.getFontMetrics().stringWidth(scoreLabel);

		backBufferGraphics.drawString(scoreLabel, scoreX - labelWidth, 25);
		backBufferGraphics.drawString(scoreString, scoreX, 25);
	}

	/**
	 * Draws remaining lives as ship icons on screen.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param lives
	 *            Current lives.
	 */
	public void drawLives(final Screen screen, final int lives) {
		Ship dummyShip = new Ship(0, 0);
		for (int i = 0; i < lives; i++)
			drawEntity(dummyShip, 20 + 35 * i, 10);
	}

	/**
	 * Draws a thick line from side to side of the screen.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param positionY
	 *            Y coordinate of the line.
	 */
	public void drawHorizontalLine(final Screen screen, final int positionY) {
		backBufferGraphics.setColor(Color.GREEN);
		backBufferGraphics.drawLine(0, positionY, screen.getWidth(), positionY);
		backBufferGraphics.drawLine(0, positionY + 1, screen.getWidth(),
				positionY + 1);
	}

	/**
	 * Draws the Orbital Annihilator: a growing energy charge on the ship's
	 * nose while charging, then a full vertical beam going up from the ship.
	 */
	public void drawOrbitalAnnihilator(final Ship ship, final int beamWidth,
			final int topY, final boolean firing, final float chargeProgress) {
		int centerX = ship.getPositionX() + ship.getWidth() / 2;
		int startY = ship.getPositionY();
		int beamX = centerX - beamWidth / 2;

		if (firing) {
			backBufferGraphics.setColor(new Color(255, 0, 0, 120));
			backBufferGraphics.fillRect(beamX - 4, topY, beamWidth + 8, startY - topY);

			backBufferGraphics.setColor(Color.RED);
			backBufferGraphics.fillRect(beamX, topY, beamWidth, startY - topY);

			backBufferGraphics.setColor(Color.WHITE);
			backBufferGraphics.fillRect(centerX - beamWidth / 4, topY, beamWidth / 2, startY - topY);
		} else {
			backBufferGraphics.setColor(new Color(255, 0, 0, 90));
			backBufferGraphics.fillRect(centerX - 1, topY, 2, startY - topY);

			int radius = 2 + (int) (chargeProgress * beamWidth / 2);
			boolean flicker = ((int) (chargeProgress * 20)) % 2 == 0;

			backBufferGraphics.setColor(flicker ? Color.WHITE : Color.RED);
			backBufferGraphics.fillOval(centerX - radius, startY - radius, radius * 2, radius * 2);
		}
	}

	/**
	 * Draws the Orbital Annihilator recharge gauge under the score.
	 *
	 * @param screen
	 *            Screen to draw on.
	 * @param progress
	 *            Recharge progress between 0 and 1.
	 */
	public void drawAnnihilatorGauge(final Screen screen, final float progress) {
		int barWidth = 80;
		int barX = screen.getWidth() - barWidth - 20;
		int barY = 29;

		backBufferGraphics.setColor(Color.DARK_GRAY);
		backBufferGraphics.fillRect(barX, barY, barWidth, 6);

		backBufferGraphics.setColor(progress >= 1f ? Color.GREEN : Color.RED);
		backBufferGraphics.fillRect(barX, barY, (int) (barWidth * progress), 6);
	}

	/**
	 * Draws game title.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 */
	public void drawTitle(final Screen screen) {
		String titleString = "Invaders";
		String instructionsString =
				"select with w+s / arrows, confirm with space";

		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, instructionsString,
				screen.getHeight() / 2);

		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, titleString, screen.getHeight() / 3);
	}

	/**
	 * Draws main menu.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param option
	 *            Option selected.
	 */
	public void drawMenu(final Screen screen, final int option) {
		String playString = "Play";
		String highScoresString = "High scores";
		String settingsString = "Settings";
		String exitString = "exit";

		if (option == 2)
			backBufferGraphics.setColor(Color.GREEN);
		else
			backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, playString,
				screen.getHeight() / 3 * 2);
		if (option == 3)
			backBufferGraphics.setColor(Color.GREEN);
		else
			backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, highScoresString, screen.getHeight()
				/ 3 * 2 + fontRegularMetrics.getHeight() * 2);
		if (option == 4)
			backBufferGraphics.setColor(Color.GREEN);
		else
			backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, settingsString, screen.getHeight()
				/ 3 * 2 + fontRegularMetrics.getHeight() * 4);
		if (option == 0)
			backBufferGraphics.setColor(Color.GREEN);
		else
			backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, exitString, screen.getHeight() / 3
				* 2 + fontRegularMetrics.getHeight() * 6);
	}

	/**
	 * Draws game results.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param score
	 *            Score obtained.
	 * @param livesRemaining
	 *            Lives remaining when finished.
	 * @param shipsDestroyed
	 *            Total ships destroyed.
	 * @param accuracy
	 *            Total accuracy.
	 * @param isNewRecord
	 *            If the score is a new high score.
	 */
	public void drawResults(final Screen screen, final int score,
			final int livesRemaining, final int shipsDestroyed,
			final float accuracy, final boolean isNewRecord) {
		String scoreString = String.format("score %04d", score);
		String livesRemainingString = "lives remaining " + livesRemaining;
		String shipsDestroyedString = "enemies destroyed " + shipsDestroyed;
		String accuracyString = String
				.format("accuracy %.2f%%", accuracy * 100);

		int height = isNewRecord ? 4 : 2;

		backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, scoreString, screen.getHeight()
				/ height);
		drawCenteredRegularString(screen, livesRemainingString,
				screen.getHeight() / height + fontRegularMetrics.getHeight()
						* 2);
		drawCenteredRegularString(screen, shipsDestroyedString,
				screen.getHeight() / height + fontRegularMetrics.getHeight()
						* 4);
		drawCenteredRegularString(screen, accuracyString, screen.getHeight()
				/ height + fontRegularMetrics.getHeight() * 6);
	}

	/**
	 * Draws interactive characters for name input.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param name
	 *            Current name selected.
	 * @param nameCharSelected
	 *            Current character selected for modification.
	 */
	public void drawNameInput(final Screen screen, final char[] name,
			final int nameCharSelected) {
		String newRecordString = "New Record!";
		String introduceNameString = "Introduce name:";

		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredRegularString(screen, newRecordString, screen.getHeight()
				/ 4 + fontRegularMetrics.getHeight() * 10);
		backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, introduceNameString,
				screen.getHeight() / 4 + fontRegularMetrics.getHeight() * 12);

		// 3 letters name.
		int positionX = screen.getWidth()
				/ 2
				- (fontRegularMetrics.getWidths()[name[0]]
						+ fontRegularMetrics.getWidths()[name[1]]
						+ fontRegularMetrics.getWidths()[name[2]]
								+ fontRegularMetrics.getWidths()[' ']) / 2;

		for (int i = 0; i < 3; i++) {
			if (i == nameCharSelected)
				backBufferGraphics.setColor(Color.GREEN);
			else
				backBufferGraphics.setColor(Color.WHITE);

			positionX += fontRegularMetrics.getWidths()[name[i]] / 2;
			positionX = i == 0 ? positionX
					: positionX
							+ (fontRegularMetrics.getWidths()[name[i - 1]]
									+ fontRegularMetrics.getWidths()[' ']) / 2;

			backBufferGraphics.drawString(Character.toString(name[i]),
					positionX,
					screen.getHeight() / 4 + fontRegularMetrics.getHeight()
							* 14);
		}
	}

	/**
	 * Draws basic content of game over screen.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param acceptsInput
	 *            If the screen accepts input.
	 * @param isNewRecord
	 *            If the score is a new high score.
	 * @param gameCleared
	 *            If the game was cleared successfully.
	 */
	public void drawGameOver(final Screen screen, final boolean acceptsInput,
			final boolean isNewRecord, final boolean gameCleared) {
		String gameOverString = gameCleared ? "Game Clear" : "Game Over";
		String continueOrExitString =
				"Press Space to play again, Escape to exit";

		int height = isNewRecord ? 4 : 2;

		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, gameOverString, screen.getHeight()
				/ height - fontBigMetrics.getHeight() * 2);

		if (acceptsInput)
			backBufferGraphics.setColor(Color.GREEN);
		else
			backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, continueOrExitString,
				screen.getHeight() / 2 + fontRegularMetrics.getHeight() * 10);
	}

	/**
	 * Draws which stage was failed, right below the game over title.
	 *
	 * @param screen
	 *            Screen to draw on.
	 * @param stage
	 *            Number of the stage that was failed.
	 * @param isNewRecord
	 *            If the score is a new high score (changes the layout).
	 */
	public void drawStageFailed(final Screen screen, final int stage,
			final boolean isNewRecord) {
		int height = isNewRecord ? 4 : 2;

		backBufferGraphics.setColor(Color.RED);
		drawCenteredRegularString(screen, "Stage " + stage + " Failed",
				screen.getHeight() / height - fontBigMetrics.getHeight());
	}

	/**
	 * Draws high score screen title and instructions.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 */
	public void drawHighScoreMenu(final Screen screen) {
		String highScoreString = "High Scores";
		String instructionsString = "Press Space to return";

		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, highScoreString, screen.getHeight() / 8);

		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, instructionsString,
				screen.getHeight() / 5);
	}

	/**
	 * Draws high scores.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param highScores
	 *            List of high scores.
	 */
	public void drawHighScores(final Screen screen,
			final List<Score> highScores) {
		backBufferGraphics.setColor(Color.WHITE);
		int i = 0;
		String scoreString = "";

		for (Score score : highScores) {
			scoreString = String.format("%s        %04d", score.getName(),
					score.getScore());
			drawCenteredRegularString(screen, scoreString, screen.getHeight()
					/ 4 + fontRegularMetrics.getHeight() * (i + 1) * 2);
			i++;
		}
	}

	/**
	 * Draws a centered string on regular font.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param string
	 *            String to draw.
	 * @param height
	 *            Height of the drawing.
	 */
	public void drawCenteredRegularString(final Screen screen,
			final String string, final int height) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.drawString(string, screen.getWidth() / 2
				- fontRegularMetrics.stringWidth(string) / 2, height);
	}

	/**
	 * Draws a centered string on big font.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param string
	 *            String to draw.
	 * @param height
	 *            Height of the drawing.
	 */
	public void drawCenteredBigString(final Screen screen, final String string,
			final int height) {
		backBufferGraphics.setFont(fontBig);
		backBufferGraphics.drawString(string, screen.getWidth() / 2
				- fontBigMetrics.stringWidth(string) / 2, height);
	}

	/**
	 * Countdown to game start.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param level
	 *            Level number.
	 * @param number
	 *            Countdown number.
	 * @param bonusLife
	 *            Checks if a bonus life is received.
	 */
	public void drawCountDown(final Screen screen, final int level,
			final int number, final boolean bonusLife) {
		String title = "Level " + level;
		if (bonusLife)
			title += " - Bonus life!";

		drawCountDown(screen, title, number);
	}

	/**
	 * Draws the countdown with the given title.
	 *
	 * @param screen
	 *            Screen to draw on.
	 * @param title
	 *            Title shown before the countdown numbers.
	 * @param number
	 *            Countdown number.
	 */
	public void drawCountDown(final Screen screen, final String title,
			final int number) {
		int rectWidth = screen.getWidth();
		int rectHeight = screen.getHeight() / 6;

		backBufferGraphics.setColor(Color.BLACK);
		backBufferGraphics.fillRect(0,
				screen.getHeight() / 2 - rectHeight / 2,
				rectWidth, rectHeight);
		backBufferGraphics.setColor(Color.GREEN);

		if (number >= 4)
			drawCenteredBigString(screen, title,
					screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
		else if (number != 0)
			drawCenteredBigString(screen, Integer.toString(number),
					screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
		else
			drawCenteredBigString(screen, "GO!",
					screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
	}

    /** Draws the shop cards and purchase/save feedback. */
    public void drawShop(final Screen screen, final List<ShopProduct> items,
            final List<ShopProduct> unlocks, final PlayerAssetsState assets, final int section,
            final int itemIndex, final int unlockIndex, final int actionIndex,
            final String feedback, final boolean purchaseSucceeded, final SaveStatus saveStatus) {
        int width = screen.getWidth();
        int height = screen.getHeight();
        backBufferGraphics.setColor(Color.GREEN);
        drawCenteredBigString(screen, "Shop", 34);
        backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, "Credits: " + assets.getBalance(), 61);

        int cardSize = Math.max(76, Math.min(120, Math.min((width - 80) / 3, (height - 248) / 2)));
        drawShopShelf(screen, items, assets, "Items", "No items available",
                96, cardSize, itemIndex, section == 0);
        drawShopShelf(screen, unlocks, assets, "Unlocks", "No unlocks available",
                132 + cardSize, cardSize, unlockIndex, section == 1);

        int actionsY = height - 96;
        drawShopText("Back (Esc)", 31, actionsY, 12,
                section == 2 && actionIndex == 0 ? Color.GREEN : Color.WHITE, 120);
        if (saveStatus == SaveStatus.FAILED) {
            drawShopText("Retry save (R)", width / 2, actionsY, 12,
                    section == 2 && actionIndex == 1 ? Color.GREEN : Color.WHITE, width / 2 - 24);
        }
        backBufferGraphics.setColor(purchaseSucceeded ? Color.GREEN : Color.ORANGE);
        drawShopCentered(screen, feedback, height - 70);
        String saveMessage = saveStatus == SaveStatus.FAILED ? "Save failed. Progress kept this session."
                : saveStatus == SaveStatus.SAVED ? "Progress saved." : "";
        backBufferGraphics.setColor(saveStatus == SaveStatus.FAILED ? Color.ORANGE : Color.GRAY);
        drawShopCentered(screen, saveMessage, height - 52);
        backBufferGraphics.setColor(Color.GRAY);
        drawShopCentered(screen, "Up/Down: section   Left/Right: product", height - 37);
        drawShopCentered(screen, "WASD also works   Space/Enter: confirm", height - 21);
    }

    private void drawShopShelf(Screen screen, List<ShopProduct> products, PlayerAssetsState assets,
            String heading, String emptyText, int top, int size, int index, boolean active) {
        int width = screen.getWidth();
        drawShopText(heading, 31, top - 12, 12, active ? Color.GREEN : Color.WHITE, width - 110);
        String count = products.isEmpty() ? "0 / 0" : (index + 1) + " / " + products.size();
        drawShopText(count, width - 70, top - 12, 10, Color.GRAY, 52);
        if (products.isEmpty()) {
            backBufferGraphics.setColor(Color.DARK_GRAY);
            backBufferGraphics.drawRect(31, top, width - 63, size);
            backBufferGraphics.setColor(Color.GRAY);
            drawShopCentered(screen, emptyText, top + size / 2);
            return;
        }

        int first = index / 3 * 3;
        int end = Math.min(products.size(), first + 3);
        int rowWidth = (end - first) * size + (end - first - 1) * 10;
        int startX = (width - rowWidth) / 2;
        for (int productIndex = first; productIndex < end; productIndex++) {
            drawShopCard(products.get(productIndex), assets,
                    startX + (productIndex - first) * (size + 10), top, size,
                    active && productIndex == index);
        }
        if (products.size() > 1) {
            int cy = top + size / 2;
            backBufferGraphics.setColor(active ? Color.GREEN : Color.GRAY);
            backBufferGraphics.fillPolygon(new int[] {12, 18, 18}, new int[] {cy, cy - 5, cy + 5}, 3);
            backBufferGraphics.fillPolygon(new int[] {width - 13, width - 19, width - 19},
                    new int[] {cy, cy - 5, cy + 5}, 3);
        }
    }

    private void drawShopCard(ShopProduct product, PlayerAssetsState assets,
            int x, int y, int size, boolean selected) {
        backBufferGraphics.setColor(selected ? Color.GREEN : Color.DARK_GRAY);
        backBufferGraphics.drawRect(x, y, size, size);
        boolean permanent = product.getCategory() == Category.UNLOCKABLE;
        boolean owned = permanent && assets.getUnlockedContentIds().contains(product.getId());
        Integer quantity = assets.getItemQuantities().get(product.getId());
        String badge = owned ? "Owned" : !permanent ? "x" + (quantity == null ? 0 : quantity) : "";
        drawShopText(badge, x + 7, y + 12, 9, Color.GRAY, size - 14);
        backBufferGraphics.setColor(selected ? Color.GREEN : Color.WHITE);
        drawShopCategoryIcon(permanent, x + size / 2 - 10, y + 20);
        drawShopText(product.getName(), x + 9, y + 60, 11,
                selected ? Color.GREEN : Color.WHITE, size - 18);
        drawShopDescription(product.getDescription(), x + 9, y + 75, size - 18);
        backBufferGraphics.setColor(Color.DARK_GRAY);
        backBufferGraphics.drawLine(x + 9, y + size - 25, x + size - 9, y + size - 25);
        String price = owned ? "Unlocked" : product.getPrice() + " C";
        drawShopText(price, x + 9, y + size - 9, 11, owned ? Color.GRAY : Color.WHITE, size - 18);
    }

    /** Neutral placeholders until content owners supply agreed product artwork. */
    private void drawShopCategoryIcon(boolean permanent, int x, int y) {
        if (permanent) {
            backBufferGraphics.drawRect(x + 5, y, 10, 9);
            backBufferGraphics.fillRect(x, y + 8, 21, 15);
            backBufferGraphics.setColor(Color.BLACK);
            backBufferGraphics.fillRect(x + 9, y + 12, 3, 7);
        } else {
            backBufferGraphics.drawRect(x, y + 3, 20, 19);
            backBufferGraphics.drawRect(x + 1, y + 4, 18, 17);
            backBufferGraphics.drawLine(x + 1, y + 9, x + 19, y + 9);
            backBufferGraphics.drawRect(x + 7, y + 1, 6, 4);
        }
    }

    private void drawShopDescription(String description, int x, int y, int width) {
        backBufferGraphics.setFont(fontRegular.deriveFont(10f));
        backBufferGraphics.setColor(Color.GRAY);
        FontMetrics metrics = backBufferGraphics.getFontMetrics();
        String remaining = description.trim();
        for (int line = 0; line < 2 && !remaining.isEmpty(); line++) {
            String text = remaining;
            if (line == 0 && metrics.stringWidth(text) > width) {
                int end = text.length();
                while (end > 1 && metrics.stringWidth(text.substring(0, end)) > width) end--;
                int space = text.lastIndexOf(' ', end);
                if (space > 0) end = space;
                text = remaining.substring(0, end);
                remaining = remaining.substring(end).trim();
            } else {
                remaining = "";
            }
            drawShopLabel(text, x, y + line * 12, width);
        }
    }

    private void drawShopText(String text, int x, int y, int size, Color color, int width) {
        backBufferGraphics.setFont(fontRegular.deriveFont((float) size));
        backBufferGraphics.setColor(color);
        drawShopLabel(text, x, y, width);
    }

    private void drawShopCentered(final Screen screen, final String text, final int y) {
        Font fitted = fontRegular.deriveFont(11f);
        while (fitted.getSize() > 9
                && backBufferGraphics.getFontMetrics(fitted).stringWidth(text) > screen.getWidth() - 32) {
            fitted = fitted.deriveFont((float) fitted.getSize() - 1);
        }
        backBufferGraphics.setFont(fitted);
        int x = Math.max(16, (screen.getWidth() - backBufferGraphics.getFontMetrics().stringWidth(text)) / 2);
        drawShopLabel(text, x, y, screen.getWidth() - x - 16);
    }

    private void drawShopLabel(final String text, final int x, final int y, final int maxWidth) {
        String label = text;
        FontMetrics metrics = backBufferGraphics.getFontMetrics();
        if (metrics.stringWidth(label) > maxWidth) {
            while (!label.isEmpty() && metrics.stringWidth(label + "...") > maxWidth) {
                label = label.substring(0, label.length() - 1);
            }
            label += "...";
        }
        backBufferGraphics.drawString(label, x, y);
    }

	public void drawSettingsMenu(final Screen screen, final int selectedOption) {
		String[] options = { "Audio", "Controls", "Gameplay", "Back" };

		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "Settings", screen.getHeight() / 5);

		for (int i = 0; i < options.length; i++) {
			backBufferGraphics.setColor(
					i == selectedOption ? Color.GREEN : Color.WHITE);
			drawCenteredRegularString(screen, options[i],
					screen.getHeight() / 2
							+ i * fontRegularMetrics.getHeight() * 2);
		}

		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, "Up/Down: select  Space: confirm",
				screen.getHeight() / 8 * 7);
	}

	public void drawControlsSettings(final Screen screen,
			final int selectedOption, final boolean waitingForKey) {
		String[] options = {
			"Move Left", "Move Right", "Fire", "Back"
		};
		String[] keys = {
			getKeyDisplayText(ControlsConfig.getKey(ControlsConfig.LEFT)),
			getKeyDisplayText(ControlsConfig.getKey(ControlsConfig.RIGHT)),
			getKeyDisplayText(ControlsConfig.getKey(ControlsConfig.FIRE)),
			""
		};

		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "Controls", screen.getHeight() / 5);

		for (int i = 0; i < options.length; i++) {
			String text = options[i];

			if (i < 3) {
				text += ": ";
				text += waitingForKey && i == selectedOption
						? "Press a key (Esc to cancel)" : keys[i];
			}

			backBufferGraphics.setColor(
					i == selectedOption ? Color.GREEN : Color.WHITE);
			drawCenteredRegularString(screen, text,
					screen.getHeight() / 2
							+ i * fontRegularMetrics.getHeight() * 2);
		}

		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, "Up/Down: select  Space: change",
				screen.getHeight() / 8 * 7);
	}

	public void drawGameplaySettings(final Screen screen, final int selectedOption) {
		String[] options = { "Easy", "Normal", "Hard" };

		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "Gameplay", screen.getHeight() / 5);

		for (int i = 0; i < options.length; i++) {
			backBufferGraphics.setColor(
					i == selectedOption ? Color.GREEN : Color.WHITE);
			drawCenteredRegularString(screen, options[i],
					screen.getHeight() / 2
							+ i * fontRegularMetrics.getHeight() * 2);
		}

		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, "Up/Down: select  Space: confirm",
				screen.getHeight() / 8 * 7);
	}

	/**
	 * Draws the audio settings screen.
	 *
	 * @param screen
	 *            Screen to draw on.
	 * @param option
	 *            Option selected.
	 * @param bgmVolume
	 *            Current BGM volume, from 0 to 100.
	 * @param sfxVolume
	 *            Current SFX volume, from 0 to 100.
	 * @param muted
	 *            If the audio is muted.
	 */
	public void drawAudioSettings(final Screen screen, final int option,
			final int bgmVolume, final int sfxVolume, final boolean muted) {
		String titleString = "Audio Settings";
		String selectString = "Up/Down: select";
		String adjustString = "Left/Right: adjust  M: mute";
		String returnString = "Press Esc to return";
		// In the order of the options: BGM volume, SFX volume, mute.
		String[] optionStrings = { "BGM Volume", "SFX Volume", "Mute" };
		// Values shown on the right side of each option.
		String[] valueStrings = { Integer.toString(bgmVolume),
				Integer.toString(sfxVolume), muted ? "ON" : "OFF" };
		// Volumes shown as bars. Only the volume options have a bar.
		int[] barVolumes = { bgmVolume, sfxVolume };

		// Centered column that holds the option names, values, and bars.
		int columnWidth = 200;
		int columnLeft = screen.getWidth() / 2 - columnWidth / 2;
		// Vertical position of the first option and the space between options.
		int firstOptionY = screen.getHeight() / 3;
		int optionSpacing = fontRegularMetrics.getHeight() * 4;
		// Mute is the last option.
		int muteIndex = optionStrings.length - 1;
		// Space taken by the widest value, so the brackets do not move.
		int valueRight = columnLeft + columnWidth;
		int valueLeft = valueRight - fontRegularMetrics.stringWidth("< 100 >");
		int valueCenter = (valueLeft + valueRight) / 2;
		// Volume bars are drawn right below the volume names.
		int barGap = 6;
		int barHeight = 8;

		// Title.
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, titleString, screen.getHeight() / 8);

		// Control instructions.
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, selectString,
				screen.getHeight() / 5);
		drawCenteredRegularString(screen, adjustString,
				screen.getHeight() / 5 + fontRegularMetrics.getHeight());

		// Options, one row each.
		backBufferGraphics.setFont(fontRegular);
		for (int i = 0; i < optionStrings.length; i++) {
			// The selected option is green and the others are white.
			if (i == option)
				backBufferGraphics.setColor(Color.GREEN);
			else
				backBufferGraphics.setColor(Color.WHITE);
			// Option name on the left edge of the column.
			backBufferGraphics.drawString(optionStrings[i], columnLeft,
					firstOptionY + optionSpacing * i);
			// Value centered in the value space.
			backBufferGraphics.drawString(valueStrings[i], valueCenter
					- fontRegularMetrics.stringWidth(valueStrings[i]) / 2,
					firstOptionY + optionSpacing * i);
			// Brackets at fixed positions around the volume values, to show
			// that they can be changed with Left and Right.
			if (i != muteIndex) {
				backBufferGraphics.drawString("<", valueLeft,
						firstOptionY + optionSpacing * i);
				backBufferGraphics.drawString(">", valueRight
						- fontRegularMetrics.stringWidth(">"),
						firstOptionY + optionSpacing * i);
			}
			// Strike through the volume numbers while muted.
			if (muted && i != muteIndex) {
				int numberWidth = fontRegularMetrics
						.stringWidth(valueStrings[i]);
				backBufferGraphics.fillRect(valueCenter - numberWidth / 2 - 2,
						firstOptionY + optionSpacing * i
								- fontRegularMetrics.getAscent() / 2,
						numberWidth + 4, 2);
			}
			// Volume bar below the volume names, gray while muted.
			if (i < barVolumes.length) {
				Color barColor;
				if (muted)
					barColor = Color.GRAY;
				else if (i == option)
					barColor = Color.GREEN;
				else
					barColor = Color.WHITE;
				drawVolumeBar(columnLeft,
						firstOptionY + optionSpacing * i + barGap,
						columnWidth, barHeight, barVolumes[i], barColor);
			}
		}

		// Return instruction near the bottom.
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, returnString,
				screen.getHeight() / 8 * 7);
	}

	/**
	 * Draws a horizontal volume bar.
	 * The whole bar is drawn in dark gray, and the part up to the volume is
	 * filled with the given color.
	 *
	 * @param positionX
	 *            Left edge of the bar.
	 * @param positionY
	 *            Top edge of the bar.
	 * @param width
	 *            Width of the bar at volume 100.
	 * @param height
	 *            Height of the bar.
	 * @param volume
	 *            Volume to show, from 0 to 100.
	 * @param color
	 *            Color of the filled part.
	 */
	private void drawVolumeBar(final int positionX, final int positionY,
			final int width, final int height, final int volume,
			final Color color) {
		backBufferGraphics.setColor(Color.DARK_GRAY);
		backBufferGraphics.fillRect(positionX, positionY, width, height);

		backBufferGraphics.setColor(color);
		backBufferGraphics.fillRect(positionX, positionY,
				width * volume / 100, height);
	}

	/**
	 * Dessine un bouclier protecteur autour du vaisseau.
	 *
	 * @param screen Écran de jeu cible.
	 * @param ship Vaisseau à entourer.
	 */
	public void drawShield(final Screen screen, final Entity ship) {
		backBufferGraphics.setColor(new Color(0, 200, 255, 120)); // Cyan semi-transparent

		int padding = 4;
		int x = ship.getPositionX() - padding;
		int y = ship.getPositionY() - padding;
		int width = ship.getWidth() + (padding * 2);
		int height = ship.getHeight() + (padding * 2);

		// Dessine l'intérieur lumineux du bouclier
		backBufferGraphics.fillOval(x, y, width, height);

		// Dessine le contour plus net du bouclier
		backBufferGraphics.setColor(Color.CYAN);
		backBufferGraphics.drawOval(x, y, width, height);
	}

	private String getKeyDisplayText(final int keyCode) {
		switch (keyCode) {
		case java.awt.event.KeyEvent.VK_LEFT:
			return "Left Arrow";
		case java.awt.event.KeyEvent.VK_RIGHT:
			return "Right Arrow";
		case java.awt.event.KeyEvent.VK_UP:
			return "Up Arrow";
		case java.awt.event.KeyEvent.VK_DOWN:
			return "Down Arrow";
		case java.awt.event.KeyEvent.VK_SPACE:
			return "Space";
		default:
			if (keyCode >= java.awt.event.KeyEvent.VK_A
					&& keyCode <= java.awt.event.KeyEvent.VK_Z) {
				return String.valueOf((char) keyCode);
			}
			return java.awt.event.KeyEvent.getKeyText(keyCode);
		}
	}
		/**
	 * Draws every live explosion particle.
	 */
	public void drawParticles(final vfx.ParticleSystem particleSystem) {
		for (vfx.Particle p : particleSystem.getParticles()) {
			backBufferGraphics.setColor(p.getColor());
			backBufferGraphics.fillRect(p.getX(), p.getY(), p.getSize(),
					p.getSize());
		}
	}
}