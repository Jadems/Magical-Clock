Magical-Clock
=============

A simple open-source Magic Clock plugin for Bukkit that works correctly. 
This will hide other players from users with their clocks toggled on and does not fail to hide users that logged in after the clock was toggled.

Any player that has the permission "magicalclock.alwaysvisible" will not disappear even when the clock is toggled.

Includes a few configurable options:
  * AlwaysShowOps - If true, ops will always be displayed even when a player has the clock toggled on (default true)
  * GiveClockOnLogin - If true, a player will be given a magical clock if they do not already have one when they log in (default true)
  * GiveClockOnRespawn - If true, a player will be given a new magical clock when they respawn (default true)
  * PreventDroppingClock - If true, a player will be unable to drop their clock and it will not drop when they die (default true)
  * AllowNormalClockUse - If true, a normal clock that wasn't supplied by this plugin can still be used to toggle visibility (default false)
  * ToggleDelayMillis - Antispam, if a player attempts to toggle the clock faster than this they will get a message telling them to slow down (default 1000)
  
