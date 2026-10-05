package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Score}.
 *
 * @author <a href="malito:jhs0622@hanyang.ac.kr">Hyunseung Je</a>
 */
class ScoreTest {

    @Test
    void gettersReturnConstructorValues() {
        Score score = new Score("ABC", 100);

        assertEquals("ABC", score.getName());
        assertEquals(100, score.getScore());
    }

    @Test
    void higherScoreComesFirst() {
        Score high = new Score("AAA", 200);
        Score low = new Score("BBB", 100);

        assertTrue(high.compareTo(low) < 0);
        assertTrue(low.compareTo(high) > 0);
    }

    @Test
    void equalScoresCompareAsZero() {
        Score a = new Score("AAA", 100);
        Score b = new Score("BBB", 100);

        assertEquals(0, a.compareTo(b));
    }
}