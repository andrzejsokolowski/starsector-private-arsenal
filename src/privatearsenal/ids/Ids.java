package privatearsenal.ids;

public class Ids {
    // Submarkets
    public static final String REVERSE_ENG_SUB = "RePA_ReversEngSub";
    public static final String PRIVATE_ARSENAL_SUB = "RePA_PrivateArsenalSub";

    // Industries (reverse engineering hub tiers)
    public static final String REVERSE_ENG_1_IND = "RePA_ReverseEngPhase1Ind";
    public static final String REVERSE_ENG_2_IND = "RePA_ReverseEngPhase2Ind";
    public static final String REVERSE_ENG_3_IND = "RePA_ReverseEngPhase3Ind";

    // Persistent sets of items the hub has reverse-engineered.
    // These drive what the integrated Private Arsenal is allowed to stock.
    public static final String PRODUCED_WEAPONS_MEMORY = "$RePA_ProducedWeapons";
    public static final String PRODUCED_FIGHTERS_MEMORY = "$RePA_ProducedFighters";
    public static final String PRODUCED_SHIPS_MEMORY = "$RePA_ProducedShips";
}
