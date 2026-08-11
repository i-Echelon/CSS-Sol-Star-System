package bugatti.scripts;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

public final class MegaBootstrap {

    private static final Logger log = Global.getLogger(MegaBootstrap.class);

    private MegaBootstrap() {}

    public static void queue() {

        var mem = Global.getSector().getMemoryWithoutUpdate();

        if (mem.getBoolean("$sol_mega_bootstrap_done")) return;
        mem.set("$sol_mega_bootstrap_done", true);

        Global.getSector().addTransientScript(new RunnableScript());
    }

    private static class RunnableScript implements com.fs.starfarer.api.EveryFrameScript {

        private boolean done = false;

        @Override
        public void advance(float amount) {
            if (done) return;
            if (Global.getSector() == null) return;

            var sol = Global.getSector().getStarSystem("Sol");
            if (sol == null) {
                log.warn("[SOL] Sol system not found; aborting mega bootstrap.");
                done = true;
                return;
            }

            try {
                MegaSpawner.run(sol);
            } catch (Throwable t) {
                log.error("[SOL] MegaSpawner failed", t);
            }

            done = true;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }
    }
}
