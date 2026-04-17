package com.fexa.anydimensionblock.registry;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import com.fexa.anydimensionblock.item.DimensionWandItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, AnyDimensionBlock.MODID);

    public static final DeferredHolder<Item, DimensionWandItem> DIMENSION_WAND =
            ITEMS.register("dimension_wand", DimensionWandItem::new);
}
