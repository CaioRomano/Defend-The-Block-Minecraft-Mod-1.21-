package com.defendtheblock.entity.ai;

import com.defendtheblock.entity.invader.InvaderAbility;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.entity.projectile.WebShotEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;

/**
 * Aranha invasora cuspindo teia no alvo. As aranhas ja escalam paredes sozinhas
 * no vanilla, entao a teia e o unico extra que elas precisam.
 */
public class SpiderWebShotGoal extends Goal {

    private static final double MIN_RANGE_SQ = 9.0D;
    private static final double MAX_RANGE_SQ = 256.0D;
    private static final int COOLDOWN = 90;

    private final MobEntity spider;
    private final InvaderData data;

    private int cooldown;

    public SpiderWebShotGoal(MobEntity spider) {
        this.spider = spider;
        this.data = InvaderAccess.of(spider);
        setControls(EnumSet.of(Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.hasAbility(InvaderAbility.WEB_SHOT)) {
            return false;
        }
        LivingEntity target = spider.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = spider.squaredDistanceTo(target);
        return distance > MIN_RANGE_SQ && distance < MAX_RANGE_SQ && spider.getVisibilityCache().canSee(target);
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    @Override
    public void start() {
        cooldown = 20;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = spider.getTarget();
        if (target == null || !(spider.getWorld() instanceof ServerWorld world)) {
            return;
        }
        spider.getLookControl().lookAtEntity(target, 30.0F, 30.0F);

        if (--cooldown > 0) {
            return;
        }
        cooldown = COOLDOWN;

        WebShotEntity web = new WebShotEntity(world, spider);
        web.setPosition(spider.getX(), spider.getEyeY() - 0.1D, spider.getZ());

        double dx = target.getX() - spider.getX();
        double dy = target.getBodyY(0.3333D) - web.getY();
        double dz = target.getZ() - spider.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        web.setVelocity(dx, dy + horizontal * 0.2D, dz, 1.1F, 6.0F);

        world.spawnEntity(web);
        world.playSound(null, spider.getBlockPos(), SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 1.0F, 1.4F);
    }
}
