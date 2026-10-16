# Test timeline (ticks). Each attack is forced once with a dev tag (hl_*: dev runs only; hl_hold = no random attacks),
# with game screenshots (HLSHOT name -> run/screenshots/name.png) at the attack's own event times. Then free fights.
execute if score #t ht matches -1 if entity @a run function homelandertest:setup
execute if score #t ht matches 0.. run scoreboard players add #t ht 1
# 1-2.5 s: his face up close (idle: moods, blinks, glow)
execute if score #t ht matches 1 run tp @a 0 100.1 1.5 180 4
execute if score #t ht matches 1 run tellraw @a "HLHUD off"
execute if score #t ht matches 20 run tellraw @a "HLSHOT hl_face_1"
execute if score #t ht matches 35 run tellraw @a "HLSHOT hl_face_2"
execute if score #t ht matches 40 run tp @a 0.6 100.2 -2.6 -12 6
execute if score #t ht matches 52 run tellraw @a "HLSHOT hl_cape_back"
# 3 s: side camera; villager in front, AI on -> Punch (hit 0.65 s)
execute if score #t ht matches 58 run tp @a 3.4 100.2 0.6 90 4
execute if score #t ht matches 60 run function homelandertest:victim
execute if score #t ht matches 60 run data merge entity @e[type=homelander:homelander,limit=1] {NoAI:0b}
execute if score #t ht matches 60 run tag @e[type=homelander:homelander] add hl_punch
execute if score #t ht matches 69 run tellraw @a "HLSHOT hl_punch_a"
execute if score #t ht matches 75 run tellraw @a "HLSHOT hl_punch_b"
execute if score #t ht matches 82 run tellraw @a "HLSHOT hl_punch_c"
# 7 s: Head clap (voice 0.15, swing 1.08, hit 1.2 s); the victim: hands on ears, falls at hit+0.95, lies until hit+3.4
execute if score #t ht matches 138 run tp @a 5.5 101.8 0.5 90 16
execute if score #t ht matches 140 run function homelandertest:victim
execute if score #t ht matches 140 run tag @e[type=homelander:homelander] add hl_clap
execute if score #t ht matches 158 run tellraw @a "HLSHOT hl_clap_a"
execute if score #t ht matches 164 run tellraw @a "HLSHOT hl_clap_b"
execute if score #t ht matches 172 run tellraw @a "HLSHOT hl_clap_c"
execute if score #t ht matches 192 run tellraw @a "HLSHOT hl_clap_d"
execute if score #t ht matches 215 run tellraw @a "HLSHOT hl_clap_e"
# 14 s: Throw (grab 0.42, release 1.52 s)
execute if score #t ht matches 280 run function homelandertest:victim
execute if score #t ht matches 280 run tag @e[type=homelander:homelander] add hl_throw
execute if score #t ht matches 292 run tellraw @a "HLSHOT hl_throw_a"
execute if score #t ht matches 302 run tellraw @a "HLSHOT hl_throw_b"
execute if score #t ht matches 310 run tellraw @a "HLSHOT hl_throw_c"
execute if score #t ht matches 318 run tellraw @a "HLSHOT hl_throw_d"
execute if score #t ht matches 330 run tellraw @a "HLSHOT hl_throw_e"
# 22 s: Throat laser (grab 0.45, burn 1.6-2.6, release 2.85 s)
execute if score #t ht matches 440 run function homelandertest:victim
execute if score #t ht matches 440 run tag @e[type=homelander:homelander] add hl_burn
execute if score #t ht matches 455 run tellraw @a "HLSHOT hl_burn_a"
execute if score #t ht matches 476 run tellraw @a "HLSHOT hl_burn_b"
execute if score #t ht matches 490 run tellraw @a "HLSHOT hl_burn_c"
execute if score #t ht matches 500 run tellraw @a "HLSHOT hl_burn_d"
# 28 s: Heat vision at 4 blocks (beams 0.85-2.75 s), camera further back
execute if score #t ht matches 558 run tp @a 2.4 100.2 1.2 60 12
execute if score #t ht matches 560 run function homelandertest:victim
execute if score #t ht matches 560 run tp @e[type=minecraft:villager] 0 100 4 180 0
execute if score #t ht matches 560 run tag @e[type=homelander:homelander] add hl_laser
execute if score #t ht matches 582 run tellraw @a "HLSHOT hl_laser_a"
execute if score #t ht matches 600 run tellraw @a "HLSHOT hl_laser_b"
execute if score #t ht matches 620 run tellraw @a "HLSHOT hl_laser_c"
# 42 s: Run: a villager 14 blocks away, he chases it (sprinting; the log prints his speed every second)
execute if score #t ht matches 860 run kill @e[type=minecraft:villager]
execute if score #t ht matches 860 run tp @e[type=homelander:homelander] -14 100 0 -90 0
execute if score #t ht matches 860 run tp @a 0 101 -15 0 4
execute if score #t ht matches 862 run summon minecraft:villager 14 100 0 {PersistenceRequired:1b,NoAI:1b,Health:1000f,attributes:[{id:"minecraft:generic.max_health",base:1000d}]}
execute if score #t ht matches 864 run tag @e[type=homelander:homelander] add hl_fight
execute if score #t ht matches 872 run tellraw @a "HLSHOT hl_run_a"
execute if score #t ht matches 875 run tellraw @a "HLSHOT hl_run_b"
execute if score #t ht matches 878 run tellraw @a "HLSHOT hl_run_c"
execute if score #t ht matches 881 run tellraw @a "HLSHOT hl_run_d"
# 46 s: free fight against a tough zombie (random attacks, walk while attacking)
execute if score #t ht matches 920 run kill @e[type=minecraft:villager]
execute if score #t ht matches 920 run tag @e[type=homelander:homelander] remove hl_hold
execute if score #t ht matches 920 run tp @e[type=homelander:homelander] 0 100 0 0 0
execute if score #t ht matches 920 run tp @a 6 101 6 135 15
execute if score #t ht matches 920 run summon minecraft:zombie 0 100 7 {PersistenceRequired:1b,Health:300f,ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",count:1}],attributes:[{id:"minecraft:generic.max_health",base:300d}]}
execute if score #t ht matches 960 run tellraw @a "HLSHOT hl_zombie_1"
execute if score #t ht matches 1040 run tellraw @a "HLSHOT hl_zombie_2"
execute if score #t ht matches 1120 run tellraw @a "HLSHOT hl_zombie_3"
execute if score #t ht matches 1200 run tellraw @a "HLSHOT hl_zombie_4"
# 66 s: free fight against the player (cannot die)
execute if score #t ht matches 1320 run kill @e[type=minecraft:zombie]
execute if score #t ht matches 1320 run tag @e[type=homelander:homelander] remove hl_fight
execute if score #t ht matches 1320 run tag @e[type=homelander:homelander] add hl_player
execute if score #t ht matches 1320 run gamemode survival @a
execute if score #t ht matches 1320 run effect give @a minecraft:resistance infinite 255 true
execute if score #t ht matches 1320 run effect give @a minecraft:regeneration infinite 255 true
execute if score #t ht matches 1320 run tp @a 0 100 6 180 8
execute if score #t ht matches 1420 run tellraw @a "HLSHOT hl_player_1"
execute if score #t ht matches 1540 run tellraw @a "HLSHOT hl_player_2"
# 86 s: done; HUD back, spawn egg in slot 1; CMTEST ui (client) shoots the menus + update notice, then logs "HLTEST done"
execute if score #t ht matches 1720 run kill @e[type=homelander:homelander]
execute if score #t ht matches 1720 run gamemode creative @a
execute if score #t ht matches 1720 run tellraw @a "HLHUD on"
execute if score #t ht matches 1720 run item replace entity @a hotbar.0 with homelander:homelander_spawn_egg 4
execute if score #t ht matches 1740 run tellraw @a "CMTEST ui"
