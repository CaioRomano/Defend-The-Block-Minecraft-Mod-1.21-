package com.defendtheblock.entity.invader;

/**
 * Habilidades extras que um invasor pode receber.
 *
 * <p>Subir escada ({@link #LADDER_CLIMB}) e arrombar porta ({@link #DOOR_BREACHER})
 * sao dadas a <em>todo</em> invasor exceto o creeper. As outras sao sorteadas
 * com chances baixas configuraveis em {@link com.defendtheblock.config.DtbConfig}.
 */
public enum InvaderAbility {
    /** Todo invasor sobe escadas que encontrar no caminho. */
    LADDER_CLIMB,
    /**
     * Todo invasor, exceto o creeper, arromba portas fechadas em vez de
     * simplesmente abri-las — nunca so passa por elas.
     */
    DOOR_BREACHER,
    /** Creeper que se explode no obstaculo quando nao ha caminho ate o Nexus. */
    SUICIDE_BREACH,
    /** Aranha que atira teias nos alvos. */
    WEB_SHOT,
    /** Zumbi com picareta: minera o bloco que atrapalha. */
    PICKAXE_MINER,
    /** Zumbi com escadas: constroi uma coluna de escadas para subir o obstaculo. */
    LADDER_BUILDER,
    /** Zumbi com TNT: planta e acende TNT na frente do obstaculo. */
    TNT_SAPPER,
    /** Zumbi com isqueiro: incendeia obstaculos de madeira em vez de quebra-los. */
    FIRE_STARTER,
    /**
     * Zumbi construtor: carrega blocos e ergue caminho/pilar ate o Nexus muito
     * mais rapido e mais cedo que um invasor comum.
     */
    BLOCK_BUILDER;

    private static final InvaderAbility[] VALUES = values();

    public int bit() {
        return 1 << ordinal();
    }

    public static int toMask(Iterable<InvaderAbility> abilities) {
        int mask = 0;
        for (InvaderAbility ability : abilities) {
            mask |= ability.bit();
        }
        return mask;
    }

    public static InvaderAbility[] all() {
        return VALUES;
    }
}
