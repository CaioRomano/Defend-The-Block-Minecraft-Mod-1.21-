package com.defendtheblock.item;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.invader.InvaderAbility;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.entity.invader.InvaderGoals;
import com.defendtheblock.invasion.InvaderEquipment;
import com.defendtheblock.invasion.InvasionData;
import com.defendtheblock.invasion.NexusManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Ovo que invoca um invasor com uma habilidade especifica ja garantida, em vez
 * de depender do sorteio da onda.
 *
 * <p>Serve para testar (e para o jogador brincar com) cada tipo de zumbi
 * especial isoladamente: o das escadas, o da TNT, o construtor, o do isqueiro
 * e o da picareta. O mob nasce ja recrutado pela invasao — marcha para o Nexus
 * e usa a habilidade como qualquer invasor da horda — mas <b>nao conta na
 * contagem oficial da onda</b>, entao invocar um punhado deles nao bagunca o
 * HUD nem o fim da noite.
 */
public class InvaderEggItem extends Item {

    private final EntityType<? extends MobEntity> type;
    private final InvaderAbility ability;

    public InvaderEggItem(Settings settings, EntityType<? extends MobEntity> type, InvaderAbility ability) {
        super(settings);
        this.type = type;
        this.ability = ability;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        MobEntity mob = type.create(world);
        if (mob == null) {
            return ActionResult.FAIL;
        }
        mob.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                world.getRandom().nextFloat() * 360.0F, 0.0F);
        DtbCompat.initializeMob(serverWorld, mob, pos);

        InvaderData data = InvaderAccess.of(mob);
        if (data == null) {
            return ActionResult.FAIL;
        }

        InvasionData invasion = NexusManager.getData(serverWorld);
        int wave = Math.max(1, invasion.getCurrentWave());

        // Vira invasor, mas fora da contagem da onda: e um mob invocado a mao,
        // nao parte do lote, entao nao entra em activeInvaders.
        data.setInvader(true);
        data.setWave(wave);
        if (invasion.hasNexus()) {
            data.setNexusPos(invasion.getNexusPos());
        }

        // As duas habilidades que todo invasor tem, mais a deste ovo.
        data.addAbility(InvaderAbility.LADDER_CLIMB);
        data.addAbility(InvaderAbility.DOOR_BREACHER);
        data.addAbility(ability);

        InvaderEquipment.equip(serverWorld, mob, data, wave, world.getRandom());
        InvaderGoals.install(mob, data);
        mob.setPersistent();

        if (!serverWorld.spawnEntity(mob)) {
            return ActionResult.FAIL;
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
