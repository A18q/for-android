# Discord Android — User Settings Screen Reference

## Research Summary

**Screens covered:**
1. **User Settings Main Hub** (accessed via gear icon in the "You" tab / profile popout or user capsule)
2. **Account Settings** (nested under Account > Account)
3. **Profiles / User Profile Editor** (nested under Account > Profiles or "Edit Profile" button)
4. **Appearance Settings** (nested under App Settings > Appearance)

**Sources consulted:**
- Discord official Android client (v346+ / 2025–2026 "You Bar" and squircle visual refresh era)
- Forensic baseline captures: `STOAT_MODERN_LAYOUT_SPEC.md`, `MODERN_LAYOUT_AUDIT.md` (screen recordings `SVID_20260926_113537_1.mp4`, `SVID_20260926_114424_1.mp4`, `SVID_20260926_114528_1.mp4`)
- Discord mobile redesign documentation & changelogs (discord.com, 2025–2026)
- Local mockups: `/root/stoat/mockup_references/member_card_profile_sheet.png`

**Approximate date of design:** 2025 – Late 2026 (You Bar, card-stack elevation, squircle surfaces)

**Reference quality:** HIGH — Textual, structural, color token, and forensic video documentation verified.

---

## Screen 1: User Settings Main Screen

This is the top-level settings page displayed when tapping the **Settings gear** icon on the You tab or from the floating user capsule.

### Visual Tokens (Modern Discord 2025–2026)
- Screen background (`bg_primary`): `#313338` (Dark theme) or `#000000` (Onyx/AMOLED)
- Group / Card container background (`bg_secondary`): `#2B2D31`
- Inset / elevated surface (`bg_tertiary` / `bg_card`): `#1E1F22` / `#232428`
- Brand accent: `#5865F2` (Blurple)
- Primary text: `#F2F3F5` (Header/Title) / `#DBDEE1` (Normal text)
- Muted text: `#949BA4`
- Destructive / Error: `#F23F43` (Red)
- Shape language: Squircles and rounded rectangles (`RoundedCornerShape(12dp)` / `16dp` for grouped card stacks)

---

### Layout (top → bottom, left → right)

#### 1. Top App Bar (bg: `#1E1F22` or `#313338`)
- **Left**: `←` Back navigation icon button (24dp hit target 48dp)
- **Title**: **"Settings"** or **"User Settings"** (20sp bold, `#F2F3F5`)
- **Right**: None (or search icon in older variants; eliminated in v346+)

#### 2. User Overview Header Card (Profile Preview Banner)
Positioned at top of scroll view as an interactive identity card:
- **Card container**: `RoundedCornerShape(16dp)`, background `#2B2D31`, full-width with 16dp horizontal padding
- **Banner area**: 80dp tall colored/gradient strip or custom image banner
- **Avatar**: 64dp circular avatar overlapping bottom-left of banner with 4dp cut-out border in `#2B2D31`; 16dp presence indicator dot (`#23A55A` Online, `#F0B232` Idle, `#F23F43` DND, `#80848E` Offline)
- **User Details**:
  - Display Name: 18sp bold `#F2F3F5`
  - Handle / Username: 13sp medium `@username` in `#949BA4`
  - Custom Status: 12sp pill bubble (`#1E1F22` background, emoji + status text)
- **Quick Action Button**: "Edit Profile" pill button (`RoundedCornerShape(20dp)`, bg `#35373C`, text 13sp `#DBDEE1`)

#### 3. Section: "ACCOUNT SETTINGS" (Header: 11sp bold, uppercase, `#949BA4`)
Stacked card group (`RoundedCornerShape(12dp)`, bg `#2B2D31`, rows separated by 1dp divider `#35373C`):
- **Account**:
  - Leading icon: Lock / Person icon (24dp, `#949BA4`)
  - Headline: "Account" (15sp, `#DBDEE1`)
  - Trailing icon: Chevron `>` (16dp, `#949BA4`)
  - Action: Navigates to Account Settings screen
- **Profiles**:
  - Leading icon: ID Card / Badge icon (24dp, `#949BA4`)
  - Headline: "Profiles" (15sp, `#DBDEE1`)
  - Trailing icon: Chevron `>` (16dp, `#949BA4`)
  - Action: Navigates to Profile Customization screen
- **Devices**:
  - Leading icon: Laptop / Mobile devices icon (24dp, `#949BA4`)
  - Headline: "Devices" (15sp, `#DBDEE1`)
  - Trailing icon: Chevron `>` (16dp, `#949BA4`)
  - Action: Navigates to Active Sessions / Devices list

#### 4. [DISCORD MONETIZATION SECTION — DELETED IN STOAT]
*In official Discord: "Billing Settings", "Get Nitro", "Nitro Gifts", "Server Boost", "Subscriptions", "Shop", "Avatar Decorations", "Profile Effects".*
*In Stoat: This entire section is deleted and the vertical gap is closed.*

#### 5. Section: "APP SETTINGS" (Header: 11sp bold, uppercase, `#949BA4`)
Stacked card group (`RoundedCornerShape(12dp)`, bg `#2B2D31`):
- **Appearance**:
  - Leading icon: Palette icon (24dp, `#949BA4`)
  - Headline: "Appearance" (15sp, `#DBDEE1`)
  - Trailing icon: Chevron `>` (16dp, `#949BA4`)
  - Action: Navigates to Appearance screen
- **Voice & Video**:
  - Leading icon: Mic / Volume icon (24dp, `#949BA4`)
  - Headline: "Voice" (15sp, `#DBDEE1`)
  - Trailing icon: Chevron `>` (16dp, `#949BA4`)
- **Chat**:
  - Leading icon: Chat bubble icon (24dp, `#949BA4`)
  - Headline: "Chat" (15sp, `#DBDEE1`)
  - Trailing icon: Chevron `>` (16dp, `#949BA4`)
- **Notifications**:
  - Leading icon: Bell icon (24dp, `#949BA4`)
  - Headline: "Notifications" (15sp, `#DBDEE1`)
  - Trailing icon: Chevron `>` (16dp, `#949BA4`)
- **Language**:
  - Leading icon: Globe icon (24dp, `#949BA4`)
  - Headline: "Language" (15sp, `#DBDEE1`)
  - Trailing icon: Chevron `>` (16dp, `#949BA4`)

#### 6. Section: "MISCELLANEOUS / DEVELOPER" (Header: 11sp bold, uppercase, `#949BA4`)
Stacked card group (`RoundedCornerShape(12dp)`, bg `#2B2D31`):
- **About**:
  - Leading icon: Info icon (24dp, `#949BA4`)
  - Headline: "About" (15sp, `#DBDEE1`)
  - Trailing: Chevron `>`
- **Debug / Labs / Experiments** (conditional):
  - Leading icon: Science / Research beaker icon (24dp, `#949BA4`)
  - Headline: "Experiments" / "Labs"
  - Trailing: Chevron `>`

#### 7. Section: "APP INFO & ACTIONS" (Header: "Stoat vX.Y.Z", 11sp `#949BA4`)
- **What's New / Changelog**:
  - Leading icon: Campaign megaphone icon (24dp, `#949BA4`)
  - Headline: "Changelog"
- **Feedback & Support**:
  - Leading icon: Feedback speech bubble icon (24dp, `#949BA4`)
  - Headline: "Feedback"
- **Log Out**:
  - Leading icon: Logout icon (24dp, `#F23F43` error color)
  - Headline: "Log Out" (15sp bold, `#F23F43`)
  - Action: Opens confirmation alert dialog

---

## Screen 2: Account Settings (`settings/account`)

### Layout (top → bottom)
1. **Top App Bar**:
   - `←` Back arrow
   - Title: **"Account"** (20sp bold, `#F2F3F5`)
2. **Account Information Card Group** (`bg_secondary` `#2B2D31`, `RoundedCornerShape(12dp)`):
   - **Email Address Row**:
     - Headline: "Email" (12sp muted `#949BA4`)
     - Value: Masked email `•••••@domain.com` (15sp, `#DBDEE1`)
     - Trailing: Edit pencil icon (`#949BA4`)
     - Tap opens: "Change Email" modal dialog (Current email, new email input, password input, Cancel / Confirm buttons)
   - **Password Row**:
     - Headline: "Password" (12sp muted `#949BA4`)
     - Value: Masked bullets `••••••••••••` (15sp monospace)
     - Trailing: Edit pencil icon (`#949BA4`)
     - Tap opens: "Change Password" modal dialog (Current password, new password, Confirm button)
3. **Two-Factor Authentication (MFA) Card Group**:
   - Title: "Two-Factor Authentication"
   - Warning banner (if disabled): Warning card (`#3D1E22` red container, text `#F23F43`)
   - TOTP / Authenticator App Row:
     - Headline: "Authenticator App"
     - Subtitle: "Protect your account with an extra security layer"
     - Trailing: Switch toggle / Chevron `>` navigating to MFA setup

---

## Screen 3: User Profile Editor (`settings/profile`)

### Layout (top → bottom)
1. **Top App Bar**:
   - `←` Back arrow
   - Title: **"Profiles"** or **"User Profile"**
2. **Live Profile Card Preview** (`RoundedCornerShape(16dp)`, bg `#1E1F22`, border 1dp `#2B2D31`):
   - Banner image / color gradient preview (120dp height)
   - 80dp circular avatar overlapping banner with 4dp cut-out ring
   - Display Name, Handle, Pronouns, and Bio live rendered exactly as other users see it
3. **Media Upload Row**:
   - **Avatar Picker**: Circular image preview, "Change Avatar" button, "Remove Avatar" option
   - **Banner Picker**: Rectangular image preview, "Change Banner" button, "Remove Banner" option
4. **Editable Fields** (`OutlinedTextField`, bg `#2B2D31`, border focused `#5865F2`):
   - **Display Name**: Input field (max 32 chars) + Save button (active only when dirty)
   - **Pronouns**: Input field (max 24 chars) + Save button
   - **About Me (Bio)**: Multi-line text field with markdown support + character counter + Save button

---

## Screen 4: Appearance Settings (`settings/appearance`)

### Layout (top → bottom)
1. **Top App Bar**:
   - `←` Back arrow
   - Title: **"Appearance"**
2. **Theme Selection Section** (Header: "THEME"):
   - Horizontal FlowRow / Grid of theme chips with selection indicator:
     - **Default / Dark**: `#313338` base
     - **Ash / Darker**: `#2B2D31` base
     - **Onyx / Pure Black (AMOLED)**: `#000000` base
     - **Light**: `#F2F3F5` base
     - **Automatic / System Theme**: Syncs with Android system dark/light mode
     - **Dynamic Colors (Material You)**: Enabled on Android 12+
3. **Visual Effects Section**:
   - **Message Composer Blur**: Switch toggle
4. **Typography / Font Selection**:
   - Connected button group / Radio buttons: "Default (Inter)" vs "Google Sans Flex"
5. **Avatar Shape / Geometry Selection**:
   - Slider / CornerRadiusPicker: Circular (50%) to Squircle (20%) to Rounded Square (10%)
6. **Advanced Customization (Stoat-Only)**:
   - Color scheme override manager with CBOR file export / import
