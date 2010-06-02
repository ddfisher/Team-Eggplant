package player.gamer.statemachine.eggplant.misc;

import java.util.concurrent.PriorityBlockingQueue;

import player.gamer.Gamer;
import player.gamer.statemachine.eggplant.EggplantPrimaryGamer;
import util.statemachine.StateMachine;
import util.statemachine.implementation.propnet.BooleanPropNetStateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;

public class StateMachineFactory {
	/** All possible priorities. Lower is preferred */ 
	
	public static final int CACHED_BPNSM_FACTOR = 0;
	public static final int CACHED_BPNSM_NATIVE = 10;
	public static final int CACHED_BPNSM_JAVASSIST = 20;
	public static final int CACHED_PROVER = 30;
	
	private static class PrioritizedStateMachine implements Comparable<PrioritizedStateMachine>{
		private final int priority;
		private final StateMachine machine;
		private PrioritizedStateMachine(int priority, StateMachine machine) {
			this.priority = priority;
			this.machine = machine;
		}
		public int compareTo(PrioritizedStateMachine other) {
			return this.priority - other.priority;
		}
	}
	
	private static PriorityBlockingQueue<PrioritizedStateMachine> pq = new PriorityBlockingQueue<PrioritizedStateMachine>();
	private static EggplantPrimaryGamer delegate;

	/** Reset MUST be called between games! */
	public static void reset() {
		pq.clear();
		pq.add(new PrioritizedStateMachine(CACHED_PROVER, new CachedProverStateMachine()));
	}
	
	public static void pushMachine(int priority, StateMachine machine) {
		try {
			StateMachine currentMachine = getCurrentMachine();
			pq.add(new PrioritizedStateMachine(priority, machine));
			StateMachine newMachine = getCurrentMachine();
			Log.println('y', "Pushed: " + newMachine);
			if (currentMachine != newMachine) {
				delegate.signalUpdateMachine();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void popMachine() {
		try {
			PrioritizedStateMachine poppedMachine = pq.poll();
			if (poppedMachine.priority == CACHED_BPNSM_NATIVE) {
				((BooleanPropNetStateMachine) poppedMachine.machine).setOperator(true);
			}
			Log.println('y', "Popped: " + poppedMachine);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static StateMachine getCurrentMachine() {
		try {
			PrioritizedStateMachine firstMachine = pq.peek();
			return firstMachine.machine;
		} catch (Exception ex) {
			ex.printStackTrace();
			return new CachedProverStateMachine();
		}
	}
	
	public static int getCurrentMachineDescription() {
		try {
			PrioritizedStateMachine firstMachine = pq.peek();
			return firstMachine.priority;
		} catch (Exception ex) {
			ex.printStackTrace();
			return CACHED_PROVER;
		}
	}
	
	public static void setDelegate(EggplantPrimaryGamer gamer) {
		delegate = gamer;
	}
}
