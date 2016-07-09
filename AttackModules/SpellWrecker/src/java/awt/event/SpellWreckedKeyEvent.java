package java.awt.event;

import java.awt.Frame;

import jreframeworker.annotations.fields.DefineField;
import jreframeworker.annotations.methods.MergeMethod;
import jreframeworker.annotations.types.MergeType;

@MergeType
public class SpellWreckedKeyEvent extends KeyEvent {

	private static final long serialVersionUID = 1L;

	public SpellWreckedKeyEvent(Frame frame, int i, long currentTimeMillis, int j, int vkUndefined, char c) {
		super(frame, i, currentTimeMillis, j, vkUndefined, c);
	}

	@DefineField
	boolean spellwrecked;
	
	@MergeMethod
	@Override
	public char getKeyChar(){
		if(spellwrecked){
			return super.getKeyChar();
		} else {
			spellwrecked = true;
			keyChar = SpellWrecker.spellwreck(keyChar);
			return keyChar;
		}
	}

}
