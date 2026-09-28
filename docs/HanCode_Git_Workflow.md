# HanCode Git Workflow

This document defines the Git workflow used within the **HanCode team repository**. Its purpose is to ensure that all team members follow the same development, review, and integration process.

---

## 1. Selected Git Workflow and Rationale

Our team uses a combination of the following workflows:

- **Centralized Workflow**
- **Feature Branch Workflow**
- **GitHub Flow**

All team members collaborate through a single shared **team repository**. Each feature, bug fix, or documentation task is developed on a separate short-lived branch rather than directly on `main`. Completed work is integrated into `main` through a Pull Request (PR), automated checks, and code review.

This approach is suitable for our project because it:

- allows multiple team members to work in parallel without directly interfering with one another,
- keeps unfinished work isolated from `main`,
- provides a clear review process before integration,
- allows automated build and test checks to validate changes before merge,
- keeps the `main` branch stable and easy to understand.

The team repository is the center of collaboration. The workflow described in this document focuses on how individual team members contribute to that repository.

---

## 2. Branch Strategy

### 2.1 `main`

`main` is the stable integration branch of the team repository.

Rules:

- `main` should always contain integrated and reviewed work.
- Normal development must not be performed directly on `main`.
- New working branches must be created from the latest `main`.
- Changes should normally enter `main` only through Pull Requests.
- Direct pushes and force pushes to `main` are prohibited during normal development.

### 2.2 `feature/*`

Used for implementing new features.

Examples:

```text
feature/currency-system
feature/reward-system
feature/save-load
```

Lifecycle:

1. Create the branch from the latest `main`.
2. Implement the feature and commit logical changes.
3. Push the branch to the team repository.
4. Open a Pull Request to `main`.
5. Merge after all required checks and review conditions are satisfied.
6. Delete the branch after the PR is merged.

### 2.3 `fix/*`

Used for bug fixes or corrections to existing behavior.

Examples:

```text
fix/currency-negative-balance
fix/save-load-error
```

The lifecycle is the same as for `feature/*` branches.

### 2.4 `docs/*`

Used for documentation-only changes.

Examples:

```text
docs/team-document
docs/git-workflow
```

The lifecycle is the same as for other working branches.

### 2.5 Branch Lifetime

Working branches should be short-lived and focused on one task or one coherent change. Unrelated work should not be mixed into the same branch.

---

## 3. Commit Rules

### 3.1 Commit Scope

Each commit should represent **one logical change**.

Good examples:

```text
feat: add CurrencyBalance class
fix: reject invalid currency deductions
test: add unit tests for CurrencyBalance
docs: update team roles
```

Avoid commits that mix unrelated changes or contain several independent tasks at once.

### 3.2 Commit Message Format

The team follows a simplified **Conventional Commits** format:

```text
<type>: <short description>
```

Allowed types:

| Type | Purpose |
|---|---|
| `feat` | Add a new feature |
| `fix` | Fix a bug |
| `refactor` | Restructure code without changing intended behavior |
| `test` | Add or modify tests |
| `docs` | Modify documentation |
| `chore` | Maintenance, configuration, or other non-feature work |

Examples:

```text
feat: add reward system
fix: prevent negative currency balance
refactor: simplify purchase validation
test: add CurrencyBalance unit tests
docs: update Git workflow
```

Commit descriptions should be short, specific, and written so that the purpose of the change is clear from the history.

---

## 4. Pull Request and Code Review Rules

### 4.1 When to Open a Pull Request

A Pull Request should normally be opened when the intended task is complete and ready for review.

Before opening a PR, the author should verify that:

- the intended task has been implemented,
- the project builds successfully,
- relevant tests pass,
- unnecessary debug code has been removed,
- the branch does not contain unrelated changes.

A **Draft Pull Request** may be opened earlier when early feedback or discussion is useful.

### 4.2 Pull Request Description

Each Pull Request should clearly explain:

- **What changed**
- **Why it changed**
- **How it was tested**

### 4.3 Review and Approval Conditions

A Pull Request may be merged only when all of the following conditions are satisfied:

- automated build checks pass,
- automated tests pass,
- at least **one other team member** approves the PR,
- unresolved review comments have been addressed,
- no unresolved merge conflict remains.

The PR author's own approval does not count toward the required approval.

Reviewers should focus on:

- correctness of the implementation,
- consistency with the agreed design,
- logical errors,
- possible regressions,
- readability and maintainability.

Automated checks should handle mechanical validation such as build and test execution whenever possible.

### 4.4 Direct Pushes to `main`

Direct pushes to `main` are prohibited during normal development.

The only exceptions are cases where a conflict must be resolved during:

1. synchronization between the upstream repository and the team repository, or
2. integration of the team repository into the upstream repository.

These exceptional operations are handled by the **Dev Lead**, or by another team member explicitly delegated by the Dev Lead when the Dev Lead is unavailable or unable to perform the resolution.

---

## 5. Merge Strategy

### 5.1 Default PR Merge Method: Squash and Merge

All normal Pull Requests from working branches into `main` use **Squash and Merge**.

A working branch may contain several development commits, but they are combined into one logical commit when merged into `main`.

Example:

```text
feature/currency-system
  ├─ feat: add CurrencyBalance class
  ├─ fix: correct validation logic
  ├─ test: add CurrencyBalance tests
  └─ refactor: simplify deduction logic
```

After Squash and Merge:

```text
main
  └─ feat: implement currency balance system (#PR)
```

This keeps the `main` history concise and organized around completed tasks or features.

### 5.2 Rebase Rules

Rebase may be used **only on a personal working branch**.

A developer may rebase a personal branch onto the latest `main` when they want to update the branch before integration.

Example:

```bash
git fetch origin
git rebase origin/main
```

Rules:

- Rebase must not be performed on `main`.
- Rebase must not be performed on a branch that is actively shared by multiple developers.
- History rewriting on `main` is prohibited.
- Force pushes to `main` and shared branches are prohibited.
- If a personal branch has already been pushed and must be updated after a rebase, `git push --force-with-lease` may be used only on that personal branch.

### 5.3 Regular Merge

Regular merge commits are not used for normal internal Pull Requests. Internal PRs use **Squash and Merge**.

When synchronizing with the upstream repository, preserving upstream history may require a regular merge depending on the synchronization situation. Such integration is handled under the upstream conflict rules below.

### 5.4 Merge Conflict Resolution

#### Individual Pull Request Conflicts

Merge conflicts in an individual Pull Request are the responsibility of the **PR author**.

The PR author must:

1. update the branch with the latest required changes,
2. resolve the conflict,
3. rebuild and retest the affected code,
4. push the resolved branch,
5. complete the normal review process.

If the conflict involves code primarily maintained by another member, the PR author may coordinate with that member, but the PR author remains responsible for completing the resolution.

#### Upstream Synchronization and Upstream PR Conflicts

Merge conflicts that occur during:

- synchronization between the upstream repository and the team repository, or
- integration of the team repository into the upstream repository

are primarily the responsibility of the **Dev Lead**.

If the Dev Lead is unavailable or unable to resolve the conflict, the Dev Lead may delegate the task to another team member. The delegated member then performs the resolution on behalf of the team.

After any merge conflict is resolved, the affected code must be built and tested again before integration continues.

---

## 6. Overall Development Workflow

The standard workflow for a team member is:

1. Select or receive a task.
2. Update the local `main` branch.
3. Create a new working branch from the latest `main`.
4. Implement the task.
5. Commit logical changes using the agreed commit format.
6. Build and test the changes locally.
7. Push the working branch to the team repository.
8. Open a Pull Request to `main`.
9. Wait for automated build and test checks.
10. Receive code review from at least one other team member.
11. If changes are requested, update the same branch and push additional commits.
12. Resolve any merge conflicts if necessary.
13. After all checks pass and at least one approval is received, use **Squash and Merge**.
14. Delete the completed working branch.

### Workflow Diagram

```mermaid
flowchart TD
    A[Select or Receive Task] --> B[Update Local main]
    B --> C[Create Working Branch]
    C --> D[Implement Changes]
    D --> E[Commit Logical Changes]
    E --> F[Build and Test Locally]
    F --> G[Push Branch]
    G --> H[Open Pull Request]
    H --> I[Automated Build and Tests]

    I -->|Failed| D
    I -->|Passed| J[Code Review]

    J -->|Changes Requested| D
    J -->|Approved| K{Merge Conflict?}

    K -->|Yes| L[PR Author Resolves Conflict]
    L --> F

    K -->|No| M[Squash and Merge]
    M --> N[Delete Working Branch]
    N --> O[Updated main]
```

---

## 7. Summary of Team Rules

- All team members collaborate through the shared team repository.
- Development is performed on short-lived `feature/*`, `fix/*`, or `docs/*` branches.
- Normal development directly on `main` is prohibited.
- Each commit should represent one logical change.
- Commit messages follow the `<type>: <description>` format.
- Every normal change to `main` must go through a Pull Request.
- At least one other team member must approve a PR before merge.
- Automated build and tests must pass before merge.
- Normal PRs use **Squash and Merge**.
- Rebase is allowed only on personal working branches.
- Rebase and force push on `main` are prohibited.
- Individual PR conflicts are resolved by the PR author.
- Upstream synchronization and upstream PR conflicts are handled by the Dev Lead, or by a team member explicitly delegated by the Dev Lead.

