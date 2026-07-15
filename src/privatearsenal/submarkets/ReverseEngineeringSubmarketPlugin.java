package privatearsenal.submarkets;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import privatearsenal.ids.Ids;
import privatearsenal.industries.reverseEngineering.AbstractReverseEngineeringIndustry;
import privatearsenal.utils.BaseArsenalSubmarketPlugin;

public class ReverseEngineeringSubmarketPlugin extends BaseArsenalSubmarketPlugin {
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }

    @Override
    public String getName() {
        String toReturn = "Reverse\n" +
                "Engineering\n" +
                "Storage ";
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
            toReturn += "I";
        } else if (market.hasIndustry(Ids.REVERSE_ENG_2_IND)) {
            toReturn += "II";
        } else if (market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
            toReturn += "III";
        }
        return toReturn;
    }

    @Override
    public String getDesc() {
        String toReturn = "Deposit ships / weapons / wing for analysis here, to gain progress on the reverse engineering of the item.";
        toReturn += "\n\n";
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
            toReturn += "Tier 1: Only weapons are allowed.";
        } else if (market.hasIndustry(Ids.REVERSE_ENG_2_IND)) {
            toReturn += "Tier 2: Only weapons and fighter wings are allowed.";
        } else if (market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
            toReturn += "Tier 3: Weapons, fighter wings, and ships are allowed.";
        }
        toReturn += "\n\n";
        toReturn += "Choose in priority the item you don t have discovered yet.";
        return toReturn;
    }

    @Override
    public boolean showInCargoScreen() {
        return true;
    }

    @Override
    public String getBuyVerb() {
        return "Take";
    }

    @Override
    public String getSellVerb() {
        return "Leave";
    }

    @Override
    public float getTariff() {
        return 0f;
    }

    @Override
    public boolean isFreeTransfer() {
        return true;
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }

    @Override
    public SubmarketPlugin.PlayerEconomyImpactMode getPlayerEconomyImpactMode() {
        return PlayerEconomyImpactMode.NONE;
    }

    @Override
    public boolean showInFleetScreen() {
        return market.isPlayerOwned();
    }

    // FIX: To fix this
    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        // ((ReverseEngineeringIndustry)
        // market.getIndustry(Ids.REVERSE_ENG_IND)).addCurrentProjectTooltip(tooltip,
        // Industry.IndustryTooltipMode.NORMAL);
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL && alreadyReverseEngineered(member)) {
            return "Already reverse-engineered - available in the Private Arsenal.";
        }

        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND) || market.hasIndustry(Ids.REVERSE_ENG_2_IND)
                || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {

            if (!market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
                return "Can not be Reverse Engineered - Ships are allowed in Tier 3.";
            } else if (!isHubImproved()) {
                return "Can not be Reverse Engineered - Improve the hub with a story point to allow ships.";
            } else {
                return "";
            }
        }

        return "something broke.";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
        if (action == TransferAction.PLAYER_SELL && alreadyReverseEngineered(stack)) {
            return "Already reverse-engineered - available in the Private Arsenal.";
        }

        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND) || market.hasIndustry(Ids.REVERSE_ENG_2_IND)
                || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {

            if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
                if (!stack.isWeaponStack()) {
                    return "Can not be Reverse Engineered - Only weapons are allowed in Tier 1.";
                }
            } else if (market.hasIndustry(Ids.REVERSE_ENG_2_IND) || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
                if (!stack.isFighterWingStack() && !stack.isWeaponStack()) {
                    return "Can not be Reverse Engineered - Only weapons and fighter wings are allowed in Tier 2.";
                }
            }
        }

        return "something broke.";
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
        if (market.hasIndustry(Ids.REVERSE_ENG_1_IND) || market.hasIndustry(Ids.REVERSE_ENG_2_IND)
                || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {

            // Refuse re-depositing anything the hub has already reverse-engineered.
            if (action == TransferAction.PLAYER_SELL && alreadyReverseEngineered(stack)) {
                return true;
            }

            if (market.hasIndustry(Ids.REVERSE_ENG_1_IND)) {
                if (!stack.isWeaponStack()) {
                    return true;
                }
            } else if (market.hasIndustry(Ids.REVERSE_ENG_2_IND) || market.hasIndustry(Ids.REVERSE_ENG_3_IND)) {
                if (!stack.isWeaponStack() && !stack.isFighterWingStack()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
        // Ships may only be reverse-engineered by a Tier 3 hub improved with a story point.
        if (market.hasIndustry(Ids.REVERSE_ENG_3_IND) && isHubImproved()) {
            // Refuse re-depositing a ship the hub has already reverse-engineered.
            return action == TransferAction.PLAYER_SELL && alreadyReverseEngineered(member);
        }
        return true;
    }

    /** True when the Tier 3 hub has been improved with a story point (required to reverse-engineer ships). */
    private boolean isHubImproved() {
        Industry hub = market.getIndustry(Ids.REVERSE_ENG_3_IND);
        return hub != null && hub.isImproved();
    }

    /**
     * True when this stack's weapon/fighter has already been reverse-engineered
     * here (and is therefore stocked in the Private Arsenal). Other items are
     * handled by the tier checks.
     */
    private boolean alreadyReverseEngineered(CargoStackAPI stack) {
        if (stack.isWeaponStack()) {
            WeaponSpecAPI spec = stack.getWeaponSpecIfWeapon();
            return spec != null && AbstractReverseEngineeringIndustry
                    .getProducedSet(Ids.PRODUCED_WEAPONS_MEMORY).contains(spec.getWeaponId());
        }
        if (stack.isFighterWingStack()) {
            FighterWingSpecAPI spec = stack.getFighterWingSpecIfWing();
            return spec != null && AbstractReverseEngineeringIndustry
                    .getProducedSet(Ids.PRODUCED_FIGHTERS_MEMORY).contains(spec.getId());
        }
        return false;
    }

    /**
     * True when this ship's hull has already been reverse-engineered here. A "(D)"
     * hull is judged by its base: a Hammerhead (D) counts as done once a Hammerhead
     * has been reverse-engineered, since the hub produces the base hull, not the
     * D-mod skin. Uses the same normalization the ship industry produces with, so the
     * refusal and the product always agree.
     */
    private boolean alreadyReverseEngineered(FleetMemberAPI member) {
        if (member == null || member.getVariant() == null) {
            return false;
        }
        String hullId = AbstractReverseEngineeringIndustry.reverseEngHullId(member.getVariant().getHullSpec());
        return hullId != null
                && AbstractReverseEngineeringIndustry.getProducedSet(Ids.PRODUCED_SHIPS_MEMORY).contains(hullId);
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, SubmarketPlugin.TransferAction action) {
        return true;
    }
}
