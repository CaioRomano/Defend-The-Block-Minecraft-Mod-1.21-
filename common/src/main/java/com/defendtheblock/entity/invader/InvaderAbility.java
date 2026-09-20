package com.defendtheblock.entity.invader;

/**
 * Habilidades extras que um invasor pode receber.
 *
 * <p>Subir escada ({@link #LADDER_CLIMB}) e arrombar porta ({@link #DOOR_BREACHER})
 * sao dadas a <em>todo</em> invasor exceto o creeper. As outras sao sorteadas
 * com chances baixas configuraveis em {@link com.defendtheblock.config.DtbConfig}.
 */
public enum InvaderAbility {
    /** Todo invasor sobe escadas que encontrar no caminho — inclusive as montadas por outro mob. */
    LADDER_CLIMB(0),
    /**
     * Todo invasor, exceto o creeper, arromba portas fechadas em vez de
     * simplesmente abri-las — nunca so passa por elas.
     */
    DOOR_BREACHER(1),
    /** Creeper que se explode no obstaculo quando nao ha caminho ate o Nexus. */
    SUICIDE_BREACH(2),
    /** Aranha que atira teias nos alvos. */
    WEB_SHOT(3),
    /**
     * Zumbi <b>sapador</b>: abre passagem pelo obstaculo escolhendo a ferramenta
     * pelo material. Madeira ele queima com o isqueiro; o resto ele cava com a
     * picareta.
     *
     * <p>Era duas habilidades separadas (picareta e isqueiro). Virou uma so
     * porque a diferenca entre elas nunca foi de <i>papel</i> — as duas existem
     * para derrubar o que esta na frente — e sim de material do bloco, que e
     * uma decisao que o mob pode tomar na hora em vez de nascer com ela.
     */
    PICKAXE_MINER(4),
    /**
     * Zumbi <b>construtor</b>: o unico invasor que coloca bloco no mundo.
     *
     * <p>Ele monta a escalada tanto para passar por cima de um obstaculo quanto
     * para alcancar um Nexus suspenso, e cada bloco que ele poe ganha escada em
     * todos os lados livres — e assim que o resto da horda, que nao constroi
     * nada, aproveita o caminho.
     */
    LADDER_BUILDER(5),
    /** Zumbi com TNT: arremessa uma unica banana em arco. */
    TNT_SAPPER(6);

    private final int bit;

    InvaderAbility(int bit) {
        this.bit = bit;
    }

    /**
     * Posicao desta habilidade na mascara de bits guardada em {@code InvaderData}.
     *
     * <p>O numero e <b>explicito</b>, e nao {@code ordinal()}, de proposito: a
     * mascara e persistida no NBT do mob, entao remover ou reordenar uma
     * constante faria todo invasor ja salvo carregar com as habilidades
     * trocadas. Com o bit fixo, as posicoes 7 e 8 (o antigo isqueiro e o antigo
     * construtor de pilar) simplesmente ficam vagas e os saves antigos seguem
     * lendo certo o que sobrou.
     */
    public int bit() {
        return 1 << bit;
    }
}
