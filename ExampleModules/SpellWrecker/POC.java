import java.awt.Frame;
import java.awt.event.KeyEvent;


public class POC {

	public static void main(String[] args) {
		
		Frame frame = new Frame();
		
		char[][] keyboard = {{'`', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', ' '},
							 {' ', 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\\'},
							 {' ', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', ' ', ' '},
							 {' ', 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/', ' ', ' ', ' '}};

		char[][] shiftKeyboard = {{'~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', ' '},
								  {' ', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', '|'},
								  {' ', 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '"', ' ', ' '},
								  {' ', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?', ' ', ' ', ' '}};
		
		int numSpellwrecks = 0;
		
		for(int i=0; i<keyboard.length; i++){
			for(int j=0; j<keyboard[i].length; j++){
				if(keyboard[i][j] == ' ') continue;
				char input = keyboard[i][j];
				KeyEvent e = new KeyEvent(frame, KeyEvent.RESERVED_ID_MAX + 1, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, keyboard[i][j]);
				char output = e.getKeyChar();
				if(input != output){
					numSpellwrecks++;
					System.out.println("Detected Spellwreck (" + numSpellwrecks + "): Input: " + input + ", Output: " + output + "\n");
				}
			}
		}
		
		for(int i=0; i<shiftKeyboard.length; i++){
			for(int j=0; j<shiftKeyboard[i].length; j++){
				if(shiftKeyboard[i][j] == ' ') continue;
				char input = shiftKeyboard[i][j];
				KeyEvent e = new KeyEvent(frame, KeyEvent.RESERVED_ID_MAX + 1, System.currentTimeMillis(), KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_UNDEFINED, shiftKeyboard[i][j]);
				char output = e.getKeyChar();
				if(input != output){
					numSpellwrecks++;
					System.out.println("Detected Spellwreck (" + numSpellwrecks + "): Input: " + input + ", Output: " + output + "\n");
				}
				
			}
		}
		
		System.out.println("Detected " + numSpellwrecks + " spellwrecks!");
	}
	
}
