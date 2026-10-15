# Fight test: the King against a tough husk (200 HP, so the fight lasts ~25 s and stalls show up in the log).
data merge entity @e[type=theking:the_king,limit=1] {NoAI:0b}
summon minecraft:husk 4 100 -1 {PersistenceRequired:1b,Rotation:[90f,0f],Health:200f,attributes:[{id:"minecraft:generic.max_health",base:200d}]}
damage @e[type=theking:the_king,limit=1] 1 minecraft:mob_attack by @e[type=minecraft:husk,limit=1]