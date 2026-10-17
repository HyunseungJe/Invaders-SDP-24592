package screen;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;

import entity.item.ChameleonHull;
import entity.item.ItemManager;
import entity.item.ShieldItem;
import engine.Cooldown;
import engine.Core;
import engine.GameSettings;
import engine.GameState;
import engine.ControlsConfig;
import entity.Bullet;
import entity.BulletPool;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.Entity;
import entity.Ship;
import audio.AudioManager;
import entity.item.Hyperfire;
import entity.item.OrbitalAnnihilator;

/**
 * Implements the game screen, where the action happens.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class GameScreen extends Screen {

	/** Milliseconds until the screen accepts user input. */
	private static final int INPUT_DELAY = 6000;
	/** Bonus score for each life remaining at the end of the level. */
	private static final int LIFE_SCORE = 100;
	/** Minimum time between bonus ship's appearances. */
	private static final int BONUS_SHIP_INTERVAL = 20000;
	/** Maximum variance in the time between bonus ship's appearances. */
	private static final int BONUS_SHIP_VARIANCE = 10000;
	/** Time until bonus ship explosion disappears. */
	private static final int BONUS_SHIP_EXPLOSION = 500;
	/** Time from finishing the level to screen change. */
	private static final int SCREEN_CHANGE_INTERVAL = 1500;
	/** Height of the interface separation line. */
	private static final int SEPARATION_LINE_HEIGHT = 40;
	/** Time the space key must be held to trigger the Annihilator. */
	private static final int ANNIHILATOR_HOLD_TIME = 1000;

	private ItemManager itemManager;
	private final Random itemDropRandom = new Random();
	private boolean chameleonDroppedThisLevel = false;
	private boolean shieldDroppedThisLevel = false;

	/** Current game difficulty settings. */
	private GameSettings gameSettings;
	/** Settings of every wave in this stage, in play order. */
	private List<GameSettings> waves;
	/** Index of the wave currently being played. */
	private int currentWave;
	/** Current difficulty level number. */
	private int level;
	/** Formation of enemy ships. */
	private EnemyShipFormation enemyShipFormation;
	/** Player's ship. */
	private Ship ship;
	/** Bonus enemy ship that appears sometimes. */
	private EnemyShip enemyShipSpecial;
	/** Minimum time between bonus ship appearances. */
	private Cooldown enemyShipSpecialCooldown;
	/** Time until bonus ship explosion disappears. */
	private Cooldown enemyShipSpecialExplosionCooldown;
	/** Time from finishing the level to screen change. */
	private Cooldown screenFinishedCooldown;
	/** Set of all bullets fired by on screen ships. */
	private Set<Bullet> bullets;
	/** Current score. */
	private int score;
	/** Player lives left. */
	private int lives;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;
	/** Moment the game starts. */
	private long gameStartTime;
	/** Checks if the stage was cleared. */
	private boolean stageCleared;
	/** Checks if the level is finished. */
	private boolean levelFinished;
	/** Checks if a bonus life is received. */
	private boolean bonusLife;
	/** Orbital Annihilator (Doomsday Laser), only available on level 7. */
	private OrbitalAnnihilator annihilator;
	/** Moment the space key started being held, 0 if not held. */
	private long spaceHeldSince;
	/** Last countdown number that played a sound. */
	private int lastCountdown = -1;
	/** Prevents repeated activation while holding H. */
	private boolean hyperfireKeyPressed = false;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param gameState
	 *            Current game state.
	 * @param gameSettings
	 *            Current game settings.
	 * @param bonusLife
	 *            Checks if a bonus life is awarded this level.
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public GameScreen(final GameState gameState,
					  final GameSettings gameSettings, final boolean bonusLife,
					  final int width, final int height, final int fps) {
		this(gameState, Collections.singletonList(gameSettings), bonusLife,
				width, height, fps);
	}

	/**
	 * Constructor for a stage with one or more waves.
	 *
	 * @param gameState
	 * 			Current game state.
	 * @param waves
	 * 			Settings of each wave, in play order.
	 * @param bonusLife
	 * 			Checks if a bonus life is awarded this level.
	 * @param width
	 * 			Screen width.
	 * @param height
	 * 			Screen height.
	 * @param fps
	 * 			Frames per second, frame rate at which the game is run.
	 */
	public GameScreen(final GameState gameState,
					  final List<GameSettings> waves, final boolean bonusLife,
					  final int width, final int height, final int fps) {
		super(width, height, fps);

		this.waves = copyWaves(waves);
		this.currentWave = 0;
		this.gameSettings = this.waves.get(0);
		this.bonusLife = bonusLife;
		this.level = gameState.getLevel();
		this.score = gameState.getScore();
		this.lives = gameState.getLivesRemaining();
		if (this.bonusLife)
			this.lives++;
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();
	}

	/**
	 * Initializes basic screen properties, and adds necessary elements.
	 */
	public final void initialize() {
		super.initialize();

		enemyShipFormation = new EnemyShipFormation(this.gameSettings);
		enemyShipFormation.attach(this);
		this.ship = new Ship(this.width / 2, this.height - 30);
		this.itemManager = new ItemManager(this.height);

		this.shieldDroppedThisLevel = false;
		this.chameleonDroppedThisLevel = false;
		this.hyperfireKeyPressed = false;

		// Appears each 10-30 seconds.
		this.enemyShipSpecialCooldown = Core.getVariableCooldown(
				BONUS_SHIP_INTERVAL, BONUS_SHIP_VARIANCE);
		this.enemyShipSpecialCooldown.reset();
		this.enemyShipSpecialExplosionCooldown = Core
				.getCooldown(BONUS_SHIP_EXPLOSION);
		this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
		this.bullets = new HashSet<Bullet>();
		this.annihilator = new OrbitalAnnihilator();

		// Special input delay / countdown.
		this.gameStartTime = System.currentTimeMillis();
		this.inputDelay = Core.getCooldown(INPUT_DELAY);
		this.inputDelay.reset();

        AudioManager.playBGM("audio/bgm/music_game.wav");
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		// Life bonus is only awarded when the stage was cleared.
		if (this.stageCleared)
			this.score += LIFE_SCORE * (this.lives - 1);
		this.logger.info("Screen cleared with a score of " + this.score);

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();

		if (this.inputDelay.checkFinished() && !this.levelFinished) {

			if (!this.ship.isDestroyed()) {
				boolean moveRight = inputManager.isKeyDown(
						ControlsConfig.getKey(ControlsConfig.RIGHT));
				boolean moveLeft = inputManager.isKeyDown(
						ControlsConfig.getKey(ControlsConfig.LEFT));

				boolean isRightBorder = this.ship.getPositionX()
						+ this.ship.getWidth() + this.ship.getSpeed() > this.width - 1;
				boolean isLeftBorder = this.ship.getPositionX()
						- this.ship.getSpeed() < 1;

				if (moveRight && !isRightBorder) {
					this.ship.moveRight();
					clampShipPosition();
				}
				if (moveLeft && !isLeftBorder) {
					this.ship.moveLeft();
					clampShipPosition();
				}

				// Normal shooting using configured controls.
				if (inputManager.isKeyDown(
						ControlsConfig.getKey(ControlsConfig.FIRE))) {
					if (!this.annihilator.isCharging()) {
						if (this.ship.shoot(this.bullets)) {
							this.bulletsShot++;
						}
					}
				}

				// Hyperfire test activation with the H key.
				boolean hyperfireKeyDown = inputManager.isKeyDown(KeyEvent.VK_H);
				if (hyperfireKeyDown && !this.hyperfireKeyPressed) {
					this.itemManager.getBuffManager().addBuff(
							new Hyperfire(0, 0), this.ship);
				}
				this.hyperfireKeyPressed = hyperfireKeyDown;

				// Orbital Annihilator charging mechanic.
				if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
					if (!this.annihilator.isReady()) {
						this.spaceHeldSince = 0;
					} else if (this.spaceHeldSince == 0) {
						this.spaceHeldSince = System.currentTimeMillis();
					}

					if (this.spaceHeldSince != 0
							&& this.level == OrbitalAnnihilator.REQUIRED_LEVEL
							&& System.currentTimeMillis() - this.spaceHeldSince >= ANNIHILATOR_HOLD_TIME) {
						this.annihilator.trigger();
					}
				} else {
					this.spaceHeldSince = 0;
				}
			} else {
				// Cancel charging when the ship is destroyed.
				this.annihilator.cancelCharge();
				this.spaceHeldSince = 0;
			}

			if (this.enemyShipSpecial != null) {
				if (!this.enemyShipSpecial.isDestroyed())
					this.enemyShipSpecial.move(2, 0);
				else if (this.enemyShipSpecialExplosionCooldown.checkFinished())
					this.enemyShipSpecial = null;
			}
			if (this.enemyShipSpecial == null
					&& this.enemyShipSpecialCooldown.checkFinished()) {
				this.enemyShipSpecial = new EnemyShip();
				this.enemyShipSpecialCooldown.reset();
				this.logger.info("A special ship appears");
			}
			if (this.enemyShipSpecial != null
					&& this.enemyShipSpecial.getPositionX() > this.width) {
				this.enemyShipSpecial = null;
				this.logger.info("The special ship has escaped");
			}

			updateBallMovement();

			this.ship.update();
			this.annihilator.update();
			this.enemyShipFormation.update();
			this.enemyShipFormation.shoot(this.bullets);
		}

		if (this.inputDelay.checkFinished() && !this.levelFinished) {
			this.itemManager.update(this.ship);

			if (!this.ship.isDestroyed()) {
				this.itemManager.checkCollisions(this.ship);
			}

			clampShipPosition();
			clampBallPositionY();
		}

		manageCollisions();
		manageBallContact();

		if (this.annihilator.isFiring())
			manageAnnihilatorBeam();

		cleanBullets();
		draw();

		if (!this.levelFinished) {
			if (this.lives == 0) {
				this.levelFinished = true;
				this.stageCleared = false;
				this.screenFinishedCooldown.reset();
				this.logger.info("Stage " + this.level + " failed.");
			} else if (this.enemyShipFormation.isEmpty()) {
				if (this.currentWave < this.waves.size() - 1) {
					startNextWave();
				} else {
					this.levelFinished = true;
					this.stageCleared = true;
					this.screenFinishedCooldown.reset();
					this.logger.info("Stage " + this.level + " cleared.");
				}
			}
		}

		if (this.levelFinished && this.screenFinishedCooldown.checkFinished())
			this.isRunning = false;
	}

	/**
	 * Copies the wave list after checking that it can be played.
	 *
	 * @param waves
	 *            Settings of each wave, in play order.
	 * @return A copy of the wave list.
	 * @throws IllegalArgumentException
	 *             If the list is null or empty, or contains a null wave.
	 */
	static List<GameSettings> copyWaves(final List<GameSettings> waves) {
		if (waves == null || waves.isEmpty())
			throw new IllegalArgumentException(
					"A stage must contain at least one wave.");
		List<GameSettings> copy = new ArrayList<GameSettings>(waves);
		for (int i = 0; i < copy.size(); i++)
			if (copy.get(i) == null)
				throw new IllegalArgumentException(
						"Wave " + (i + 1) + " must not be null.");
		return copy;
	}

	/**
	 * Replaces the empty formation with the next wave's formation.
	 */
	private void startNextWave() {
		this.logger.info("Stage " + this.level + " wave " +
				(this.currentWave + 1) + " cleared.");
		this.currentWave++;
		this.gameSettings = this.waves.get(this.currentWave);
		this.enemyShipFormation = new EnemyShipFormation(this.gameSettings);
		this.enemyShipFormation.attach(this);
		this.logger.info("Stage " + this.level + " wave " + (this.currentWave + 1) +
				" of " + this.waves.size() + " started.");
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		if (this.annihilator.isCharging() || this.annihilator.isFiring())
			drawManager.drawOrbitalAnnihilator(
					this.ship,
					OrbitalAnnihilator.BEAM_WIDTH,
					SEPARATION_LINE_HEIGHT,
					this.annihilator.isFiring(),
					this.annihilator.getChargeProgress()
			);

		drawManager.drawEntity(this.ship, this.ship.getPositionX(),
				this.ship.getPositionY());

		if (this.ship.isShieldActive() && !this.ship.isDestroyed()) {
			drawManager.drawShield(this, this.ship);
		}

		if (this.enemyShipSpecial != null)
			drawManager.drawEntity(this.enemyShipSpecial,
					this.enemyShipSpecial.getPositionX(),
					this.enemyShipSpecial.getPositionY());

		enemyShipFormation.draw();

		for (Bullet bullet : this.bullets)
			drawManager.drawEntity(bullet, bullet.getPositionX(),
					bullet.getPositionY());

		this.itemManager.draw(this.drawManager);

		// Interface.
		drawManager.drawScore(this, this.score);
		drawManager.drawLives(this, this.lives);

		if (this.level == OrbitalAnnihilator.REQUIRED_LEVEL)
			drawManager.drawAnnihilatorGauge(
					this,
					this.annihilator.getRechargeProgress()
			);

		drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);

		// Countdown to game start.
		if (!this.inputDelay.checkFinished()) {
			int countdown = (int) ((INPUT_DELAY
					- (System.currentTimeMillis()
					- this.gameStartTime)) / 1000);
			playCountdownSound(countdown);
			drawManager.drawCountDown(this, this.level, countdown,
					this.bonusLife);
			drawManager.drawHorizontalLine(this, this.height / 2 - this.height
					/ 12);
			drawManager.drawHorizontalLine(this, this.height / 2 + this.height
					/ 12);
		}

		drawManager.completeDrawing(this);
	}

	/**
	 * Cleans bullets that go off screen.
	 */
	private void cleanBullets() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets) {
			bullet.update();
			if (bullet.getPositionY() < SEPARATION_LINE_HEIGHT
					|| bullet.getPositionY() > this.height)
				recyclable.add(bullet);
		}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);
	}

	/**
	 * Manages collisions between bullets and ships.
	 */
	private void manageCollisions() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets)
			if (bullet.getSpeed() > 0) {
				if (checkCollision(bullet, this.ship) && !this.levelFinished) {
					recyclable.add(bullet);
					if (!this.ship.isChameleonHullActive()) {
						if (!this.ship.isDestroyed()) {
							if (this.ship.isShieldActive()) {
								this.logger.info("Enemy shot absorbed by Energy Shield.");
							} else {
								this.ship.destroy();
								this.lives = Math.max(0, this.lives - 1);
								this.itemManager.getBuffManager().resetAllBuffs(this.ship);
								this.logger.info("Hit on player ship, " + this.lives + " lives remaining.");
							}
						}
					}
				}
			} else {
				for (EnemyShip enemyShip : this.enemyShipFormation)
					if (!enemyShip.isDestroyed()
							&& checkCollision(bullet, enemyShip)) {
						this.score += enemyShip.getPointValue();
						this.shipsDestroyed++;
						this.enemyShipFormation.destroy(enemyShip);
						tryDropChameleonHull(enemyShip);
						recyclable.add(bullet);

						// --- DROP DU BOUCLIER ---
						if (!this.shieldDroppedThisLevel) {
							boolean shouldDrop = (Math.random() < 0.15)
									|| (this.enemyShipFormation.isEmpty());
							if (shouldDrop) {
								this.itemManager.spawnItem(
										new ShieldItem(
												enemyShip.getPositionX(),
												enemyShip.getPositionY()
										)
								);
								this.shieldDroppedThisLevel = true;
								this.logger.info("Shield item dropped from enemy ship at ("
										+ enemyShip.getPositionX() + ", "
										+ enemyShip.getPositionY() + ").");
							}
						}
					}
				if (this.enemyShipSpecial != null
						&& !this.enemyShipSpecial.isDestroyed()
						&& checkCollision(bullet, this.enemyShipSpecial)) {
					this.score += this.enemyShipSpecial.getPointValue();
					this.shipsDestroyed++;
					this.enemyShipSpecial.destroy();
					tryDropChameleonHull(this.enemyShipSpecial);
					this.enemyShipSpecialExplosionCooldown.reset();
					recyclable.add(bullet);
				}
			}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);
	}

	/**
	 * Applies the Orbital Annihilator beam: destroys every enemy in the
	 * corridor and neutralizes enemy projectiles inside it.
	 */
	private void manageAnnihilatorBeam() {
		for (EnemyShip enemyShip : this.enemyShipFormation)
			if (!enemyShip.isDestroyed()
					&& OrbitalAnnihilator.hits(this.ship, enemyShip)) {
				this.score += enemyShip.getPointValue();
				this.shipsDestroyed++;

				this.enemyShipFormation.destroy(enemyShip);
				tryDropChameleonHull(enemyShip);
			}

		if (this.enemyShipSpecial != null
				&& !this.enemyShipSpecial.isDestroyed()
				&& OrbitalAnnihilator.hits(this.ship, this.enemyShipSpecial)) {
			this.score += this.enemyShipSpecial.getPointValue();
			this.shipsDestroyed++;

			this.enemyShipSpecial.destroy();
			tryDropChameleonHull(this.enemyShipSpecial);

			this.enemyShipSpecialExplosionCooldown.reset();
		}

		Set<Bullet> neutralized = new HashSet<Bullet>();

		for (Bullet bullet : this.bullets)
			if (bullet.getSpeed() > 0
					&& OrbitalAnnihilator.hits(this.ship, bullet))
				neutralized.add(bullet);

		this.bullets.removeAll(neutralized);
		BulletPool.recycle(neutralized);
	}

	/**
	 * Checks if two entities are colliding.
	 *
	 * @param a
	 *            First entity, the bullet.
	 * @param b
	 *            Second entity, the ship.
	 * @return Result of the collision test.
	 */
	private boolean checkCollision(final Entity a, final Entity b) {
		// Calculate center point of the entities in both axis.
		int centerAX = a.getPositionX() + a.getWidth() / 2;
		int centerAY = a.getPositionY() + a.getHeight() / 2;
		int centerBX = b.getPositionX() + b.getWidth() / 2;
		int centerBY = b.getPositionY() + b.getHeight() / 2;
		// Calculate maximum distance without collision.
		int maxDistanceX = a.getWidth() / 2 + b.getWidth() / 2;
		int maxDistanceY = a.getHeight() / 2 + b.getHeight() / 2;
		// Calculates distance.
		int distanceX = Math.abs(centerAX - centerBX);
		int distanceY = Math.abs(centerAY - centerBY);

		return distanceX < maxDistanceX && distanceY < maxDistanceY;
	}

	/**
	 * Checks if the stage was cleared.
	 *
	 * @return True if all enemies were defeated with the ship alive.
	 */
	public final boolean isStageCleared() {
		return this.stageCleared;
	}

	/**
	 * Returns a GameState object representing the status of the game.
	 *
	 * @return Current game state.
	 */
	public final GameState getGameState() {
		return new GameState(this.level, this.score, this.lives,
				this.bulletsShot, this.shipsDestroyed);
	}

	/**
	 * Plays the countdown sound once each time the countdown number changes.
	 *
	 * @param countdown
	 *            Number currently shown in the countdown.
	 */
	private void playCountdownSound(final int countdown) {
		if (countdown == this.lastCountdown)
			return;
		this.lastCountdown = countdown;
		if (countdown == 0)
			AudioManager.playSFX("audio/sfx/countdown_go.wav");
		else if (countdown <= 3)
			AudioManager.playSFX("audio/sfx/countdown_beep.wav");
	}

	/** Keep the extra movement inside the existing playfield borders. */
	private void clampShipPosition() {
		int maximumX = this.width - this.ship.getWidth() - 1;
		this.ship.setPositionX(
				Math.max(1, Math.min(maximumX, this.ship.getPositionX()))
		);
	}

	private void tryDropChameleonHull(final EnemyShip enemy) {
		if (!this.chameleonDroppedThisLevel
				&& this.level >= ChameleonHull.REQUIRED_LEVEL
				&& !this.levelFinished
				&& this.lives > 0
				&& this.itemDropRandom.nextDouble() < ChameleonHull.DROP_CHANCE) {

			this.itemManager.spawnItem(
					new ChameleonHull(
							enemy.getPositionX(),
							enemy.getPositionY()
					)
			);
			this.chameleonDroppedThisLevel = true;
		}
	}

	/** Vertical keys are enabled only during the star transformation. */
	private void updateBallMovement() {
		if (!this.ship.isChameleonHullActive() || this.ship.isDestroyed()) {
			return;
		}

		boolean up = inputManager.isKeyDown(KeyEvent.VK_UP)
				|| inputManager.isKeyDown(KeyEvent.VK_W);
		boolean down = inputManager.isKeyDown(KeyEvent.VK_DOWN)
				|| inputManager.isKeyDown(KeyEvent.VK_S);

		if (up && !down) {
			this.ship.moveUp();
		}
		if (down && !up) {
			this.ship.moveDown();
		}

		clampBallPositionY();
	}

	private void clampBallPositionY() {
		if (this.ship.isChameleonHullActive()) {
			int minimumY = SEPARATION_LINE_HEIGHT + 1;
			int maximumY = this.height - this.ship.getHeight() - 1;

			this.ship.setPositionY(
					Math.max(
							minimumY,
							Math.min(maximumY, this.ship.getPositionY())
					)
			);
		}
	}

	/** Contact kills use the existing score, destruction and drop systems. */
	private void manageBallContact() {
		if (!this.inputDelay.checkFinished()
				|| this.levelFinished
				|| this.ship.isDestroyed()
				|| !this.ship.isChameleonHullActive()) {
			return;
		}

		for (EnemyShip enemy : this.enemyShipFormation) {
			if (!enemy.isDestroyed() && checkCollision(this.ship, enemy)) {
				this.score += enemy.getPointValue();
				this.shipsDestroyed++;

				this.enemyShipFormation.destroy(enemy);
				tryDropChameleonHull(enemy);
			}
		}

		if (this.enemyShipSpecial != null
				&& !this.enemyShipSpecial.isDestroyed()
				&& checkCollision(this.ship, this.enemyShipSpecial)) {
			this.score += this.enemyShipSpecial.getPointValue();
			this.shipsDestroyed++;

			this.enemyShipSpecial.destroy();
			this.enemyShipSpecialExplosionCooldown.reset();

			tryDropChameleonHull(this.enemyShipSpecial);
		}
	}
}