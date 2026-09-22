# Cricket Auction Mobile App

A modern, full-featured native Android app for conducting real-time cricket auctions, built with **Jetpack Compose**, **Kotlin**, and **Room Database**.

---

## 🏏 Clean Slate Design (Zero Prebuilt Data)

As required, this app **does not contain any prebuilt tournaments, teams, or players**. Everything starts completely empty, giving users 100% control to configure and conduct auctions for any tournament, club, corporate league, or fantasy draft:

- **User-Defined Tournaments**: Configure custom tournament names, seasons, team purse limits (e.g. ₹100 Cr, 50 Cr, points, USD), min/max squad sizes, overseas player limits, and default base prices.
- **User-Defined Teams**: Add participating teams with custom names, short codes, custom branding colors (12-palette picker), owner names, and custom team purse budgets.
- **User-Defined Players**: Add players individually with role (Batsman, Bowler, All-Rounder, Wicket Keeper), batting/bowling styles, overseas status, nationality, tier/set, base price, and notes.
- **Batch Import Support**: Easily paste entire player lists (CSV / line-by-line format) to import full squads in a single tap without tedious manual form filling.

---

## ⚡ Key Features & Capabilities

### 1. 🏟️ Tournament Hub & Manager
- Create multiple tournaments with independent rules, budgets, and squads.
- Custom currency formats supported:
  - **₹ Crores & Lakhs** (e.g., ₹20 L, ₹1.5 Cr, ₹100 Cr)
  - **Points (Pts)** (e.g., 1,500 Pts)
  - **USD ($)** (e.g., $500K, $2M)
- Flexible squad rules: minimum required squad size, maximum squad cap, and overseas player quota.

### 2. 👥 Teams Management
- Interactive 12-color palette picker for team branding.
- Automatic team initials / short code generation.
- Dynamic purse tracking with real-time balance updates.
- Delete and edit team configurations at any time.

### 3. 🏃 Player Auction Pool
- Single player form with role badges and styles.
- **Batch Import**: paste multiple players formatted as `Name, Role, Base Price, Overseas/Country` (with built-in sample loader).
- Real-time search bar (by player name, country, or tier).
- Filter chips: `All`, `Batsman`, `Bowler`, `All-Rounder`, `Wicket Keeper`, `Overseas`, `Upcoming`, `Sold`, `Unsold`.
- "Auction Now" button to spotlight any player immediately.

### 4. 🔨 Live Auction Arena (Real-Time Bidding Room)
- **Spotlight Player Card**: Displays player name, category, role badge, batting/bowling style, and base price.
- **Hero Bid Display**: Prominent animated banner highlighting the current leading bid and leading team.
- **Dynamic Increments**: Smart increment buttons that adapt as bidding increases (+10L, +20L, +50L, +1 Cr, etc.) plus custom bid input dialog.
- **Interactive Team Paddles**: Tap any team card to place a bid instantly. Smart validation prevents bids if:
  - Team purse is insufficient.
  - Team cannot maintain minimum base price reserve for mandatory squad slots.
  - Squad limit has been reached.
  - Overseas player quota is full.
- **Auctioneer Gavel Controls**:
  - Countdown timer (15s) with pause, reset, and +5s extensions.
  - Phased gavel: **"Going Once..."** ➔ **"Going Twice..."** ➔ **"Hammer Final"**
  - **"SOLD!"** button with congratulatory banner, auto purse deduction, and roster assignment.
  - **"UNSOLD"** button to mark player unsold for re-auction.
  - **"Pass / Skip"** and **"Random Pick"** (spin the wheel for the next player).
  - **"Undo Last Bid"** button to revert accidental bids.

### 5. 📋 Squads & Roster Inspector
- Scrollable team tabs with live squad counts.
- Purse spent vs remaining purse with visual progress bar.
- Role composition breakdown (Batsmen, Bowlers, All-Rounders, WKs, Overseas).
- Full player roster with prices paid.
- **"Release / Undo Sale"**: Allows releasing a player back to the auction pool with instant purse refund.

### 6. 📊 Analytics & Tournament Summary
- Overview metrics: Total spent, players sold, unsold count, remaining upcoming players.
- **Top 10 Buys Leaderboard**: Ranked highest-value acquisitions with gold, silver, and bronze podium medals.
- **Accelerated Re-Auction Round**: One-tap action to queue all unsold players back into the auction pool.
- **Export & Share Report**: Copy formatted auction report to clipboard or share via Android share sheet (WhatsApp, email, notes).
- **Auction Reset**: Reset bids and sales back to initial state while preserving created teams and players.

---

## 🛠️ Architecture & Tech Stack

- **UI**: 100% Declarative Jetpack Compose with Material Design 3.
- **Architecture**: MVVM + Clean Repository Pattern + Kotlin Coroutines / Flow.
- **Database**: Android Room Database with SQLite (`TournamentEntity`, `TeamEntity`, `PlayerEntity`, `BidHistoryEntity`).
- **Navigation**: Jetpack Navigation Compose.
- **Kotlin**: Kotlin 2.0.21.
- **Target SDK**: Android API 35 (Android 15), Min SDK: 26 (Android 8.0+).

---

## 🚀 Building & Running

To build the debug APK:
```bash
./gradlew --gradle-user-home .gradle assembleDebug
```

Output APK location:
```
app/build/outputs/apk/debug/app-debug.apk
```

To run unit tests:
```bash
./gradlew --gradle-user-home .gradle test
```
