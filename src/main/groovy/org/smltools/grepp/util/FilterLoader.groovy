package org.smltools.grepp.util

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.CompilationUnit.SourceUnitOperation;
import org.codehaus.groovy.control.CompilationUnit.ProgressCallback;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.control.messages.WarningMessage;
import org.smltools.grepp.filters.FilterParams;
import org.smltools.grepp.filters.Filter;


import java.security.CodeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterLoader extends GroovyClassLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterLoader.class);

	@Override
	Class parseClass(File file) throws CompilationFailedException, IOException {
		if (!(file.name ==~ /\w[\w0-9]*DslFilter.groovy/)) { //skipping non-dsl files
			LOGGER.warn "$file.name plugin was not identified as a dsl filter"
			return null
		}
		return super.parseClass(file)
	}

	protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource codeSource) {
		LOGGER.trace "Target bytecode ${config.getTargetBytecode()}"

    	config.setDebug(true)
    	config.setVerbose(true)
		config.setWarningLevel(WarningMessage.PARANOIA)
    	config.setOutput(new PrintWriter(System.err))

        CompilationUnit cu = super.createCompilationUnit(config, codeSource);
        cu.setProgressCallback({ context, phase ->
			LOGGER.trace "${Phases.getDescription(phase)}"
			LOGGER.trace "warn: ${context.getErrorCollector().getWarnings()}"
			LOGGER.trace "err: ${context.getErrorCollector().getErrors()}"
        } as ProgressCallback)
		cu.addPhaseOperation(new SourceUnitOperation() {
			public void call(SourceUnit source) {
		            try {
						def moduleNode = source.getAST()
						moduleNode.addImport('org.smltools.grepp.filters.Filter', ClassHelper.make(Filter.class, boolean as Boolean))
						moduleNode.addImport('org.smltools.grepp.filters.FilterParams', new ClassNode(FilterParams.class))

						LOGGER.trace "moduleNode: ${moduleNode.getText()}"

						def statementBlock = moduleNode.getStatementBlock()

						LOGGER.trace "Statements: ${statementBlock.getStatements()}"

						moduleNode.getClasses().clear()

						def classNode = new ClassNode(moduleNode.getMainClassName() + "_FilterImpl", ClassNode.ACC_PUBLIC, ClassNode.SUPER)

						AnnotationNode filterParamsAnnotation = new AnnotationNode(new ClassNode(FilterParams.class))
						statementBlock.getStatements().each { statement ->
							if (statement instanceof ExpressionStatement) {
								def expr = statement.getExpression()
								if (expr instanceof BinaryExpression) {
									def members = [:]
									def var = expr.leftExpression.getText()
									switch(var) {
										case ~/order/: case ~/configIdPath/:
											members[var] = expr.rightExpression
											break
									}

									if (!members.isEmpty()) {
										members.each { memberName, memberExpr ->
											filterParamsAnnotation.setMember(memberName, memberExpr)
										}
									}

								}
								else if (expr instanceof MethodCallExpression) {
									if (expr.getMethodAsString() =~ /filter/) {
										expr.getArguments().each { arg ->
											if (arg instanceof ClosureExpression) {
												classNode.addMethod('filter', 1, new ClassNode(String.class),(Parameter[]) [new Parameter(new ClassNode(String.class), 'blockData')] ,(ClassNode[]) [], arg.getCode())
												LOGGER.trace "Added filter method"
											}
										}
									}
								}
							}
						}

						if (!filterParamsAnnotation.getMembers().isEmpty()) {
							if (filterParamsAnnotation.getMember('configIdPath') == null) {
								filterParamsAnnotation.setMember('configIdPath', new ConstantExpression(moduleNode.getMainClassName().toLowerCase().replace('dslfilter', '')))
							}
							filterParamsAnnotation.setMember('isStatic', new ConstantExpression(true))
							classNode.addAnnotation(filterParamsAnnotation)
							LOGGER.trace "Added annotaion"
						}


						statementBlock.getStatements().clear()

						LOGGER.trace "Statements left: ${statementBlock.getStatements()}"

						LOGGER.trace "$classNode.name in conversion"

						ClassNode filterInterface = new ClassNode(Filter.class)
						filterInterface.setGenericsTypes([new GenericsType(new ClassNode(String.class))] as GenericsType[])
						classNode.addInterface(filterInterface)

						moduleNode.addClass(classNode)
						LOGGER.trace "Imports: ${moduleNode.getImports()}"
						LOGGER.trace "Annotations: ${classNode.getAnnotations()}"
						LOGGER.trace "Interfaces: ${classNode.getInterfaces()}"
						LOGGER.trace "Fields: ${classNode.getFields()}"
						LOGGER.trace "Methods: ${classNode.getMethods()}"
						LOGGER.trace "Conversion finished"
	            	}
	            	catch (Exception ex) {
						LOGGER.trace "An exception during AST", ex
	            	}
	            }
            }, Phases.CONVERSION);
        return cu;
    }
}
