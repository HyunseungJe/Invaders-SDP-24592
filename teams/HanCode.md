# Team HanCode

## 1. Team Introduction

Requirements: 4. Currency System

Course: CSE2024 Software Development Practices

### Goals and Vision:

Our goal is to provide shared currency balances, gameplay rewards, purchases and persistent ownership, with integration for the HUD, Level Design, and Player & Enemy Ship Variety systems.

### Members:

| Member                      | Role                    | GitHub |
|:----------------------------|:------------------------| :--- |
| Seyun Oh                    | PM / TeamLeader         | https://github.com/ogaji |
| Khuvituguldur               | Developer               | https://github.com/tuugy-rvn |
| Isaac de Jesus Rojas Torres | Developer               | https://github.com/isaacrt54 |
| Joshua Hernández Ruiz       | Developer               | https://github.com/Jperf0 |
| Byambakhishig Anukhishig    | QA Tester               | https://github.com/Anukhishig |
| Byambakhishig Khishigjin    | Documentation           | https://github.com/hishigjinb-svg |
| Hyunseung Je                | Dev Lead / Collaborator | https://github.com/HyunseungJe |
| Minkyung Yeo                | Documentation           | https://github.com/yeominkyung |

---
## 2. Team Requirements

### Overall Requirement

Currency System. Our team is responsible for managing the in-game currency economy, including currency balances, gameplay rewards, persistence, purchases, and the ownership or unlock status of purchasable content.

---

## 3. Detailed Requirements

1. Balance Management  
   Manage the player's in-game currency balance independently from the player's score. Support balance queries, additions, and deductions while preventing negative balances, overspending, and invalid balance updates. Rejected operations must leave the balance unchanged.

2. Reward System  
   Define and apply currency reward amounts and drop rates for gameplay events such as enemy defeats and level completion. Use level-completion events and difficulty data from the Level Design System, and agreed enemy-defeat data from the Player & Enemy Ship Variety System.

3. Persistence  
   Save and load the player's currency-related state between game sessions. Validate loaded data before applying it, report persistence failures, and ensure that invalid data does not overwrite valid runtime or saved state.

4. Shop / Purchase System  
   Provide purchase-related operations that allow purchasable content to be acquired using in-game currency. Validate the player's available balance and the requested price before completing a purchase. A successful purchase must deduct the required currency, while a failed or rejected purchase must leave the player's balance and ownership state unchanged. HanCode also provides the Shop screen and purchase/save feedback, including a save retry that does not repeat the purchase.

5. Ownership Management  
   Track purchased permanent upgrades and other agreed unlockable content. Provide an interface that allows other systems to determine whether specific content has been purchased or unlocked. Ownership and unlock states must persist across game sessions.

---

## 4. Dependencies on Other Teams

1. Level Design System (Octopus):
   Use the existing `LevelCompletedEvent` with the completed level and difficulty. HanCode awards and saves the clear bonus after the Campaign stage screen finishes.

2. Player & Enemy Ship Variety (KimchiBaguette):
   Use the existing ship baseline-stat API for permanent Campaign upgrades. Coordinate stable enemy-type data for defeat rewards, and agree on ship prices and access rules before enabling paid unlocks.

3. Gameplay HUD (friends):
   HanCode provides `Core.getAssetHudSnapshot().getCredits()` for the balance plus pending rewards from finished stage screens. The current stage's clear bonus is excluded while its screen is active. The HUD team handles display and layout.

---

## 5. Implementation and Integration

| Area | Implemented components | Remaining integration |
| --- | --- | --- |
| Shop | [ShopScreen](../src/screen/ShopScreen.java), [ShopController](../src/shop/ShopController.java), Main Menu entry and startup loading of shared assets. Purchases report save failures separately and allow a save-only retry. | Agree on ship prices and access rules before enabling paid unlocks. |
| Permanent upgrades | HanCode implements [Hull, Engine and Reloader](../docs/Permanent-Upgrades.md) in the upper Shop row. Three purchase levels persist and apply to subsequent Campaign runs. | Review provisional prices and bonuses. |
| Rewards | [CampaignRewards](../src/engine/CampaignRewards.java) connects Campaign clears to [RewardSystem](../src/currency/RewardSystem.java), shared assets, saving and retry handling. | Coordinate and connect enemy-type rewards. |
| HUD | [AssetHudSnapshot](../src/hud/AssetHudSnapshot.java) exposes the Credits total without consumable slots. | Connect the Credits display with the HUD team. |

The production catalog contains the three permanent stat upgrades and lists all three ships as available without purchase. Default ship availability is not saved as ownership, and Hangar access rules are unchanged. Separate unlock samples remain in tests and previews. Consumable purchasing and use services have been removed, while legacy save quantities are retained for compatibility.
