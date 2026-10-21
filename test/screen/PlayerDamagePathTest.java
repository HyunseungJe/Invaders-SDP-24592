package screen;

import java.lang.reflect.Field;

import engine.GameSettings;
import engine.GameState;
import entity.PlayerStatus;
import entity.Ship;

/**
 * Standalone checks that every source of damage reaches the player through
 * one path, so that a protection cannot apply to an enemy bullet and be
 * skipped by a boss bullet.
 */
public final class PlayerDamagePathTest {

	private PlayerDamagePathTest() {
	}

	private static void check(final boolean condition, final String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	public static void main(final String[] args) throws Exception {
		checkUnprotectedHitCostsOneLife();
		checkEnergyShieldStopsBossDamage();
		checkChameleonHullStopsBossDamage();
		checkTimedShieldStopsDamageAndEffects();
		checkOverlappingBulletsCostOneLifeBetweenThem();

		System.out.println("Player damage path checks passed.");
	}

	/** Without a protection a hit costs exactly one life. */
	private static void checkUnprotectedHitCostsOneLife() throws Exception {
		GameScreen game = newGame();
		PlayerStatus status = statusOf(game);

		check(game.damagePlayer(1, "test"), "An unprotected hit is accepted");
		check(status.getLives() == 2,
				"An unprotected hit costs exactly one life");
		check(shipOf(game).isDestroyed(),
				"An accepted hit starts the recovery period");
	}

	/**
	 * The collected Energy Shield lives on Ship. A boss bullet used to skip
	 * this check entirely.
	 */
	private static void checkEnergyShieldStopsBossDamage() throws Exception {
		GameScreen game = newGame();
		PlayerStatus status = statusOf(game);
		shipOf(game).setShieldActive(true);

		check(!game.damagePlayer(3, "boss"),
				"A boss bullet is refused while the Energy Shield is up");
		check(status.getLives() == 3,
				"The Energy Shield protects against boss damage");
		check(!shipOf(game).isDestroyed(),
				"A shielded hit does not start the recovery period");
	}

	/** The Chameleon Hull makes the ship immune, bosses included. */
	private static void checkChameleonHullStopsBossDamage() throws Exception {
		GameScreen game = newGame();
		PlayerStatus status = statusOf(game);
		shipOf(game).activateChameleonHull();

		check(!game.damagePlayer(3, "boss"),
				"A boss bullet is refused while the Chameleon Hull is active");
		check(status.getLives() == 3,
				"The Chameleon Hull protects against boss damage");
	}

	/**
	 * PlayerStatus owns the timed shield. When it refuses the damage, the
	 * destruction effects must not run either.
	 */
	private static void checkTimedShieldStopsDamageAndEffects()
			throws Exception {
		GameScreen game = newGame();
		PlayerStatus status = statusOf(game);
		status.activateShield();

		check(!game.damagePlayer(1, "test"),
				"A hit is refused while the timed shield is up");
		check(status.getLives() == 3, "The timed shield preserves lives");
		check(!shipOf(game).isDestroyed(),
				"A hit blocked by the timed shield does not destroy the ship");
	}

	/**
	 * Several bullets can overlap the ship in the same frame. Only the first
	 * accepted hit costs a life; the rest meet the recovery period.
	 */
	private static void checkOverlappingBulletsCostOneLifeBetweenThem()
			throws Exception {
		GameScreen game = newGame();
		PlayerStatus status = statusOf(game);

		check(game.damagePlayer(1, "first"), "The first overlapping hit lands");
		check(!game.damagePlayer(1, "second"),
				"The second overlapping hit is refused");
		check(!game.damagePlayer(1, "third"),
				"The third overlapping hit is refused");
		check(status.getLives() == 2,
				"Overlapping bullets cost one life between them, not three");
	}

	private static GameScreen newGame() {
		GameScreen game = new GameScreen(new GameState(1, 0, 3, 0, 0),
				new GameSettings(5, 4, 60, 2000), false, 448, 520, 60);
		game.initialize();
		return game;
	}

	private static PlayerStatus statusOf(final GameScreen game)
			throws Exception {
		return (PlayerStatus) read(game, "playerStatus");
	}

	private static Ship shipOf(final GameScreen game) throws Exception {
		return (Ship) read(game, "ship");
	}

	private static Object read(final Object target, final String name)
			throws Exception {
		Field field = GameScreen.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
