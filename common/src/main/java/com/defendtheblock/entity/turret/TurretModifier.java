package com.defendtheblock.entity.turret;

/**
 * Os <b>modulos</b> da torreta: personalizacao de atributo isolada, paralela e
 * independente da subida de nivel.
 *
 * <p>O nivel ({@link TurretTier}) sobe <b>tudo junto</b> — dano, alcance, vida,
 * recarga e carregador — e por isso nao permite escolha nenhuma: duas torretas
 * de esmeralda sao identicas. Os modulos existem justamente para o outro lado:
 * cada um mexe em <b>um</b> eixo, e como a torreta so aceita
 * {@value TurretModifiers#MAX_SLOTS} deles, montar uma torreta e escolher o que
 * ela faz de melhor. Uma de alcance+recarga e artilharia de apoio; uma de
 * dano+veneno e um posto de execucao; uma de aljava+catador quase nao precisa
 * de reabastecimento.
 *
 * <p>Cada modulo e um item de livro proprio do mod, com receita propria (ver
 * {@code data/defendtheblock/recipes/turret_module_*.json}). O <b>grau</b> nao
 * e um item separado: aplicar o mesmo livro de novo na mesma torreta sobe o
 * grau ate {@link #maxGrade()}. Isso mantem uma receita por tipo, em vez de uma
 * receita por combinacao de tipo e grau.
 */
public enum TurretModifier {

    /** Aumenta o alcance de tiro. */
    RANGE("range", 3),
    /** Aumenta o dano de cada flecha. */
    DAMAGE("damage", 3),
    /** Aumenta a vida maxima da torreta. */
    FORTITUDE("fortitude", 3),
    /** Reduz o tempo entre dois tiros. */
    RAPID("rapid", 3),
    /** Aumenta a capacidade do carregador. */
    QUIVER("quiver", 3),
    /**
     * Dispara flechas a mais por tiro, <b>sem consumir municao extra</b>: o
     * disparo inteiro continua custando uma flecha so.
     */
    VOLLEY("volley", 2),
    /** Chance de o disparo nao consumir flecha nenhuma do carregador. */
    SCAVENGER("scavenger", 3),
    /** As flechas passam a aplicar Lentidao em quem acertam. */
    FROST("frost", 3),
    /** As flechas passam a aplicar Veneno em quem acertam. */
    VENOM("venom", 3);

    private final String id;
    private final int maxGrade;

    TurretModifier(String id, int maxGrade) {
        this.id = id;
        this.maxGrade = maxGrade;
    }

    /** Nome curto, usado no NBT e para montar o id do item. */
    public String id() {
        return id;
    }

    /** Grau maximo que este modulo alcanca numa torreta. */
    public int maxGrade() {
        return maxGrade;
    }

    /** Id de registro do item deste modulo, sem o namespace. */
    public String itemId() {
        return "turret_module_" + id;
    }

    public String translationKey() {
        return "turret_module.defendtheblock." + id;
    }

    /**
     * Este modulo faz a flecha carregar um efeito? Se sim, o nome que a camada
     * de compat traduz para o {@code StatusEffect} da versao.
     *
     * <p>Devolver uma {@code String} e proposital: {@code StatusEffects} e
     * {@code StatusEffect} direto no 1.20.1 e {@code RegistryEntry<StatusEffect>}
     * no 1.21, entao o codigo compartilhado nunca toca neles — quem monta o
     * efeito e {@code DtbCompat.applyArrowEffect}, uma por versao. E a mesma
     * regra que ja vale para os sons de besta.
     *
     * @return o nome do efeito, ou null se este modulo nao mexe em efeito
     */
    public String arrowEffect() {
        return switch (this) {
            case FROST -> "slowness";
            case VENOM -> "poison";
            default -> null;
        };
    }

    /** Procura pelo nome curto guardado no NBT. */
    public static TurretModifier byId(String id) {
        for (TurretModifier modifier : values()) {
            if (modifier.id.equals(id)) {
                return modifier;
            }
        }
        return null;
    }
}
