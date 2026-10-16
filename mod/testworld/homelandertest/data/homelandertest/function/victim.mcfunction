# A tough villager 1 block in front of Homelander, facing him (slowness: it stays where the attack needs it).
kill @e[type=minecraft:villager]
summon minecraft:villager 0 100 1.1 {PersistenceRequired:1b,Rotation:[180f,0f],Health:1000f,attributes:[{id:"minecraft:generic.max_health",base:1000d}],active_effects:[{id:"minecraft:slowness",amplifier:10b,duration:-1,show_particles:0b}]}
tp @e[type=homelander:homelander] 0 100 0 0 0
