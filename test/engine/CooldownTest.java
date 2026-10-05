package engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Cooldown}.
 *
 * @author <a href="malito:jhs0622@hanyang.ac.kr">Hyunseung Je</a>
 */
class CooldownTest {

    @Test
    void newCooldownIsFinished() {
        Cooldown cooldown = new Cooldown(1000);

        assertTrue(cooldown.checkFinished());
    }

    @Test
    void notFinishedImmediatelyAfterReset() {
        Cooldown cooldown = new Cooldown(60_000);

        cooldown.reset();

        assertFalse(cooldown.checkFinished());
    }

    @Test
    void finishedAfterDurationPasses() throws InterruptedException {
        Cooldown cooldown = new Cooldown(20);

        cooldown.reset();
        Thread.sleep(60);

        assertTrue(cooldown.checkFinished());
    }

    @Test
    void resetRestartsCooldown() throws InterruptedException {
        Cooldown cooldown = new Cooldown(100);

        cooldown.reset();
        Thread.sleep(200);
        assertTrue(cooldown.checkFinished());

        cooldown.reset();
        assertFalse(cooldown.checkFinished());
    }
}
