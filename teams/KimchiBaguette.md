# KimchiBaguette

## Team Introduction
Our team's goal is to diversify the player and enemy ship systems in Space Invaders. We aim to implement selectable player ships with unique stats, varied enemy movement and attack patterns, and a bonus system (such as extra lives and shields) as well as a penalty system (such as obstacles).

## Members
| Name | Role | GitHub |
| --- | --- | --- |
| Julie Znaor | Team leader & Enemy Ship&Pattern Developer | [Julie Znaor](https://github.com/JulieZnaor) |
| Felicie Pene | Weapon Developer & projectile developer 1| [Felicie Pene](https://github.com/feliciepene) |
| Lena Matejic | player ship Developer | [Lena Matejic](https://github.com/Lenamtj) |
| BRAMI Édouard | Enemy Movement Developer | [BRAMI Édouard](https://github.com/EdouardBrami) |
| Yongwon Kim | Collaborator & Player Status&Buff/Debuff 1 | [Yongwon Kim](https://github.com/YitWub/YitWub) |
| Chaemin Oh | Boss System Developer 1| [Chaemin Oh](https://github.com/Littlecocoa/Littlecocoa) |
| Jiye Kwon | Player Status&Buff/Debuff 2| [Jiye Kwon](https://github.com/kwon-jiye/kwon-jiye.git) |
| Yoon-ah Ji | Boss System Developer 2 | [Yoon-ah Ji](https://github.com/jiyoonah/jiyoonah) |
| Junah noh | Weapon Developer & projectile developer 2| [Junah noh](https://github.com/noh1216) |


## Team Requirements
We are developing the **Player & Enemy Ship Variety** system of the Space Invaders game. Our team is responsible for giving players a choice of ships with distinct stats, giving enemies varied movement and attack patterns (including bosses), and adding bonus and penalty elements that change the moment-to-moment gameplay.

## Detailed Requirements
* Multiple types of enemy ships using various attack methods (long-range attacks, multi-shot, etc.)
* Player ships selectable on a pre-game interface, each with different attributes (movement speed, fire rate, number of lives, etc.)
* Bosses with varying difficulty levels and diverse projectile attacks
* Multiple types of enemy ships with distinct movement trajectories; making movement tempo and direction irregular.
* Bonus elements (extra lives, 3-second shields, etc.) and penalty elements (obstacles, etc.)

## Dependencies on Other Teams
* Team 3 (Level Design System): The Level Design team needs integration support to place and spawn the new enemy ship types and mini-bosses within enemy waves and each level's configuration.
* Team 8 (Gameplay HUD): The Gameplay HUD team needs to be able to display UI tailored to the player's selected ship (e.g., remaining health/shield gauge for tanker-type ships, the selected ship's icon, and special ability gauge display).
* Team 5 (Visual Effect System): The Visual Effect team needs to support customized visual effects per ship, such as engine thrust effects (booster particles) and mini-boss-specific explosion and hit animations.

## Git Workflow
The team's Git workflow — branch strategy, commit rules, pull request and review
rules, merge strategy and the overall process diagram — is documented separately
in [`docs/KimchiBaguette_Git_Workflow.md`](../docs/KimchiBaguette_Git_Workflow.md).
