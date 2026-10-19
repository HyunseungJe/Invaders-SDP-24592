# Team HanCode

## 1. Team Introduction

Requirements: 4. Currency System

Course: CSE2024 Software Development Practices

### Goals and Vision:

Our goal is to develop a robust, bug-free, and scalable currency system that feels rewarding, enhances the player's progression and seamlessly integrates with other gameplay mechanics. The system should integrate smoothly with other gameplay systems such as the HUD, item system, and enemy variety system.

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
   Define and apply currency reward amounts and drop rates for gameplay events such as enemy defeats and level completion. Level-completion events and difficulty data will be provided by the Level Design System, while enemy-defeat information will be provided by the Player & Enemy Ship Variety System.

3. Persistence  
   Save and load the player's currency-related state between game sessions. Validate loaded data before applying it, report persistence failures, and ensure that invalid data does not overwrite valid runtime or saved state.

4. Shop / Purchase System  
   Provide purchase-related operations that allow purchasable content to be acquired using in-game currency. Validate the player's available balance and the requested price before completing a purchase. A successful purchase must deduct the required currency, while a failed or rejected purchase must leave the player's balance and ownership state unchanged. HanCode also provides the Shop screen and purchase/save feedback, including a save retry that does not repeat the purchase.

5. Ownership Management  
   Track the ownership or unlock status of content acquired through purchases, such as items, upgrades, BGM, stages, or other future purchasable content. Provide an interface that allows other systems to determine whether specific content has been purchased or unlocked. Ownership and unlock states must persist across game sessions. For purchased consumables, provide a separate use service that consumes one unit on successful activation without charging currency again. Ordinary rejected uses leave stock unchanged.

---

## 4. Dependencies on Other Teams

1. Level Design System:  
   We depend on this team to trigger and broadcast a 'Level Completed' event containing the level ID and the difficulty data required to calculate and award the appropriate end-of-level currency reward.

2. Item System:  
   Coordinate purchasable item identifiers and prices with the Item System, which provides effect behavior and a supported activation interface. HanCode provides purchase and consumable-use operations and manages owned quantities. Effect application, timers, and field pickups remain with the Item System.

3. Player & Enemy Ship Variety:  
   We depend on this team to broadcast an 'Enemy Defeated' event containing the enemy information required to determine the appropriate drop rate and reward amount.

---

## 5. Implementation and Integration

| Area | Implemented components | Remaining integration |
| --- | --- | --- |
| Shop | [ShopScreen](../src/screen/ShopScreen.java), [ShopController](../src/shop/ShopController.java), and startup loading of shared assets. Purchases report save failures separately and allow a save-only retry. | Frenchies connects the supplied screen to its existing Main Menu Shop entry and coordinates the return path. |
| Consumable use | [ItemUseService](../src/currency/ItemUseService.java) uses registered effect adapters through [ConsumableActivation](../src/currency/ConsumableActivation.java). Ordinary rejection restores reserved stock; unexpected adapter failures block the service until the state is reconciled and the service is replaced. | Connect actual effects and gameplay input. Agree on repeat-use rules and saving after consumption with the relevant teams. |
| Rewards | [RewardSystem](../src/currency/RewardSystem.java) accumulates rewards and settles them into the shared assets. | Coordinate settlement, saving, and failure recovery, then connect gameplay events and stage exit handling. |

The production Shop catalog remains empty until product IDs and prices are agreed with the content teams. Sample products are used only in tests and the preview. These components do not yet constitute a complete gameplay purchase/use/reward flow.
