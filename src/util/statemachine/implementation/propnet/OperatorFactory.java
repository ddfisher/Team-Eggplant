package util.statemachine.implementation.propnet;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import player.gamer.statemachine.eggplant.misc.Log;
import util.propnet.architecture.Component;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;

public class OperatorFactory {
	private static final int MAX_LENGTH = 60000;
	private static final String TRANSITION = "transition";
	private static final String INTERNAL = "propagateInternal";
	private static final String TERMINAL = "propagateTerminalOnly";
	private static final String LEGAL = "propagateLegalOnly";
	private static final String GOAL = "propagateGoalOnly";
	
	private static int classCount = 0;
	private static int constantProps = 0;
	private static int internalProps = 0;

	public static Operator buildOperator(Map<Proposition, Integer> propMap, List<Proposition> transitionOrdering, List<Proposition> internalOrdering,
			List<Proposition> terminalOrdering, List<List<Proposition>> legalOrderings, List<List<Proposition>> goalOrderings) {
		try {
			CtClass operatorSuperclass = ClassPool.getDefault().get("util.statemachine.implementation.propnet.Operator");
			CtClass operatorClass = ClassPool.getDefault().makeClass("util.statemachine.implementation.propnet.OperatorClass" + (classCount++));
			operatorClass.setSuperclass(operatorSuperclass);

			addTransition(operatorClass, transitionOrdering, propMap);
			addInternalPropagate(operatorClass, internalOrdering, propMap);
			Log.println('c', "Constant Propositions: " + constantProps + "\tInternal Propositions: " + internalProps);
			addTerminalPropagate(operatorClass, terminalOrdering, propMap);
			addLegalPropagate(operatorClass, legalOrderings, propMap);
			addGoalPropagate(operatorClass, goalOrderings, propMap);

			
			Operator operator = (Operator) operatorClass.toClass().newInstance();
			return operator;
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		} catch (InstantiationException ex) {
			ex.printStackTrace();
		} catch (CannotCompileException ex) {
			ex.printStackTrace();
		} catch (NotFoundException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	private static void addTransition(CtClass operatorClass, List<Proposition> transitionOrdering, Map<Proposition, Integer> propMap)
			throws CannotCompileException {
		StringBuilder[] parts = generateTransitionMethodBody(transitionOrdering, propMap);
		addMethod(operatorClass, parts, TRANSITION);
	}

	private static void addInternalPropagate(CtClass operatorClass, List<Proposition> internalOrdering, Map<Proposition, Integer> propMap)
			throws CannotCompileException {
		StringBuilder[] parts = generateInternalMethodBody(internalOrdering, propMap);
		addMethod(operatorClass, parts, INTERNAL);
	}

	private static void addTerminalPropagate(CtClass operatorClass, List<Proposition> terminalOrdering, Map<Proposition, Integer> propMap)
			throws CannotCompileException {
		StringBuilder[] parts = generateInternalMethodBody(terminalOrdering, propMap);
		addMethod(operatorClass, parts, TERMINAL);
	}

	private static void addLegalPropagate(CtClass operatorClass, List<List<Proposition>> legalOrderings, Map<Proposition, Integer> propMap)
			throws CannotCompileException {
		addRoleDependentHelpers(legalOrderings, propMap, operatorClass, LEGAL);
		StringBuilder body = generateRoleDependentBody(LEGAL, legalOrderings.size());
		addRoleDependentMethod(operatorClass, body, LEGAL);
	}

	private static void addGoalPropagate(CtClass operatorClass, List<List<Proposition>> goalOrderings, Map<Proposition, Integer> propMap)
			throws CannotCompileException {
		addRoleDependentHelpers(goalOrderings, propMap, operatorClass, GOAL);
		StringBuilder body = generateRoleDependentBody(GOAL, goalOrderings.size());
		addRoleDependentMethod(operatorClass, body, GOAL);
	}

	private static void addMethod(CtClass operatorClass, StringBuilder[] parts, String methodName) throws CannotCompileException {
		StringBuilder method = new StringBuilder();
		method.append("public void " + methodName + "(boolean[] props) {\n");
		if (parts.length == 1) {
			method.append(parts[0]);
		} else {
			for (int i = 0; i < parts.length; i++) {
				StringBuilder partMethod = new StringBuilder("private void " + methodName + "_" + i + "(boolean[] props) {\n");
				partMethod.append(parts[i]);
				partMethod.append("}\n");

				operatorClass.addMethod(CtNewMethod.make(partMethod.toString(), operatorClass));
				method.append(methodName + "_" + i + "(props);\n");
			}
		}

		method.append("}\n");
//		System.out.println(method);
		operatorClass.addMethod(CtNewMethod.make(method.toString(), operatorClass));
	}
	
	private static void addRoleDependentMethod(CtClass operatorClass, StringBuilder body, String methodName) throws CannotCompileException {
		StringBuilder method = new StringBuilder();
		method.append("public void " + methodName + "(boolean[] props, int roleIndex) {\n");
		method.append(body);
		method.append("}\n");
//		System.out.println(method);
		operatorClass.addMethod(CtNewMethod.make(method.toString(), operatorClass));
	}

	private static StringBuilder[] generateInternalMethodBody(List<Proposition> ordering, Map<Proposition, Integer> propMap) {
		List<StringBuilder> bodies = new LinkedList<StringBuilder>();
		StringBuilder body = new StringBuilder();
		for (Proposition p : ordering) {
			addInternalComponent(p, body, propMap);

			if (body.length() > MAX_LENGTH) {
				bodies.add(body);
				body = new StringBuilder();
			}
		}
		bodies.add(body);
		return bodies.toArray(new StringBuilder[0]);
	}

	private static StringBuilder[] generateTransitionMethodBody(List<Proposition> ordering, Map<Proposition, Integer> propMap) {
		List<StringBuilder> bodies = new LinkedList<StringBuilder>();
		StringBuilder body = new StringBuilder();
		for (Proposition p : ordering) {
			addTransitionComponent(p, body, propMap);

			if (body.length() > MAX_LENGTH) {
				bodies.add(body);
				body = new StringBuilder();
			}
		}
		bodies.add(body);
		return bodies.toArray(new StringBuilder[0]);
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

	private static void addRoleDependentHelpers(List<List<Proposition>> orderings, Map<Proposition, Integer> propMap, CtClass operatorClass,
			String name) throws CannotCompileException {
		for (int roleIndex = 0; roleIndex < orderings.size(); roleIndex++) {
			StringBuilder[] parts = generateInternalMethodBody(orderings.get(roleIndex), propMap);
			addMethod(operatorClass, parts, name + "Role" + roleIndex);
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
					// if the proposition is not in the proposition map, it is never changed: it is effectively a constant
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
					// if the proposition is not in the proposition map, it is never changed: it is effectively a constant
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
}
