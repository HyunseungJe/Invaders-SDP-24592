
package entity.item;

import entity.Ship;
import engine.DrawManager.SpriteType;

/**
 * Hyperfire item.
 * Reduces the player's shooting cooldown by 60%
 * for 10 seconds.
 */
public class Hyperfire extends Item {

    private static final long DURATION_MS = 10_000;

    /**
     * Creates a Hyperfire item at the given position.
     */
    public Hyperfire(final int positionX, final int positionY) {
        super(positionX, positionY, 12, 12, SpriteType.Bullet);
    }

    /**
     * Activates the Hyperfire effect on the player ship.
     */
    @Override
    public void applyEffect(final Ship ship) {
        ship.setHyperfireEnabled(true);
    }

    /**
     * Removes the Hyperfire effect from the player ship.
     */
    @Override
    public void removeEffect(final Ship ship) {
        ship.setHyperfireEnabled(false);
    }

    /**
     * Returns the duration of the buff in milliseconds.
     */
    @Override
    public long getDuration() {
        return DURATION_MS;
    }

    /**
     * Returns the name of the item.
     */
    @Override
    public String getName() {
        return "Hyperfire";
    }
}
