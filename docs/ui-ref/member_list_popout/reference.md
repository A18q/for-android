# Discord Android UI Reference — Member List (Popout / Sheet)

## Research Summary

**Screen:** Member List — accessible via channel name tap → Members option, or dedicated Members button in channel toolbar area.

**Sources consulted:**
- Web search synthesis (Reddit, Discord support, discord.com)
- Discord mobile behaviour documentation (2025–2026)

**Screenshot availability:** No screenshot captured. Reference is textual/structural.

**Reference quality: MEDIUM-LOW** — Discord Android does NOT expose a persistent right-panel member list the way desktop does. On mobile, the member list is accessed as a **bottom sheet / modal overlay**, not a slide-in right panel. The structure below reflects confirmed 2025–2026 behaviour.

---

## Screen: Member List Sheet (Bottom Sheet Modal)

> Triggered by tapping channel name / info icon → "Members" option. Presented as a `ModalBottomSheet`. No persistent right panel exists on mobile.

### Layout (top → bottom)

1. **Sheet drag handle** — standard system handle, centered top
2. **"Members" heading** — `headlineSmall` typography, padded via `SheetHeaderPadding`
3. **Loading state** — centered loading indicator (`LoadingIndicator`, 200 dp height) shown until data arrives
4. **Member list** (`LazyColumn`, full height):
   - **Role/category sticky header** (per hoisted role or Online/Offline section):
     - Optional role icon (16 dp square remote image) + role/category name (uppercase) + member count
     - `CountableListHeader` composable, sticky, bg matches sheet surface
   - **Member row** (per member, 2 dp spacer between rows):
     - Avatar (circle, 40 dp default `UserAvatar`, with presence dot)
     - Display name (member nickname → display name → username priority)
     - Role colour applied via gradient brush to name text
     - Custom status text (12 sp muted) below name, only shown if user is online
     - Trailing content slot (optional)
     - Row corners: first-in-group has rounded top corners; last-in-group has rounded bottom corners; middle rows have minimal rounding; single-item group fully rounded (`shapes.large`)
     - Long-press → member context sheet (kick/ban/role management for servers, remove for group DMs)
     - Tap → `UserInfoSheet` / `UserInfoSheet2` (experiment flag)

### Section ordering
1. Hoisted role groups (in role order, highest first) — only online members
2. **Online** — unhoisted online members
3. **Offline** — all offline members (excluded for very large servers)

### Group DM variant
- Same sheet structure but uses `UserItem` instead of `MemberItem`
- No role colours or hoisted role sections
- Only Online / Offline categories

### Visual Notes (2025–2026 Discord)
- Sheet background: `surfaceContainerLow` for category headers, `surfaceContainer` for member rows
- No persistent right panel on Android — pure bottom sheet
- No "Server Boost" or Nitro badges visible in member rows (Nitro-free context)
- Member count shown in category header (e.g. "ONLINE — 42")
