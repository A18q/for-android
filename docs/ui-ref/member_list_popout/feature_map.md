# Feature Map: Member List & Popout

## Discord Layout (top → bottom, left → right)

### 1. Member List Sheet (Modal Bottom Sheet on Mobile)
1. **Sheet Drag Handle** — Standard Material3 centered bottom sheet handle pill.
2. **"Members" Sheet Title** — `headlineSmall` (20sp bold, `#F2F3F5`), padded via `SheetHeaderPadding`.
3. **Empty / Loading State** — Centered `LoadingIndicator` (200dp box height) displayed while fetching server or group members.
4. **Member List Container (`LazyColumn`)**:
   - **Sticky Category / Role Header (`CountableListHeader`)**:
     - Optional role icon (16dp square `RemoteImage`).
     - Category / Role Name in uppercase (11sp bold, letter spacing 0.8sp, color `#949BA4`): hoisted roles ordered by rank, followed by "ONLINE", and finally "OFFLINE".
     - Member count badge (e.g. "— 42").
     - Sticky surface background (`surfaceContainerLow` / `#1E1F22`).
   - **Member Row Item (`MemberListItem`)**:
     - Grouped rounded corner clipping: 12dp top for first-in-group, 12dp bottom for last-in-group, 4dp for middle rows, 12dp all corners for single-member group.
     - Row container: `surfaceContainer` (`#232428` / `#2B2D31`), 2dp vertical spacing between rows.
     - Leading Avatar: 40dp circular `UserAvatar` (supports custom server avatar via `rawUrl`), with 14dp live presence badge dot:
       - Online: `#23A55A`
       - Idle: `#F0B232`
       - Do Not Disturb: `#F23F43`
       - Offline: `#80848E`
     - Display Name: Resolves member nickname → display name → username → user ID; bold 14-15sp text styled with role color brush (`BrushCompat.parseColour`).
     - Custom Status Text: 12sp muted `#949BA4`, single line, ellipsis truncation (rendered only when user is online).
     - Trailing Content Slot: Intentionally null in Stoat (zero booster badges, zero nitro icons).
     - Tap Interaction: Opens in-server profile modal (`UserInfoSheet` / `UserInfoSheet2`).
     - Long-Press Interaction: Opens member context sheet (`ServerMemberContextSheet` for servers, `GroupDMMemberContextSheet` for group DMs).
5. **Offline Member Caching Rule** — Excluded for very large servers (e.g. Lounge `01F7ZSBSFHQ8TA81725KQCSDDP`) to ensure sub-50ms render performance.

---

### 2. Member Popout / In-Server User Profile (`UserInfoSheet.kt`)
6. **Sheet Container** — Background `#111214` (Discord Dark Canvas), top rounded corners (`ModalBottomSheet`).
7. **Banner & Staged Avatar Area (175dp total height)**:
   - **User Banner Image** — 135dp cropped `RemoteImage` or default aesthetic vertical gradient (`#5865F2` blurple to `#232428` card surface).
   - **Overlapping 96dp Circular Avatar** — Centered/anchored bottom-left (16dp start padding), 6dp solid cut-out border matching canvas (`#111214`), 26dp live presence indicator badge.
   - **Badge Capsule** — Pinned bottom-right of banner; rounded 10dp pill (`#1E1F22` background, 1dp border `#2B2D31`), displaying active native platform badges (Developer, Supporter, Founder, Early Adopter, Platform Moderation).
8. **Identity Block**:
   - **Display Name** — 24sp bold header (`#F2F3F5`), server nickname prioritized over global name.
   - **Subtitle Handle & Pronouns** — `@username#discriminator • pronouns` (14sp medium, `#949BA4`).
   - **Custom Status Bubble** — `RoundedCornerShape(12dp)`, background `#232428`, text `#F2F3F5` (13sp medium, max 2 lines).
9. **Action Bar**:
   - **Self Profile** — Full-width "Edit Profile" button (42dp height, `#5865F2`, white text, opens profile settings).
   - **Other Member** — `UserButtons` action bar: Send Direct Message, Voice Call, Video Call, Add/Remove Friend, Block/Unblock, Server Nickname Manager, and Moderation Menu.
10. **Content Divider** — 0.5dp horizontal divider (`#2B2D31`).
11. **Stacked Elevated Cards (`#232428`, 1dp border `#2B2D31` 50% alpha, 8dp radius)**:
    - **Card 1: "ABOUT ME"** — 11sp bold `#949BA4` header, selectable `ChatMarkdown` bio content.
    - **Card 2: "MEMBER SINCE"** — 11sp bold `#949BA4` header, 2-row layout with icons:
      - Server Join Date (explore icon + relative timestamp).
      - Stoat Registration Date (chat icon + relative timestamp).
    - **Card 3: "ROLES — {count}"** — 11sp bold `#949BA4` header, `FlowRow` of hierarchical rank-sorted `DiscordRolePill` items (11dp colored circle dot + 12sp text on inset `#1E1F22` surface).
    - **Card 4: Moderator Actions** — Quick moderation rail via `UserButtons` / `MemberModerationDialog`: Timeout duration picker, Kick with reason, Ban with purge window, and Nickname editing.

---

## Mapping Table

| Discord Element | Status | Stoat File/Composable | Notes |
|---|---|---|---|
| **Sheet Drag Handle** | MAPPED | `MemberListSheet.kt:19-21`, `ModalBottomSheet` | Standard system drag handle rendered at top of sheet. |
| **"Members" Sheet Heading** | MAPPED | `MemberListSheet.kt:324-329` | `Text` with `headlineSmall` style inside `SheetHeaderPadding`. |
| **Loading State Indicator** | MAPPED | `MemberListSheet.kt:312-322` | Centered `LoadingIndicator` (200dp height) while fetching data. |
| **Category / Role Sticky Header** | MAPPED | `MemberListSheet.kt:334-353`, `CountableListHeader.kt` | Sticky header with 16dp role icon, uppercase title, member count, bg `surfaceContainerLow`. |
| **Member Row Item Container** | MAPPED | `MemberListSheet.kt:355-382`, `MemberListItem.kt:53-90` | `ListItem` with grouped corner radii (`12dp` top/bottom, 4dp middle), bg `surfaceContainer`. |
| **Member Circular Avatar & Status Dot** | MAPPED | `MemberListItem.kt:114-128` (`UserAvatar`) | 40dp circular avatar, custom server avatar support (`rawUrl`), live status presence dot. |
| **Member Display Name & Role Color** | MAPPED | `MemberListItem.kt:91-102` | Nickname priority, brush styling from `Roles.resolveHighestRole`. |
| **Custom Status Snippet (Online Only)** | MAPPED | `MemberListItem.kt:103-112` | 12sp muted text, 1 line ellipsis, only shown when online. |
| **Member Row Tap Interaction** | MAPPED | `MemberListSheet.kt:368-371`, `UserInfoSheet.kt` | Tapping row launches in-server `UserInfoSheet` (or `UserInfoSheet2` via experiment flag). |
| **Member Row Long-Press Interaction** | MAPPED | `MemberListSheet.kt:372-375`, `MemberContextSheet.kt` | Long-press opens `ServerMemberContextSheet` or `GroupDMMemberContextSheet`. |
| **Group DM Member List Variant** | MAPPED | `MemberListSheet.kt:169-214, 384-412` | Fetches group participants, categorizes strictly into Online / Offline without roles. |
| **Profile Popout Outer Canvas** | MAPPED | `UserInfoSheet.kt:80, 156-161` | High-elevation dark surface `#111214` (`DiscordDarkCanvas`). |
| **Profile Header Banner** | MAPPED | `UserInfoSheet.kt:163-200` | 135dp cropped banner (`RemoteImage`) or vertical blurple gradient fallback. |
| **Profile Overlapping Avatar (96dp)** | MAPPED | `UserInfoSheet.kt:201-220` | 96dp `UserAvatar` with 6dp cut-out border (`#111214`), 26dp presence badge. |
| **Platform Badge Capsule (No Nitro)** | MAPPED | `UserInfoSheet.kt:221-232, 493-536` (`DiscordBadgeCapsule`) | Inset pill `#1E1F22`, border `#2B2D31`, maps native badges only (Developer, Supporter, Mod, Founder, etc.). |
| **Identity Block (Name & @handle)** | MAPPED | `UserInfoSheet.kt:235-267` | 24sp bold header, `@handle • pronouns` subtitle in `#949BA4`. |
| **Custom Status Bubble** | MAPPED | `UserInfoSheet.kt:269-290` | 12dp rounded card, `#232428` surface, 13sp medium text. |
| **Profile Edit Button (Self)** | MAPPED | `UserInfoSheet.kt:295-322` | 42dp blurple button (`#5865F2`), white text with edit icon. |
| **Profile Action Buttons (Other Member)** | MAPPED | `UserInfoSheet.kt:323-329`, `UserButtons.kt` | DM, Voice Call, Video Call, Friend, Moderation actions. |
| **Profile "ABOUT ME" Card** | MAPPED | `UserInfoSheet.kt:341-363` | Elevated card (`#232428`), uppercase header, `ChatMarkdown` prose rendering. |
| **Profile "MEMBER SINCE" Card** | MAPPED | `UserInfoSheet.kt:365-454` | Elevated card (`#232428`), Server Join date + Stoat registration date with icons. |
| **Profile "ROLES" Card & Role Pills** | MAPPED | `UserInfoSheet.kt:456-488, 538-564` (`DiscordRolePill`) | Inset pills (`#1E1F22`, 6dp radius) with 11dp role color circle + 12sp role name. |
| **Moderation Actions Quick-Rail** | MAPPED | `UserButtons.kt:78-100`, `MemberModerationDialog.kt` | Timeout, Kick, Ban, Server Nickname editing. |
| **Server Booster Gem Badge (Member Row)** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. Discord places a pink booster diamond next to members who boosted. Stoat leaves `trailingContent = null`. |
| **Nitro Subscriber / Booster Profile Badges** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. Discord shows Nitro and Server Booster badges in badge capsule. Stoat only renders native platform badges. |
| **Avatar Decorations & Profile Effects** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. GPU-burning synthetic WebGL overlays and shop assets are completely purged. |
| **Profile Shop / "Try Nitro" Banners** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. Discord embeds upsell banners in profile bottom sheets. Stoat strictly bans them. |
| **"Boost Server" Button in Member View** | DISCORD-ONLY (MONETIZATION) | None (BANNED) | **DELETE SLOT / CLOSE GAP**. Zero server boost upsell triggers. |

---

## Changes Required (Restyle Only)

1. **Member List Sheet Container Background**:
   - `MemberListSheet.kt`: Currently relies on default `ModalBottomSheet` container color.
   - Restyle to explicit dark surface `#1E1F22` (`bg_tertiary`) to match Discord 2026 bottom sheet aesthetics.
2. **Category Header Typography & Background**:
   - `CountableListHeader` background should be `#1E1F22` (`bg_tertiary`) or `#17181B` (`surfaceContainerLow`).
   - Title text: 11sp bold, uppercase, letter spacing 0.8sp, color `#949BA4` (`text_muted`).
   - Member count: Muted monospace or regular font formatted as `— {count}`.
3. **Member List Item Styling**:
   - `MemberListItem.kt:54-56`: Container color is `MaterialTheme.colorScheme.surfaceContainer`. Ensure surface is `#232428` (`bg_card`) or `#2B2D31` (`bg_secondary`).
   - Row heights: 48–52dp compact height.
   - Spacing: 2dp spacer between adjacent member items (`MemberListSheet.kt:379`).
   - Grouped shapes: Ensure top item in group has `topStart = 12.dp, topEnd = 12.dp`, bottom item has `bottomStart = 12.dp, bottomEnd = 12.dp`, and single item is fully rounded `12.dp`.
4. **Member Profile Popout (`UserInfoSheet.kt`)**:
   - Already aligned 1:1 with Discord 2026 dark surface tokens:
     - Outer canvas: `#111214`
     - Card body: `#232428`
     - Inset pill: `#1E1F22`
     - Text header: `#F2F3F5`
     - Text normal: `#DBDEE1`
     - Text muted: `#949BA4`
     - Brand blurple: `#5865F2`
     - Border/divider: `#2B2D31`
   - Overlapping avatar: 96dp circle with 6dp cut-out border `#111214`.
   - Role pills: 6dp radius, `#1E1F22` inset background, 11dp color indicator dot.

---

## Slots to Delete (Monetization)

1. **Server Booster Gem in Member Rows**:
   - Modern Discord shows a pink diamond icon at the trailing edge of every member row who purchased server boosts.
   - **Stoat Rule**: Permanently banned. `trailingContent` remains `null`. No booster badge, no icon slot, no spacing gap.
2. **Nitro Subscription Badges in Profile Capsule**:
   - Discord popouts show Nitro badges, Server Booster badges, and subscriber tenure icons.
   - **Stoat Rule**: Permanently banned. `DiscordBadgeCapsule` only maps native open-source badges (Developer, Supporter, Early Adopter, Founder, Platform Moderation). Zero Nitro or boost badges.
3. **Avatar Decorations & Profile Effects**:
   - Discord renders animated WebGL / APNG borders, frames, and screen effects over user profiles.
   - **Stoat Rule**: Completely banned and purged. Preserves native rendering performance and battery life. Only pure user avatar images and custom banners are rendered.
4. **Profile Shop / "Preview Nitro" Upsells**:
   - Discord injects promotional banners ("Get Nitro to customize your profile banner and colors") into user sheets.
   - **Stoat Rule**: Permanently banned. All customizable fields (custom banner, bio, status) are 100% unlocked for all users natively with zero upsells.
