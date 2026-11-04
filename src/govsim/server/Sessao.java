package govsim.server;

import govsim.engine.MotorEconomico;
import govsim.model.Estado;

/** Partida e fonte aleatória isoladas, com registro do último acesso HTTP. */
public final class Sessao {
    private final Estado estado;
    private final MotorEconomico motor;
    private volatile long ultimoAcesso;

    Sessao(long agora) {
        estado = new Estado();
        motor = new MotorEconomico();
        motor.iniciarPartida(estado, false, Estado.TURNOS_PADRAO);
        ultimoAcesso = agora;
    }

    public Estado getEstado() { return estado; }
    public MotorEconomico getMotor() { return motor; }
    public long getUltimoAcesso() { return ultimoAcesso; }
    void registrarAcesso(long agora) { ultimoAcesso = agora; }
}
