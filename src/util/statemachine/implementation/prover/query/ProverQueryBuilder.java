package util.statemachine.implementation.prover.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.gdl.grammar.GdlVariable;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;

public final class ProverQueryBuilder
{

	private final static GdlConstant DOES = GdlPool.getConstant("does");
	private final static GdlConstant GOAL = GdlPool.getConstant("goal");
	private final static GdlRelation INIT_QUERY = GdlPool.getRelation(GdlPool.getConstant("init"), new GdlTerm[] { GdlPool.getVariable("?x") });
	private final static GdlConstant LEGAL = GdlPool.getConstant("legal");
	private final static GdlRelation NEXT_QUERY = GdlPool.getRelation(GdlPool.getConstant("next"), new GdlTerm[] { GdlPool.getVariable("?x") });
	private final static GdlRelation ROLE_QUERY = GdlPool.getRelation(GdlPool.getConstant("role"), new GdlTerm[] { GdlPool.getVariable("?x") });
	private final static GdlProposition TERMINAL_QUERY = GdlPool.getProposition(GdlPool.getConstant("terminal"));
	private final static GdlVariable VARIABLE = GdlPool.getVariable("?x");

	public static Set<GdlSentence> getContext(MachineState state)
	{
		return state.getContents();
	}

	public static Set<GdlSentence> getContext(MachineState state, List<Role> roles, List<Move> moves)
	{
		Set<GdlSentence> context = new HashSet<GdlSentence>(state.getContents());
		for (int i = 0; i < roles.size(); i++)
		{
			context.add(toDoes(roles.get(i), moves.get(i)));
		}
		return context;
	}

	public static GdlRelation getGoalQuery(Role role)
	{
		return GdlPool.getRelation(GOAL, new GdlTerm[] { role.getName().toTerm(), VARIABLE });
	}

	public static GdlRelation getInitQuery()
	{
		return INIT_QUERY;
	}

	public static GdlRelation getLegalQuery(Role role)
	{
		return GdlPool.getRelation(LEGAL, new GdlTerm[] { role.getName().toTerm(), VARIABLE });
	}

	public static GdlRelation getNextQuery()
	{
		return NEXT_QUERY;
	}

	public static GdlRelation getRoleQuery()
	{
		return ROLE_QUERY;
	}

	public static GdlProposition getTerminalQuery()
	{
		return TERMINAL_QUERY;
	}

	public static GdlRelation toDoes(Role role, Move move)
	{
		return GdlPool.getRelation(DOES, new GdlTerm[] { role.getName().toTerm(), move.getContents().toTerm() });
	}

}
