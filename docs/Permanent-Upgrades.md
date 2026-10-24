# Permanent Campaign upgrades

The Shop's upper row sells three permanent upgrades in place of consumable items, each with three sequential purchase levels. The existing lower **Unlocks** row and shop controls are retained. A card shows the next level and its total effect; it becomes `MAX` after level 3. Each level costs 100, 250 and 500 Credits respectively (850 per completed upgrade).

| Upgrade | Level 1 | Level 2 | Level 3 |
| --- | --- | --- | --- |
| Hull | +1 maximum life | +2 maximum lives | +3 maximum lives |
| Engine | +1 movement unit/update | +2 movement units/update | +3 movement units/update |
| Reloader | 10% shorter shot interval | 20% shorter shot interval | 30% shorter shot interval |

These are provisional balance values in [`PermanentUpgrade`](../src/currency/PermanentUpgrade.java). Bonuses are totals relative to the selected ship's original stats. The game uses lives, not a separate HP pool. Purchases apply to the next Campaign, persist across restarts, and remain in effect when moving between stages or restoring stats after temporary effects. Infinite mode and custom levels retain their existing behavior.

Campaign clears now award 50 Credits on Easy, 100 on Normal and 200 on Hard, using the difficulty captured for that stage. Each attempt pays once and saves immediately; replaying a stage in a new run can earn another reward. Failed stages, incomplete waves, custom levels and Infinite mode do not grant this bonus. Enemy-type rewards remain pending coordination.

For normal play, run `./gradlew.bat run` on Windows or `sh ./gradlew run` on macOS/Linux. Clear Campaign stages, return to the Shop and spend the earned Credits. The preview below provides a separate starting balance for quick testing.

## Try it

With JDK 17 or newer, run:

```powershell
.\gradlew.bat previewUpgrades
```

On macOS/Linux: `sh ./gradlew previewUpgrades`. IntelliJ also has **Invaders - Upgrade Preview**.

1. Open **Shop**. The upper **Upgrades** row contains Hull, Engine and Reloader. Use Left/Right to select an upgrade and Space/Enter to buy the next level. Each new key press buys at most one level.
2. Return to the menu and start **Campaign** with any ship. Hull increases starting/max lives; Engine and Reloader increase movement and firing speed.
3. Exit and rerun the preview to check that purchased levels and the remaining balance are restored.

The preview keeps the existing lower-row unlock samples (Unlock A, Ship B and Ship C). These sample purchases record ownership only; actual ship products, prices and Hangar access rules are unchanged.

The preview starts with 3,000 Credits once and keeps saves under `build/upgrade-preview/`. Preview controls/audio/gameplay settings stay in memory and reset on exit; normal saves and preferences are separate. `clean` resets the preview and its test Credits; it does not reset normal development saves. Prices and the production balance are not changed by the preview.

## Integration and checks

Each level uses an ordinary permanent unlock ID, such as `upgrade.hull.1`. The existing asset save format is unchanged. Old item quantities are retained for compatibility, but consumable purchase and use services have been removed. Field pickups and temporary item effects remain with the Item System. Catalog prerequisites prevent skipping levels; rejected purchases do not charge or save. A failed save retains the purchase in memory, and **Retry save** writes the latest state without purchasing again.

If a reward save fails, the Credits remain available in memory. The stage-end dialog offers **Retry** or **Continue**; retries save the latest balance and purchases without paying the bonus again. Pending rewards survive new runs within the session if the balance is full and are retried after leaving the Shop. Quit through the main menu to retry outstanding saves before exiting.

`Core` captures one immutable `CampaignUpgrades` snapshot per run. `GameScreen` uses its max lives and initializes new ships through the existing baseline-stat setters. Existing constructors default to no upgrades.

```powershell
.\gradlew.bat check
```

Tests cover ordered purchases, insufficient funds, repeat/max purchases, save reload/retry, held input, both shop rows, all ship types, stage transitions, restoration after temporary stat changes, and Campaign clear rewards through the purchase/save flow. Run `build` for checks plus packaging.
