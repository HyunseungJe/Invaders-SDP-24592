
import entity.Ship;
import entity.item.Hyperfire;

public class TestHyperfire {

    public static void main(String[] args) {

        Ship ship = new Ship(100, 100);
        Hyperfire hyperfire = new Hyperfire(100, 100);

        // Test 1: Hyperfire initially disabled
        System.out.println("Initially active: "
                + ship.isHyperfireEnabled());

        // Test 2: Activate Hyperfire
        hyperfire.applyEffect(ship);
        System.out.println("After activation: "
                + ship.isHyperfireEnabled());

        // Test 3: Disable Hyperfire
        hyperfire.removeEffect(ship);
        System.out.println("After removal: "
                + ship.isHyperfireEnabled());
    }
}
