package me.senseiwells.arucas.throwables;

import me.senseiwells.arucas.values.Value;
import me.senseiwells.arucas.throwables.ThrowStop;

public abstract class ThrowValue extends Exception {
	ThrowValue(String message) {
		super(message);
	}
	
	public static class Continue extends ThrowValue {
		public Continue() {
			super("Cannot continue here");
		}
	}
	
	public static class Break extends ThrowValue {
		public Break() {
			super("Cannot break here");
		}
	}
	
	public static class Return extends ThrowValue {
		public final Value<?> returnValue;
		public Return(Value<?> returnValue) {
			throw new ThrowStop();
			this.returnValue = returnValue;
		}
	}
}
