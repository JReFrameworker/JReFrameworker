import java.awt.Frame;
import java.awt.event.KeyEvent;

public class SimulatedTypingTest {

	private static Frame frame = new Frame();
	
	public static void main(String[] args) {
		System.out.println("Typing: ");
		System.out.println("This is a test of how evil spellwrecker can be.");
		type('T'); sleep(583);
		type('h'); sleep(411);
		type('i'); sleep(176);
		type('s'); sleep(152);
		type(' '); sleep(144);
		type('i'); sleep(200);
		type('s'); sleep(136);
		type(' '); sleep(120);
		type('a'); sleep(128);
		type(' '); sleep(119);
		type('t'); sleep(155);
		type('e'); sleep(83);
		type('s'); sleep(83);
		type('t'); sleep(99);
		type(' '); sleep(477);
		type('o'); sleep(157);
		type('f'); sleep(83);
		type(' '); sleep(67);
		type('h'); sleep(269);
		type('o'); sleep(96);
		type('w'); sleep(102);
		type(' '); sleep(378);
		type('e'); sleep(704);
		type('v'); sleep(505);
		type('i'); sleep(879);
		type('l'); sleep(160);
		type(' '); sleep(648);
		type('s'); sleep(1394);
		type('p'); sleep(132);
		type('e'); sleep(74);
		type('l'); sleep(560);
		type('l'); sleep(167);
		type('w'); sleep(153);
		type('r'); sleep(168);
		type('e'); sleep(96);
		type('c'); sleep(152);
		type('k'); sleep(119);
		type('e'); sleep(73);
		type('r'); sleep(72);
		type(' '); sleep(483);
		type('c'); sleep(381);
		type('a'); sleep(152);
		type('n'); sleep(192);
		type(' '); sleep(143);
		type('b'); sleep(201);
		type('e'); sleep(80);
		type('.');
	}

	private static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}
	}

	private static void type(char c) {
		KeyEvent e = new KeyEvent(frame, KeyEvent.RESERVED_ID_MAX + 1, System.currentTimeMillis(), KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_UNDEFINED, c);
		// Debug: KeyEvent e = new SpellWreckedKeyEvent(frame, KeyEvent.RESERVED_ID_MAX + 1, System.currentTimeMillis(), KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_UNDEFINED, c);
		System.out.print(e.getKeyChar());
	}

}
