package bugatti.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.BaseModPlugin;
import bugatti.scripts.SolMegaSpawner;

public class SolPlugin extends BaseModPlugin {

    @Override
    public void onNewGameAfterEconomyLoad() {
        Global.getLogger(this.getClass()).info("[SOL] Spawning Megastructures on Sol...");
        Global.getSector().addScript(new SolMegaSpawner());
    }
}