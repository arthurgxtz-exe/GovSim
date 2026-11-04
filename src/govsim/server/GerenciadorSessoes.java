package govsim.server;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Operações atômicas no registro; ordem de acesso implementa descarte LRU. */
final class GerenciadorSessoes {
    private final Map<String, Sessao> sessoes = new LinkedHashMap<String, Sessao>(16, 0.75f, true);
    private final long inatividadeMs;
    private final int limite;
    private final LongSupplier relogio;

    GerenciadorSessoes(long inatividadeMs, int limite, LongSupplier relogio) {
        if (inatividadeMs <= 0 || limite <= 0) throw new IllegalArgumentException("Limites de sessão devem ser positivos.");
        this.inatividadeMs = inatividadeMs;
        this.limite = limite;
        this.relogio = relogio;
    }

    synchronized Sessao obter(String id) {
        long agora = relogio.getAsLong();
        expirar(agora);
        Sessao sessao = sessoes.get(id);
        if (sessao == null) {
            if (sessoes.size() >= limite) {
                Iterator<String> antigos = sessoes.keySet().iterator();
                antigos.next();
                antigos.remove();
            }
            sessao = new Sessao(agora);
            sessoes.put(id, sessao);
        }
        sessao.registrarAcesso(agora);
        return sessao;
    }

    synchronized void expirarInativas() { expirar(relogio.getAsLong()); }

    private void expirar(long agora) {
        Iterator<Sessao> it = sessoes.values().iterator();
        while (it.hasNext()) {
            if (agora - it.next().getUltimoAcesso() >= inatividadeMs) it.remove();
        }
    }

    synchronized int quantidade() { return sessoes.size(); }
    synchronized void limpar() { sessoes.clear(); }
}
