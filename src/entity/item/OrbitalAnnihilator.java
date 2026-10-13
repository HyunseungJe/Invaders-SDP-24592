package entity.item;

import entity.Entity;
import entity.Ship;

/**
 * Orbital Annihilator (Doomsday Laser).
 *
 * Ultimate item: when triggered it charges for 1.5 seconds, then fires a
 * full-screen vertical piercing beam for 2 seconds. The beam starts at the
 * ship and goes up to the top of the play area. It destroys every standard
 * enemy and neutralizes enemy projectiles inside its corridor.
 *
 * The class only manages the state and the geometry of the beam; the game
 * screen uses it to apply the effects and the draw manager to render it.
 */
public class OrbitalAnnihilator {

    /** Charging phase duration, in milliseconds. */
    public static final int CHARGE_DURATION = 1500;
    /** Beam duration, in milliseconds. */
    public static final int BEAM_DURATION = 2000;
    /** Width of the beam, in pixels. */
    public static final int BEAM_WIDTH = 24;
    /** Level in which the item is introduced. */
    public static final int REQUIRED_LEVEL = 7;
    /** Recharge duration, in milliseconds. */
    public static final int RECHARGE_DURATION = 20000;

    /** Possible states of the item. */
    private enum State {
        /** Ready to be triggered. */
        IDLE,
        /** Charging, no damage yet. */
        CHARGING,
        /** Beam firing. */
        FIRING,
        /** Recharging after use, cannot be triggered. */
        RECHARGING
    }

    /** Current state. */
    private State state;
    /** Moment the current phase started. */
    private long phaseStart;

    /**
     * Constructor, the item starts empty and must recharge before use.
     */
    public OrbitalAnnihilator() {
        // Starts empty, the recharge begins at the first update().
        this.state = State.RECHARGING;
        this.phaseStart = 0;
    }

    /**
     * Triggers the item, if it is ready.
     *
     * @return True if the charging phase started.
     */
    public final boolean trigger() {
        if (this.state != State.IDLE)
            return false;
        this.state = State.CHARGING;
        this.phaseStart = System.currentTimeMillis();
        return true;
    }

    /**
     * Updates the state: charging becomes firing, firing becomes spent.
     */
    public final void update() {
        // First update (countdown finished): the recharge starts now.
        if (this.state == State.RECHARGING && this.phaseStart == 0) {
            this.phaseStart = System.currentTimeMillis();
            return;
        }
        long elapsed = System.currentTimeMillis() - this.phaseStart;
        if (this.state == State.CHARGING && elapsed >= CHARGE_DURATION) {
            this.state = State.FIRING;
            this.phaseStart = System.currentTimeMillis();
        } else if (this.state == State.FIRING && elapsed >= BEAM_DURATION) {
            this.state = State.RECHARGING;
            this.phaseStart = System.currentTimeMillis();
        } else if (this.state == State.RECHARGING
                && elapsed >= RECHARGE_DURATION) {
            this.state = State.IDLE;
        }
    }

    /**
     * Cancels a charge in progress (the ship was hit), the item can be
     * triggered again.
     */
    public final void cancelCharge() {
        if (this.state == State.CHARGING) {
            this.state = State.RECHARGING;
            this.phaseStart = System.currentTimeMillis();
        }
    }

    /**
     * Checks if the item is charging.
     *
     * @return True during the charging phase.
     */
    public final boolean isCharging() {
        return this.state == State.CHARGING;
    }

    /**
     * Checks if the beam is firing.
     *
     * @return True while the beam is active and deals damage.
     */
    public final boolean isFiring() {
        return this.state == State.FIRING;
    }

    /**
     * Checks if the item can be triggered.
     *
     * @return True if ready.
     */
    public final boolean isReady() {
        return this.state == State.IDLE;
    }

    /**
     * Progress of the charging phase.
     *
     * @return Value between 0 (just started) and 1 (fully charged).
     */
    public final float getChargeProgress() {
        if (this.state != State.CHARGING)
            return this.state == State.FIRING ? 1f : 0f;
        float progress = (System.currentTimeMillis() - this.phaseStart)
                / (float) CHARGE_DURATION;
        return Math.min(1f, progress);
    }
    /**
     * Progress of the recharge.
     *
     * @return Value between 0 (empty) and 1 (ready to use).
     */
    public final float getRechargeProgress() {
        if (this.state == State.IDLE)
            return 1f;
        if (this.state == State.RECHARGING) {
            // Recharge not started yet (countdown still running).
            if (this.phaseStart == 0)
                return 0f;
            return Math.min(1f, (System.currentTimeMillis() - this.phaseStart)
                    / (float) RECHARGE_DURATION);
        }
        return 0f;
    }

    /**
     * Left side of the beam corridor, centered on the ship.
     *
     * @param ship
     *            Ship that fires the beam.
     * @return X coordinate of the left side of the beam.
     */
    public static int getBeamLeft(final Ship ship) {
        return ship.getPositionX() + ship.getWidth() / 2 - BEAM_WIDTH / 2;
    }

    /**
     * Right side of the beam corridor.
     *
     * @param ship
     *            Ship that fires the beam.
     * @return X coordinate of the right side of the beam.
     */
    public static int getBeamRight(final Ship ship) {
        return getBeamLeft(ship) + BEAM_WIDTH;
    }

    /**
     * Checks if an entity touches the beam corridor (horizontal overlap,
     * the beam covers everything above the ship).
     *
     * @param ship
     *            Ship that fires the beam.
     * @param target
     *            Entity to test.
     * @return True if the entity is inside the corridor.
     */
    public static boolean hits(final Ship ship, final Entity target) {
        return target.getPositionX() < getBeamRight(ship)
                && target.getPositionX() + target.getWidth() > getBeamLeft(ship)
                && target.getPositionY() < ship.getPositionY();
    }
}

