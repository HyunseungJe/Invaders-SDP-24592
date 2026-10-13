package entity.item;

import engine.DrawManager.SpriteType;
import entity.Ship;

/**
 * Item qui octroie un bouclier d'energie temporaire (15 secondes).
 * Tant qu'il est actif, le vaisseau est protege contre les tirs ennemis.
 */
public class ShieldItem extends Item {

    /** Duree de 15 secondes en millisecondes. */
    private static final long DURATION = 15000;
    private static final int ITEM_WIDTH = 13;
    private static final int ITEM_HEIGHT = 8;

    public ShieldItem(final int positionX, final int positionY) {
        // On utilise temporairement un sprite existant pour l'affichage
        super(positionX, positionY, ITEM_WIDTH, ITEM_HEIGHT, SpriteType.EnemyShipSpecial);
    }

    @Override
    public void applyEffect(final Ship ship) {
        if (ship != null) {
            ship.setShieldActive(true);
            System.out.println("[ITEM] Energy Shield activé pour 15 secondes !");
        }
    }

    @Override
    public void removeEffect(final Ship ship) {
        if (ship != null) {
            ship.setShieldActive(false);
            System.out.println("[ITEM] Energy Shield expiré !");
        }
    }

    @Override
    public long getDuration() {
        return DURATION;
    }

    @Override
    public String getName() {
        return "Energy Shield";
    }
}