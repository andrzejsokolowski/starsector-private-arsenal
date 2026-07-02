# Changelog

All notable changes to Reverse Engineered Private Arsenal are documented here.

## 1.2.0

### Added
- **Sell reverse-engineered ships in the fleet screen (Tier 3).** New LunaSettings
  toggle `Sell reverse-engineered ships in fleet screen (Tier 3)`
  (`repa_re_sell_ships_in_fleet`), **off by default**. When enabled, a Tier 3 hub's
  Private Arsenal stocks the ships it has reverse-engineered and lets you buy copies
  directly in the fleet screen, skipping the usual Heavy Industry build step. When
  off, ships are not stocked and the fleet-screen tab stays hidden (weapons and
  fighter wings are unaffected).

### Changed
- **Ships now require a story-point improvement to reverse-engineer.** A Tier 3 hub
  will only accept ship deposits and research ships once it has been improved with a
  story point. Weapons and fighter wings are unchanged. The storage submarket shows
  an explanatory message when a ship is refused for this reason, and the hub
  description notes the requirement.

### Fixed
- The Private Arsenal is now strictly buy-only for ships as well (selling ships to
  the arsenal is refused), matching the existing weapon/fighter behavior.
- Reverse-engineered ships no longer multiply in the Private Arsenal every time the
  colony view is opened. The stockpile now keeps exactly one copy of each ship and
  prunes any duplicates an earlier build accumulated (existing saves self-heal on the
  next visit).

## 1.1.1

### Fixed
- Save corruption caused by the arsenal submarket.

## 1.1.0

### Added
- Parallel research slots: the hub researches several items of each type at once,
  scaled by hub tier and item size.

## 1.0.0

- Initial release (forked and reworked): reverse-engineering hub with an integrated
  Private Arsenal submarket.
