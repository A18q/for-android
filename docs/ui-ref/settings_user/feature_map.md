# Feature Map: User Settings

## Discord Layout (top → bottom, left → right)

1. **Top App Bar**:
   - `←` Back navigation icon button
   - Title: "Settings" / "User Settings"
2. **User Overview Profile Card (Card Stack Header)**:
   - Banner backdrop (custom image or gradient `#5865F2` → `#232428`)
   - 64dp Circular Avatar with 4dp cut-out border and live status indicator dot (`#23A55A`, `#F0B232`, `#F23F43`, `#80848E`)
   - Display Name (18sp bold `#F2F3F5`)
   - Username/Handle (`@username` in `#949BA4`)
   - Custom Status bubble pill (`#1E1F22`, emoji + status text)
   - "Edit Profile" pill button (`RoundedCornerShape(20dp)`, bg `#35373C`)
3. **Category: Account Settings**:
   - Section header: "ACCOUNT SETTINGS" (11sp bold, uppercase `#949BA4`)
   - **Account** row: Lock icon + "Account" + chevron `>`
     - [Subpage] Email row (masked `•••••@domain.com`) with edit dialog
     - [Subpage] Password row (masked bullets `••••••••••••`) with change dialog
     - [Subpage] Two-Factor Authentication (TOTP / Authenticator App switch)
   - **Profiles** row: ID card icon + "Profiles" + chevron `>`
     - [Subpage] Live profile preview card (Banner + Avatar + Bio)
     - [Subpage] Avatar media picker (upload / crop / remove)
     - [Subpage] Banner media picker (upload / remove)
     - [Subpage] Display Name input field (max 32 chars)
     - [Subpage] Pronouns input field (max 24 chars)
     - [Subpage] About Me / Bio input field (markdown)
   - **Devices** row: Devices icon + "Devices" + chevron `>`
     - [Subpage] Active sessions list + remote sign-out
4. **[DISCORD MONETIZATION SECTION]**:
   - Nitro, Nitro Gifts, Server Boost, Subscriptions, Shop, Avatar Decorations, Profile Effects, Nameplate
5. **Category: App Settings**:
   - Section header: "APP SETTINGS" (11sp bold, uppercase `#949BA4`)
   - **Appearance** row: Palette icon + "Appearance" + chevron `>`
     - [Subpage] Theme selection chips (Default Dark `#313338`, Ash `#2B2D31`, Onyx `#000000`, Light `#F2F3F5`, System)
     - [Subpage] Composer Blur effect switch
     - [Subpage] Typeface selector (Inter vs Google Sans Flex)
     - [Subpage] Avatar Corner Radius slider
   - **Chat** row: Chat icon + "Chat" + chevron `>`
   - **Voice & Video** row: Mic icon + "Voice" + chevron `>`
   - **Notifications** row: Bell icon + "Notifications" + chevron `>`
   - **Language** row: Globe icon + "Language" + chevron `>`
6. **Category: Miscellaneous / Info**:
   - Section header: "ABOUT / STOAT"
   - **About** row: Info icon + "About"
   - **Changelog** row: Campaign megaphone icon + "Changelog"
   - **Feedback** row: Feedback speech bubble icon + "Feedback"
   - **Log Out** row: Logout icon + "Log Out" (danger red `#F23F43`)

---

## Mapping Table

| Discord Element | Status | Stoat File/Composable | Notes |
|---|---|---|---|
| Top App Bar (`←` Back, "Settings") | MAPPED | `chat.stoat.screens.settings.SettingsScreen` (`LargeTopAppBar`) | Restyle to compact 48dp header `#1E1F22`, remove oversized M3 expansion |
| Profile Overview Header Card | MAPPED | `chat.stoat.composables.screens.settings.RawUserOverview` | Integrate card at top of `SettingsScreen` matching Discord's You-header preview |
| "Account" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `settings/account` (`AccountSettingsScreen.kt`) |
| Account: Email (Masked + Edit) | MAPPED | `chat.stoat.screens.settings.AccountSettingsScreen.kt` | Uses masked email with bullet format and edit dialog |
| Account: Password Change | MAPPED | `chat.stoat.screens.settings.AccountSettingsScreen.kt` | Uses bullet placeholder with `SecureTextField` edit dialog |
| Account: Two-Factor Auth (MFA) | MAPPED | `chat.stoat.screens.settings.AccountSettingsScreen.kt` | TOTP switch & warning banner navigating to `settings/account/mfa` |
| "Profiles" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `settings/profile` (`ProfileSettingsScreen.kt`) |
| Profiles: Live Card Preview | MAPPED | `chat.stoat.screens.settings.ProfileSettingsScreen.kt` (`RawUserOverview`) | Live updates avatar, banner, pronouns, and bio |
| Profiles: Avatar Upload/Remove | MAPPED | `chat.stoat.screens.settings.ProfileSettingsScreen.kt` (`InlineMediaPicker`) | Uploads to Autumn media microservice |
| Profiles: Banner Upload/Remove | MAPPED | `chat.stoat.screens.settings.ProfileSettingsScreen.kt` (`InlineMediaPicker`) | Uploads to Autumn media microservice |
| Profiles: Display Name Field | MAPPED | `chat.stoat.screens.settings.ProfileSettingsScreen.kt` (`OutlinedTextField`) | Max 32 characters, save button enabled on change |
| Profiles: Pronouns Field | MAPPED | `chat.stoat.screens.settings.ProfileSettingsScreen.kt` (`OutlinedTextField`) | Max 24 characters |
| Profiles: About Me / Bio Field | MAPPED | `chat.stoat.screens.settings.ProfileSettingsScreen.kt` (`OutlinedTextField`) | Multi-line text field |
| "Devices" / "Sessions" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `settings/sessions` (`SessionSettingsScreen.kt`) |
| Nitro / Nitro Gifts / Boosts | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| Shop / Avatar Decorations | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| Profile Effects / Nameplate | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| Billing / Subscriptions | DISCORD-ONLY (MONETIZATION) | None | **HARD BAN**: Delete slot, close vertical gap completely |
| "Appearance" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `settings/appearance` (`AppearanceSettingsScreen.kt`) |
| Appearance: Theme Selection Chips | MAPPED | `chat.stoat.screens.settings.AppearanceSettingsScreen.kt` (`ColourChip`) | Restyle chips to Discord squircle cards `#313338`, `#2B2D31`, `#000000`, `#F2F3F5` |
| Appearance: Composer Blur Switch | MAPPED | `chat.stoat.screens.settings.AppearanceSettingsScreen.kt` (`Switch`) | Controls dynamic composer background blur |
| Appearance: Typeface Selector | MAPPED | `chat.stoat.screens.settings.AppearanceSettingsScreen.kt` (`ToggleButton`) | Inter vs Google Sans Flex |
| Appearance: Avatar Shape Slider | MAPPED | `chat.stoat.screens.settings.AppearanceSettingsScreen.kt` (`CornerRadiusPicker`) | Controls circular to squircle avatar radius |
| Appearance: CBOR Theme Overrides | STOAT-ONLY | `chat.stoat.screens.settings.AppearanceSettingsScreen.kt` | Custom theme import/export feature; keep as-is |
| "Language" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `settings/language` |
| "Chat" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `settings/chat` |
| "Notifications" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `settings/notifications` |
| "About" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `about` |
| "Debug" / "Labs" / "Experiments" | STOAT-ONLY | `chat.stoat.screens.settings.SettingsScreen.kt` | Developer flags; keep as-is |
| "Changelog" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Navigates to `changelog/latest` |
| "Feedback" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Opens testers community invite |
| "Log Out" Row | MAPPED | `chat.stoat.screens.settings.SettingsScreen.kt` (`SettingsListItem`) | Red destructive style with alert confirmation |

---

## Changes Required (Restyle Only)

1. **Color Token Alignment**:
   - Screen surface: Replace default Material3 container background with Discord `bg_primary` (`#313338`) for dark theme and `#000000` for Onyx AMOLED.
   - Group container: Wrap list items inside card blocks with background `bg_secondary` (`#2B2D31`) and 1dp border `#35373C`.
   - Text colors: Section headers `11sp bold` in `text_muted` (`#949BA4`), item titles in `text_header` (`#F2F3F5`), subtitles/descriptions in `#DBDEE1`.
   - Brand color: Ensure save buttons and active indicators use Discord Blurple (`#5865F2`).
2. **Card Stack Shape & Margins**:
   - Change `SettingsListItem` corner rounding from M3 default to modern Discord squircle card group:
     - Outer grouped cards: `RoundedCornerShape(12dp)`
     - Horizontal margins: 16dp
     - Intra-group dividers: 1dp solid `#35373C` (no margin gaps between rows inside the same group)
     - Inter-group spacing: 16dp vertical spacing between section categories
3. **Typography**:
   - Section headers: 11sp bold, uppercase, letter-spacing 0.8sp (`#949BA4`).
   - Row headlines: 15sp medium (`#DBDEE1`).
   - Trailing chevrons: 16dp tint `#949BA4`.
4. **Header Profile Preview**:
   - Add mini profile preview header card above "ACCOUNT SETTINGS" in `SettingsScreen` for instant identity feedback.

---

## Slots to Delete (Monetization)

- ❌ **Nitro / Nitro Gifts**: Purged. No Nitro banner, no gift buttons, no subscription management.
- ❌ **Server Boost Status / Boost Inventory**: Purged.
- ❌ **Shop Tab / Quests**: Purged.
- ❌ **Avatar Decorations / Profile Effects / Nameplates**: Purged. No store links or locked previews.
- ❌ **Billing & Payment Methods**: Purged.
- ❌ **"Try Nitro for 2 Weeks Free" Nag Banners**: Purged.
- **Vertical Spacing Rule**: Ensure no empty whitespace or stub dividers remain where monetization elements existed. The App Settings category immediately follows the Account Settings category.
