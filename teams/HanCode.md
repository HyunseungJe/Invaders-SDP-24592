# Team HanCode

## 1. Team Introduction

Requirements: 4. Currency System

Course: CSE2024 Software Development Practices

### Goals and Vision:

Our goal is to develop a robust, bug-free, and scalable currency system that feels rewarding, enhances the player's progression and seamlessly integrates with other gameplay mechanics. The system should integrate smoothly with other gameplay systems such as the HUD, item system, and enemy variety system.

### Members:

| Member                      | Role | GitHub |
|:----------------------------| :--- | :--- |
| Seyun Oh                    | PM / TeamLeader | https://github.com/ogaji |
| Khuvituguldur               | Developer | https://github.com/tuugy-rvn |
| Isaac de Jesus Rojas Torres | Developer | https://github.com/isaacrt54 |
| Joshua Hernández Ruiz       | Developer | https://github.com/Jperf0 |
| Anukhishig                  | Documentation | https://github.com/Anukhishig |
| Byambakhishig Khishigjin    | QA Tester | https://github.com/hishigjinb-svg |
| Hyunseung Je                | Dev Lead / Collaborator | https://github.com/HyunseungJe |
| Minkyung Yeo                | Documentation | https://github.com/yeominkyung |

---

## 2. Team Requirements

### Overall Requirement:

Currency System. Our team is responsible for managing the logic, balance, and persistence of the in-game currency earned by players during gameplay.

---

## 3. Detailed Requirements

1. Implement a core currency class to add, deduct, and track balances
   independently from the player's score. Keep balances non-negative.

2. Define reward amounts and drop rates for enemy defeats or level completion.
   Prevent duplicate rewards from the same event. The level-completion events
   and level difficulty data will be provided by the Level Design System.

3. Save and load the currency balance between game sessions. Validate loaded
   values and report failures without replacing valid balances or save data
   with invalid data.

4. Provide an interface for balance queries and purchase-related deductions.
   Ensure that currency is permanently deducted only when the corresponding
   item purchase is successfully completed.

5. Prevent overspending and invalid balance updates. Rejected operations
   must leave the balance unchanged. Test normal transactions,
   insufficient funds, and invalid input handling.

---

## 4. Dependencies on Other Teams

1. Level Design System: We depend on this team to trigger and broadcast a
   'Level Completed' event containing the level ID and the difficulty data
   required to calculate and award the correct end-of-level currency bonus.

2. Item System: We depend on this team to send valid purchase requests with
   accurate item prices. We also require a definitive success/failure status
   for item delivery so that currency is permanently deducted only when the
   purchase is successfully completed.

3. Player & Enemy Ship Variety: We depend on this team to broadcast an
   'Enemy Defeated' event that includes the enemy type or ID. We need this
   information to apply our drop-rate tables and award the correct amount of
   currency for each defeat.