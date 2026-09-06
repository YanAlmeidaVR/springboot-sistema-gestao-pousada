package dev.YanAlmeida.SistemaDeGestaoDePousada.service;

import dev.YanAlmeida.SistemaDeGestaoDePousada.dto.reserva.ReservaCreateDTO;
import dev.YanAlmeida.SistemaDeGestaoDePousada.entity.HospedeModel;
import dev.YanAlmeida.SistemaDeGestaoDePousada.entity.QuartoModel;
import dev.YanAlmeida.SistemaDeGestaoDePousada.entity.ReservaModel;
import dev.YanAlmeida.SistemaDeGestaoDePousada.enums.quarto.QuartoStatus;
import dev.YanAlmeida.SistemaDeGestaoDePousada.enums.quarto.TipoQuarto;
import dev.YanAlmeida.SistemaDeGestaoDePousada.enums.reserva.*;
import dev.YanAlmeida.SistemaDeGestaoDePousada.exception.reserva.*;
import dev.YanAlmeida.SistemaDeGestaoDePousada.repository.HospedeRepository;
import dev.YanAlmeida.SistemaDeGestaoDePousada.repository.QuartoRepository;
import dev.YanAlmeida.SistemaDeGestaoDePousada.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @InjectMocks
    private ReservaService reservaService;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private HospedeRepository hospedeRepository;

    @Mock
    private QuartoRepository quartoRepository;

    @Test
    void deveCriarReservaComSucesso(){
        // Given
        ReservaCreateDTO dto = new ReservaCreateDTO();
        dto.setHospedeId(1L);
        dto.setNumeroQuarto(101);
        dto.setDataCheckIn(LocalDate.now());
        dto.setDataCheckOut(LocalDate.now().plusDays(2));
        dto.setMetodoPagamento(MetodoPagamento.CARTAO_CREDITO);

        HospedeModel hospede = new HospedeModel();
        hospede.setNomeHospede("João");
        hospede.setCpf("12345678901");
        hospede.setTelefone("999999999");

        QuartoModel quarto = new QuartoModel();
        quarto.setNumeroQuarto(101);
        quarto.setTipoQuarto(TipoQuarto.CASAL);
        quarto.setPrecoPorNoite(BigDecimal.valueOf(100));
        quarto.setQuartoStatus(QuartoStatus.DISPONIVEL);

        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(quartoRepository.findByNumeroQuarto(101)).thenReturn(Optional.of(quarto));
        when(reservaRepository.findByNumeroQuarto(101)).thenReturn(List.of());
        when(reservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var response = reservaService.criarReserva(dto);

        // Then
        assertNotNull(response);
        verify(quartoRepository).save(quarto);
        verify(reservaRepository).save(any(ReservaModel.class));
    }

    @Test
    void deveLancarExcecaoQuandoDatasInvalidas(){
        // Given
        ReservaCreateDTO dto = new ReservaCreateDTO();
        dto.setDataCheckIn(LocalDate.now());
        dto.setDataCheckOut(LocalDate.now());

        // When / Then
        assertThrows(CheckInInvalidoException.class,
                () -> reservaService.criarReserva(dto));
    }

    @Test
    void deveLancarExcecaoQuandoQuartoOcupado(){
        // Given
        ReservaCreateDTO dto = new ReservaCreateDTO();
        dto.setHospedeId(1L);
        dto.setNumeroQuarto(101);
        dto.setDataCheckIn(LocalDate.now());
        dto.setDataCheckOut(LocalDate.now().plusDays(1));
        dto.setMetodoPagamento(MetodoPagamento.PIX);

        HospedeModel hospede = new HospedeModel();
        QuartoModel quarto = new QuartoModel();

        ReservaModel reservaAtiva = new ReservaModel();
        reservaAtiva.setStatusReserva(StatusReserva.ATIVA);
        reservaAtiva.setDataCheckIn(LocalDate.now().minusDays(1));
        reservaAtiva.setDataCheckOut(LocalDate.now().plusDays(2));

        when(hospedeRepository.findById(1L)).thenReturn(Optional.of(hospede));
        when(quartoRepository.findByNumeroQuarto(101)).thenReturn(Optional.of(quarto));
        when(reservaRepository.findByNumeroQuarto(101)).thenReturn(List.of(reservaAtiva));

        // When / Then
        assertThrows(QuartoOcupadoException.class,
                () -> reservaService.criarReserva(dto));
    }

    @Test
    void deveRealizarCheckOutComSucesso(){
        // Given
        ReservaModel reserva = new ReservaModel();
        reserva.setStatusReserva(StatusReserva.ATIVA);
        reserva.setStatusPagamento(StatusPagamento.PAGO);
        reserva.setStatusChave(StatusChave.DEVOLVIDA);
        reserva.setNumeroQuarto(101);

        QuartoModel quarto = new QuartoModel();
        quarto.setNumeroQuarto(101);
        quarto.setQuartoStatus(QuartoStatus.OCUPADO);

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(quartoRepository.findByNumeroQuarto(101)).thenReturn(Optional.of(quarto));
        when(reservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var response = reservaService.fazerCheckOut(1L);

        // Then
        assertEquals(StatusReserva.FINALIZADA, response.getStatusReserva());
        assertEquals(QuartoStatus.DISPONIVEL, quarto.getQuartoStatus());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoPendenteNoCheckOut(){
        // Given
        ReservaModel reserva = new ReservaModel();
        reserva.setStatusReserva(StatusReserva.ATIVA);
        reserva.setStatusPagamento(StatusPagamento.PENDENTE);
        reserva.setStatusChave(StatusChave.DEVOLVIDA);

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        // When / Then
        assertThrows(PagamentoInvalidoException.class,
                () -> reservaService.fazerCheckOut(1L));
    }

    @Test
    void deveCancelarReservaComSucesso(){
        // Given
        ReservaModel reserva = new ReservaModel();
        reserva.setStatusReserva(StatusReserva.ATIVA);
        reserva.setNumeroQuarto(101);

        QuartoModel quarto = new QuartoModel();
        quarto.setNumeroQuarto(101);
        quarto.setQuartoStatus(QuartoStatus.OCUPADO);

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(quartoRepository.findByNumeroQuarto(101)).thenReturn(Optional.of(quarto));
        when(reservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var response = reservaService.cancelarReserva(1L);

        // Then
        assertEquals(StatusReserva.CANCELADA, response.getStatusReserva());
        assertEquals(QuartoStatus.DISPONIVEL, quarto.getQuartoStatus());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void deveCalcularReceitaCorretamente(){
        // Given
        ReservaModel r1 = new ReservaModel();
        r1.setStatusPagamento(StatusPagamento.PAGO);
        r1.setValorTotal(BigDecimal.valueOf(200));
        r1.setDataCheckIn(LocalDate.now().minusDays(3));
        r1.setDataCheckOut(LocalDate.now());

        ReservaModel r2 = new ReservaModel();
        r2.setStatusPagamento(StatusPagamento.PAGO);
        r2.setValorTotal(BigDecimal.valueOf(300));
        r2.setDataCheckIn(LocalDate.now().minusDays(2));
        r2.setDataCheckOut(LocalDate.now());

        when(reservaRepository.findAll()).thenReturn(List.of(r1, r2));

        // When
        BigDecimal receita = reservaService.calcularReceita(
                LocalDate.now().minusDays(5),
                LocalDate.now()
        );

        // Then
        assertEquals(BigDecimal.valueOf(500), receita);
    }

    @Test
    void deveCalcularTaxaDeOcupacao(){
        // Given
        when(quartoRepository.count()).thenReturn(10L);
        when(quartoRepository.findByQuartoStatus(QuartoStatus.OCUPADO))
                .thenReturn(List.of(new QuartoModel(), new QuartoModel()));

        // When
        Double taxa = reservaService.calcularTaxaOcupacao();

        // Then
        assertEquals(20.0, taxa);
    }
}
