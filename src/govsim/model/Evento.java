package govsim.model;

/**
 * Representa uma carta de evento/crise apresentada ao Presidente em um turno (estilo Reigns).
 * Contém dados do personagem (nome, cargo, avatar, diálogo), descrição econômica,
 * contextualização, consequências potenciais e duas alternativas de decisão:
 * - Esquerda (Decreto A)
 * - Direita (Decreto B)
 */
public class Evento {
    private final String id;
    private final String nome;
    private final String categoria;
    private final String personagemNome;
    private final String personagemCargo;
    private final String personagemAvatar;
    private final String dialogo;
    private final String descricao;
    private final String contextualizacao;
    private final String consequenciasPotenciais;
    private final Decreto decretoA;
    private final Decreto decretoB;

    public Evento(String id, String nome, String categoria,
                  String personagemNome, String personagemCargo, String personagemAvatar, String dialogo,
                  String descricao, String contextualizacao, String consequenciasPotenciais,
                  Decreto decretoA, Decreto decretoB) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.personagemNome = personagemNome;
        this.personagemCargo = personagemCargo;
        this.personagemAvatar = personagemAvatar;
        this.dialogo = dialogo;
        this.descricao = descricao;
        this.contextualizacao = contextualizacao;
        this.consequenciasPotenciais = consequenciasPotenciais;
        this.decretoA = decretoA;
        this.decretoB = decretoB;
    }

    public Evento inverter(){return new Evento(id,nome,categoria,personagemNome,personagemCargo,personagemAvatar,dialogo,descricao,contextualizacao,consequenciasPotenciais,decretoB.comCodigo("A"),decretoA.comCodigo("B"));}
    public Decreto getDecreto(String codigo) {
        if ("A".equalsIgnoreCase(codigo)) {
            return decretoA;
        }
        if ("B".equalsIgnoreCase(codigo)) {
            return decretoB;
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getPersonagemNome() {
        return personagemNome;
    }

    public String getPersonagemCargo() {
        return personagemCargo;
    }

    public String getPersonagemAvatar() {
        return personagemAvatar;
    }

    public String getDialogo() {
        return dialogo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getContextualizacao() {
        return contextualizacao;
    }

    public String getConsequenciasPotenciais() {
        return consequenciasPotenciais;
    }

    public Decreto getDecretoA() {
        return decretoA;
    }

    public Decreto getDecretoB() {
        return decretoB;
    }
}
