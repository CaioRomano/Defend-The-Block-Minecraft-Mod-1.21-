package com.defendtheblock.compat;

import com.defendtheblock.invasion.InvasionData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

/** Wrapper de {@link PersistentState} para 1.20.1. */
public class InvasionPersistentState extends PersistentState {

    public static final String KEY = "defendtheblock_invasion";

    public final InvasionData data = new InvasionData();

    public InvasionPersistentState() {
        data.setDirtyMarker(this::markDirty);
    }

    public static InvasionPersistentState fromNbt(NbtCompound nbt) {
        InvasionPersistentState state = new InvasionPersistentState();
        state.data.readNbt(nbt);
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return data.writeNbt(nbt);
    }
}
