# Homelander test stage (gradlew runHomelanderTest): sky stage, walls, Homelander in the middle (no AI yet), player watching from the side.
scoreboard players set #t ht 0
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule doMobSpawning false
gamerule sendCommandFeedback false
gamerule mobGriefing false
time set 6000
weather clear
difficulty normal
gamemode creative @a
kill @e[type=!player]
fill -15 99 -15 15 99 15 minecraft:grass_block
fill -15 100 -15 15 112 15 minecraft:air
fill -16 99 -16 16 102 -16 minecraft:glass
fill -16 99 16 16 102 16 minecraft:glass
fill -16 99 -16 -16 102 16 minecraft:glass
fill 16 99 -16 16 102 16 minecraft:glass
tp @a 0 100.3 2.3 180 8
summon homelander:homelander 0 100 0 {Rotation:[0f,0f],NoAI:1b,PersistenceRequired:1b,Tags:["hl_hold"]}
tellraw @a "HLTEST setup"
