package screen;

import audio.AudioManager;
import currency.PlayerAssetsState;
import currency.PurchaseResult;
import currency.ShopCatalog.Category;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import shop.ShopController;
import shop.ShopController.SaveStatus;
import shop.ShopProduct;

/** Displays products using the shared session controller. */
public class ShopScreen extends Screen {
    public static final int RETURN_TO_MAIN_MENU = 1;

    private final ShopController controller;
    private final List<ShopProduct> items = new ArrayList<ShopProduct>();
    private final List<ShopProduct> unlocks = new ArrayList<ShopProduct>();
    private PlayerAssetsState assets;
    /** 0: items, 1: permanent unlocks, 2: footer actions. */
    private int selectedSection;
    private int itemIndex;
    private int unlockIndex;
    /** 0: Back, 1: Retry save (only while a save is pending). */
    private int selectedAction;
    private PurchaseResult lastResult;
    private boolean upHeld;
    private boolean downHeld;
    private boolean leftHeld;
    private boolean rightHeld;
    private boolean confirmHeld;
    private boolean backHeld;
    private boolean retryHeld;

    public ShopScreen(int width, int height, int fps, ShopController controller) {
        super(validateArguments(width, height, fps, controller), height, fps);
        this.controller = controller;
        for (ShopProduct product : controller.getProducts()) {
            if (product.getCategory() == Category.INVENTORY) items.add(product);
            else unlocks.add(product);
        }
        this.returnCode = RETURN_TO_MAIN_MENU;
    }

    private static int validateArguments(int width, int height, int fps, ShopController controller) {
        if (width <= 0 || height <= 0 || fps <= 0 || controller == null) {
            throw new IllegalArgumentException("Shop requires positive dimensions/FPS and a controller.");
        }
        return width;
    }

    @Override
    public void initialize() {
        assets = controller.snapshot();
        selectedSection = !items.isEmpty() ? 0 : !unlocks.isEmpty() ? 1 : 2;
        itemIndex = 0;
        unlockIndex = 0;
        selectedAction = 0;
        lastResult = null;
        inputDelay.reset();
        // Consume held state on entry, including a confirm key from the Main Menu.
        processInput(false);
    }

    @Override
    public int run() {
        super.run();
        return RETURN_TO_MAIN_MENU;
    }

    @Override
    protected void update() {
        processInput(inputDelay.checkFinished());
        draw();
    }

    /** Track key edges even during the entry delay; never replay a held purchase. */
    void processInput(boolean acceptsInput) {
        boolean up = inputManager.isKeyDown(KeyEvent.VK_UP) || inputManager.isKeyDown(KeyEvent.VK_W);
        boolean down = inputManager.isKeyDown(KeyEvent.VK_DOWN) || inputManager.isKeyDown(KeyEvent.VK_S);
        boolean left = inputManager.isKeyDown(KeyEvent.VK_LEFT) || inputManager.isKeyDown(KeyEvent.VK_A);
        boolean right = inputManager.isKeyDown(KeyEvent.VK_RIGHT) || inputManager.isKeyDown(KeyEvent.VK_D);
        boolean confirm = inputManager.isKeyDown(KeyEvent.VK_SPACE) || inputManager.isKeyDown(KeyEvent.VK_ENTER);
        boolean back = inputManager.isKeyDown(KeyEvent.VK_ESCAPE);
        boolean retry = inputManager.isKeyDown(KeyEvent.VK_R);
        boolean moveUp = up && !upHeld;
        boolean moveDown = down && !downHeld;
        boolean moveLeft = left && !leftHeld;
        boolean moveRight = right && !rightHeld;
        boolean select = confirm && !confirmHeld;
        boolean leave = back && !backHeld;
        boolean retryPressed = retry && !retryHeld;
        upHeld = up;
        downHeld = down;
        leftHeld = left;
        rightHeld = right;
        confirmHeld = confirm;
        backHeld = back;
        retryHeld = retry;
        if (!acceptsInput) {
            return;
        }

        if (controller.getSaveStatus() != SaveStatus.FAILED) selectedAction = 0;
        if (leave) {
            leaveShop();
        } else if (moveUp || moveDown) {
            moveSection(moveUp ? -1 : 1);
            AudioManager.playSFX("audio/sfx/menu_move.wav");
        } else if (moveLeft || moveRight) {
            moveWithinSection(moveLeft ? -1 : 1);
            AudioManager.playSFX("audio/sfx/menu_move.wav");
        } else if (retryPressed && controller.getSaveStatus() == SaveStatus.FAILED) {
            retrySave();
        } else if (select) {
            if (selectedSection == 2) {
                if (selectedAction == 1) retrySave();
                else leaveShop();
            } else {
                buySelectedProduct();
            }
        }
    }

    private void moveSection(int direction) {
        do {
            selectedSection = (selectedSection + direction + 3) % 3;
        } while ((selectedSection == 0 && items.isEmpty())
                || (selectedSection == 1 && unlocks.isEmpty()));
    }

    private void moveWithinSection(int direction) {
        if (selectedSection == 0) {
            itemIndex = (itemIndex + direction + items.size()) % items.size();
        } else if (selectedSection == 1) {
            unlockIndex = (unlockIndex + direction + unlocks.size()) % unlocks.size();
        } else if (controller.getSaveStatus() == SaveStatus.FAILED) {
            selectedAction = 1 - selectedAction;
        }
    }

    private void buySelectedProduct() {
        ShopProduct product = selectedSection == 0 ? items.get(itemIndex) : unlocks.get(unlockIndex);
        // Refresh before the ownership check in case another game-loop action changed assets.
        assets = controller.snapshot();
        if (product.getCategory() == Category.UNLOCKABLE
                && assets.getUnlockedContentIds().contains(product.getId())) {
            lastResult = PurchaseResult.ALREADY_UNLOCKED;
        } else {
            lastResult = controller.purchase(product.getId());
        }
        assets = controller.snapshot();
        AudioManager.playSFX("audio/sfx/menu_select.wav");
    }

    private void retrySave() {
        controller.retrySave();
        assets = controller.snapshot();
        if (controller.getSaveStatus() != SaveStatus.FAILED) selectedAction = 0;
        AudioManager.playSFX("audio/sfx/menu_select.wav");
    }

    private void leaveShop() {
        AudioManager.playSFX("audio/sfx/menu_back.wav");
        returnCode = RETURN_TO_MAIN_MENU;
        isRunning = false;
    }

    protected void draw() {
        drawManager.initDrawing(this);
        drawManager.drawShop(this, items, unlocks, assets, selectedSection,
                itemIndex, unlockIndex, selectedAction,
                resultMessage(lastResult), lastResult == PurchaseResult.SUCCESS,
                controller.getSaveStatus());
        drawManager.completeDrawing(this);
    }

    static String resultMessage(PurchaseResult result) {
        if (result == null) return "";
        switch (result) {
        case SUCCESS: return "Purchase complete.";
        case INSUFFICIENT_FUNDS: return "Not enough credits.";
        case ALREADY_UNLOCKED: return "Already owned. No credits spent.";
        case INVALID_ITEM_ID: return "Invalid product. Nothing purchased.";
        case CATEGORY_MISMATCH: return "Unknown product or wrong category.";
        case INVALID_QUANTITY: return "Invalid quantity. Nothing purchased.";
        case INTEGER_OVERFLOW_DETECTED: return "Purchase exceeds the asset limit.";
        case ITEM_NOT_AVAILABLE: return "This product is unavailable.";
        default: return "Purchase could not be completed.";
        }
    }
}
