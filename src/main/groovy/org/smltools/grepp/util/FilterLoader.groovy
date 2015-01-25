package org.smltools.grepp.util;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.CompilationUnit.PrimaryClassNodeOperation;
import org.codehaus.groovy.control.CompilationUnit.SourceUnitOperation;
import org.codehaus.groovy.control.CompilationUnit.ProgressCallback;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.Parameter;
import groovy.lang.GroovyClassLoader;
import org.smltools.grepp.filters.FilterParams;
import org.smltools.grepp.filters.Filter;
import org.smltools.grepp.exceptions.FilteringIsInterruptedException;
import java.security.CodeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FilterLoader extends GroovyClassLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterLoader.class);

    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource codeSource) {
    	println "${config.getTargetBytecode()}"

    	config.setDebug(true)
    	config.setVerbose(true)
    	config.setOutput(new PrintWriter(System.out))
    	config.setTolerance(50)
        CompilationUnit cu = super.createCompilationUnit(config, codeSource);
        cu.setProgressCallback({ context, phase ->
        	println "${Phases.getDescription(phase)}"
        	println "warn: ${context.getErrorCollector().getWarnings()}"
        	println "err: ${context.getErrorCollector().getErrors()}"
        } as ProgressCallback)
//        cu.addPhaseOperation(new PrimaryClassNodeOperation() {
//	            public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
		cu.addPhaseOperation(new SourceUnitOperation() {
			public void call(SourceUnit source) {
		            try {
		            	def moduleNode = source.getAST()
		            	moduleNode.addImport('org.smltools.grepp.filters.Filter', new ClassNode(Filter.class))
		            	moduleNode.addImport('org.smltools.grepp.filters.FilterParams', new ClassNode(FilterParams.class))

		            	println "moduleNode: ${moduleNode.getText()}"

		            	def statementBlock = moduleNode.getStatementBlock()

		            	println "Statements: ${statementBlock.getStatements()}"

		            	classNode.getMethods().clear()

		            	statementBlock.getStatements().each { statement ->
		            		if (statement instanceof ExpressionStatement) {
		            			def expr = statement.getExpression()
		            			if (expr instanceof BinaryExpression) {
				            		if (expr.getText() =~ /order/) {
								        AnnotationNode filterParamsAnnotation = new AnnotationNode(new ClassNode(FilterParams.class))
					            		filterParamsAnnotation.setMember('order', expr)
				            			classNode.addAnnotation(filterParamsAnnotation)
				            			println "Added annotaion"
				            		}
			            		}
			            		else if (expr instanceof MethodCallExpression) {
			            			if (expr.getMethodAsString() =~ /filter/) {
			            				expr.getArguments().each { arg ->
			            					if (arg instanceof ClosureExpression) {
					            				classNode.addMethod('filter', 1, new ClassNode(String.class),(Parameter[]) [new Parameter(new ClassNode(String.class), 'blockData')] ,(ClassNode[]) [], arg.getCode())
					            				println "Added filter method"
			            					}
			            				}
			            			}
			            		}
		            		}
		            	}

		            	statementBlock.getStatements().clear()

		            	println "Statements left: ${statementBlock.getStatements()}"

		            	println "$classNode.name in conversion"
		            	ClassNode filterInterface = new ClassNode(Filter.class)
		            	filterInterface.setGenericsTypes((GenericsType[]) [new GenericsType(new ClassNode(String.class))])
		            	classNode.addInterface(filterInterface)
		            	println "Imports: ${moduleNode.getImports()}"
		            	println "Annotations: ${classNode.getAnnotations()}"
		            	println "Interfaces: ${classNode.getInterfaces()}"
		            	println "Fields: ${classNode.getFields()}"
		            	println "Methods: ${classNode.getMethods()}"		            	
		            	println "Conversion finished"
	            	} 
	            	catch (Exception ex) {
	            		ex.printStackTrace()
	            	}
	            }
            }, Phases.PARSING);
        return cu;
    }
}
