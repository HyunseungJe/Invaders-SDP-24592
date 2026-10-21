package entity;

import java.awt.Color;

import engine.Cooldown;
import engine.Core;
import engine.GameColors;
import engine.DrawManager.SpriteType;

/**
 * Implements a enemy ship, to be destroyed by the player.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class EnemyShip extends Entity {

	/** Point value of a type A enemy. */
	private static final int A_TYPE_POINTS = 10;
	/** Point value of a type B enemy. */
	private static final int B_TYPE_POINTS = 20;
	/** Point value of a type C enemy. */
	private static final int C_TYPE_POINTS = 30;
	/** Point value of a bonus enemy. */
	private static final int BONUS_TYPE_POINTS = 100;

	/** Cooldown between sprite changes. */
	private Cooldown animationCooldown;
	/** Checks if the ship has been hit by a bullet. */
	private boolean isDestroyed;
	/** Values of the ship, in points, when destroyed. */
	private int pointValue;
	/** Cooldown between this ship's shots. Null if the ship cannot shoot. */
	private Cooldown shootingCooldown;
	/**
	 * Constructor, establishes the ship's properties.
	 *
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 * @param spriteType
	 *            Sprite type, image corresponding to the ship.
	 */
	public EnemyShip(final int positionX, final int positionY,
			final SpriteType spriteType) {
		super(positionX, positionY, 12 * 2, 8 * 2, colorFor(spriteType));
		this.spriteType = spriteType;
		this.animationCooldown = Core.getCooldown(500);
		this.isDestroyed = false;

		switch (this.spriteType) {
		case EnemyShipA1:
		case EnemyShipA2:
			this.pointValue = A_TYPE_POINTS;
			break;
		case EnemyShipB1:
		case EnemyShipB2:
			this.pointValue = B_TYPE_POINTS;
			break;
		case EnemyShipC1:
		case EnemyShipC2:
			this.pointValue = C_TYPE_POINTS;
			break;
		default:
			this.pointValue = 0;
			break;
		}
	}

	/**
	 * Constructor, establishes the ship's properties for a special ship, with
	 * known starting properties.
	 */
	public EnemyShip() {
		 // super(-32, 60, 16 * 2, 7 * 2, Color.RED);
		super(-32, 60, 16 * 2, 7 * 2, GameColors.ENEMY_SPECIAL);
		this.spriteType = SpriteType.EnemyShipSpecial;
		this.isDestroyed = false;
		this.pointValue = BONUS_TYPE_POINTS;

	}

	private static Color colorFor(final SpriteType spriteType) {
		switch (spriteType) {
			case EnemyShipB1:
			case EnemyShipB2:
				return GameColors.ENEMY_B;
			case EnemyShipC1:
			case EnemyShipC2:
				return GameColors.ENEMY_C;
			default:
				return GameColors.ENEMY_A;
		}
	}

	/**
	 * Getter for the score bonus if this ship is destroyed.
	 *
	 * @return Value of the ship.
	 */
	public final int getPointValue() {
		return this.pointValue;
	}

	/**
	 * Moves the ship the specified distance.
	 *
	 * @param distanceX
	 *            Distance to move in the X axis.
	 * @param distanceY
	 *            Distance to move in the Y axis.
	 */
	public final void move(final int distanceX, final int distanceY) {
		this.positionX += distanceX;
		this.positionY += distanceY;
	}

	/**
	 * Updates attributes, mainly used for animation purposes.
	 */
	public final void update() {
		if (this.animationCooldown.checkFinished()) {
			this.animationCooldown.reset();

			switch (this.spriteType) {
			case EnemyShipA1:
				this.spriteType = SpriteType.EnemyShipA2;
				break;
			case EnemyShipA2:
				this.spriteType = SpriteType.EnemyShipA1;
				break;
			case EnemyShipB1:
				this.spriteType = SpriteType.EnemyShipB2;
				break;
			case EnemyShipB2:
				this.spriteType = SpriteType.EnemyShipB1;
				break;
			case EnemyShipC1:
				this.spriteType = SpriteType.EnemyShipC2;
				break;
			case EnemyShipC2:
				this.spriteType = SpriteType.EnemyShipC1;
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Destroys the ship, causing an explosion.
	 */
	public final void destroy() {
		this.isDestroyed = true;
		this.spriteType = SpriteType.Explosion;
	}

	/**
	 * Checks if the ship has been destroyed.
	 *
	 * @return True if the ship has been destroyed.
	 */
	public final boolean isDestroyed() {
		return this.isDestroyed;
	}
	/** Gives this ship a shooting cooldown, without restarting it. */
	public final void setShootingCooldown(final Cooldown cooldown) {
		this.shootingCooldown = cooldown;
	}

	/** Returns this ship's shooting cooldown, so a replacement can inherit it. */
	public final Cooldown getShootingCooldown() {
		return this.shootingCooldown;
	}

	/** True if this ship is alive, allowed to shoot, and its cooldown is over. */
	public final boolean canShoot() {
		return !this.isDestroyed
				&& this.shootingCooldown != null
				&& this.shootingCooldown.checkFinished();
	}

	/** Restarts this ship's cooldown after it has fired. */
	public final void resetShootingCooldown() {
		if (this.shootingCooldown != null)
			this.shootingCooldown.reset();
	}

	/**
	 * Creates an enemy ship of the given type, for the Level Design system.
	 *
	 * @param type      Type of enemy to create (A, B or C).
	 * @param positionX Initial position in the X axis.
	 * @param positionY Initial position in the Y axis.
	 * @return New enemy ship of the given type.
	 */
	public static EnemyShip createByType(final EnemyType type,
			final int positionX, final int positionY) {
		if (type == null)
			throw new IllegalArgumentException("Enemy type cannot be null.");
		return new EnemyShip(positionX, positionY, type.getSpriteType());
	}
}
