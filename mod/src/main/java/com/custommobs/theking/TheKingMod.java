package com.custommobs.theking;

import com.custommobs.theking.entity.KingEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/* ───────── The King: main mod class (both sides) ─────────
   Registers the mob, its spawn egg, its sounds and its own creative tab, plus spawn rules and the config. */
@Mod(TheKingMod.MODID)
public final class TheKingMod {
    public static final String MODID = "theking";

    /* ───────── Registries ───────── */
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Hitbox 0.7 x 1.95: fits through 2-block doors (the crown and hair top reach a bit higher, like the model).
    public static final DeferredHolder<EntityType<?>, EntityType<KingEntity>> KING = ENTITIES.register("the_king",
            () -> EntityType.Builder.of(KingEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.95F).eyeHeight(1.85F)
                    .clientTrackingRange(10).updateInterval(2) // position updates every 2 ticks: a smooth dropkick leap
                    .build("the_king"));

    // Spawn egg colors: pink shirt + golden crown.
    public static final DeferredItem<DeferredSpawnEggItem> KING_SPAWN_EGG = ITEMS.register("the_king_spawn_egg",
            () -> new DeferredSpawnEggItem(KING, 0xF9D5E5, 0xFACC15, new Item.Properties()));

    // Voice lines (mouth lip-synced on the client) and the dance music. Order of VOICE = rig.json "voice".
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_CUSSING_SHORT = sound("voice_cussing_short");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_CUSSING_LONG = sound("voice_cussing_long");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_TAKE_YOUR_STUFF = sound("voice_take_your_stuff");
    public static final DeferredHolder<SoundEvent, SoundEvent> DANCE_MUSIC = sound("dance_music");
    @SuppressWarnings("unchecked")
    public static final DeferredHolder<SoundEvent, SoundEvent>[] VOICE = new DeferredHolder[]{VOICE_CUSSING_SHORT, VOICE_CUSSING_LONG, VOICE_TAKE_YOUR_STUFF};

    // Own creative tab (after Spawn Eggs).
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("the_king", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.theking"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> KING_SPAWN_EGG.get().getDefaultInstance())
            .displayItems((params, output) -> output.accept(KING_SPAWN_EGG.get()))
            .build());

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, name)));
    }

    /* ───────── Setup ───────── */
    public TheKingMod(IEventBus modBus, ModContainer container) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, KingConfig.COMMON_SPEC);
        modBus.addListener(EntityAttributeCreationEvent.class, e -> e.put(KING.get(), KingEntity.createAttributes().build()));
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> e.register(KING.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, KingEntity::checkKingSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        // QoL: the egg also sits in the vanilla Spawn Eggs tab.
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, e -> {
            if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) e.accept(KING_SPAWN_EGG.get());
        });
    }
}
