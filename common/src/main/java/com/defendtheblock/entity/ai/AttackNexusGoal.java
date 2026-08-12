package com.defendtheblock.entity.ai;

import com.defendtheblock.config.DtbConfig;
import com.defendtheblock.entity.invader.InvaderAccess;
import com.defendtheblock.entity.invader.InvaderCombatPriority;
import com.defendtheblock.entity.invader.InvaderData;
import com.defendtheblock.invasion.NexusManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Goal principal do invasor: caminhar ate o Nexus e bater nele.
 *
 * <p>Cede a vez para as IAs vanilla de combate sempre que o mob tem um alvo
 * vivo por perto <b>e ainda nao chegou no Nexus</b> — assim os mobs continuam
 * caçando o jogador/torreta que estiver no caminho. Mas uma vez que o mob
 * esta mesmo dentro do alcance de ataque do Nexus, {@link #canStart()} volta
 * a valer true incondicionalmente: o Nexus e sempre a prioridade final,
 * mesmo que uma torreta ou o jogador estejam bem do lado. Essa goal e
 * instalada com prioridade numerica bem abaixo das goals vanilla de combate
 * (ver {@code InvaderGoals#install}), entao quando ela quer rodar, ela ganha
 * o controle de movimento/olhar de qualquer goal vanilla que tambem queira.
 */
public class AttackNexusGoal extends Goal {

    /**
     * Alcance de golpe no Nexus. Nao e private porque a
     * {@link YieldToWorkerGoal} precisa do <b>mesmo</b> numero: ela so deixa de
     * recuar quando o mob ja pode bater no bloco, e duas constantes separadas
     * abririam uma faixa em que o mob recua exatamente onde deveria atacar.
     */
    static final double ATTACK_RANGE = 2.8D;
    private static final int REPATH_INTERVAL = 20;
    private static final int STUCK_CHECK_INTERVAL = 40;
    /** Raio, em blocos, varrido em busca de um desvio quando o mob empaca. */
    private static final int DETOUR_RADIUS = 4;
    /** O desvio precisa aproximar pelo menos isso do Nexus para valer a pena. */
    private static final double MIN_DETOUR_GAIN = 1.5D;

    private final MobEntity mob;
    private final InvaderData data;
    private final double speed;

    private int repathTimer;
    private int stuckTimer;

    public AttackNexusGoal(MobEntity mob, double speed) {
        this.mob = mob;
        this.data = InvaderAccess.of(mob);
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (data == null || !data.isInvader() || data.getNexusPos() == null) {
            return false;
        }
        if (!(mob.getWorld() instanceof ServerWorld world) || NexusManager.getData(world).isGameOver()) {
            return false;
        }
        return isAtNexus() || !hasCloseTarget();
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    /** Ja esta dentro do alcance de golpe do Nexus — nesse caso nada mais tem prioridade. */
    private boolean isAtNexus() {
        return mob.squaredDistanceTo(NexusPathing.center(data.getNexusPos())) <= ATTACK_RANGE * ATTACK_RANGE;
    }

    /**
     * Alvo vivo e perto o bastante para o mob preferir resolve-lo primeiro.
     *
     * <p>Enquanto isto e true, esta goal <b>nao roda</b> e o controle de
     * movimento fica com as IAs de combate do vanilla. Para um esqueleto isso
     * e o que o faz recuar e ficar atirando (o {@code ProjectileAttackGoal}
     * do vanilla sabe manter distancia) em vez de marchar para o bloco.
     *
     * <p>O raio vem de {@link InvaderCombatPriority#engageRange}, o mesmo que
     * decide ate quando o alvo e mantido. Usar um numero diferente aqui era
     * justamente o bug: um esqueleto com uma torreta a 15 blocos mantinha o
     * alvo (20 de alcance la) mas, como aqui o raio era 6, esta goal rodava e
     * o empurrava para o Nexus — ele avancava em vez de atirar.
     */
    private boolean hasCloseTarget() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double range = InvaderCombatPriority.engageRange(mob, target);
        return mob.squaredDistanceTo(target) < range * range;
    }

    @Override
    public void start() {
        // Espalhados de proposito, em vez de zerados. Um lote de invasores nasce
        // no mesmo tick e comeca esta goal no mesmo tick, entao com o contador
        // zerado a horda inteira recalcula rota <b>no mesmo tick</b>, para
        // sempre — e busca de caminho e de longe a coisa mais cara que um mob
        // faz. Com o teto em 100 isso seria um pico periodico de 100 buscas num
        // tick e zero nos 19 seguintes; assim a carga fica diluida.
        repathTimer = mob.getRandom().nextInt(REPATH_INTERVAL);
        stuckTimer = mob.getRandom().nextInt(STUCK_CHECK_INTERVAL);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos nexus = data.getNexusPos();
        Vec3d center = NexusPathing.center(nexus);
        double distance = Math.sqrt(mob.squaredDistanceTo(center));

        mob.getLookControl().lookAt(center.x, center.y, center.z);

        if (distance <= ATTACK_RANGE) {
            // Perto o bastante nao basta: precisa haver linha limpa ate o
            // bloco. Nexus emparedado nao toma dano atraves da parede — a
            // cobertura vira o obstaculo a ser arrombado.
            BlockPos cover = NexusPathing.findCover(mob.getWorld(), mob.getEyePos(), nexus);
            if (cover == null) {
                mob.getNavigation().stop();
                attackNexus(nexus);
                return;
            }
            data.setObstacle(cover);
            // Nao para a navegacao: segue para o codigo de movimento abaixo,
            // que faz o mob procurar um lado exposto (ou empacar e acionar a
            // reavaliacao de rota). Parar aqui deixaria a horda encostada na
            // parede sem nunca tentar contornar.
        }

        // Uma porta fechada bem na frente sempre vira obstaculo marcado, mesmo
        // que a navegacao vanilla conseguisse abri-la sozinha e passar: o
        // invasor tem que arrombar (BreachObstacleGoal, prioridade mais alta),
        // nunca so atravessar.
        if (data.getObstacle() == null) {
            BlockPos door = NexusPathing.findClosedDoorAhead(mob, nexus);
            if (door != null) {
                data.setObstacle(door);
            }
        }

        if (--repathTimer <= 0) {
            repathTimer = REPATH_INTERVAL;
            boolean moving = mob.getNavigation().startMovingTo(center.x, center.y, center.z, speed);
            if (!moving) {
                // Sem caminho: voadores usam o move control direto, os de chao
                // marcam o obstaculo para as IAs de arrombamento.
                mob.getMoveControl().moveTo(center.x, center.y, center.z, speed);
                markBlocked(nexus);
            }
        }

        if (++stuckTimer >= STUCK_CHECK_INTERVAL) {
            stuckTimer = 0;
            double previous = data.getLastDistanceToNexus();
            if (distance > previous - 0.75D) {
                // Antes de assumir que o caminho esta bloqueado e chamar as IAs
                // de arrombar/construir, tenta um desvio: talvez exista um
                // caminho limpo a poucos blocos daqui que ja aproxima do Nexus.
                if (!tryDetour(nexus, distance)) {
                    markBlocked(nexus);
                    // Sinal compartilhado com BridgeToNexusGoal: quantos ciclos seguidos
                    // o mob nao avancou. Um Nexus suspenso no ar e o caso tipico.
                    data.setStuckTicks(data.getStuckTicks() + 1);
                }
            } else {
                data.setObstacle(null);
                data.setStuckTicks(0);
            }
            data.setLastDistanceToNexus(distance);
        }
    }

    /**
     * Reavaliacao de rota: procura, num raio curto em volta do mob, algum
     * ponto que (a) fique mais perto do Nexus do que ele esta agora e (b) o
     * pathfinding vanilla consiga alcancar de verdade. Se achar, anda para la
     * em vez de tratar o obstaculo a frente como intransponivel.
     *
     * <p>E o que resolve o caso "tem uma passagem limpa tres blocos ao lado,
     * mas o mob fica batendo na parede porque ela esta exatamente na linha
     * reta ate o Nexus". Sem isso o invasor so tinha dois caminhos: quebrar o
     * que estava na frente ou empilhar bloco para subir — nunca simplesmente
     * contornar.
     *
     * @return true se um desvio foi encontrado e o mob ja esta indo para la
     */
    private boolean tryDetour(BlockPos nexus, double currentDistance) {
        double limit = currentDistance - MIN_DETOUR_GAIN;
        if (limit <= 0.0D) {
            // Ja praticamente em cima do Nexus: nenhum desvio aproximaria mais.
            return false;
        }

        // O cubo tem 9x9x3 = 243 posicoes e roda a cada STUCK_CHECK_INTERVAL em
        // cada invasor empacado. A versao antiga criava um Vec3d e um BlockPos
        // por posicao so para medir distancia; aqui a conta e feita em escalares
        // e so o melhor candidato ate agora vira objeto.
        double cx = nexus.getX() + 0.5D;
        double cy = nexus.getY() + 0.5D;
        double cz = nexus.getZ() + 0.5D;
        BlockPos origin = mob.getBlockPos();

        BlockPos best = null;
        // Comparar ao quadrado evita uma raiz por posicao; o limite so pode ser
        // elevado com seguranca porque ja garantimos que ele e positivo acima.
        double bestSquared = limit * limit;

        for (int dx = -DETOUR_RADIUS; dx <= DETOUR_RADIUS; dx++) {
            for (int dz = -DETOUR_RADIUS; dz <= DETOUR_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                double ox = origin.getX() + dx + 0.5D - cx;
                double oz = origin.getZ() + dz + 0.5D - cz;
                double horizontal = ox * ox + oz * oz;
                for (int dy = -1; dy <= 1; dy++) {
                    double oy = origin.getY() + dy + 0.5D - cy;
                    double squared = horizontal + oy * oy;
                    if (squared >= bestSquared) {
                        continue;
                    }
                    bestSquared = squared;
                    best = origin.add(dx, dy, dz);
                }
            }
        }

        if (best == null) {
            return false;
        }
        // So agora paga o custo da busca de caminho, e so para o melhor
        // candidato: fazer isso para cada posicao do cubo seria caro demais
        // rodando em dezenas de mobs ao mesmo tempo.
        Path path = mob.getNavigation().findPathTo(best, 0);
        if (path == null || !path.reachesTarget()) {
            return false;
        }
        return mob.getNavigation().startMovingAlong(path, speed);
    }

    private void markBlocked(BlockPos nexus) {
        BlockPos obstacle = NexusPathing.findObstacle(mob, nexus);
        if (obstacle != null) {
            data.setObstacle(obstacle);
        }
    }

    private void attackNexus(BlockPos nexus) {
        if (!data.canHitNexus() || !(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        // O mob guarda a SUA PROPRIA copia da posicao do Nexus, tirada quando
        // foi recrutado. Se o Nexus foi removido/recolocado nesse meio tempo
        // (por exemplo via /dtb removenexus), essa copia fica desatualizada —
        // sem essa checagem, o dano cairia no Nexus atual (estado global) mesmo
        // com o mob fisicamente parado no lugar do Nexus antigo, "atacando o
        // vento" e ainda assim ferindo um Nexus novo em outro lugar.
        if (!nexus.equals(NexusManager.getData(world).getNexusPos())) {
            data.setInvader(false);
            mob.setTarget(null);
            return;
        }
        // O creeper e o unico invasor que NAO danifica o Nexus. O papel dele na
        // horda e abrir passagem: ele se explode em obstaculos (ver
        // BreachObstacleGoal) para o resto da horda entrar, e nada mais. Sem
        // isso um punhado de creepers derrubava o bloco sozinho e tirava a
        // graca de todo o resto da invasao.
        if (mob instanceof CreeperEntity) {
            return;
        }

        DtbConfig config = DtbConfig.get();
        data.setNexusHitCooldown(config.nexusHitCooldown);
        data.setObstacle(null);
        data.setStuckTicks(0);

        mob.swingHand(Hand.MAIN_HAND);
        int damage = config.nexusDamagePerHit + data.getWave() / 4;
        world.playSound(null, nexus, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.HOSTILE, 0.8F, 1.0F);
        NexusManager.damage(world, damage, mob);
    }
}
