package bugatti.systems;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import org.lwjgl.util.vector.Vector2f;

import static bugatti.utils.UniHelp.*;

public class BetaCentauri {

    public static void genSys(SectorAPI sec) {
        StarSystemAPI sys = sec.createStarSystem("Beta Centauri");
        sys.setAge(StarAge.OLD);

        // Position it wherever you want (example offset from Alpha Centauri)
        StarSystemAPI centauri = sec.getStarSystem("Alpha Centauri");
        Vector2f pos = new Vector2f(-52000f, -34000f);
        if (centauri != null) {
            pos.set(centauri.getLocation().x + 6000f,
                    centauri.getLocation().y + 3000f);
        }
        sys.getLocation().set(pos);

        // Star (Hadar = Beta Centauri, blue giant)
        PlanetAPI star = sys.initStar("hadar_star", StarTypes.BLUE_GIANT, 1200f, 600f);

        // Assign to Centaurus constellation
        //var centaurus = doConstellation(sys, "Centaurus");
        doNebula(sys, 0f,0f);

        //sys.setStar(star);
        star.setName("Hadar");

        // Minimal config
        sys.setProcgen(false);
        sys.addTag(Tags.THEME_INTERESTING_MINOR);
        sys.autogenerateHyperspaceJumpPoints(false, true, false);
    }
}
