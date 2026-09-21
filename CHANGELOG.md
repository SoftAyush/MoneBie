# Changelog

All notable changes to Aurex are recorded here.

## 1.1.0 — 2026-09-20

### Added

- **Budgets.** Set a monthly limit on any spending category and see how much
  of it is left as the month goes on. The dashboard shows the three that most
  need attention; the Budgets screen shows all of them and pages back through
  earlier months. Spent figures are measured from the ledger, never stored.
- **Recurring transactions.** Rent, salary, subscriptions and transfers that
  repeat weekly, fortnightly, monthly, quarterly or yearly. Each occurrence is
  posted to your history on its day, on launch and by a quiet daily check.
  Schedules can be paused, skipped, posted early, edited or removed without
  touching anything already recorded. Month-end schedules stay on the last
  day of short months and return to their day afterwards.
- **App lock.** A 4–6 digit PIN, with fingerprint or face unlock where the
  phone supports it, and a grace period (immediately, 1, 5 or 15 minutes)
  before the lock asks again. The PIN is stored only as a salted hash on this
  phone and never leaves it.
- **CSV import.** Bring transactions in from the file Aurex exports, or any
  CSV with Date, Type, Amount and Account columns. Accounts are matched by
  name and anything unfamiliar is mapped by hand before a single row is
  written; categories the phone does not have are created; likely duplicates
  are flagged and skipped by default.

### Changed

- Cloud backup now carries budgets and recurring schedules. Backups made by
  1.0.0 restore unchanged; backups made by 1.1.0 restore into 1.0.0 without
  the new tables.
- Transactions posted by a recurring schedule can be edited and deleted like
  hand-entered ones, unlike debt, EMI and loan payments which stay owned by
  their record.
- Database schema moves from version 9 to 10 (two new tables, no changes to
  existing rows).

### Fixed

- The action pill on empty states ("Add a transaction", "Add an account",
  "Set a budget"...) was drawn but not tappable.

### Housekeeping

- `versionCode` 2, `versionName` 1.1.0.
- Android Gradle Plugin 9.4.1.

## 1.0.0

First release: accounts, transactions, categories, debts, EMIs, borrowing and
lending, upcoming payments and reminders, spending insights, CSV export,
cloud backup and restore, light/dark/system theme.
