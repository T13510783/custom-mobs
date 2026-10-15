# Test timeline (ticks): setup when the player is in, zombie fight at 5 s, player fight at 30 s.
execute if score #t kt matches -1 if entity @a run function kingtest:setup
execute if score #t kt matches 0.. run scoreboard players add #t kt 1
execute if score #t kt matches 100 run function kingtest:fight_zombie
execute if score #t kt matches 600 run function kingtest:fight_player
# Kick tests: every 10 s put the King in the middle and the player 6 blocks away (dropkick range), 4 times.
execute if score #t kt matches 800 run tp @e[type=theking:the_king] 0 100 0
execute if score #t kt matches 800 run tp @a 0 100 6
execute if score #t kt matches 1000 run tp @e[type=theking:the_king] 0 100 0
execute if score #t kt matches 1000 run tp @a 6 100 0
execute if score #t kt matches 1200 run tp @e[type=theking:the_king] 0 100 0
execute if score #t kt matches 1200 run tp @a 0 100 -6
execute if score #t kt matches 1400 run tp @e[type=theking:the_king] 0 100 0
execute if score #t kt matches 1400 run tp @a -6 100 0
# Run test: King and player 26 blocks apart (he sprints = Run animation), twice.
execute if score #t kt matches 1600 run tp @e[type=theking:the_king] 0 100 -13 0 0
execute if score #t kt matches 1600 run tp @a 0 100 13 180 5
execute if score #t kt matches 1800 run tp @e[type=theking:the_king] -13 100 0 -90 0
execute if score #t kt matches 1800 run tp @a 13 100 0 90 5
# Spawn egg test: no Kings left, player looks at the floor with eggs in slot 1 (a right-click spawns one), count at 105 s.
execute if score #t kt matches 2000 run kill @e[type=theking:the_king]
execute if score #t kt matches 2000 run gamemode creative @a
execute if score #t kt matches 2000 run tp @a 0 100 8 180 50
execute if score #t kt matches 2000 run item replace entity @a hotbar.0 with theking:the_king_spawn_egg 4
execute if score #t kt matches 2100 store result score #kings kt if entity @e[type=theking:the_king]
execute if score #t kt matches 2100 run tellraw @a ["egg test kings: ",{"score":{"name":"#kings","objective":"kt"}}]