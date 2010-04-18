package player.gamer.statemachine.eggplant;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import player.gamer.event.GamerNewMatchEvent;
import util.observer.Event;
import apps.common.table.JZebraTable;
import apps.player.detail.DetailPanel;

@SuppressWarnings("serial")
public final class EggplantDetailPanel extends DetailPanel
{
	private final JZebraTable moveTable;

	public EggplantDetailPanel()
	{
		super(new GridBagLayout());

		DefaultTableModel model = new DefaultTableModel();

		model.addColumn("Move");
		model.addColumn("Minimax Value");
		model.addColumn("Computation Time");
		model.addColumn("States Expanded");
		model.addColumn("Leaves Expanded");
		model.addColumn("Cache Hits");
		model.addColumn("Cache Misses");



		moveTable = new JZebraTable(model)
		{

			@Override
			public boolean isCellEditable(int rowIndex, int colIndex)
			{
				return false;
			}
		};
		moveTable.setShowHorizontalLines(true);
		moveTable.setShowVerticalLines(true);

		this.add(new JScrollPane(moveTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 5, 5));
	}

	public void observe(Event event)
	{
		if (event instanceof GamerNewMatchEvent)
		{
			observe((GamerNewMatchEvent) event);
		}
		else if (event instanceof EggplantMoveSelectionEvent)
		{
			observe((EggplantMoveSelectionEvent) event);
		}
	}

	private void observe(GamerNewMatchEvent event)
	{
		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.setRowCount(0);
	}

	private void observe(EggplantMoveSelectionEvent event)
	{
		String move = event.getSelection().toString();
		String value = Integer.toString(event.getValue());
		String computationTime = Long.toString(event.getTime()) + " ms";
		String states = Integer.toString(event.getStatesSearched());
		String leaves = Integer.toString(event.getLeavesSearched());
		String cacheHits = Integer.toString(event.getCacheHits());
		String cacheMisses = Integer.toString(event.getCacheMisses());
		
		DefaultTableModel model = (DefaultTableModel) moveTable.getModel();
		model.addRow(new String[] { move, value, computationTime, states, leaves, cacheHits, cacheMisses });
	}
}
