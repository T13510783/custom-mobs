package com.custommobs.homelander;

import com.custommobs.homelander.entity.HomelanderEntity;
import com.custommobs.theking.TheKingMod;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/* ───────── Homelander: main class (both sides) ─────────
   Same mod (jar + mod id "theking") as The King, own namespace "homelander" for its entity, egg, sounds, tab,
   textures and data. Registers the mob, its spawn egg, its sounds, its creative tab, spawn rules and the config. */
@Mod(TheKingMod.MODID)
public final class HomelanderMod {
    public static final String NS = "homelander";

    /* ───────── Registries ───────── */
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NS);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NS);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NS);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NS);

    // Hitbox 0.6 x 1.95 (player size: his model is a 32-texel humanoid). Position every tick: held victims stay on his hand.
    public static final DeferredHolder<EntityType<?>, EntityType<HomelanderEntity>> HOMELANDER = ENTITIES.register("homelander",
            () -> EntityType.Builder.of(HomelanderEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.75F)
                    .clientTrackingRange(10).updateInterval(1)
                    .build("homelander"));

    // Spawn egg colors: navy suit + red cape.
    public static final DeferredItem<DeferredSpawnEggItem> HOMELANDER_SPAWN_EGG = ITEMS.register("homelander_spawn_egg",
            () -> new DeferredSpawnEggItem(HOMELANDER, 0x1E3A8A, 0xB91C1C, new Item.Properties()));

    // Voice line (mouth lip-synced on the client) + sound effects (viewer SFX_EVENTS). Order of VOICE = rig.json "voice".
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICE_I_AM_GOD = sound("voice_i_am_god");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUNCH = sound("punch");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAB = sound("grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHOOSH = sound("whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLAP = sound("slap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LASER = sound("laser");
    @SuppressWarnings("unchecked")
    public static final DeferredHolder<SoundEvent, SoundEvent>[] VOICE = new DeferredHolder[]{VOICE_I_AM_GOD};

    // Own creative tab (after Spawn Eggs).
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("homelander", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.homelander"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> HOMELANDER_SPAWN_EGG.get().getDefaultInstance())
            .displayItems((params, output) -> output.accept(HOMELANDER_SPAWN_EGG.get()))
            .build());

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NS, name)));
    }

    /* ───────── Setup ───────── */
    public HomelanderMod(IEventBus modBus, ModContainer container) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, HomelanderConfig.COMMON_SPEC, "theking-homelander-common.toml");
        modBus.addListener(EntityAttributeCreationEvent.class, e -> e.put(HOMELANDER.get(), HomelanderEntity.createAttributes().build()));
        modBus.addListener(RegisterSpawnPlacementsEvent.class, e -> e.register(HOMELANDER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HomelanderEntity::checkHomelanderSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        // QoL: the egg also sits in the vanilla Spawn Eggs tab.
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, e -> {
            if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) e.accept(HOMELANDER_SPAWN_EGG.get());
        });
        // A victim held by the throat cannot climb off (players press sneak to dismount).
        NeoForge.EVENT_BUS.addListener(EntityMountEvent.class, e -> {
            if (e.isDismounting() && e.getEntityBeingMounted() instanceof HomelanderEntity h && h.isHolding(e.getEntityMounting()))
                e.setCanceled(true);
        });
    }
}
