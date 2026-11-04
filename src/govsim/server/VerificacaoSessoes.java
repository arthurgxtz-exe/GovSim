package govsim.server;

import java.util.concurrent.atomic.AtomicLong;

/** Relógio controlado: expiração e limite são testados sem sleeps. */
public final class VerificacaoSessoes {
    private static void exigir(boolean ok, String mensagem) {
        if (!ok) throw new AssertionError(mensagem);
    }

    public static void executar() {
        AtomicLong agora = new AtomicLong(0);
        GerenciadorSessoes registro = new GerenciadorSessoes(1800000, 2, agora::get);
        Sessao a = registro.obter("a");
        a.getEstado().getCaixaPublico().definirValor(13);
        agora.set(1799999);
        exigir(registro.obter("a") == a, "Não expirar antes do limite");
        exigir(a.getUltimoAcesso() == 1799999, "Renovar último acesso");
        agora.set(3599998);
        registro.expirarInativas();
        exigir(registro.quantidade() == 1, "Acesso renova prazo de expiração");
        agora.set(3599999);
        registro.expirarInativas();
        exigir(registro.quantidade() == 0, "Expirar exatamente após 30 minutos inativos");
        Sessao nova = registro.obter("a");
        exigir(nova != a && nova.getEstado() != a.getEstado() && nova.getMotor() != a.getMotor(), "Expiração descarta estado e motor");
        exigir(nova.getEstado().getCaixaPublico().getValorAtual() == 100, "Sessão expirada reinicia limpa");

        agora.incrementAndGet();
        Sessao b = registro.obter("b");
        agora.incrementAndGet();
        exigir(registro.obter("a") == nova, "Reutilizar sessão ativa");
        agora.incrementAndGet();
        registro.obter("c");
        exigir(registro.quantidade() == 2, "Respeitar limite máximo");
        exigir(registro.obter("a") == nova, "Preservar sessão acessada recentemente");
        exigir(registro.obter("b") != b, "Remover a menos recentemente acessada");
        exigir(registro.quantidade() == 2, "Não ultrapassar limite ao recriar ID removido");
        agora.addAndGet(1800000);
        registro.expirarInativas();
        exigir(registro.quantidade() == 0, "Limpeza remove todas as inativas");
        registro.obter("x");
        registro.limpar();
        exigir(registro.quantidade() == 0, "Encerramento libera sessões");
        System.out.println("[APROVADO] Sessões: expiração em 30 min, renovação de acesso, descarte LRU e limite máximo.");
    }
}
