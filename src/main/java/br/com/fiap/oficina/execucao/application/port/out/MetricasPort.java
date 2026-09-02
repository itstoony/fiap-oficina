package br.com.fiap.oficina.execucao.application.port.out;

import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;

public interface MetricasPort {

    void registrarOsCriada();

    void registrarTransicaoStatus(StatusOS status);

    void registrarTempoExecucaoSegundos(long segundos);
}
