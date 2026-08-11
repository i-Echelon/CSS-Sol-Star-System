package bugatti.plugins;

import bugatti.systems.*;
import bugatti.scripts.*;
import bugatti.utils.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.BaseModPlugin;
import org.apache.log4j.Logger;
import static bugatti.utils.UniHelp.*;

public class SolPlug extends BaseModPlugin {

    private static final Logger log = Global.getLogger(Sol.class);

    @Override
    public void onNewGame() {
        Settings.reload();

        if (Settings.FEATURES_DISABLED) {
            log.info("[SOL] All scripted features disabled.");
            return;
        }
        // Register Cryo
        Global.getSector().registerPlugin(new CryoPlug());
    }
    @Override
    public void onNewGameAfterProcGen() {
        super.onNewGameAfterProcGen();

        if (Settings.FEATURES_DISABLED) {
            log.info("[SOL] ProcGen-stage features disabled.");
            return;
        }
        // Generate Sol
        if (Settings.GEN_SOL) {
            log.info("[SOL] Generating Sol System...");
            Sol.genSys(Global.getSector());
        }
        // Generate Centaurus
        if (Settings.GEN_CENTAURUS) {
            log.info("[SOL] Generating Centaurus Constellation...");
            genCentaurusConstellation();
        }
        // WH support
        ModCheck.registerWideHorizons();
    }

    @Override
    public void onNewGameAfterEconomyLoad() {

        if (Settings.FEATURES_DISABLED) {
            log.info("[SOL] Economy-stage features disabled.");
            return;
        }

        if (ModCheck.hasIND) {
            log.info("[SOL] Registering IndEvo meteor system");
            ShowerSpawner.forecast(Global.getSector());
        }

        if (ModCheck.hasVOK) {
            log.info("[SOL] Handling AoTD megastructures");
            MegaSuppressor.run();
            MegaBootstrap.queue();
        }
    }
}