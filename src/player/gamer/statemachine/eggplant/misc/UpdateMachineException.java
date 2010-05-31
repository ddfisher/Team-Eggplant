package player.gamer.statemachine.eggplant.misc;

import util.statemachine.StateMachine;

@SuppressWarnings("serial")
public class UpdateMachineException extends Exception {
	
	private final boolean upgrade;
	public UpdateMachineException(boolean upgrade) {
		this(upgrade, "");
	}
	
	public UpdateMachineException(boolean upgrade, String message) {
		super(message);
		this.upgrade = upgrade;
	}
	
	public boolean isUpgrade() {
		return upgrade;
	}
}
