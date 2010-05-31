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
	
	public static final int CACHED_BPNSM_NATIVE = 70;
	public static final int CACHED_BPNSM_JAVASSIST = 80;
	public static final int CACHED_PROVER = 90;
	public static final int PROVER = 100;
	
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
		pq.add(new PrioritizedStateMachine(PROVER, new ProverStateMachine()));
	}
	
	public static void pushMachine(int priority, StateMachine machine) {
		try {
			pq.add(new PrioritizedStateMachine(priority, machine));
			delegate.signalUpdateMachine();
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
	
	public static void setDelegate(EggplantPrimaryGamer gamer) {
		delegate = gamer;
	}
}
