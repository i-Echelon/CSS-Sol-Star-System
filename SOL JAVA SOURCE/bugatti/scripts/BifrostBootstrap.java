package bugatti.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.impl.bifrost.BifrostMega;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.models.GPManager;
import data.kaysaar.aotd.vok.campaign.econ.globalproduction.models.megastructures.GPBaseMegastructure;
import org.apache.log4j.Logger;

public final class BifrostBootstrap {

    private static final Logger log = Global.getLogger(BifrostBootstrap.class);

    private BifrostBootstrap() {}

    public static BifrostMega getOrCreateNetwork(Object anchor) {

        var mgr = GPManager.getInstance();

        for (GPBaseMegastructure m : mgr.getMegastructures()) {
            if (m instanceof BifrostMega) {
                return (BifrostMega) m;
            }
        }

        var bifrost = new BifrostMega();
        bifrost.trueInit("aotd_bifrost", (SectorEntityToken) anchor);
        mgr.getMegastructures().add(bifrost);

        log.info("[SOL] Created new Bifrost network (bootstrap).");
        return bifrost;
    }
}
