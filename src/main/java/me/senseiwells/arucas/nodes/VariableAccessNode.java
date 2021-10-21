package me.senseiwells.arucas.nodes;

import me.senseiwells.arucas.utils.Context;
import me.senseiwells.arucas.throwables.ErrorRuntime;
import me.senseiwells.arucas.tokens.Token;
import me.senseiwells.arucas.values.Value;

public class VariableAccessNode extends Node {
	public VariableAccessNode(Token token) {
		super(token, token.startPos, token.endPos);
	}

	@Override
	public Value<?> visit(Context context) throws ErrorRuntime {
		Value<?> value = context.getVariable(this.token.content);
		if (value == null)
			throw new ErrorRuntime("%s is not defined".formatted(this.token.content), this.startPos, this.endPos, context);
		value = value.copy();
		value.setPos(this.startPos, this.endPos);
		return value;
	}
}