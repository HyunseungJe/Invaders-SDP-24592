# The Diversity Hires

## 1. Team Introduction

**Team Name:** The Diversity Hires

**Team Github** : [The Diversity Hires](https://github.com/AymericGe/The-Diversity-Hires)

**Team Focus:** Visual Effect System

**Vision:**
We aim to bring the Space Invaders remake to life visually — making every explosion, hit, and menu screen feel satisfying, readable, and polished, while keeping our effects lightweight enough not to hurt performance for the rest of the game.

**Team Roles:**

| Role | Member                                                  | Responsibility |
|---|---------------------------------------------------------|---|
| Team Leader | [Aymeric Geron](https://github.com/AymericGe/AymericGe) | Coordinates tasks, tracks progress, communicates with other teams, manages the GitHub board/PRs |
| Particle Effects Lead | [Junlin Chan](https://github.com/jiyurin)               | Builds and tunes explosion particle systems |
| Color & Shader Artist | [Helena Ding](https://github.com/helena-ding), [Liya Aklil](https://github.com/aklil24)         | Defines color palettes, lighting/flash effects, shader-based visuals |
| Background Artist | [ZHUMAKHMETOV MIKHAIL](https://github.com/mdsn13)       | Designs and implements background art/parallax scrolling |
| UI/Icon Designer | [Oluwadamilola Tinubu](https://github.com/DamiT123)     | Creates new icons and visual assets used across the game |
| QA & Integration Lead | [Zhang ZEWEI](http://github.com/qingjiu-hash)           | Tests effects in-game, checks performance, integrates with other teams' systems |

## 2. Team Requirements

Our team is responsible for the **Visual Effect System** — all non-audio visual feedback in the game, including particle effects, color treatments, background art, UI iconography, and menu/homepage layout. Our goal is to make gameplay feel more dynamic and readable, and to give the game a cohesive visual identity across all screens.

## 3. Detailed Requirements

1. **Explosion Particles**
*1.1 Enemy Explosion*
• Display an explosion at the enemy's exact position when it is destroyed.
• Generate 10–15 small particles that spread in different directions.
• Use orange, yellow, and red particles.
• Make the particles disappear within 0.5 seconds.
*1.2 Player Explosion*
• Trigger a larger explosion when the player's spaceship is destroyed.
• Generate 20–30 particles spreading outward.
• Display the explosion for approximately 1 second.
• Remove all particles after the animation ends.

2. **Color System** 
*2.1 Game Color Palette*
• Define a shared color palette for the entire game.
• Use dark blue or black for backgrounds.
• Use red and orange for damage and explosions.
• Use green or blue for positive effects and power-ups.
• Store color definitions in one reusable location.
*2.2 Player Damage Feedback*
• Make the player's spaceship flash red immediately after taking damage.
• Alternate between its normal appearance and red for 0.5 seconds.
• Restore the original appearance automatically.
*2.3 Power-Up Highlight*
• Add a colored glow around active power-ups.
• Use different colors to distinguish different power-up types.
• Remove the glow when the power-up expires.

3. **Background Effects**
*3.1 Dynamic Background*
• Replace the plain black background with a moving starfield.
• Use multiple layers of stars moving at different speeds to create a sense of depth.
• Keep the background moving during menus and countdowns, not just active gameplay.
• Make the background feel slightly more intense on higher levels to reinforce rising difficulty.
• Example: the background is calm and slow on early levels, and noticeably faster and busier on later, harder levels.

4. **Icon Set**
*4.1 UI Icons*
• Design a small icon for the score display instead of using plain text alone.
• Design a distinct life/heart icon separate from the player's ship sprite, so the HUD doesn't reuse gameplay art.
• Design an icon for the currency/coin system.
• Design icons for pause and settings functions.
• Example: the score in the corner shows a small coin or star icon next to the number, and lives are shown as heart icons instead of tiny ship copies.

## 4. Dependencies on Other Teams

1. **Sound Effects/BGM Team** — Need audio cues synced to our visual effects (e.g., explosion sound timed with particle burst) for combined feedback.
2. **Gameplay HUD Team** — Need event triggers/hooks (e.g., damage taken, score change) so our visual effects can fire at the correct moments.
3. **Player & Enemy Ship Variety Team** — Need finalized ship sprites/states so we can apply visual effects (e.g., explosions, damage flashes) accurately to each ship type.

# Team Git Workflow Plan

## 1. Workflow and Rationale

**Chosen workflow : Forking workflow.**

Our workflow is the forking workflow. It’s the one that works best for our team. All the developers on our team work locally on their personal computers. These versions are then pushed and merged into the AymericGe/The-Diversity-Hires page, which is our team page. All changes are then sent via a pull request to the main project: oh-gnues/Invaders-SDP-24592

## 2. The Branch Strategy

Each developer creates a fork of the team repository (AymericGe/The-Diversity-Hires). Each developer works on their fork in their personal workspace. They make their code changes. Then, the team developers submit a pull request to the team repository. The team then merges the code from the pull request. Once this is done, the team leader submits a pull request to the main repository (oh-gnues/Invaders-SDP-24592).
It is a three-tier architecture: the global repository, the team repository, and the developers' personal forks.

## 3. Committing Rules

- A commit should represent **one logical change** — e.g. one bug fix, one small feature step, or one refactor. Avoid mixing unrelated changes (e.g. a UI fix and a backend refactor) in the same commit.
- Commit message format follows **Conventional Commits**:

  ```
  <type>(<optional scope>): <short summary>

  <optional longer description>
  ```

  Types used: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `style`.

  Examples:
  - `feat(auth): add password reset flow`
  - `fix(api): handle empty response from user endpoint`
  - `docs: update setup instructions in README`

- Summary line is written in the imperative mood ("add", not "added"/"adds"), kept under ~72 characters, with more detail in the body if needed.
- Commit early and often on your feature branch — commits don't need to be squashed until merge (see Section 5).

## 4. Pull Request and Code Review Rules

**When a pull request is opened**
- A PR is opened when a developer did a change on their own personal fork.
- The PR description states what the change does and links the related task/issue.

**Review and approval conditions before merging**
- At least **one approval** from a teammate other than the author is required before a PR can be merged. No self-approval.
- Reviewers check: the code works as intended, is reasonably readable, and follows the conventions in this document.
- Reviewers leave comments within 24 hours of a PR being opened where possible; the author addresses comments before merge.
- Any CI checks (build/tests, once set up) must pass before merging.

**Direct pushes to `main`**
- **Not allowed.** `main` is a protected branch — every change, including small fixes like typos, must go through a PR.

## 5. The Merge Strategy

**Method: Squash and Merge**, used for all PRs into `main`. Each feature/fix collapses into a single, clean commit on `main`, using the PR title (written in Conventional Commit format) as the commit message.

- Why squash: our feature branches accumulate small "WIP"/fixup commits during development. Squashing keeps `main`'s history readable as one entry per feature/fix, while the full commit history is still visible on the closed PR for reference.

**Conflict resolution**
- If a conflict touches code owned by another teammate, the author contacts them directly to resolve it together rather than guessing at intent.
- PR should be made regularly (at least before requesting review) to minimize last-minute conflicts.

## 6. Overall Development Workflow

**Step by step:**
1. Pick up a task from the team board.
2. Do the edits on you own fork.
3. Commit small, logical changes following the format in Section 3.
4. Open a PR (draft if work-in-progress).
5. Get at least one teammate's approval; address review comments.
6. Resolve any conflicts with `main` before merging.
7. Squash and merge into `main`
8. Pull the latest `main` locally before starting the next task.

## Additional Team Rules

- No force-pushing to `main`, ever.
- Broken builds on `main` are treated as top priority — whoever caused it fixes it immediately or reverts the merge.
- Any change to this workflow document must itself go through a PR and be agreed on by the whole team.
