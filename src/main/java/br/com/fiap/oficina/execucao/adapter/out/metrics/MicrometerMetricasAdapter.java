package br.com.fiap.oficina.execucao.adapter.out.metrics;

import br.com.fiap.oficina.execucao.application.port.out.MetricasPort;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MicrometerMetricasAdapter implements MetricasPort {

    private static final String TAG_STATUS = "status";

    private final Counter osCriadaCounter;
    private final MeterRegistry meterRegistry;

    public MicrometerMetricasAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.osCriadaCounter = Counter.builder("oficina.os.criadas")
                .description("Total de ordens de serviço criadas")
                .register(meterRegistry);
    }

    @Override
    public void registrarOsCriada() {
        osCriadaCounter.increment();
    }

    @Override
    public void registrarTransicaoStatus(StatusOS status) {
        Counter.builder("oficina.os.transicoes")
                .description("Total de transições de status das ordens de serviço")
                .tag(TAG_STATUS, status.name())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void registrarTempoExecucaoSegundos(long segundos) {
        Timer.builder("oficina.os.tempo_execucao")
                .description("Tempo de execução das ordens de serviço em segundos")
                .register(meterRegistry)
                .record(segundos, TimeUnit.SECONDS);
    }
}
