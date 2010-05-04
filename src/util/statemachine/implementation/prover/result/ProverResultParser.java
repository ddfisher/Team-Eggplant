package util.statemachine.implementation.prover.result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.implementation.prover.ProverMachineState;
import util.statemachine.implementation.prover.ProverMove;
import util.statemachine.implementation.prover.ProverRole;

public final class ProverResultParser
{

	private final static GdlConstant TRUE = GdlPool.getConstant("true");

	public static List<Move> toMoves(Set<GdlSentence> results)
	{
		List<Move> moves = new ArrayList<Move>();
		for (GdlSentence result : results)
		{
			moves.add(new ProverMove(result.get(1).toSentence()));
		}

		return moves;
	}

	public static List<Role> toRoles(List<GdlSentence> results)
	{
		List<Role> roles = new ArrayList<Role>();
		for (GdlSentence result : results)
		{
			GdlProposition name = (GdlProposition) result.get(0).toSentence();
			roles.add(new ProverRole(name));
		}

		return roles;
	}

	public static MachineState toState(Set<GdlSentence> results)
	{
		Set<GdlSentence> trues = new HashSet<GdlSentence>();
		for (GdlSentence result : results)
		{
			trues.add(GdlPool.getRelation(TRUE, new GdlTerm[] { result.get(0) }));
		}
		return new ProverMachineState(trues);
	}
}