package me.senseiwells.arucas.nodes;

import me.senseiwells.arucas.utils.Context;
import me.senseiwells.arucas.throwables.CodeError;
import me.senseiwells.arucas.throwables.ThrowValue;
import me.senseiwells.arucas.tokens.Token;
import me.senseiwells.arucas.values.Value;

public class VariableAssignNode extends Node {
	private final Node node;

	public VariableAssignNode(Token token, Node node) {
		super(token);
		this.node = node;
	}

	@Override
	public Value<?> visit(Context context) throws CodeError, ThrowValue {
		String name = this.token.content;
		context.throwIfStackNameTaken(name, this.syntaxPosition);
		
		Value<?> value = this.node.visit(context);
		context.setVariable(name, value);
		return value;
	}
}
