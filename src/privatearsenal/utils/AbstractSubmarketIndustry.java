package privatearsenal.utils;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

/**
 * Base industry that owns one or more player-colony submarkets and keeps them
 * (and their cargo) alive across the industry's lifecycle.
 *
 * The submarket objects are stashed in the market's memory rather than in a
 * per-instance field, so they survive a tier upgrade - which replaces the
 * industry instance entirely. This is what fixes deposited items vanishing when
 * the Reverse Engineering Hub is upgraded.
 */
public abstract class AbstractSubmarketIndustry extends BaseIndustry {
    protected String submarketId;
    protected String factionSubMarketID;

    private static final String STASH_PREFIX = "$repa_savedSub_";

    public AbstractSubmarketIndustry(String submarketId, String factionSubMarketID) {
        this.submarketId = submarketId;
        this.factionSubMarketID = factionSubMarketID;
    }

    /**
     * The submarkets this industry manages, as {submarketId, factionColourId}
     * pairs. Built in code (never restored from save state) so it is always
     * correct after a load or upgrade.
     */
    protected List<String[]> getManagedSubmarkets() {
        List<String[]> list = new ArrayList<String[]>();
        list.add(new String[] { submarketId, factionSubMarketID });
        return list;
    }

    @Override
    public void apply() {
        super.apply(true);

        if (isFunctional() && market.isPlayerOwned()) {
            for (String[] s : getManagedSubmarkets()) {
                ensureSubmarket(s[0], s[1]);
            }
        } else if (market.isPlayerOwned()) {
            for (String[] s : getManagedSubmarkets()) {
                stashAndRemove(s[0]);
            }
        }

        if (!isFunctional()) {
            unapply();
        }
    }

    @Override
    public void unapply() {
        super.unapply();

        if (market.isPlayerOwned()) {
            for (String[] s : getManagedSubmarkets()) {
                stashAndRemove(s[0]);
            }
        }
    }

    /**
     * Adds the submarket if it is missing. Restores the previously stashed
     * submarket object (cargo intact) when one exists; only builds a fresh,
     * empty submarket when there is nothing to restore.
     */
    private void ensureSubmarket(String subId, String colourId) {
        if (market.getSubmarket(subId) != null) {
            return;
        }

        SubmarketAPI stashed = getStashed(subId);
        if (stashed != null) {
            market.addSubmarket(stashed);
            clearStashed(subId);
        } else {
            market.addSubmarket(subId);
            SubmarketAPI sub = market.getSubmarket(subId);
            sub.setFaction(Global.getSector().getFaction(colourId));
            Global.getSector().getEconomy().forceStockpileUpdate(market);
        }
    }

    /** Keeps the live submarket (and its cargo) in market memory, then detaches it. */
    private void stashAndRemove(String subId) {
        SubmarketAPI open = market.getSubmarket(subId);
        if (open != null) {
            market.getMemoryWithoutUpdate().set(STASH_PREFIX + subId, open);
            market.removeSubmarket(subId);
        }
    }

    private SubmarketAPI getStashed(String subId) {
        Object o = market.getMemoryWithoutUpdate().get(STASH_PREFIX + subId);
        return (o instanceof SubmarketAPI) ? (SubmarketAPI) o : null;
    }

    private void clearStashed(String subId) {
        market.getMemoryWithoutUpdate().unset(STASH_PREFIX + subId);
    }
}
