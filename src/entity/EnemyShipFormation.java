package entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import screen.Screen;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.GameSettings;

/**
 * Groups enemy ships into a formation that moves together.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class EnemyShipFormation implements Iterable<EnemyShip> {

	/** Distance between ships. */
	private static final int SEPARATION_DISTANCE = 40;
	/** Proportion of C-type ships. */
	private static final double PROPORTION_C = 0.2;
	/** Proportion of B-type ships. */
	private static final double PROPORTION_B = 0.4;
	/** Lateral speed of the formation. */
	private static final int X_SPEED = 8;
	/** Downwards speed of the formation. */
	private static final int Y_SPEED = 4;
	/** Speed of the bullets shot by the members. */
	private static final int BULLET_SPEED = 4;
	/** Proportion of differences between shooting times. */
	private static final double SHOOTING_VARIANCE = .2;
	/** Margin on the sides of the screen. */
	private static final int SIDE_MARGIN = 20;
	/** Margin on the bottom of the screen. */
	private static final int BOTTOM_MARGIN = 80;
	/** Distance to go down each pass. */
	private static final int DESCENT_DISTANCE = 20;
	/** Minimum speed allowed. */
	private static final int MINIMUM_SPEED = 10;
	/** Horizontal speed of the wave movement. */
	private static final int WAVE_X_SPEED = 6;
	/** Maximum vertical amplitude of the wave. */
	private static final int WAVE_AMPLITUDE = 25;
	/** Frequency of the vertical oscillation. */
	private static final double WAVE_FREQUENCY = 0.15;

	/** DrawManager instance. */
	private DrawManager drawManager;
	/** Application logger. */
	private Logger logger;
	/** Screen to draw ships on. */
	private Screen screen;

	/** Available movement patterns for enemy formations. */
    private enum MovementPattern {
        CLASSIC,
        WAVE_BOUNCE,
        DIAGONAL_BOUNCE
    }

	/** Current movement pattern. */
    private MovementPattern movementPattern = MovementPattern.DIAGONAL_BOUNCE;
	/** Horizontal direction of the wave: 1 = right, -1 = left. */
	private int waveDirection = 1;
	/** Current phase of the wave. */
	private double wavePhase = 0;
	/** Reference vertical position the wave oscillates around. */
	private int waveBaseY;
    /** Horizontal and vertical speed of the diagonal movement. */
    private static final int DIAGONAL_SPEED = 4;
    /** Current segment of the diamond loop (0 to 3). */
    private int diagonalPhase = 0;
    /** Initial vertical position of the diamond loop. */
    private int diagonalBaseY;
	/** List of enemy ships forming the formation. */
	private List<List<EnemyShip>> enemyShips;
	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Number of ships in the formation - horizontally. */
	private int nShipsWide;
	/** Number of ships in the formation - vertically. */
	private int nShipsHigh;
	/** Time between shots. */
	private int shootingInterval;
	/** Variance in the time between shots. */
	private int shootingVariance;
	/** Initial ship speed. */
	private int baseSpeed;
	/** Speed of the ships. */
	private int movementSpeed;
	/** Current direction the formation is moving on. */
	private Direction currentDirection;
	/** Direction the formation was moving previously. */
	private Direction previousDirection;
	/** Interval between movements, in frames. */
	private int movementInterval;
	/** Total width of the formation. */
	private int width;
	/** Total height of the formation. */
	private int height;
	/** Position in the x-axis of the upper left corner of the formation. */
	private int positionX;
	/** Position in the y-axis of the upper left corner of the formation. */
	private int positionY;
	/** Width of one ship. */
	private int shipWidth;
	/** Height of one ship. */
	private int shipHeight;
	/** List of ships that are able to shoot. */
	private List<EnemyShip> shooters;
	/** Number of not destroyed ships. */
	private int shipCount;

	/** Directions the formation can move. */
	private enum Direction {
		/** Movement to the right side of the screen. */
		RIGHT,
		/** Movement to the left side of the screen. */
		LEFT,
		/** Movement to the bottom of the screen. */
		DOWN
	};

	/**
	 * Constructor, sets the initial conditions.
	 *
	 * @param gameSettings
	 *            Current game settings.
	 * @throws IllegalArgumentException If the initial position is outside the
	 *            left/top bounds or is not aligned to the descent distance.
	 */
	public EnemyShipFormation(final GameSettings gameSettings) {
		this.drawManager = Core.getDrawManager();
		this.logger = Core.getLogger();
		this.enemyShips = new ArrayList<List<EnemyShip>>();
		this.currentDirection = Direction.RIGHT;
		this.movementInterval = 0;
		this.nShipsWide = gameSettings.getFormationWidth();
		this.nShipsHigh = gameSettings.getFormationHeight();
		this.shootingInterval = gameSettings.getShootingFrecuency();
		this.shootingVariance = (int) (gameSettings.getShootingFrecuency()
				* SHOOTING_VARIANCE);
		this.baseSpeed = gameSettings.getBaseSpeed();
		this.movementSpeed = this.baseSpeed;
		this.positionX = gameSettings.getInitialX();
		this.positionY = gameSettings.getInitialY();
		if (this.positionX < SIDE_MARGIN || this.positionY < 0
				|| this.positionY % DESCENT_DISTANCE != 0)
			throw new IllegalArgumentException("Initial formation position must "
					+ "have X >= " + SIDE_MARGIN + " and non-negative Y divisible by "
					+ DESCENT_DISTANCE + ": (" + positionX + "," + positionY + ")");
		this.waveBaseY = this.positionY;
        // Initial position of the diagonal movement.
        this.diagonalBaseY = this.positionY;
		this.shooters = new ArrayList<EnemyShip>();
		EnemyType enemyType;

		this.logger.info("Initializing " + nShipsWide + "x" + nShipsHigh
				+ " ship formation in (" + positionX + "," + positionY + ")");

		// Each sub-list is a column on the formation.
		for (int i = 0; i < this.nShipsWide; i++)
			this.enemyShips.add(new ArrayList<EnemyShip>());

		for (List<EnemyShip> column : this.enemyShips) {
			for (int i = 0; i < this.nShipsHigh; i++) {
				if (i / (float) this.nShipsHigh < PROPORTION_C)
					enemyType = EnemyType.C;
				else if (i / (float) this.nShipsHigh < PROPORTION_B
						+ PROPORTION_C)
					enemyType = EnemyType.B;
				else
					enemyType = EnemyType.A;

				column.add(EnemyShip.createByType(enemyType,
						(SEPARATION_DISTANCE * this.enemyShips.indexOf(column))
								+ positionX,
						(SEPARATION_DISTANCE * i) + positionY));
				this.shipCount++;
			}
		}

		this.shipWidth = this.enemyShips.get(0).get(0).getWidth();
		this.shipHeight = this.enemyShips.get(0).get(0).getHeight();

		this.width = (this.nShipsWide - 1) * SEPARATION_DISTANCE
				+ this.shipWidth;
		this.height = (this.nShipsHigh - 1) * SEPARATION_DISTANCE
				+ this.shipHeight;

		for (List<EnemyShip> column : this.enemyShips) {
			EnemyShip shooter = column.get(column.size() - 1);
			shooter.setShootingCooldown(createShooterCooldown());
			this.shooters.add(shooter);
		}
	}

	/** Creates a cooldown for one shooter, based on the level's settings. */
	private Cooldown createShooterCooldown() {
		return Core.getVariableCooldown(this.shootingInterval / 2,
				this.shootingVariance / 2);
	}

	/**
	 * Calculates a wave movement with horizontal bouncing, keeping the
	 * formation inside the screen boundaries. The wave oscillates around the
	 * initial vertical position the level settings asked for.
	 *
	 * @return Movement to apply on the X and Y axes.
	 */
	private int[] moveWaveBounce() {
		int minX = SIDE_MARGIN;
		int maxX = screen.getWidth() - SIDE_MARGIN - this.width;
		int minY = this.waveBaseY;
		int maxY = screen.getHeight() - BOTTOM_MARGIN - this.height;

		int nextX = this.positionX + WAVE_X_SPEED * this.waveDirection;
		if (nextX >= maxX) {
			nextX = maxX;
			this.waveDirection = -1;
		} else if (nextX <= minX) {
			nextX = minX;
			this.waveDirection = 1;
		}

		this.wavePhase += WAVE_FREQUENCY;

		int amplitude = Math.min(WAVE_AMPLITUDE,
				Math.max(0, (maxY - minY) / 2));
		int centerY = Math.max(minY,
				Math.min(this.waveBaseY + amplitude, maxY - amplitude));
		int nextY = centerY
				+ (int) Math.round(amplitude * Math.sin(this.wavePhase));
		nextY = Math.max(minY, Math.min(nextY, maxY));

		return new int[] { nextX - this.positionX, nextY - this.positionY };
	}


    /**
     * Moves the formation along a diamond-shaped loop.
     * The path adapts to the formation's size.
     *
     * @return Movement on the X and Y axes.
     */
    private int[] moveDiagonalBounce() {
        int minX = SIDE_MARGIN;
        int maxX = screen.getWidth() - SIDE_MARGIN - this.width;
        int minY = Math.max(0, this.diagonalBaseY);
        int maxY = screen.getHeight() - BOTTOM_MARGIN - this.height;

        maxX = Math.max(minX, maxX);
        maxY = Math.max(minY, maxY);

        // Adapt the diamond to the available space.
        int radiusX = Math.min(80, (maxX - minX) / 2);
        int radiusY = Math.min(40, (maxY - minY) / 2);

        int centerX = minX + (maxX - minX) / 2;
        int centerY = minY + (maxY - minY) / 2;

        // Diamond vertices: top, right, bottom, left.
        int[] pointsX = {
                centerX,
                centerX + radiusX,
                centerX,
                centerX - radiusX
        };

        int[] pointsY = {
                centerY - radiusY,
                centerY,
                centerY + radiusY,
                centerY
        };

        // Current destination vertex.
        int targetX = pointsX[this.diagonalPhase];
        int targetY = pointsY[this.diagonalPhase];

        int dx = targetX - this.positionX;
        int dy = targetY - this.positionY;

        double distance = Math.hypot(dx, dy);

        // Move towards the destination without jumping.
        int movementX;
        int movementY;

        if (distance <= DIAGONAL_SPEED) {
            movementX = dx;
            movementY = dy;

            // Move to the next vertex.
            this.diagonalPhase = (this.diagonalPhase + 1) % 4;
        } else {
            movementX = (int) Math.round(
                    dx / distance * DIAGONAL_SPEED);
            movementY = (int) Math.round(
                    dy / distance * DIAGONAL_SPEED);
        }

        // Keep the formation inside the screen.
        int nextX = Math.max(minX,
                Math.min(this.positionX + movementX, maxX));
        int nextY = Math.max(minY,
                Math.min(this.positionY + movementY, maxY));

        return new int[] {
                nextX - this.positionX,
                nextY - this.positionY
        };
    }

	/**
	 * Associates the formation to a given screen.
	 *
	 * @param newScreen
	 *            Screen to attach.
	 * @throws IllegalArgumentException If the formation exceeds the screen's
	 *            right or bottom margin.
	 */
	public final void attach(final Screen newScreen) {
		if ((long) positionX + this.width > newScreen.getWidth() - SIDE_MARGIN
				|| (long) positionY + this.height
						> newScreen.getHeight() - BOTTOM_MARGIN)
			throw new IllegalArgumentException("Formation does not fit within "
					+ "the screen margins: (" + positionX + "," + positionY + "), "
					+ this.width + "x" + this.height + " on "
					+ newScreen.getWidth() + "x" + newScreen.getHeight());
		screen = newScreen;
	}

	/**
	 * Draws every individual component of the formation.
	 */
	public final void draw() {
		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip enemyShip : column)
				drawManager.drawEntity(enemyShip, enemyShip.getPositionX(),
						enemyShip.getPositionY());
	}

	/**
	 * Updates the position of the ships.
	 */
	public final void update() {
		if(this.shootingCooldown == null) {
			this.shootingCooldown = Core.getVariableCooldown(shootingInterval,
					shootingVariance);
			this.shootingCooldown.reset();
		}

		cleanUp();

		int movementX = 0;
		int movementY = 0;
		double remainingProportion = (double) this.shipCount
				/ (this.nShipsHigh * this.nShipsWide);
		this.movementSpeed = (int) (Math.pow(remainingProportion, 2)
				* this.baseSpeed);
		this.movementSpeed += MINIMUM_SPEED;

        movementInterval++;

        int movementDelay = this.movementSpeed;

// Update diagonal movement more frequently for smooth animation.
        if (this.movementPattern == MovementPattern.DIAGONAL_BOUNCE) {
            movementDelay = 2;
        }

        if (movementInterval >= movementDelay) {
            movementInterval = 0;

            if (this.movementPattern == MovementPattern.WAVE_BOUNCE) {
                int[] waveMovement = moveWaveBounce();
                movementX = waveMovement[0];
                movementY = waveMovement[1];
            } else if (this.movementPattern == MovementPattern.DIAGONAL_BOUNCE) {
                int[] diagonalMovement = moveDiagonalBounce();
                movementX = diagonalMovement[0];
                movementY = diagonalMovement[1];
            } else {

			boolean isAtBottom = positionY
					+ this.height > screen.getHeight() - BOTTOM_MARGIN;
			boolean isAtRightSide = positionX
					+ this.width >= screen.getWidth() - SIDE_MARGIN;
			boolean isAtLeftSide = positionX <= SIDE_MARGIN;
			boolean isAtHorizontalAltitude = positionY % DESCENT_DISTANCE == 0;

			if (currentDirection == Direction.DOWN) {
				if (isAtHorizontalAltitude)
					if (previousDirection == Direction.RIGHT) {
						currentDirection = Direction.LEFT;
						this.logger.info("Formation now moving left 1");
					} else {
						currentDirection = Direction.RIGHT;
						this.logger.info("Formation now moving right 2");
					}
			} else if (currentDirection == Direction.LEFT) {
				if (isAtLeftSide)
					if (!isAtBottom) {
						previousDirection = currentDirection;
						currentDirection = Direction.DOWN;
						this.logger.info("Formation now moving down 3");
					} else {
						currentDirection = Direction.RIGHT;
						this.logger.info("Formation now moving right 4");
					}
			} else {
				if (isAtRightSide)
					if (!isAtBottom) {
						previousDirection = currentDirection;
						currentDirection = Direction.DOWN;
						this.logger.info("Formation now moving down 5");
					} else {
						currentDirection = Direction.LEFT;
						this.logger.info("Formation now moving left 6");
					}
			}

			if (currentDirection == Direction.RIGHT)
				movementX = X_SPEED;
			else if (currentDirection == Direction.LEFT)
				movementX = -X_SPEED;
			else
				movementY = Y_SPEED;
			}

			positionX += movementX;
			positionY += movementY;

			// Cleans explosions.
			List<EnemyShip> destroyed;
			for (List<EnemyShip> column : this.enemyShips) {
				destroyed = new ArrayList<EnemyShip>();
				for (EnemyShip ship : column) {
					if (ship != null && ship.isDestroyed()) {
						destroyed.add(ship);
						this.logger.info("Removed enemy "
								+ column.indexOf(ship) + " from column "
								+ this.enemyShips.indexOf(column));
					}
				}
				column.removeAll(destroyed);
			}

			for (List<EnemyShip> column : this.enemyShips)
				for (EnemyShip enemyShip : column) {
					enemyShip.move(movementX, movementY);
					enemyShip.update();
				}
		}
	}

	/**
	 * Cleans empty columns, adjusts the width and height of the formation.
	 */
	private void cleanUp() {
		Set<Integer> emptyColumns = new HashSet<Integer>();
		int maxColumn = 0;
		int minPositionY = Integer.MAX_VALUE;
		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				// Height of this column
				int columnSize = column.get(column.size() - 1).positionY
						- this.positionY + this.shipHeight;
				maxColumn = Math.max(maxColumn, columnSize);
				minPositionY = Math.min(minPositionY, column.get(0)
						.getPositionY());
			} else {
				// Empty column, we remove it.
				emptyColumns.add(this.enemyShips.indexOf(column));
			}
		}
		for (int index : emptyColumns) {
			this.enemyShips.remove(index);
			logger.info("Removed column " + index);
		}

		int leftMostPoint = 0;
		int rightMostPoint = 0;

		for (List<EnemyShip> column : this.enemyShips) {
			if (!column.isEmpty()) {
				if (leftMostPoint == 0)
					leftMostPoint = column.get(0).getPositionX();
				rightMostPoint = column.get(0).getPositionX();
			}
		}

		this.width = rightMostPoint - leftMostPoint + this.shipWidth;
		this.height = maxColumn;

		this.positionX = leftMostPoint;
		this.positionY = minPositionY;
	}

	/**
	 * Shoots a bullet downwards.
	 *
	 * @param bullets
	 *            Bullets set to add the bullet being shot.
	 */
	public final void shoot(final Set<Bullet> bullets) {
		shoot(bullets, null);
	}

	/**
	 * Shoots a bullet downwards, aimed at the player when one is given.
	 *
	 * @param bullets
	 *            Bullets set to add the bullet being shot.
	 * @param player
	 *            Player ship to aim at, or null to shoot straight down.
	 */
	public final void shoot(final Set<Bullet> bullets, final Ship player) {
		if (this.shootingCooldown == null
				|| !this.shootingCooldown.checkFinished())
			return;

		// Only ships in the bottom row shoot, and each has its own cooldown.
		List<EnemyShip> readyShooters = new ArrayList<EnemyShip>();
		for (EnemyShip shooter : this.shooters)
			if (shooter.canShoot())
				readyShooters.add(shooter);
		if (readyShooters.isEmpty())
			return;

		int index = (int) (Math.random() * readyShooters.size());
		EnemyShip shooter = readyShooters.get(index);
		this.shootingCooldown.reset();
		shooter.resetShootingCooldown();

		int enemyX = shooter.getPositionX() + shooter.width / 2;
		int enemyY = shooter.getPositionY();

		if (player == null) {
			bullets.add(BulletPool.getBullet(enemyX, enemyY, BULLET_SPEED,
					Bullet.Owner.ENEMY));
			return;
		}

		// Aim the bullet at the player instead of straight down.
		int playerX = player.getPositionX() + player.width / 2;
		int playerY = player.getPositionY() + player.height / 2;
		double dx = playerX - enemyX;
		double dy = playerY - enemyY;
		double distance = Math.sqrt(dx * dx + dy * dy);

		int speedX = 0;
		int speedY = BULLET_SPEED;
		if (distance > 0) {
			speedX = (int) Math.round((dx / distance) * BULLET_SPEED);
			speedY = (int) Math.round((dy / distance) * BULLET_SPEED);
		}

		// An aimed shot can travel sideways or upwards, so the owner is set
		// explicitly rather than inferred from the speed.
		bullets.add(BulletPool.getBullet(enemyX, enemyY, speedY, speedX,
				Bullet.Owner.ENEMY));
	}

	/**
	 * Destroys a ship.
	 *
	 * @param destroyedShip
	 *            Ship to be destroyed.
	 */
	public final void destroy(final EnemyShip destroyedShip) {
		for (List<EnemyShip> column : this.enemyShips)
			for (int i = 0; i < column.size(); i++)
				if (column.get(i).equals(destroyedShip)) {
					column.get(i).destroy();
					this.logger.info("Destroyed ship in ("
							+ this.enemyShips.indexOf(column) + "," + i + ")");
				}

		// Updates the list of ships that can shoot the player.
		if (this.shooters.contains(destroyedShip)) {
			int destroyedShipIndex = this.shooters.indexOf(destroyedShip);
			int destroyedShipColumnIndex = -1;

			for (List<EnemyShip> column : this.enemyShips)
				if (column.contains(destroyedShip)) {
					destroyedShipColumnIndex = this.enemyShips.indexOf(column);
					break;
				}

			EnemyShip nextShooter = getNextShooter(this.enemyShips
					.get(destroyedShipColumnIndex));

			if (nextShooter != null) {
				nextShooter.setShootingCooldown(
						destroyedShip.getShootingCooldown());
				this.shooters.set(destroyedShipIndex, nextShooter);
			} else {
				this.shooters.remove(destroyedShipIndex);
				this.logger.info("Shooters list reduced to "
						+ this.shooters.size() + " members.");
			}
		}

		this.shipCount--;
	}

	/**
	 * Destroys a ship that touched the player, removing it from the shooters
	 * in the same frame.
	 *
	 * @param enemyShip
	 *            Ship that collided with the player.
	 * @return True if the ship was destroyed now, false if it already was.
	 */
	public final boolean destroyOnPlayerCollision(final EnemyShip enemyShip) {
		if (enemyShip == null || enemyShip.isDestroyed())
			return false;
		destroy(enemyShip);
		return true;
	}

	/**
	 * Returns the number of enemy ships still alive, for the HUD.
	 *
	 * @return Remaining enemy ships.
	 */
	public final int getRemainingCount() {
		return this.shipCount;
	}

	/**
	 * Returns the number of enemy ships at the start of the level.
	 *
	 * @return Total enemy ships.
	 */
	public final int getTotalCount() {
		return this.nShipsWide * this.nShipsHigh;
	}

	/**
	 * Returns the number of enemy ships destroyed in this level.
	 *
	 * @return Destroyed enemy ships.
	 */
	public final int getDestroyedCount() {
		return getTotalCount() - this.shipCount;
	}

	/**
	 * Returns the points the player can still earn from alive ships.
	 *
	 * @return Remaining points.
	 */
	public final int getRemainingPoints() {
		int points = 0;
		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip ship : column)
				if (!ship.isDestroyed())
					points += ship.getPointValue();
		return points;
	}

	/**
	 * Gets the ship on a given column that will be in charge of shooting.
	 *
	 * @param column
	 *            Column to search.
	 * @return New shooter ship.
	 */
	public final EnemyShip getNextShooter(final List<EnemyShip> column) {
		Iterator<EnemyShip> iterator = column.iterator();
		EnemyShip nextShooter = null;
		while (iterator.hasNext()) {
			EnemyShip checkShip = iterator.next();
			if (checkShip != null && !checkShip.isDestroyed())
				nextShooter = checkShip;
		}

		return nextShooter;
	}

	/**
	 * Returns an iterator over the ships in the formation.
	 *
	 * @return Iterator over the enemy ships.
	 */
	@Override
	public final Iterator<EnemyShip> iterator() {
		Set<EnemyShip> enemyShipsList = new HashSet<EnemyShip>();

		for (List<EnemyShip> column : this.enemyShips)
			for (EnemyShip enemyShip : column)
				enemyShipsList.add(enemyShip);

		return enemyShipsList.iterator();
	}

	/**
	 * Checks if there are any ships remaining.
	 *
	 * @return True when all ships have been destroyed.
	 */
	public final boolean isEmpty() {
		return this.shipCount <= 0;
	}
}
