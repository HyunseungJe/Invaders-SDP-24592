package engine;

import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import screen.Screen;

/**
 * Implements a frame to show screens on.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
@SuppressWarnings("serial")
public class Frame extends JFrame {

	/** Frame width. */
	private int width;
	/** Frame height. */
	private int height;
	/** Screen currently shown. */
	private volatile Screen currentScreen;
	private volatile boolean closeRequested;

	/**
	 * Initializes the new frame.
	 *
	 * @param width
	 *               Frame width.
	 * @param height
	 *               Frame height.
	 */
	public Frame(final int width, final int height) {
		setSize(width, height);
		setResizable(false);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent event) {
				closeRequested = true;
				Screen screen = currentScreen;
				if (screen != null)
					screen.requestStop();
			}
		});

		setLocationRelativeTo(null);
		setVisible(true);

		Insets insets = getInsets();
		this.width = width - insets.left - insets.right;
		this.height = height - insets.top + insets.bottom;
		setTitle("Invaders");

		addKeyListener(Core.getInputManager());
		addWindowFocusListener(Core.getInputManager());
		getContentPane().addMouseListener(Core.getInputManager());
		getContentPane().addMouseMotionListener(Core.getInputManager());
	}

	/**
	 * Sets current screen.
	 *
	 * @param screen
	 *               Screen to show.
	 * @return Return code of the finished screen.
	 */
	public final int setScreen(final Screen screen) {
		currentScreen = screen;
		if (closeRequested)
			return 0;
		try {
			currentScreen.initialize();
			return currentScreen.run();
		} catch (RuntimeException | Error failure) {
			// A failed game loop cannot process a later close request.
			dispose();
			throw failure;
		}
	}

	public final boolean isCloseRequested() {
		return this.closeRequested;
	}

	/** Keeps the window open when the player returns to the menu after a save failure. */
	public final void cancelCloseRequest() {
		this.closeRequested = false;
	}

	/**
	 * Getter for frame width.
	 *
	 * @return Frame width.
	 */
	public final int getWidth() {
		return this.width;
	}

	/**
	 * Getter for frame height.
	 *
	 * @return Frame height.
	 */

	public final int getHeight() {
		return this.height;
	}
}
