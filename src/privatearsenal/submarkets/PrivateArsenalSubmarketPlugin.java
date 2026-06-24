package privatearsenal.submarkets;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import privatearsenal.ids.Ids;
import privatearsenal.industries.reverseEngineering.AbstractReverseEngineeringIndustry;
import privatearsenal.utils.BaseArsenalSubmarketPlugin;
import privatearsenal.utils.ReverseEngSettings;

/**
 * The Private Arsenal is now an output of the Reverse Engineering Hub: it only
 * stocks weapons, fighter wings and ships the hub has actually reverse-engineered
 * here, gated by the hub's current tier (T1 weapons, T2+ also fighters, T3+ also
 * ships). Items merely unlocked elsewhere (a blueprint found in the world) do not
 * appear.
 */
public class PrivateArsenalSubmarketPlugin extends BaseArsenalSubmarketPlugin {

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        if (!hasReverseEngHub()) {
            getCargo().clear();
            getCargo().initMothballedShips(safeFactionId());
            return;
        }
        refreshMarketStock();
    }

    public void refreshMarketStock() {
        getCargo().clear();
        // The mothballed-ship stockpile must be (re)initialised with a valid, loaded
        // faction id. Otherwise its FleetData serialises a null faction and crashes the
        // save with an NPE in FleetData.readResolve - even after the hub is gone and the
        // submarket is left stashed in colony memory. The colony owner is always loaded.
        getCargo().initMothballedShips(safeFactionId());
        int tier = getHubTier();
        addProducedWeapons(); // tier 1+
        if (tier >= 2) {
            addProducedFighters(); // tier 2+
        }
        if (tier >= 3) {
            addProducedShips(); // tier 3+
        }
        getCargo().sort();
    }

    /** A faction id guaranteed to resolve, for initialising the mothballed-ship stockpile. */
    private String safeFactionId() {
        if (market != null && market.getFaction() != null) {
            return market.getFaction().getId();
        }
        return Factions.NEUTRAL;
    }

    private boolean hasReverseEngHub() {
        return market.hasIndustry(Ids.REVERSE_ENG_1_IND)
                || market.hasIndustry(Ids.REVERSE_ENG_2_IND)
                || market.hasIndustry(Ids.REVERSE_ENG_3_IND);
    }

    private int getHubTier() {
        if (market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
            return 3;
        }
        if (market.hasIndustry(Ids.REVERSE_ENG_2_IND)) {
            return 2;
        }
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
            return 1;
        }
        return 0;
    }

    private Industry getReverseEngHub() {
        if (market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
            return market.getIndustry(Ids.REVERSE_ENG_3_IND);
        }
        if (market.hasIndustry(Ids.REVERSE_ENG_2_IND)) {
            return market.getIndustry(Ids.REVERSE_ENG_2_IND);
        }
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
            return market.getIndustry(Ids.REVERSE_ENG_1_IND);
        }
        return null;
    }

    /** The AI core assigned to the hub drives the arsenal's prices. */
    private String getHubCoreId() {
        Industry hub = getReverseEngHub();
        return hub != null ? hub.getAICoreId() : null;
    }

    private void addProducedWeapons() {
        List<String> ids = new ArrayList<String>(
                AbstractReverseEngineeringIndustry.getProducedSet(Ids.PRODUCED_WEAPONS_MEMORY));
        for (String weaponId : ids) {
            try {
                WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(weaponId);
                if (spec != null) {
                    getCargo().addWeapons(weaponId, 50);
                }
            } catch (Throwable t) {
                // stale / removed weapon id -> skip
            }
        }
    }

    private void addProducedFighters() {
        List<String> ids = new ArrayList<String>(
                AbstractReverseEngineeringIndustry.getProducedSet(Ids.PRODUCED_FIGHTERS_MEMORY));
        for (String fighterId : ids) {
            try {
                FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(fighterId);
                if (spec != null) {
                    getCargo().addFighters(fighterId, 50);
                }
            } catch (Throwable t) {
                // stale / removed wing id -> skip
            }
        }
    }

    private void addProducedShips() {
        List<String> ids = new ArrayList<String>(
                AbstractReverseEngineeringIndustry.getProducedSet(Ids.PRODUCED_SHIPS_MEMORY));
        for (String hullId : ids) {
            try {
                FleetMemberAPI member = createMothballMember(hullId);
                if (member != null) {
                    getCargo().getMothballedShips().addFleetMember(member);
                }
            } catch (Throwable t) {
                // stale / removed hull id -> skip
            }
        }
    }

    /** Build an empty-variant mothballed member for a reverse-engineered hull. */
    private FleetMemberAPI createMothballMember(String hullId) {
        String hullVariantId = hullId + "_Hull";
        ShipVariantAPI variant;
        if (Global.getSettings().doesVariantExist(hullVariantId)) {
            variant = Global.getSettings().getVariant(hullVariantId);
        } else {
            ShipHullSpecAPI hull = Global.getSettings().getHullSpec(hullId);
            if (hull == null) {
                return null;
            }
            variant = Global.getSettings().createEmptyVariant(hullVariantId, hull);
        }
        if (variant == null) {
            return null;
        }
        return Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false; // Ce marché ne fait pas partie de l'économie globale
    }

    @Override
    public boolean isIllegalOnSubmarket(com.fs.starfarer.api.campaign.CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL; // Interdit la vente d'objets
    }

    @Override
    public String getIllegalTransferText(com.fs.starfarer.api.campaign.CargoStackAPI stack, TransferAction action) {
        return "You cannot sell items here.";
    }

    @Override
    public boolean isTooltipExpandable() {
        return super.isTooltipExpandable();
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);
        tooltip.addPara(
                "Stocks every weapon, fighter wing and ship your Reverse Engineering Hub has reverse-engineered. Prices scale with the AI core assigned to the hub (an Omega core makes everything free). Selling items is not allowed.",
                10f);
    }

    @Override
    public float getTariff() {
        // Player buy price = base value x priceMult; tariff is (priceMult - 1).
        float mult = ReverseEngSettings.priceMultForCore(getHubCoreId());
        return Math.max(-1f, mult - 1f);
    }

    @Override
    public boolean isFreeTransfer() {
        // Omega core (or any 0x multiplier) makes the arsenal free.
        return ReverseEngSettings.priceMultForCore(getHubCoreId()) <= 0f;
    }

    @Override
    public boolean showInFleetScreen() {
        return false;
    }
}
