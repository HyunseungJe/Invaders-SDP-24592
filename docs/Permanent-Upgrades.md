# Permanent Campaign upgrades

The Shop offers three upgrades, each with three sequential purchase levels. A card shows the next level and its total effect; it becomes `MAX` after level 3. Each level costs 100, 250 and 500 Credits respectively (850 per completed upgrade).

| Upgrade | Level 1 | Level 2 | Level 3 |
| --- | --- | --- | --- |
| Hull | +1 maximum life | +2 maximum lives | +3 maximum lives |
| Engine | +1 movement unit/update | +2 movement units/update | +3 movement units/update |
| Reloader | 10% shorter shot interval | 20% shorter shot interval | 30% shorter shot interval |

These are provisional balance values in [`PermanentUpgrade`](../src/currency/PermanentUpgrade.java). Bonuses are totals relative to the selected ship's original stats. The game uses lives, not a separate HP pool. Purchases apply to the next Campaign, persist across restarts, and remain in effect when moving between stages or restoring stats after temporary effects. Infinite mode and custom levels retain their existing behavior.

The current main-based implementation does not connect gameplay currency rewards. Existing balances can buy upgrades; use the isolated preview to try the full purchase-to-gameplay path.

## Try it

With JDK 17 or newer, run:

```powershell
.\gradlew.bat previewUpgrades
```

On macOS/Linux: `sh ./gradlew previewUpgrades`. IntelliJ also has **Invaders - Upgrade Preview**.

1. Open **Shop**. Use Left/Right to select an upgrade and Space/Enter to buy the next level. Each new key press buys at most one level.
2. Return to the menu and start **Campaign** with any ship. Hull increases starting/max lives; Engine and Reloader increase movement and firing speed.
3. Exit and rerun the preview to check that purchased levels and the remaining balance are restored.

The preview starts with 3,000 Credits once and keeps saves under `build/upgrade-preview/`. Preview controls/audio/gameplay settings stay in memory and reset on exit; normal saves and preferences are separate. `clean` resets the preview and its test Credits; it does not reset normal development saves. Prices and the production balance are not changed by the preview.

## Integration and checks

Each level uses an ordinary permanent unlock ID, such as `upgrade.hull.1`. The existing asset save format is unchanged. Catalog prerequisites prevent skipping levels; rejected purchases do not charge or save. A failed save retains the purchase in memory, and **Retry save** writes the latest state without purchasing again.

`Core` captures one immutable `CampaignUpgrades` snapshot per run. `GameScreen` uses its max lives and initializes new ships through the existing baseline-stat setters. Existing constructors default to no upgrades.

```powershell
.\gradlew.bat check
```

Tests cover ordered purchases, insufficient funds, repeat/max purchases, save reload/retry, held input, all ship types, stage transitions and restoration after temporary stat changes. Run `build` for checks plus packaging.
