package bugatti.utils;

import lunalib.lunaSettings.LunaSettings;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

public final class Settings {

    private static final Logger log = Global.getLogger(Settings.class);
    private static final String MOD_ID = "bugatti.Sol";

    public static boolean FEATURES_DISABLED;
    public static boolean DEBUG_LOOTER;

    public static boolean GEN_SOL;
    public static boolean GEN_CENTAURUS;

    public static boolean GEN_METEORS;

    public static boolean DESPAWN_MEGAS;
    public static boolean SPAWN_PLOUTON;
    public static boolean SPAWN_GUNGNIR;
    public static boolean SPAWN_TRANSFROST;
    public static boolean SPAWN_TAP;

    //public static String FROST_MODE;

    private Settings() {}

    public static void reload() {

        // Defaults (must match CSV)
        FEATURES_DISABLED   = false;
        DEBUG_LOOTER        = false;
        GEN_SOL             = true;
        GEN_CENTAURUS       = true;
        GEN_METEORS         = true;
        DESPAWN_MEGAS       = false;
        SPAWN_PLOUTON       = true;
        SPAWN_GUNGNIR       = true;
        SPAWN_TRANSFROST    = true;
        SPAWN_TAP           = true;
        //FROST_MODE        = "Prefab";

        if (!ModCheck.hasLL) {
            log.info("[SOL] LunaLib not present; using defaults.");
            return;
        }

        try {
            Boolean b;
            String s;

            b = LunaSettings.getBoolean(MOD_ID, "featuresDisabled");
            if (b != null) FEATURES_DISABLED = b;

            b = LunaSettings.getBoolean(MOD_ID, "debug_looter");
            if (b != null) DEBUG_LOOTER = b;

            b = LunaSettings.getBoolean(MOD_ID, "spawn_sol");
            if (b != null) GEN_SOL = b;

            b = LunaSettings.getBoolean(MOD_ID, "spawn_centaurus");
            if (b != null) GEN_CENTAURUS = b;

            b = LunaSettings.getBoolean(MOD_ID, "spawn_meteors");
            if (b != null) GEN_METEORS = b;

            b = LunaSettings.getBoolean(MOD_ID, "despawn_megas");
            if (b != null) DESPAWN_MEGAS = b;

            b = LunaSettings.getBoolean(MOD_ID, "spawn_plouton");
            if (b != null) SPAWN_PLOUTON = b;

            b = LunaSettings.getBoolean(MOD_ID, "spawn_gungnir");
            if (b != null) SPAWN_GUNGNIR = b;

            b = LunaSettings.getBoolean(MOD_ID, "spawn_transfrost");
            if (b != null) SPAWN_TRANSFROST = b;

            b = LunaSettings.getBoolean(MOD_ID, "spawn_hypershunt");
            if (b != null) SPAWN_TAP = b;

            //s = LunaSettings.getString(MOD_ID, "frost_mode"); //if (s != null) FROST_MODE = s;

        } catch (Throwable t) {
            log.warn("[SOL] Failed to reload Luna settings; using defaults.", t);
        }
    }
}