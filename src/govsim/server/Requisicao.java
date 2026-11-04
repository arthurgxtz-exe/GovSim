package govsim.server;

import govsim.model.Dificuldade;
import java.util.Locale;
import java.util.Map;

/** Corpo JSON interpretado uma vez, com validação antes de obter ou alterar a sessão. */
final class Requisicao {
    private final Map<?, ?> campos;

    Requisicao(String corpo) {
        Object valor = JsonUtil.lerJson(corpo);
        if (!(valor instanceof Map)) throw new IllegalArgumentException("O corpo deve ser um objeto JSON.");
        campos = (Map<?, ?>) valor;
    }

    String texto(String nome) {
        Object valor = campos.get(nome);
        if (!(valor instanceof String) || ((String) valor).trim().isEmpty())
            throw new IllegalArgumentException("Campo '" + nome + "' deve ser uma string não vazia.");
        return ((String) valor).trim();
    }

    long inteiro(String nome) {
        Object valor = campos.get(nome);
        if (!(valor instanceof Long)) throw new IllegalArgumentException("Campo '" + nome + "' deve ser um inteiro.");
        return ((Long) valor).longValue();
    }

    static final class Inicio {
        final boolean demo;
        final int turnos;
        final Dificuldade dificuldade;
        final Long seed;

        Inicio(String corpo) {
            Requisicao r = new Requisicao(corpo);
            String modo = r.texto("modo").toLowerCase(Locale.ROOT);
            if (!modo.equals("normal") && !modo.equals("demo") && !modo.equals("demonstracao") && !modo.equals("deterministico"))
                throw new IllegalArgumentException("Modo inválido.");
            demo = !modo.equals("normal");
            try { dificuldade = Dificuldade.valueOf(r.texto("dificuldade").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Dificuldade inválida."); }
            long total = r.inteiro("turnos");
            if (total != 0 && (total < 3 || total > 24))
                throw new IllegalArgumentException("Turnos deve ser 0 (padrão da dificuldade) ou estar entre 3 e 24.");
            turnos = total == 0 ? dificuldade.getTotalTurnos() : (int) total;
            seed = r.campos.containsKey("seed") ? Long.valueOf(r.inteiro("seed")) : null;
        }
    }

    static final class Decisao {
        final String decreto;
        final int turno;

        Decisao(String corpo) {
            Requisicao r = new Requisicao(corpo);
            decreto = r.texto("decreto").toUpperCase(Locale.ROOT);
            if (!decreto.equals("A") && !decreto.equals("B")) throw new IllegalArgumentException("Decreto deve ser A ou B.");
            long valor = r.inteiro("turno");
            if (valor < 1 || valor > 24) throw new IllegalArgumentException("Turno deve estar entre 1 e 24.");
            turno = (int) valor;
        }
    }
}
