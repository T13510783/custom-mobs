scoreboard players set #t kt 0
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule doMobSpawning false
gamerule sendCommandFeedback false
time set 6000
weather clear
difficulty normal
gamemode creative @a
kill @e[type=!player]
# Stage in the sky (works on any world type): grass floor at y 99, empty air above.
fill -15 99 -15 15 99 15 minecraft:grass_block
fill -15 100 -15 15 110 15 minecraft:air
# Glass walls: nobody falls (or gets kicked) off the stage.
fill -16 99 -16 16 102 -16 minecraft:glass
fill -16 99 16 16 102 16 minecraft:glass
fill -16 99 -16 -16 102 16 minecraft:glass
fill 16 99 -16 16 102 16 minecraft:glass
tp @a 0 100 6 180 8
summon theking:the_king 0 100 0 {Rotation:[0f,0f],NoAI:1b,PersistenceRequired:1b}