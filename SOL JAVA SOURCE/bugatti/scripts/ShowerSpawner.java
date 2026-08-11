package bugatti.scripts;

import bugatti.utils.Settings;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import indevo.exploration.meteor.MeteorSwarmManager;
import indevo.exploration.meteor.MeteorSwarmManager.MeteroidShowerType;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class ShowerSpawner implements EconomyTickListener {

    private StarSystemAPI sys;

    public static void forecast(SectorAPI sector) {
        Global.getSector().getListenerManager().addListener(new ShowerSpawner(), true);
    }
    @Override public void reportEconomyTick(int iterIndex) { }

    @Override
    public void reportEconomyMonthEnd() {
        /* sanity checks */
        if (!Settings.GEN_METEORS) return;
        // only if Sol exist
        if (sys == null)
            sys = Global.getSector().getStarSystem("Sol");
        if (sys == null) return;
        // Only spawn if we are in or near Sol
        if (!isPlayaNear()) return;

        int month = Global.getSector().getClock().getMonth();  // 0–11

        // --- SEASONAL METEOR TIMING MODEL ---
        float chance = grabMonthlyChance(month);

        if (Math.random() > chance)
            return;

        try {
            float intensity = grabIntensity(month);

            MeteorSwarmManager.getInstance().spawnShower(sys, intensity, MeteroidShowerType.ASTEROID);

            Global.getLogger(this.getClass()).info(
                    "[SOL] meteor shower spawned (month " + month + ", intensity " + intensity + ")"
            );

        } catch (Exception ex) {
            Global.getLogger(this.getClass()).error("[SOL] Failed meteor spawn", ex);
        }
    }
    /** Chance values modeled on real-world meteor shower calendars (reverted to seasonal) */
    private float grabMonthlyChance(int month) {

        return switch (month) {

            case 0  -> 0.60f;  // January - Quadrantids (high)
            case 3  -> 0.70f;  // April - Lyrids (moderate)
            case 6  -> 0.50f;  // July - Delta Aquariids (moderate)
            case 9  -> 0.80f;  // October - Orionids (strong)

            default -> 0.10f;  // minor sporadic showers
        };
    }
    /** Give special intensity boosts during major shower seasons */
    private float grabIntensity(int month) {

        return switch (month) {

            case 0 -> 2.0f; // sturdy (January)
            case 3 -> 3.0f; // heavy (April)
            case 6 -> 1.5f; // decent (July)
            case 9 -> 2.5f; // strong (October)

            default -> 1.0f; // mild (sporadic)
        };
    }
    /** Detect if we're inside or near System */
    private boolean isPlayaNear() {

        LocationAPI curr = Global.getSector().getCurrentLocation();
        // Inside Sol?
        if (curr instanceof StarSystemAPI) {
            if (curr == sys)
                return true;
        }
        // In hyperspace -> measure distance to Sol’s hyperspace anchor
        if (curr.isHyperspace()) {

            Vector2f player = Global.getSector().getPlayerFleet().getLocationInHyperspace();
            Vector2f star = sys.getHyperspaceAnchor().getLocation();

            return MathUtils.getDistance(player, star) < sys.getMaxRadiusInHyperspace() + 400f;
        }
        // Stations or non-space special locations
        return false;
    }
}