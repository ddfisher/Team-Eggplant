package util.statemachine.implementation.propnet;

import java.util.List;
import java.util.Map;

import util.propnet.architecture.components.Proposition;

public class CheckedOperator extends Operator {
	private Operator nativeOperator, javassistOperator;
	
	//FIXME uncomment
//	public CheckedOperator(Map<Proposition, Integer> propMap, List<Proposition> transitionOrdering, List<Proposition> internalOrdering,
//			List<Proposition> terminalOrdering, List<List<Proposition>> legalOrderings, List<List<Proposition>> goalOrderings) {
//		nativeOperator = NativeOperatorFactory.buildOperator(propMap, transitionOrdering, internalOrdering, terminalOrdering, legalOrderings, goalOrderings);
//		javassistOperator = OperatorFactory.buildOperator(propMap, transitionOrdering, internalOrdering, terminalOrdering, legalOrderings, goalOrderings);
//	}

	@Override
	public void propagate(boolean[] props){
		boolean[] check = props.clone();
		nativeOperator.propagate(props);
		javassistOperator.propagate(check);
		check(props, check);
	}
	
	@Override
	public void propagateGoalOnly(boolean[] props, int role) {
		boolean[] check = props.clone();
		nativeOperator.propagateGoalOnly(props, role);
		javassistOperator.propagateGoalOnly(check, role);
		check(props, check);
	}

	@Override
	public void propagateInternal(boolean[] props) {
		boolean[] check = props.clone();
		nativeOperator.propagateInternal(props);
		javassistOperator.propagateInternal(check);
		check(props, check);
	}

	@Override
	public void propagateLegalOnly(boolean[] props, int role, int legalIndex) {
		boolean[] check = props.clone();
		nativeOperator.propagateLegalOnly(props, role, legalIndex);
		javassistOperator.propagateLegalOnly(check, role, legalIndex);
		check(props, check);
	}

	@Override
	public void propagateTerminalOnly(boolean[] props) {
		boolean[] check = props.clone();
		nativeOperator.propagateTerminalOnly(props);
		javassistOperator.propagateTerminalOnly(check);
		check(props, check);
	}

	@Override
	public void transition(boolean[] props) {
		boolean[] check = props.clone();
		nativeOperator.transition(props);
		javassistOperator.transition(check);
		check(props, check);
	}
	
	private void printBoolArray(boolean[] arr){
		StringBuilder str = new StringBuilder("[");
		for (boolean bool : arr) {
			str.append(bool ? "1" : "0");
		}
		str.append("]");
		System.out.println(str);
	}
	
	private void check(boolean[] props, boolean[] check) {
		if (!equal(props, check)){
			System.out.println("Different!");
			printBoolArray(props);
			printBoolArray(check);
		}
	}
	
	private boolean equal(boolean[] arr1, boolean[] arr2) {
    	for (int index = 0; index<arr1.length; index++) {
    		if (arr1[index] != arr2[index]) {
    			return false;
    		}
    	}
    	return true;
	}
	
	@Override
	public boolean monteCarlo(boolean[] props, int maxDepth) {
		boolean[] check = props.clone();
		boolean nativeResult = nativeOperator.monteCarlo(props, maxDepth);
		boolean javassistResult = javassistOperator.monteCarlo(check, maxDepth);
		check(props, check);
		return nativeResult && javassistResult;
	}
	
	@Override
	public void initMonteCarlo(int[][] legalPropMap, int[] legalInputMap) {
		nativeOperator.initMonteCarlo(legalPropMap, legalInputMap);
		javassistOperator.initMonteCarlo(legalPropMap, legalInputMap);
	}
}
