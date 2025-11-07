package bugatti.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.BaseModPlugin;
import bugatti.scripts.SolMegaSpawner;
import lunalib.lunaSettings.LunaSettings;

public class SolPlugin extends BaseModPlugin {

    @Override
    public void onNewGameAfterEconomyLoad() {
        Boolean setting = LunaSettings.getBoolean("sol-modded", "featuresDisabled");
        if (setting == null || setting) { // default = true if missing
            Global.getLogger(this.getClass()).info("[SOL] Reticulating Splines on Sol...");
            Global.getSector().addScript(new SolMegaSpawner());
        } else {
            Global.getLogger(this.getClass()).info("[SOL] MegaSpawner disabled in LunaSettings.");
        }
    }
}
