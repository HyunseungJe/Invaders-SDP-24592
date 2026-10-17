package screen;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import audio.AudioManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager.SpriteType;
import engine.GameState;
import entity.Bullet;
import entity.BulletPool;
import entity.Entity;
import entity.Ship;
import entity.ZigzagEnemyShip;

/**
 * Implements the infinite mode game screen: zigzagging enemies keep spawning
 * from the top of the screen until the player runs out of lives or quits.
 */
public class InfiniteGameScreen extends Screen {

	/** Height of the interface separation line. */
	private static final int SEPARATION_LINE_HEIGHT = 40;
	/** Margin on the sides of the screen where enemies don't spawn. */
	private static final int SIDE_MARGIN = 20;
	/** Width of a regular enemy ship. */
	private static final int ENEMY_WIDTH = 12 * 2;
	/** Time between enemy spawns at the start, in milliseconds. */
	private static final int INITIAL_SPAWN_INTERVAL = 1000;
	/** Shortest time between enemy spawns, in milliseconds. */
	private static final int MINIMUM_SPAWN_INTERVAL = 300;
	/** Spawn interval reduction for each enemy destroyed, in milliseconds. */
	private static final int SPAWN_INTERVAL_STEP = 10;
	/** Enemies destroyed needed to make the enemies fall faster. */
	private static final int KILLS_PER_SPEED_UP = 20;
	/** Highest downwards speed of the enemies, in pixels per frame. */
	private static final int MAXIMUM_FALL_SPEED = 3;
	/** Time from the game ending to screen change. */
	private static final int SCREEN_CHANGE_INTERVAL = 1500;
	/** Chance an enemy fires on any given frame. */
	private static final double ENEMY_FIRE_CHANCE = 0.003;
	/** Downwards speed of the enemy bullets, in pixels per frame. */
	private static final int ENEMY_BULLET_SPEED = 4;

	/** Player's ship. */
	private Ship ship;
	/** Enemies currently on screen. */
	private List<ZigzagEnemyShip> enemies;
	/** Set of all bullets fired by the player. */
	private Set<Bullet> bullets;
	/** Set of all bullets fired by the enemies. */
	private Set<Bullet> enemyBullets;
	/** Time between enemy spawns. */
	private Cooldown spawnCooldown;
	/** Time from the game ending to screen change. */
	private Cooldown screenFinishedCooldown;
	/** Current score. */
	private int score;
	/** Player lives left. */
	private int lives;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;
	/** Checks if the game is over. */
	private boolean gameOver;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param gameState
	 *            Initial game state.
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public InfiniteGameScreen(final GameState gameState, final int width,
			final int height, final int fps) {
		super(width, height, fps);

		this.score = gameState.getScore();
		this.lives = gameState.getLivesRemaining();
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();
	}

	/**
	 * Initializes basic screen properties, and adds necessary elements.
	 */
	public final void initialize() {
		super.initialize();

		this.ship = new Ship(this.width / 2, this.height - 30);
		this.enemies = new ArrayList<ZigzagEnemyShip>();
		this.bullets = new HashSet<Bullet>();
		this.enemyBullets = new HashSet<Bullet>();
		this.spawnCooldown = Core.getCooldown(INITIAL_SPAWN_INTERVAL);
		this.spawnCooldown.reset();
		this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);

		AudioManager.playBGM("audio/bgm/music_game.wav");
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		this.logger.info("Infinite mode ended with a score of " + this.score);

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();

		if (this.inputDelay.checkFinished() && !this.gameOver) {
			if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
				// Quits the run, the score so far is kept.
				this.isRunning = false;
				return;
			}

			if (!this.ship.isDestroyed()) {
				boolean moveRight = inputManager.isKeyDown(KeyEvent.VK_RIGHT)
						|| inputManager.isKeyDown(KeyEvent.VK_D);
				boolean moveLeft = inputManager.isKeyDown(KeyEvent.VK_LEFT)
						|| inputManager.isKeyDown(KeyEvent.VK_A);

				boolean isRightBorder = this.ship.getPositionX()
						+ this.ship.getWidth() + this.ship.getSpeed() > this.width - 1;
				boolean isLeftBorder = this.ship.getPositionX()
						- this.ship.getSpeed() < 1;

				if (moveRight && !isRightBorder)
					this.ship.moveRight();
				if (moveLeft && !isLeftBorder)
					this.ship.moveLeft();
				if (inputManager.isKeyDown(KeyEvent.VK_SPACE))
					if (this.ship.shoot(this.bullets))
						this.bulletsShot++;
			}

			this.ship.update();
			spawnEnemy();
			moveEnemies();
			shootEnemies();
		}

		manageCollisions();
		cleanBullets();
		draw();

		if (this.lives == 0 && !this.gameOver) {
			this.gameOver = true;
			this.screenFinishedCooldown.reset();
		}

		if (this.gameOver && this.screenFinishedCooldown.checkFinished())
			this.isRunning = false;
	}

	/**
	 * Spawns a new enemy at the top of the screen when it's time to.
	 */
	private void spawnEnemy() {
		if (!this.spawnCooldown.checkFinished())
			return;

		int amplitude = ZigzagEnemyShip.getZigzagAmplitude();
		int minX = SIDE_MARGIN + amplitude;
		int maxX = this.width - SIDE_MARGIN - amplitude - ENEMY_WIDTH;
		int positionX = minX + (int) (Math.random() * (maxX - minX));

		SpriteType spriteType;
		double roll = Math.random();
		if (roll < 0.2)
			spriteType = SpriteType.EnemyShipC1;
		else if (roll < 0.6)
			spriteType = SpriteType.EnemyShipB1;
		else
			spriteType = SpriteType.EnemyShipA1;

		int fallSpeed = Math.min(MAXIMUM_FALL_SPEED,
				1 + this.shipsDestroyed / KILLS_PER_SPEED_UP);

		this.enemies.add(new ZigzagEnemyShip(positionX,
				SEPARATION_LINE_HEIGHT, spriteType, fallSpeed));

		// The more enemies destroyed, the faster they come.
		int interval = Math.max(MINIMUM_SPAWN_INTERVAL, INITIAL_SPAWN_INTERVAL
				- this.shipsDestroyed * SPAWN_INTERVAL_STEP);
		this.spawnCooldown = Core.getCooldown(interval);
		this.spawnCooldown.reset();
	}

	/**
	 * Moves the enemies, and removes the ones that leave through the bottom.
	 */
	private void moveEnemies() {
		Iterator<ZigzagEnemyShip> iterator = this.enemies.iterator();
		while (iterator.hasNext()) {
			ZigzagEnemyShip enemy = iterator.next();
			enemy.zigzagMove();
			enemy.update();
			if (enemy.getPositionY() > this.height)
				iterator.remove();
		}
	}

	/**
	 * Gives each enemy a small chance to fire a bullet downwards.
	 */
	private void shootEnemies() {
		for (ZigzagEnemyShip enemy : this.enemies) {
			if (Math.random() < ENEMY_FIRE_CHANCE)
				this.enemyBullets.add(BulletPool.getBullet(
						enemy.getPositionX() + enemy.getWidth() / 2,
						enemy.getPositionY() + enemy.getHeight(),
						ENEMY_BULLET_SPEED));
		}
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawManager.drawEntity(this.ship, this.ship.getPositionX(),
				this.ship.getPositionY());

		for (ZigzagEnemyShip enemy : this.enemies)
			drawManager.drawEntity(enemy, enemy.getPositionX(),
					enemy.getPositionY());

		for (Bullet bullet : this.bullets)
			drawManager.drawEntity(bullet, bullet.getPositionX(),
					bullet.getPositionY());

		for (Bullet bullet : this.enemyBullets)
			drawManager.drawEntity(bullet, bullet.getPositionX(),
					bullet.getPositionY());

		// Interface.
		drawManager.drawScore(this, this.score);
		drawManager.drawLives(this, this.lives);
		drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);

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

		Set<Bullet> recyclableEnemy = new HashSet<Bullet>();
		for (Bullet bullet : this.enemyBullets) {
			bullet.update();
			if (bullet.getPositionY() > this.height)
				recyclableEnemy.add(bullet);
		}
		this.enemyBullets.removeAll(recyclableEnemy);
		BulletPool.recycle(recyclableEnemy);
	}

	/**
	 * Manages collisions between bullets, enemies and the player's ship.
	 */
	private void manageCollisions() {
		Set<Bullet> recyclable = new HashSet<Bullet>();

		for (Bullet bullet : this.bullets) {
			Iterator<ZigzagEnemyShip> iterator = this.enemies.iterator();
			while (iterator.hasNext()) {
				ZigzagEnemyShip enemy = iterator.next();
				if (checkCollision(bullet, enemy)) {
					this.score += enemy.getPointValue();
					this.shipsDestroyed++;
					iterator.remove();
					recyclable.add(bullet);
					break;
				}
			}
		}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);

		if (this.gameOver || this.ship.isDestroyed())
			return;

		Iterator<Bullet> bulletIterator = this.enemyBullets.iterator();
		while (bulletIterator.hasNext()) {
			Bullet bullet = bulletIterator.next();
			if (checkCollision(bullet, this.ship)) {
				bulletIterator.remove();
				BulletPool.recycle(Collections.singleton(bullet));
				this.ship.destroy();
				this.lives--;
				this.logger.info("Hit by enemy bullet, " + this.lives
						+ " lives remaining.");
				return;
			}
		}

		Iterator<ZigzagEnemyShip> iterator = this.enemies.iterator();
		while (iterator.hasNext()) {
			if (checkCollision(iterator.next(), this.ship)) {
				iterator.remove();
				this.ship.destroy();
				this.lives--;
				this.logger.info("Hit on player ship, " + this.lives
						+ " lives remaining.");
				break;
			}
		}
	}

	/**
	 * Checks if two entities are colliding.
	 *
	 * @param a
	 *            First entity.
	 * @param b
	 *            Second entity.
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
	 * Returns a GameState object representing the status of the game.
	 *
	 * @return Current game state.
	 */
	public final GameState getGameState() {
		return new GameState(1, this.score, this.lives, this.bulletsShot,
				this.shipsDestroyed);
	}
}
