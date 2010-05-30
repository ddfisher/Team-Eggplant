package util.statemachine.implementation.propnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.CtClass;

import player.gamer.statemachine.eggplant.misc.Log;
import util.propnet.architecture.Component;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;

public class NativeOperatorFactory {
	private static final String GEN_DIR = "gen";
	private static String fileName = "NativeOperator";
	private static final String libPath = GEN_DIR + File.separator + "lib" + fileName + ".so";
	private static final String path = GEN_DIR + File.separator + fileName + ".c";
	private static final String HEADER_NAME = "util_statemachine_implementation_propnet_NativeOperator.h";

	private static final String PREFIX = "Java_util_statemachine_implementation_propnet_NativeOperator_";
	private static final String PROPAGATE = "propagate";
	private static final String TRANSITION = "transition";
	private static final String INTERNAL = "propagateInternal";
	private static final String TERMINAL = "propagateTerminalOnly";
	private static final String LEGAL = "propagateLegalOnly";
	private static final String GOAL = "propagateGoalOnly";
	private static final String MONTE_CARLO = "monteCarlo";
	private static final String MULTI_MONTE = "multiMonte";

	private static int constantProps = 0;
	private static int internalProps = 0;

	public static Operator buildOperator(Map<Proposition, Integer> propMap, List<Proposition> transitionOrdering, List<Proposition> internalOrdering,
			List<Proposition> terminalOrdering, List<List<List<Proposition>>> legalOrderings, List<List<Proposition>> goalOrderings,
			int[][] legalPropMap, int[] legalInputMap, int inputPropStart, int inputPropLength, int terminalIndex, int[][] goals) {
		StringBuilder source = new StringBuilder();
		
		addPrefix(source);

		addTransition(source, transitionOrdering, propMap);
		addInternalPropagate(source, internalOrdering, propMap);
		addPropagate(source);
		addTerminalPropagate(source, terminalOrdering, propMap);
		addLegalPropagate(source, legalOrderings, propMap);
		addGoalPropagate(source, goalOrderings, propMap);
		
		addMonteCarlo(source, legalPropMap.length, legalPropMap[0].length, inputPropStart, inputPropLength, terminalIndex);
		addMultiMonte(source, propMap.size(), goals.length);

		try {
			FileWriter writer = new FileWriter(path);
			writer.write(source.toString());
			writer.close();
			
			Runtime rt = Runtime.getRuntime();
			Process p = rt.exec("gcc -shared -O2 -fPIC -std=c99 -I/usr/lib/jvm/java-6-sun/include -I/usr/lib/jvm/java-6-sun/include/linux " +
					fileName + ".c -o lib" + fileName + ".so", null, new File(GEN_DIR));
			if (p.waitFor() == 0) {
				Log.println('m', "Compilation successful!");
			} else {
				BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				Log.println('m', "Compilation error!");
				String line;
				while ((line = stderr.readLine()) != null) {
					Log.println('m', line);
				}
				return null;
			}
			
			NativeOperator no = new NativeOperator(System.getProperty("user.dir") + File.separator + libPath);
			
			
			int[] goalProps = new int[goals.length];
			int[] goalValues = new int[goals.length];
			for (int i = 0; i < goals.length; i++) {
				goalProps[i] = goals[i][0];
				goalValues[i] = goals[i][1];
			}
			
			no.initMonteCarlo(legalPropMap, legalInputMap, goalProps, goalValues);
			Log.println('m', "Monte Carlo Initialized!");
			return no;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			Log.println('m', "Monte Carlo not initialized!");
		}

		return null;
	}
	
	private static void addPrefix(StringBuilder source) {
		//add includes
		source.append("#include <stdbool.h>\n");
		source.append("#include <stdlib.h>\n");
		source.append("#include <string.h>\n");
		source.append("#include \"" + HEADER_NAME + "\"\n");
		
		//add globals
		source.append("jint **legalPropMap;\n");
		source.append("jint *legalInputMap;\n");
		source.append("jint *numInputs;\n");
		source.append("jint *goalProps;\n");
		source.append("jint *goalValues;\n");
	}

	private static void addTransition(StringBuilder source, List<Proposition> transitionOrdering, Map<Proposition, Integer> propMap) {
		StringBuilder body = generateTransitionMethodBody(transitionOrdering, propMap);
		addMethod(source, body, TRANSITION);
		addWrapper(source, TRANSITION, false, false);
	}

	private static void addInternalPropagate(StringBuilder source, List<Proposition> internalOrdering, Map<Proposition, Integer> propMap) {
		StringBuilder body = generateInternalMethodBody(internalOrdering, propMap);
		addMethod(source, body, INTERNAL);
		addWrapper(source, INTERNAL, false, false);
	}

	private static void addPropagate(StringBuilder source) {
		StringBuilder body = new StringBuilder();
		body.append(INTERNAL + "(props);\n");
		body.append(TRANSITION + "(props);\n");
		addMethod(source, body, PROPAGATE);
		addWrapper(source, PROPAGATE, false, false);
	}

	private static void addTerminalPropagate(StringBuilder source, List<Proposition> terminalOrdering, Map<Proposition, Integer> propMap) {
		StringBuilder body = generateInternalMethodBody(terminalOrdering, propMap);
		addMethod(source, body, TERMINAL);
		addWrapper(source, TERMINAL, false, false);
	}

	private static void addLegalPropagate(StringBuilder source, List<List<List<Proposition>>> legalOrderings, Map<Proposition, Integer> propMap) {
		addRoleAuxDependentHelpers(legalOrderings, propMap, source, LEGAL);
		
		List<Integer> auxSizes = new ArrayList<Integer>();
		for (int i = 0; i < legalOrderings.size(); i++) {
			auxSizes.add(legalOrderings.get(i).size());
		}
		StringBuilder body = generateRoleAuxDependentBody(LEGAL, auxSizes);
		addRoleAuxDependentMethod(source, body, LEGAL);
		
		addWrapper(source, LEGAL, true, true);
	}

	private static void addGoalPropagate(StringBuilder source, List<List<Proposition>> goalOrderings, Map<Proposition, Integer> propMap) {
		addRoleDependentHelpers(goalOrderings, propMap, source, GOAL);
		StringBuilder body = generateRoleDependentBody(GOAL, goalOrderings.size());
		addRoleDependentMethod(source, body, GOAL);
		addWrapper(source, GOAL, true, false);
	}
	
	private static void addWrapper(StringBuilder source, String methodName, boolean indexNeeded, boolean auxNeeded) {
		StringBuilder method = new StringBuilder();
		method.append("JNIEXPORT void JNICALL " + PREFIX + methodName + "(JNIEnv *env, jobject obj, jbooleanArray javaArray" +
				(indexNeeded ? ", jint roleIndex" : "") + (auxNeeded ? ", jint auxIndex" : "") +
				") {\n");
//		method.append("jboolean copy = 0;\n");
//		method.append("jboolean *props = (*env)->GetPrimitiveArrayCritical(env, javaArray, &copy);\n");
		method.append("jboolean *props = (*env)->GetBooleanArrayElements(env, javaArray, NULL);\n");
//		method.append("printf(\"copy: %d\\n\", copy);");
		method.append(methodName + "(props" + (indexNeeded ? ", roleIndex" : "") + (auxNeeded ? ", auxIndex" : "") + ");\n");
		method.append("(*env)->ReleaseBooleanArrayElements(env, javaArray, props, 0);\n");
//		method.append("(*env)->ReleasePrimitiveArrayCritical(env, javaArray, props, 0);\n");
		method.append("}\n");

		source.append(method);
	}

	private static void addMethod(StringBuilder source, StringBuilder body, String methodName) {
		StringBuilder method = new StringBuilder();
		method.append("void " + methodName + "(jboolean *props) {\n");
		method.append(body);
		method.append("}\n");

		source.append(method);
	}

	private static void addRoleDependentMethod(StringBuilder source, StringBuilder body, String methodName) {
		StringBuilder method = new StringBuilder();
		method.append("void " + methodName + "(jboolean *props, jint roleIndex) {\n");
		method.append(body);
		method.append("}\n");
		// System.out.println(method);
		source.append(method);
	}
	
	private static void addRoleAuxDependentMethod(StringBuilder source, StringBuilder body, String methodName) {
		StringBuilder method = new StringBuilder();
		method.append("void " + methodName + "(jboolean *props, jint roleIndex, jint auxIndex) {\n");
		method.append(body);
		method.append("}\n");
		// System.out.println(method);
		source.append(method);
	}

	private static StringBuilder generateInternalMethodBody(List<Proposition> ordering, Map<Proposition, Integer> propMap) {
		StringBuilder body = new StringBuilder();
		for (Proposition p : ordering) {
			addInternalComponent(p, body, propMap);
		}
		return body;
	}

	private static StringBuilder generateTransitionMethodBody(List<Proposition> ordering, Map<Proposition, Integer> propMap) {
		StringBuilder body = new StringBuilder();
		for (Proposition p : ordering) {
			addTransitionComponent(p, body, propMap);
		}
		return body;
	}

	private static StringBuilder generateRoleDependentBody(String name, int roles) {
		StringBuilder body = new StringBuilder();
		body.append("switch (roleIndex) {\n");
		for (int roleIndex = 0; roleIndex < roles; roleIndex++) {
			body.append("case " + roleIndex + ":\n");
			body.append(name + "Role" + roleIndex + "(props);\n");
			body.append("break;\n");
		}
		body.append("}\n");
		return body;
	}
	
	private static StringBuilder generateRoleAuxDependentBody(String name, List<Integer> auxSizes) {
		StringBuilder body = new StringBuilder();
		body.append("switch (roleIndex) {\n");
		for (int roleIndex = 0; roleIndex < auxSizes.size(); roleIndex++) {
			body.append("case " + roleIndex + ":\n");
				body.append("switch (auxIndex) {\n");
				for (int auxData = 0; auxData < auxSizes.get(roleIndex); auxData++) {
					body.append("case " + auxData + ":\n");
					body.append(name + "Role" + roleIndex + "Aux" + auxData + "(props);\n");
					body.append("break;\n");
				}
				body.append("}\n");
			body.append("break;\n");
		}
		body.append("}\n");
		return body;
	}

	private static void addRoleDependentHelpers(List<List<Proposition>> orderings, Map<Proposition, Integer> propMap, StringBuilder source,
			String name) {
		for (int roleIndex = 0; roleIndex < orderings.size(); roleIndex++) {
			StringBuilder body = generateInternalMethodBody(orderings.get(roleIndex), propMap);
			addMethod(source, body, name + "Role" + roleIndex);
		}
	}
	
	private static void addRoleAuxDependentHelpers(List<List<List<Proposition>>> orderings, Map<Proposition, Integer> propMap, 
			StringBuilder source, String name) {
		for (int roleIndex = 0; roleIndex < orderings.size(); roleIndex++) {
			for (int auxData = 0; auxData < orderings.get(roleIndex).size(); auxData++) {
				StringBuilder body = generateInternalMethodBody(orderings.get(roleIndex).get(auxData), propMap);
				addMethod(source, body, name + "Role" + roleIndex + "Aux" + auxData);
			}
		}
	}

	private static void addInternalComponent(Proposition proposition, StringBuilder body, Map<Proposition, Integer> propMap) {
		Component comp = proposition.getSingleInput();
		int propositionIndex = propMap.get(proposition);
		internalProps++;
		if (comp instanceof Constant) {
			constantProps++;
			body.append("props[" + propositionIndex + "] = " + comp.getValue() + ";\n");
		} else if (comp instanceof Not) {
			if (!propMap.containsKey(comp.getSingleInput())) {
				body.append("props[" + propositionIndex + "] = !" + comp.getSingleInput().getValue() + ";\n");
			} else {
				body.append("props[" + propositionIndex + "] = !props[" + propMap.get(comp.getSingleInput()) + "];\n");
			}
		} else if (comp instanceof And) {
			Set<Component> connected = comp.getInputs();
			StringBuilder and = new StringBuilder();
			and.append("props[" + propositionIndex + "] = true");

			for (Component prop : connected) {
				if (!propMap.containsKey(prop)) {
					// if the proposition is not in the proposition map, it is
					// never changed: it is effectively a constant
					if (prop.getValue()) {
						continue;
					} else {
						and = new StringBuilder("props[" + propositionIndex + "] = false");
						break;
					}
				} else {
					and.append(" && props[" + propMap.get(prop) + "]");
				}
			}

			and.append(";\n");

			body.append(and);

		} else if (comp instanceof Or) {
			Set<Component> connected = comp.getInputs();
			StringBuilder or = new StringBuilder();
			or.append("props[" + propositionIndex + "] = false");

			for (Component prop : connected) {
				if (!propMap.containsKey(prop)) {
					// if the proposition is not in the proposition map, it is
					// never changed: it is effectively a constant
					if (prop.getValue()) {
						or = new StringBuilder("props[" + propositionIndex + "] = true");
						break;
					} else {
						continue;
					}
				} else {
					or.append(" || props[" + propMap.get(prop) + "]");
				}
			}

			or.append(";\n");

			body.append(or);

		} else {
			throw new RuntimeException("Unexpected Class");
		}
	}

	private static void addTransitionComponent(Proposition proposition, StringBuilder body, Map<Proposition, Integer> propMap) {
		int propositionIndex = propMap.get(proposition);
		Component comp = proposition.getSingleInput();
		if (comp instanceof Constant) {
			body.append("props[" + propositionIndex + "] = " + comp.getValue() + ";\n");
		} else if (comp instanceof Transition) {
			if (!propMap.containsKey(comp.getSingleInput())) {
				body.append("props[" + propositionIndex + "] = " + comp.getSingleInput().getValue() + ";\n");
			} else {
				body.append("props[" + propositionIndex + "] = props[" + propMap.get(comp.getSingleInput()) + "];\n");
			}
		} else {
			throw new RuntimeException("Unexpected Class");
		}
	}
	
	private static void addMultiMonte(StringBuilder source, int numProps, int numGoals) {
		StringBuilder method = new StringBuilder();
		method.append("jlong multiMonte(jboolean *props, jint probes) {\n");
		method.append("jlong sum = 0;\n");
		method.append("for (int i = 0; i < probes; i++) {\n");
		method.append("jboolean tempProps[" + numProps + "];\n");
		method.append("memcpy(tempProps, props, sizeof(jboolean) * " + numProps + ");\n");
		method.append(""+ MONTE_CARLO + "(tempProps);\n");
		method.append("for (int g = 0; g < " + numGoals + "; g++) {\n");
		
//		method.append("printf(\"%d\\n\", tempProps[goalProps[g]]);\n");
		
		method.append("if (tempProps[goalProps[g]]) {\n");
		method.append("sum+=goalValues[g];\n");
		method.append("break;\n");
		method.append("}\n");
		method.append("}\n");
//		method.append("printf(\"-------------------------%ld\\n\", sum);\n");
		method.append("}\n");
		method.append("return sum;\n");
		method.append("}\n");

		source.append(method);
		
		StringBuilder wrapperMethod = new StringBuilder();
		wrapperMethod.append("JNIEXPORT jlong JNICALL " + PREFIX + MULTI_MONTE + "(JNIEnv *env, jobject obj, jbooleanArray javaArray, jint probes) {\n");
		wrapperMethod.append("jboolean *props = (*env)->GetBooleanArrayElements(env, javaArray, NULL);\n");
		wrapperMethod.append("jlong result = " + MULTI_MONTE + "(props, probes);\n");
		wrapperMethod.append("(*env)->ReleaseBooleanArrayElements(env, javaArray, props, JNI_ABORT);\n");
		wrapperMethod.append("return result;\n");
		wrapperMethod.append("}\n");

		source.append(wrapperMethod);
	}
	
	private static void addMonteCarlo(StringBuilder source, int numRoles, int numLegals, int inputStart, int numInputs, int terminalIndex) {
		StringBuilder body = new StringBuilder();
		body.append("int input[" + numRoles + "];\n");
		body.append("jint depth = 0;\n");
		body.append("while (true) {\n");

		body.append("memset(props+" + inputStart + ", false, sizeof(jboolean)*" + numInputs + ");\n");
		body.append("for (int role = 0; role < " + numRoles + "; role++) {\n");
		body.append("int order[" + numLegals + "];\n");
		body.append("for (int index = 0; index < " + numLegals + "; index++) {\n");
		body.append("order[index] = index;\n");
		body.append("}\n");
		body.append("for (int index = " + numLegals + "; index > 0; index--) {\n");
		body.append("int swapIndex = rand() % index;\n");
		body.append("int temp = order[swapIndex];\n");
		body.append("order[swapIndex] = order[index-1];\n");
		body.append("order[index-1] = temp;\n");
		body.append("}\n");
		body.append("for (int i = 0; i < " + numLegals + "; i++) {\n");
		body.append("int index = order[i];\n");
		body.append("int inputIndex = legalInputMap[ legalPropMap[role][index] ];\n");
		body.append("props[inputIndex] = true;\n");
		body.append("propagateLegalOnly(props, role, index);\n");
		body.append("if (props[ legalInputMap[ inputIndex ] ])\n");
		body.append("break;\n");
		body.append("props[inputIndex] = false;\n");
		body.append("}\n");
		body.append("}\n");
		body.append("propagateInternal(props);\n");
		body.append("if (props[" + terminalIndex + "]) return depth;\n");
		body.append("transition(props);\n");
		body.append("depth++;\n");
		
		
		body.append("}\n");

		StringBuilder method = new StringBuilder();
		method.append("jint " + MONTE_CARLO + "(jboolean *props) {\n");
		method.append(body);
		method.append("}\n");
		source.append(method);
		
		StringBuilder wrapperMethod = new StringBuilder();
		wrapperMethod.append("JNIEXPORT jint JNICALL " + PREFIX + MONTE_CARLO + "(JNIEnv *env, jobject obj, jbooleanArray javaArray) {\n");
		wrapperMethod.append("jboolean *props = (*env)->GetBooleanArrayElements(env, javaArray, NULL);\n");
		wrapperMethod.append("jint result = " + MONTE_CARLO + "(props);\n");
		wrapperMethod.append("(*env)->ReleaseBooleanArrayElements(env, javaArray, props, 0);\n");
		wrapperMethod.append("return result;\n");
		wrapperMethod.append("}\n");

		source.append(wrapperMethod);
		
		addMonteCarloInit(source);
	}
	
	private static void addMonteCarloInit(StringBuilder source) {
		StringBuilder body = new StringBuilder();
		body.append("JNIEXPORT void JNICALL " + PREFIX + "initMonteCarlo(JNIEnv *env, jobject obj, jobjectArray javaLegalPropMap," +
				"jbooleanArray javaLegalInputMap, jintArray javaGoalProps, jintArray javaGoalValues) {\n");
//		body.append("printf(\"Monte Carlo Init!\\n\");\n");
		body.append("jint *tempLegalInputMap = (*env)->GetIntArrayElements(env, javaLegalInputMap, NULL);\n");
		body.append("int inputLen = (*env)->GetArrayLength(env, javaLegalInputMap);\n");
		body.append("legalInputMap = malloc(sizeof(jint) * inputLen);\n");
		body.append("for(int i = 0; i < inputLen; i++) legalInputMap[i] = tempLegalInputMap[i];\n");
		body.append("(*env)->ReleaseIntArrayElements(env, javaLegalInputMap, tempLegalInputMap, JNI_ABORT);\n");
		body.append("int rowLen = (*env)->GetArrayLength(env, javaLegalPropMap);\n");
		body.append("legalPropMap = malloc(sizeof(jint *) * rowLen);\n");
		body.append("numInputs = malloc(sizeof(jint) * rowLen);\n");
		body.append("for(int i=0; i<rowLen; i++) {\n");
			body.append("jintArray oneDim = (jintArray)(*env)->GetObjectArrayElement(env, javaLegalPropMap, i);\n");
			body.append("int colLen = (*env)->GetArrayLength(env, oneDim);\n");
			body.append("numInputs[i] = colLen;\n");
			body.append("legalPropMap[i] = malloc(sizeof(jint) * colLen);\n");
			body.append("jint *element=(*env)->GetIntArrayElements(env, oneDim, NULL);\n");
			body.append("for(int j=0; j<colLen; j++) {\n");
				body.append("legalPropMap[i][j]= element[j];\n");
//				body.append("printf(\"Role: %d\\tProp: %d\\tValue: %d\\n\", i, j, legalPropMap[i][j]);\n");
			body.append("}\n");
			body.append("(*env)->ReleaseIntArrayElements(env, oneDim, element, JNI_ABORT);\n");
//			body.append("(*env)->DeleteLocalRef(env, oneDim);\n");
		body.append("}\n");
		
		body.append("int numGoals = (*env)->GetArrayLength(env, javaGoalProps);\n");
		
		body.append("jint *tempGoalProps = (*env)->GetIntArrayElements(env, javaGoalProps, NULL);\n");
		body.append("goalProps = malloc(sizeof(jint) * numGoals);\n");
		body.append("memcpy(goalProps, tempGoalProps, numGoals * sizeof(jint));\n");
		body.append("(*env)->ReleaseIntArrayElements(env, javaGoalProps, tempGoalProps, JNI_ABORT);\n");
		
//		body.append("for (int i = 0; i < numGoals; i++) printf(\"Goal Prop: %d\\n\", goalProps[i]);\n"); //TODO: remove
		
		body.append("jint *tempGoalValues = (*env)->GetIntArrayElements(env, javaGoalValues, NULL);\n");
		body.append("goalValues = malloc(sizeof(jint) * numGoals);\n");
		body.append("memcpy(goalValues, tempGoalValues, numGoals * sizeof(jint));\n");
		body.append("(*env)->ReleaseIntArrayElements(env, javaGoalValues, tempGoalValues, JNI_ABORT);\n");
		
//		body.append("for (int i = 0; i < numGoals; i++) printf(\"Goal Value: %d\\n\", goalValues[i]);\n"); //TODO: remove
		
		body.append("}\n");
		
		source.append(body);
	}
}
