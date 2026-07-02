package privatearsenal.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

import lunalib.lunaSettings.LunaSettings;

/**
 * Central accessor for the reverse-engineering tunables exposed through LunaLib
 * ({@code data/config/LunaSettings.csv}). Every getter falls back to a hardcoded
 * default if LunaLib is missing or a field has not loaded yet, so callers never
 * have to null-check.
 *
 * Both the research time and the arsenal price are now driven by which AI core is
 * assigned to the hub (no core / gamma / beta / alpha / omega).
 */
public class ReverseEngSettings {
    public static final String MOD_ID = "re_private_arsenal";
    public static final String OMEGA_CORE = "omega_core";

    private static boolean lunaAvailable() {
        return Global.getSettings().getModManager().isModEnabled("lunalib");
    }

    private static int getInt(String id, int def) {
        try {
            if (lunaAvailable()) {
                Integer v = LunaSettings.getInt(MOD_ID, id);
                if (v != null) {
                    return v;
                }
            }
        } catch (Throwable t) {
            // use default
        }
        return def;
    }

    private static float getFloat(String id, float def) {
        try {
            if (lunaAvailable()) {
                Double v = LunaSettings.getDouble(MOD_ID, id);
                if (v != null) {
                    return v.floatValue();
                }
            }
        } catch (Throwable t) {
            // use default
        }
        return def;
    }

    private static boolean getBoolean(String id, boolean def) {
        try {
            if (lunaAvailable()) {
                Boolean v = LunaSettings.getBoolean(MOD_ID, id);
                if (v != null) {
                    return v;
                }
            }
        } catch (Throwable t) {
            // use default
        }
        return def;
    }

    // --- Research time (flat days per item, by assigned core) ---

    public static int daysNoCore() {
        return getInt("repa_re_days_no_core", 30);
    }

    public static int daysGamma() {
        return getInt("repa_re_days_gamma", 20);
    }

    public static int daysBeta() {
        return getInt("repa_re_days_beta", 15);
    }

    public static int daysAlpha() {
        return getInt("repa_re_days_alpha", 5);
    }

    public static int daysOmega() {
        return getInt("repa_re_days_omega", 2);
    }

    /** Flat research days for the given assigned core id (null/unknown = no core). */
    public static int daysForCore(String coreId) {
        if (Commodities.GAMMA_CORE.equals(coreId)) {
            return daysGamma();
        }
        if (Commodities.BETA_CORE.equals(coreId)) {
            return daysBeta();
        }
        if (Commodities.ALPHA_CORE.equals(coreId)) {
            return daysAlpha();
        }
        if (OMEGA_CORE.equals(coreId)) {
            return daysOmega();
        }
        return daysNoCore();
    }

    public static int droneReplicatorDayReduction() {
        return getInt("repa_re_drone_day_reduction", 1);
    }

    public static int improveBlueprintCopies() {
        return getInt("repa_re_improve_blueprint_copies", 1);
    }

    // --- Parallel research slots (per item type, scaled by hub tier and item size) ---

    /** Slot budget per hub tier. Items researched in parallel per type = this * tier / itemSize. */
    public static int baseResearchSlots() {
        return getInt("repa_re_base_research_slots", 2);
    }

    public static int sizeWeapon() {
        return getInt("repa_re_size_weapon", 1);
    }

    public static int sizeFighter() {
        return getInt("repa_re_size_fighter", 2);
    }

    public static int sizeShip() {
        return getInt("repa_re_size_ship", 3);
    }

    // --- Arsenal price multipliers (vs base item value, by assigned core) ---

    public static float priceMultNoCore() {
        return getFloat("repa_re_price_mult_no_core", 2.0f);
    }

    public static float priceMultGamma() {
        return getFloat("repa_re_price_mult_gamma", 1.6f);
    }

    public static float priceMultBeta() {
        return getFloat("repa_re_price_mult_beta", 1.0f);
    }

    public static float priceMultAlpha() {
        return getFloat("repa_re_price_mult_alpha", 0.4f);
    }

    public static float priceMultOmega() {
        return getFloat("repa_re_price_mult_omega", 0.0f);
    }

    /** Arsenal price multiplier for the given assigned core id (null/unknown = no core). */
    public static float priceMultForCore(String coreId) {
        if (Commodities.GAMMA_CORE.equals(coreId)) {
            return priceMultGamma();
        }
        if (Commodities.BETA_CORE.equals(coreId)) {
            return priceMultBeta();
        }
        if (Commodities.ALPHA_CORE.equals(coreId)) {
            return priceMultAlpha();
        }
        if (OMEGA_CORE.equals(coreId)) {
            return priceMultOmega();
        }
        return priceMultNoCore();
    }

    public static boolean debugLogging() {
        return getBoolean("repa_re_debug_logging", false);
    }

    /**
     * When enabled, a Tier 3 hub's Private Arsenal stocks reverse-engineered ships and
     * shows in the fleet screen so they can be bought directly (skipping Heavy Industry).
     * Off by default: normally you still need Heavy Industry to build ships from blueprints.
     */
    public static boolean sellShipsInFleet() {
        return getBoolean("repa_re_sell_ships_in_fleet", false);
    }
}
