package player.gamer.statemachine.eggplant;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;

import apps.player.config.ConfigPanel;

@SuppressWarnings("serial")
public class EggplantConfigPanel extends ConfigPanel {
	
	private JCheckBox useCache;

	public EggplantConfigPanel() {
		super(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0.1, 0.1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 5, 5);

		useCache = new JCheckBox("Caching", true);
		
		this.add(useCache, c);
	}
	
	public boolean useCache() {
		return useCache.isSelected();
	}

}
