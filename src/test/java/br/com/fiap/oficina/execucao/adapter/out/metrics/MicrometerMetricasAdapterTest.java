package br.com.fiap.oficina.execucao.adapter.out.metrics;

import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerMetricasAdapterTest {

    private MeterRegistry meterRegistry;
    private MicrometerMetricasAdapter adapter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        adapter = new MicrometerMetricasAdapter(meterRegistry);
    }

    @Test
    void registrarOsCriada_deveIncrementarContador() {
        adapter.registrarOsCriada();
        adapter.registrarOsCriada();

        Counter counter = meterRegistry.find("oficina.os.criadas").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void registrarTransicaoStatus_deveIncrementarContadorComTag() {
        adapter.registrarTransicaoStatus(StatusOS.EM_DIAGNOSTICO);
        adapter.registrarTransicaoStatus(StatusOS.EM_DIAGNOSTICO);
        adapter.registrarTransicaoStatus(StatusOS.FINALIZADA);

        Counter emDiagnostico = meterRegistry.find("oficina.os.transicoes")
                .tag("status", "EM_DIAGNOSTICO").counter();
        Counter finalizada = meterRegistry.find("oficina.os.transicoes")
                .tag("status", "FINALIZADA").counter();

        assertThat(emDiagnostico).isNotNull();
        assertThat(emDiagnostico.count()).isEqualTo(2.0);
        assertThat(finalizada).isNotNull();
        assertThat(finalizada.count()).isEqualTo(1.0);
    }

    @Test
    void registrarTempoExecucaoSegundos_deveRegistrarNoTimer() {
        adapter.registrarTempoExecucaoSegundos(120L);

        Timer timer = meterRegistry.find("oficina.os.tempo_execucao").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.SECONDS)).isEqualTo(120.0);
    }
}
