package com.fexa.anydimensionblock.registry;

import com.fexa.anydimensionblock.AnyDimensionBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDimensions {

    // Les types de dimensions sont définis via JSON dans data/
    // Ce register est utilisé pour d'éventuels types custom si besoin
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES =
            DeferredRegister.create(Registries.DIMENSION_TYPE, AnyDimensionBlock.MODID);
}
