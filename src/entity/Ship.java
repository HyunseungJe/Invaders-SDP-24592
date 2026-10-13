package entity;

import java.awt.Color;
import java.util.Set;

import engine.Cooldown;
import engine.Core;
import engine.DrawManager.SpriteType;

/**
 * Implements a ship, to be controlled by the player.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class Ship extends Entity {

    /** Time between shots. */
    private static final int SHOOTING_INTERVAL = 400;

    /** Speed of the bullets shot by the ship. */
    private static final int BULLET_SPEED = -6;

    /** Movement of the ship for each unit of time. */
    private static final int SPEED = 2;

    /** Whether the Hyperfire buff is currently enabled. */
    private boolean hyperfireEnabled = false;

    /** Time of the last shot. */
    private long lastShotTime = 0;

    /** Time spent inactive between hits. */
    private Cooldown destructionCooldown;

    private boolean isShieldActive = false;

    private static final Color[] CHAMELEON_COLORS = {
            Color.CYAN,
            Color.MAGENTA,
            Color.YELLOW,
            Color.ORANGE,
            Color.BLUE
    };

    private boolean chameleonHullActive;
    private long chameleonActivationTime;
    private Color colorBeforeChameleon;
    private double chameleonMovementRemainder;

    private int widthBeforeChameleon;
    private int heightBeforeChameleon;
    private int normalPositionY;

    /**
     * Constructor, establishes the ship's properties.
     *
     * @param positionX
     *            Initial position of the ship in the X axis.
     * @param positionY
     *            Initial position of the ship in the Y axis.
     */
    public Ship(final int positionX, final int positionY) {
        super(positionX, positionY, 13 * 2, 8 * 2, Color.GREEN);

        this.spriteType = SpriteType.Ship;
        this.destructionCooldown = Core.getCooldown(1000);
    }

    /**
     * Moves the ship speed uni ts right, or until the right screen border is
     * reached.
     */
    public final void moveRight() {
        this.positionX += SPEED;
        addChameleonMovement(1);
    }

    /**
     * Moves the ship speed units left, or until the left screen border is
     * reached.
     */
    public final void moveLeft() {
        this.positionX -= SPEED;
        addChameleonMovement(-1);
    }

    /**
     * Shoots a bullet upwards.
     *
     * @param bullets
     *            List of bullets on screen, to add the new bullet.
     * @return Checks if the bullet was shot correctly.
     */
    public final boolean shoot(final Set<Bullet> bullets) {
        if (this.chameleonHullActive) {
            return false;
        }

        long currentTime = System.currentTimeMillis();

        int shootingInterval = SHOOTING_INTERVAL;

        if (this.hyperfireEnabled) {
            shootingInterval = (int) (SHOOTING_INTERVAL * 0.40);
        }

        if (lastShotTime == 0
                || currentTime - lastShotTime >= shootingInterval) {

            lastShotTime = currentTime;

            bullets.add(BulletPool.getBullet(
                    positionX + this.width / 2,
                    positionY,
                    BULLET_SPEED
            ));

            return true;
        }

        return false;
    }

    /**
     * Enables or disables the Hyperfire shooting buff.
     *
     * @param enabled True to enable Hyperfire, false to disable it.
     */
    public final void setHyperfireEnabled(final boolean enabled) {
        this.hyperfireEnabled = enabled;
    }

    /**
     * Checks whether Hyperfire is currently enabled.
     */
    public final boolean isHyperfireEnabled() {
        return this.hyperfireEnabled;
    }

    /**
     * Updates status of the ship.
     */
    public final void update() {
        if (this.chameleonHullActive) {
            long elapsed = System.nanoTime() - this.chameleonActivationTime;

            int index = (int) (
                    (elapsed / entity.item.ChameleonHull.COLOR_INTERVAL_NANOS)
                            % CHAMELEON_COLORS.length
            );

            setColor(CHAMELEON_COLORS[index]);
        }

        if (!this.destructionCooldown.checkFinished())
            this.spriteType = SpriteType.ShipDestroyed;
        else
            this.spriteType = SpriteType.Ship;
    }

    /**
     * Switches the ship to its destroyed state.
     */
    public final void destroy() {
        if (this.chameleonHullActive) {
            return;
        }

        deactivateChameleonHull();
        this.destructionCooldown.reset();
    }

    /**
     * Checks if the ship is destroyed.
     *
     * @return True if the ship is currently destroyed.
     */
    public final boolean isDestroyed() {
        return !this.destructionCooldown.checkFinished();
    }

    /**
     * Getter for the ship's speed.
     *
     * @return Speed of the ship.
     */
    public final int getSpeed() {
        return SPEED;
    }

    public boolean isShieldActive() {
        return this.isShieldActive;
    }

    public void setShieldActive(final boolean active) {
        this.isShieldActive = active;
    }

    public final void activateChameleonHull() {
        if (!this.chameleonHullActive) {
            this.colorBeforeChameleon = getColor();
            this.widthBeforeChameleon = this.width;
            this.heightBeforeChameleon = this.height;
            this.normalPositionY = this.positionY;

            this.width = entity.item.ChameleonHull.BALL_SIZE;
            this.height = entity.item.ChameleonHull.BALL_SIZE;
        }

        this.chameleonHullActive = true;
        this.chameleonActivationTime = System.nanoTime();

        setColor(CHAMELEON_COLORS[0]);
    }

    public final void deactivateChameleonHull() {
        if (this.chameleonHullActive) {
            setColor(this.colorBeforeChameleon);

            this.width = this.widthBeforeChameleon;
            this.height = this.heightBeforeChameleon;
            this.positionY = this.normalPositionY;
        }

        this.chameleonHullActive = false;
        this.chameleonMovementRemainder = 0;
    }

    public final boolean isChameleonHullActive() {
        return this.chameleonHullActive;
    }

    public final double getMovementSpeed() {
        return SPEED * (
                this.chameleonHullActive
                        ? entity.item.ChameleonHull.SPEED_MULTIPLIER
                        : 1.0
        );
    }

    /** Adds the power-up movement; the original movement code is preserved. */
    private void addChameleonMovement(final int direction) {
        if (!this.chameleonHullActive) {
            return;
        }

        this.chameleonMovementRemainder += direction * SPEED
                * (entity.item.ChameleonHull.SPEED_MULTIPLIER - 1.0);

        int extraPixels = (int) (
                this.chameleonMovementRemainder >= 0
                        ? Math.floor(this.chameleonMovementRemainder + 1e-9)
                        : Math.ceil(this.chameleonMovementRemainder - 1e-9)
        );

        this.positionX += extraPixels;
        this.chameleonMovementRemainder -= extraPixels;
    }

    public final void moveUp() {
        if (this.chameleonHullActive && !isDestroyed()) {
            this.positionY -= (int) getMovementSpeed();
        }
    }

    public final void moveDown() {
        if (this.chameleonHullActive && !isDestroyed()) {
            this.positionY += (int) getMovementSpeed();
        }
    }
}