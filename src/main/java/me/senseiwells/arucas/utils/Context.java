package me.senseiwells.arucas.utils;

import me.senseiwells.arucas.values.classes.AbstractClassDefinition;
import me.senseiwells.arucas.api.IArucasExtension;
import me.senseiwells.arucas.api.IArucasOutput;
import me.senseiwells.arucas.api.ISyntax;
import me.senseiwells.arucas.throwables.CodeError;
import me.senseiwells.arucas.values.Value;
import me.senseiwells.arucas.values.functions.AbstractBuiltInFunction;

import java.util.*;

/**
 * Runtime context class of the programming language
 */
public class Context {
	private final Set<String> builtInFunctions;
	private final List<IArucasExtension> extensions;
	private final IArucasOutput arucasOutput;
	
	private final String displayName;
	private final Context parentContext;
	private StackTable stackTable;
	private boolean isDebug;
	private boolean suppressDeprecated;

	private Context(String displayName, Context parentContext, List<IArucasExtension> extensions, Collection<AbstractClassDefinition> classDefinitions, IArucasOutput arucasOutput) {
		this.builtInFunctions = new HashSet<>();
		this.extensions = extensions;
		this.arucasOutput = arucasOutput;
		
		this.displayName = displayName;
		this.stackTable = new StackTable();
		this.parentContext = parentContext;
		
		for (IArucasExtension extension : extensions) {
			for (AbstractBuiltInFunction<?> function : extension.getDefinedFunctions()) {
				this.builtInFunctions.add(function.value);
			}
		}

		for (AbstractClassDefinition classDefinition : classDefinitions) {
			this.addClassDefinition(classDefinition);
		}
	}
	
	public Context(String displayName, List<IArucasExtension> extensions, Collection<AbstractClassDefinition> classDefinitions, IArucasOutput arucasOutput) {
		this(displayName, null, extensions, classDefinitions, arucasOutput);
	}
	
	private Context(Context branch, StackTable stackTable) {
		this.displayName = branch.displayName;
		this.stackTable = stackTable;
		this.arucasOutput = branch.arucasOutput;
		this.extensions = branch.extensions;
		this.builtInFunctions = branch.builtInFunctions;
		this.parentContext = branch.parentContext;
	}

	@SuppressWarnings("unused")
	public Context createBranch() {
		return new Context(this, this.stackTable);
	}

	@SuppressWarnings("unused")
	public Context createRootBranch() {
		return new Context(this, this.stackTable.getRoot());
	}

	@SuppressWarnings("unused")
	public Context createBranchFromPosition(StackTable stackTable) {
		if (this.stackTable.getRoot() == stackTable.getRoot()) {
			return new Context(this, stackTable);
		}
		return null;
	}
	
	public Context createChildContext(String displayName) {
		return new Context(displayName, this, this.extensions, this.stackTable.getRoot().classDefinitions.values(), this.arucasOutput);
	}
	
	/**
	 * Returns this contexts output object.
	 */
	public IArucasOutput getOutput() {
		return this.arucasOutput;
	}
	
	public String getDisplayName() {
		return this.displayName;
	}
	
	public StackTable getStackTable() {
		return this.stackTable;
	}
	
	public StackTable getBreakScope() {
		return this.stackTable.getBreakScope();
	}
	
	public StackTable getContinueScope() {
		return this.stackTable.getContinueScope();
	}
	
	public StackTable getReturnScope() {
		return this.stackTable.getReturnScope();
	}
	
	public void pushScope(ISyntax syntaxPosition) {
		this.stackTable = new StackTable(this.stackTable, syntaxPosition, false, false, false);
	}
	
	public void pushLoopScope(ISyntax syntaxPosition) {
		this.stackTable = new StackTable(this.stackTable, syntaxPosition, true, true, false);
	}
	
	public void pushSwitchScope(ISyntax syntaxPosition) {
		this.stackTable = new StackTable(this.stackTable, syntaxPosition, true, false, false);
	}
	
	public void pushFunctionScope(ISyntax syntaxPosition) {
		this.stackTable = new FunctionStackTable(this.stackTable, syntaxPosition);
	}
	
	public void popScope() {
		this.stackTable = this.stackTable.getParentTable();
	}
	
	public void moveScope(StackTable stackTable) {
		// We do not want to jump to an arbitrary stackTable
		
		Iterator<StackTable> iter = this.stackTable.iterator();
		while (iter.hasNext()) {
			StackTable table = iter.next();
			if (table == stackTable) {
				this.stackTable = table;
				return;
			}
		}
		
		// This should throw an exception
	}
	
	public void setDebug(boolean debug) {
		this.isDebug = debug;
	}
	
	public boolean isDebug() {
		return this.isDebug;
	}

	public void setSuppressDeprecated(boolean suppressed) {
		this.suppressDeprecated = suppressed;
	}

	public boolean isSuppressDeprecated() {
		return this.suppressDeprecated;
	}

	public boolean isBuiltInFunction(String name) {
		return this.builtInFunctions.contains(name);
	}

	public boolean isDefinedClass(String name) {
		return this.stackTable.hasClassDefinition(name);
	}

	public void throwIfStackNameTaken(String name, ISyntax syntaxPosition) throws CodeError {
		if (this.isBuiltInFunction(name)) {
			throw new CodeError(
				CodeError.ErrorType.ILLEGAL_OPERATION_ERROR,
				"%s() is already defined as a built in function".formatted(name),
				syntaxPosition
			);
		}
	}

	public void throwIfClassNameTaken(String name, ISyntax syntaxPosition) throws CodeError {
		if (this.isDefinedClass(name)) {
			throw new CodeError(
				CodeError.ErrorType.ILLEGAL_OPERATION_ERROR,
				"%s is already defined as a class".formatted(name),
				syntaxPosition
			);
		}
	}
	
	public void setVariable(String name, Value<?> value) {
		this.stackTable.set(name, value);
	}
	
	public void setLocal(String name, Value<?> value) {
		this.stackTable.setLocal(name, value);
	}
	
	public Value<?> getVariable(String name) {
		return this.stackTable.get(name);
	}

	public void printDeprecated(String message) {
		if (!this.suppressDeprecated) {
			this.getOutput().println(message);
		}
	}
	
	public AbstractClassDefinition getClassDefinition(String name) {
		return this.stackTable.getClassDefinition(name);
	}
	
	public void addClassDefinition(AbstractClassDefinition definition) {
		this.stackTable.addClassDefinition(definition);
	}

	public AbstractBuiltInFunction<?> getBuiltInFunction(String methodName, int parameters) {
		for (IArucasExtension extension : this.extensions) {
			for (AbstractBuiltInFunction<?> function : extension.getDefinedFunctions()) {
				if (function.getName().equals(methodName) && parameters == function.getParameterCount()) {
					return function;
				}
			}
		}

		return null;
	}
	
	@Deprecated(forRemoval = true)
	public void dumpScopes() {
		StringBuilder sb = new StringBuilder();
		sb.append("----------------------------\n");
		
		Iterator<StackTable> iter = this.stackTable.iterator();
		while (iter.hasNext()) {
			sb.append(iter.next()).append("\n");
		}
		
		System.out.println(sb);
	}
}
