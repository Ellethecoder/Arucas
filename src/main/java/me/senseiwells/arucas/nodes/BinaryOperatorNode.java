package me.senseiwells.arucas.nodes;

import me.senseiwells.arucas.throwables.RuntimeError;
import me.senseiwells.arucas.utils.Context;
import me.senseiwells.arucas.throwables.CodeError;
import me.senseiwells.arucas.throwables.ThrowValue;
import me.senseiwells.arucas.tokens.Token;
import me.senseiwells.arucas.values.BooleanValue;
import me.senseiwells.arucas.values.NumberValue;
import me.senseiwells.arucas.values.Value;

public class BinaryOperatorNode extends Node {
	public final Node leftNode;
	public final Node rightNode;

	public BinaryOperatorNode(Node leftNode, Token operatorToken, Node rightNode) {
		super(operatorToken, leftNode.startPos, rightNode.endPos);
		this.leftNode = leftNode;
		this.rightNode = rightNode;
	}

	@Override
	public Value<?> visit(Context context) throws CodeError, ThrowValue {
		Value<?> left = this.leftNode.visit(context);
		Value<?> right = null;
		try {
			Value<?> result = null;
			switch (this.token.type) {
				case AND -> {
					BooleanValue leftBoolean = (BooleanValue) left;
					result = (!leftBoolean.value) ? new BooleanValue(false) : leftBoolean.isAnd((BooleanValue) (right = this.rightNode.visit(context)));
				}
				case OR -> {
					BooleanValue leftBoolean = (BooleanValue) left;
					result = leftBoolean.value ? new BooleanValue(true) : leftBoolean.isOr((BooleanValue) (right = this.rightNode.visit(context)));
				}
				default -> {
					// AND, OR has a special property that the right hand side is not evaluated
					// unless the value we read is either true or false. This means that we need
					// to specify this value after we have checked for AND, OR.
					right = this.rightNode.visit(context);
				}
			}
			
			switch (this.token.type) {
				case PLUS -> result = left.addTo(right);
				case MINUS -> result = ((NumberValue) left).subtractBy((NumberValue) right);
				case MULTIPLY -> result = ((NumberValue) left).multiplyBy((NumberValue) right);
				case DIVIDE -> result = ((NumberValue) left).divideBy((NumberValue) right);
				case POWER -> result = ((NumberValue) left).powerBy((NumberValue) right);
				case EQUALS -> result = left.isEqual(right);
				case NOT_EQUALS -> result = left.isNotEqual(right);
				case LESS_THAN, LESS_THAN_EQUAL, MORE_THAN, MORE_THAN_EQUAL -> result = ((NumberValue) left).compareNumber((NumberValue) right, this.token.type);
			}
			
			if (result == null) {
				throw new CodeError(CodeError.ErrorType.ILLEGAL_SYNTAX_ERROR, "Expected an operator", this.startPos, this.endPos);
			}
			
			return result.setPos(this.startPos, this.endPos);
		}
		catch (ClassCastException classCastException) {
			String message;
			if (right == null) {
				message = "'%s'".formatted(left.value);
			}
			else {
				message = "'%s' and '%s'".formatted(left.value, right.value);
			}
			
			throw new RuntimeError("The operation '%s' cannot be applied to %s".formatted(this.token.type, message), this.startPos, this.endPos, context);
		}
		catch (RuntimeError e) {
			throw e.setContext(context);
		}
	}

	@Override
	public String toString() {
		return "(%s %s %s)".formatted(this.leftNode, this.token, this.rightNode);
	}
}
