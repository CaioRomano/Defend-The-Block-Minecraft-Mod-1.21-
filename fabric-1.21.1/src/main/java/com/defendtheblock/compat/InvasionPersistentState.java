package com.defendtheblock.compat;

import com.defendtheblock.invasion.InvasionData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

/**
 * Wrapper de {@link PersistentState} para 1.21.x, onde {@code writeNbt} e
 * {@code readNbt} recebem tambem o {@code RegistryWrapper.WrapperLookup}.
 */
public class InvasionPersistentState extends PersistentState {

    public static final String KEY = "defendtheblock_invasion";

    public final InvasionData data = new InvasionData();

    public InvasionPersistentState() {
        data.setDirtyMarker(this::markDirty);
    }

    public static InvasionPersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        InvasionPersistentState state = new InvasionPersistentState();
        state.data.readNbt(nbt);
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        return data.writeNbt(nbt);
    }

    public static Type<InvasionPersistentState> type() {
        return new Type<>(InvasionPersistentState::new, InvasionPersistentState::fromNbt, null);
    }
}
