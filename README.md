# Gamble Rig (Fabric 1.21.11, Java 21)
Install on BOTH server and client, plus Fabric API.
Allowed users: singleplayer/LAN host, or usernames in config/gamblerig.txt on the server (one per line).
- G: menu (pick item type, fill the 2 nearest dispensers with items named 1-9, toggle auto win)
- X: toggle auto win. R: show/hide the top-right HUD (status, both dispensers' numbers, last result). Faint dot bottom-right while enabled.
- Auto win: of the 2 nearest dispensers (within 8 blocks), the RIGHT one (relative to where you face) stays random
  and the LEFT one is forced to a lower number. If the right one rolls 1, the left one gives its lowest number.
- Numbers come from each item's custom name ("1".."9"); unnamed items use their slot position.

## Modules menu (Right Shift)
- Aim Assist (mobs): while holding attack, nudges aim toward the nearest hostile mob. Singleplayer only, never targets players.
- Bridge Assist: places the held block under you when you step onto air. Singleplayer only.
- Toggle Sprint, Fullbright (local night vision), FPS Boost (lowers view distance/particles, restores on off).
Singleplayer-only modules switch themselves off if you join a multiplayer server.

## Item rig (G menu)
Say what is in each dispenser (left/right as seen from where you face) and which side wins. Defaults: left = diamond, right = totem, RIGHT wins.
With auto win ON (X): the winning dispenser is forced to drop the winning item; the losing dispenser never drops its item
(if it only holds that item it just clicks). A message above the hotbar says when the rig fires.
