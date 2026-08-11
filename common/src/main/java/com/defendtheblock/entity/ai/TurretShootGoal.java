package com.defendtheblock.entity.ai;

import com.defendtheblock.compat.DtbCompat;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.turret.TurretEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

/**
 * Mira, dispara <b>e escolhe o proprio alvo</b> da torreta.
 *
 * <p>Antes, quem escolhia o alvo era um {@code ActiveTargetGoal} separado no
 * target selector: uma vez que ele pegava um alvo "vivo e dentro do
 * follow range", o vanilla so soltava esse alvo quando ele morria ou saia do
 * follow range (bem maior que o alcance de tiro real da torreta) — nunca
 * porque ficou fora de alcance de tiro, sem visada, ou porque outro mob mais
 * perto passou na frente. Na pratica a torreta "grudava" no primeiro alvo que
 * pegasse e ficava girada pra ele parada, ignorando tudo o mais.
 *
 * <p>Agora essa goal reavalia o alvo periodicamente e so troca quando o atual
 * deixa de servir (fora de alcance, sem linha de visao, ou exatamente no eixo
 * vertical da torreta) ou quando aparece um candidato bem mais perto — assim
 * ela nao fica flutuando entre alvos parecidos a toda hora, mas tambem nao
 * fica presa para sempre num alvo que nao consegue mais atacar.
 */
public class TurretShootGoal extends Goal {

    /** Ticks entre cada reavaliacao do alvo atual. */
    private static final int RESCAN_INTERVAL = 5;
    /**
     * Um candidato so substitui o alvo atual (quando o atual ainda e valido)
     * se estiver pelo menos essa distancia (ao quadrado) mais perto — evita
     * ficar trocando de alvo a toda hora entre dois mobs quase equidistantes.
     */
    private static final double SWITCH_MARGIN_SQ = 4.0D;
    /** Folga vertical da varredura: cobre torres bem altas, o alcance real e so no plano horizontal. */
    private static final double VERTICAL_SCAN_MARGIN = 64.0D;
    /**
     * Rede de seguranca: se a torreta passa esse tempo com o mesmo alvo sem
     * conseguir disparar nenhuma vez, ela desiste dele e o ignora por um tempo.
     *
     * <p>As checagens de cone/alcance/visada ja deveriam impedir isso, mas
     * qualquer caso nao previsto (mob pulando dentro e fora do cone, alvo
     * inalcancavel por geometria estranha) faria a torreta ficar parada de
     * novo — entao aqui a regra e absoluta: nao atirou nesse tempo, troca.
     */
    private static final int NO_FIRE_TIMEOUT = 40;
    /** Por quantos ticks um alvo abandonado por timeout fica fora da escolha. */
    private static final int REJECT_MEMORY = 100;

    private final TurretEntity turret;
    private int rescanTimer;
    private int ticksOnTarget;
    private LivingEntity rejectedTarget;
    private int rejectedTimer;
    /**
     * As outras torretas que podem estar na linha de tiro, buscadas <b>uma vez
     * por rescan</b> em vez de uma vez por candidato.
     *
     * <p>Antes {@link #blockedByTurret} fazia a sua propria busca de entidades,
     * e como ele e chamado de dentro de {@link #isEngageable}, uma varredura de
     * N candidatos custava N buscas — com varias torretas e uma horda inteira
     * no alcance, isso e o laco mais caro da goal. Torreta nao anda: a lista so
     * muda quando alguem coloca ou destroi uma, entao ficar ate
     * {@code RESCAN_INTERVAL} ticks desatualizada e inofensivo (o javadoc de
     * {@link #blockedByTurret} ja dizia que a desobstrucao vale "no proximo
     * rescan"). A checagem de {@code isAlive} continua sendo feita na hora do
     * uso, para uma torreta destruida nesse meio tempo nao seguir bloqueando.
     */
    private List<TurretEntity> nearbyTurrets = List.of();

    public TurretShootGoal(TurretEntity turret) {
        this.turret = turret;
        setControls(EnumSet.of(Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return true;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (rejectedTimer > 0 && --rejectedTimer == 0) {
            rejectedTarget = null;
        }

        LivingEntity previous = turret.getTarget();
        if (--rescanTimer <= 0) {
            rescanTimer = RESCAN_INTERVAL;
            refreshNearbyTurrets();
            retarget();
        }

        LivingEntity target = turret.getTarget();
        if (target != previous) {
            ticksOnTarget = 0;
        }
        if (target == null || !(turret.getWorld() instanceof ServerWorld world) || !isEngageable(target)) {
            return;
        }

        // Rede de seguranca: tempo demais no mesmo alvo sem disparar significa
        // que algo o torna inatingivel na pratica — desiste e ignora por um
        // tempo, em vez de ficar parada mirando nele para sempre.
        if (++ticksOnTarget > NO_FIRE_TIMEOUT) {
            rejectedTarget = target;
            rejectedTimer = REJECT_MEMORY;
            turret.setTarget(null);
            ticksOnTarget = 0;
            return;
        }

        // So atira quando a besta ja estiver de fato apontada para o alvo: sem
        // isso o tiro saia primeiro e a rotacao seguia depois, visivelmente errado.
        if (!turret.aimAt(target)) {
            return;
        }
        if (turret.getCooldown() > 0 || !turret.hasAmmo()) {
            return;
        }

        turret.setCooldown(turret.getReloadTicks());
        int shots = turret.getMultishot() > 0 ? 3 : 1;
        for (int i = 0; i < shots; i++) {
            fire(world, target, i, shots);
        }
        turret.consumeAmmo();
        // Disparou: o alvo esta funcionando, zera o relogio da rede de seguranca.
        ticksOnTarget = 0;

        world.playSound(null, turret.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.NEUTRAL,
                1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + 0.3F);
    }

    private void retarget() {
        LivingEntity current = turret.getTarget();
        boolean currentOk = isEngageable(current);
        LivingEntity best = findBestTarget();

        if (best == null) {
            if (!currentOk) {
                turret.setTarget(null);
            }
            return;
        }
        if (!currentOk) {
            turret.setTarget(best);
            return;
        }
        if (best != current
                && turret.horizontalSquaredDistanceTo(best) + SWITCH_MARGIN_SQ < turret.horizontalSquaredDistanceTo(current)) {
            turret.setTarget(best);
        }
    }

    /** Alvo valido, dentro de alcance (no plano horizontal), dentro do cone de visao e visivel. */
    private boolean isEngageable(LivingEntity target) {
        if (target == null || !target.isAlive() || target == rejectedTarget) {
            return false;
        }
        double range = turret.getRange();
        return turret.horizontalSquaredDistanceTo(target) <= range * range
                && turret.isInVisionCone(target)
                && turret.getVisibilityCache().canSee(target)
                && !blockedByTurret(target);
    }

    /**
     * Outra torreta esta na frente, no caminho do tiro?
     *
     * <p>Torretas nao atiram umas nas outras (ver
     * {@code TurretEntity#damage}), mas antes disso elas tambem nao deveriam
     * <b>tentar</b> — uma torreta plantada na linha de fogo de outra bloqueia
     * o campo de visao dela, do mesmo jeito que uma parede bloquearia.
     *
     * <p>Precisa ser feito a mao porque o {@code canSee} do vanilla so testa
     * blocos: para ele, uma entidade no meio do caminho nao existe.
     *
     * <p>A "desobstrucao" e automatica e nao precisa de nenhum evento: a
     * torreta que bloqueava simplesmente deixa de estar na lista de entidades
     * quando e destruida, e o tiro volta a passar no proximo rescan.
     */
    private boolean blockedByTurret(LivingEntity target) {
        if (nearbyTurrets.isEmpty()) {
            return false;
        }
        Vec3d from = new Vec3d(turret.getX(), turret.getEyeY(), turret.getZ());
        Vec3d to = target.getBoundingBox().getCenter();

        for (TurretEntity other : nearbyTurrets) {
            if (other.isAlive() && other.getBoundingBox().expand(0.05D).raycast(from, to).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recolhe as torretas que cabem dentro do alcance de tiro. E a mesma caixa
     * de {@link #findBestTarget}, com a folga de 1 bloco que a busca antiga
     * fazia por segmento: qualquer torreta capaz de cruzar a linha ate um alvo
     * dentro do alcance esta necessariamente aqui dentro.
     */
    private void refreshNearbyTurrets() {
        double range = turret.getRange() + 1.0D;
        double vertical = VERTICAL_SCAN_MARGIN + 1.0D;
        Box box = new Box(turret.getX() - range, turret.getY() - vertical, turret.getZ() - range,
                turret.getX() + range, turret.getY() + vertical, turret.getZ() + range);
        nearbyTurrets = turret.getWorld().getEntitiesByClass(TurretEntity.class, box,
                candidate -> candidate != turret);
    }

    private LivingEntity findBestTarget() {
        double range = turret.getRange();
        Box box = new Box(turret.getX() - range, turret.getY() - VERTICAL_SCAN_MARGIN, turret.getZ() - range,
                turret.getX() + range, turret.getY() + VERTICAL_SCAN_MARGIN, turret.getZ() + range);
        List<LivingEntity> candidates = turret.getWorld().getEntitiesByClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (entity instanceof Monster || InvaderAccess.isInvader(entity)));

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            // A distancia vem primeiro de proposito: quem ja esta mais longe que
            // o melhor ate agora nao pode vencer, entao nem paga o preco do
            // cone/visada/raycast de isEngageable. O resultado e o mesmo, so
            // que sem checar o caro para candidatos que seriam descartados.
            double distance = turret.horizontalSquaredDistanceTo(candidate);
            if (distance >= bestDistance || !isEngageable(candidate)) {
                continue;
            }
            bestDistance = distance;
            best = candidate;
        }
        return best;
    }

    private void fire(ServerWorld world, LivingEntity target, int index, int shots) {
        PersistentProjectileEntity arrow = DtbCompat.createArrow(world, turret, turret.getAmmoStack());
        arrow.setPosition(turret.getX(), turret.getEyeY() + 0.25D, turret.getZ());

        double dx = target.getX() - arrow.getX();
        double dy = target.getBodyY(0.4D) - arrow.getY();
        double dz = target.getZ() - arrow.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        // Multishot abre um leque de 3 flechas, igual a besta do jogador.
        float spread = shots > 1 ? (index - (shots - 1) / 2.0F) * 10.0F : 0.0F;
        arrow.setVelocity(dx, dy + horizontal * 0.06D, dz, 2.6F, spread);

        arrow.setDamage(turret.getArrowDamage());
        arrow.setCritical(true);
        DtbCompat.applyPunch(arrow, turret.getPunch());
        DtbCompat.applyPiercing(arrow, turret.getPiercing());
        if (turret.getFlame() > 0) {
            arrow.setFireTicks(100);
        }
        world.spawnEntity(arrow);
    }
}
