package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	private static final int DIA = 17;
	private static final String PLACA = "LOP-687";
	private static final String NOMBRE_MECANICO = "Lopez";

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
		// TODO: crear aqui los datos comunes que necesiten los tests
	}

	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		// Arrange
		LocalDateTime ahora = LocalDateTime.of(2026, 9, 16, 8, 0);
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 17, 10, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(1L);
		mecanico.setNombre("Lopez");
		mecanico.setEspecialidad(TipoServicio.CAMBIO_ACEITE);

		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(List.of());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		Cita cita = servicioCitas.agendarCita(1L, "LOP-687", TipoServicio.CAMBIO_ACEITE, fechaCita);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
		assertEquals(TipoServicio.CAMBIO_ACEITE.getDuracionHoras(), cita.getDuracionHoras());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {
		// Arrange
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 17, 10, 0);

		when(repositorioMecanicos.findById(99L)).thenReturn(Optional.empty());

		// Act y Assert
		assertThrows(MecanicoNoEncontradoException.class, () -> {
			servicioCitas.agendarCita(99L, "LOP-687", TipoServicio.CAMBIO_ACEITE, fechaCita);
		});
		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		// Arrange
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 17, 10, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(1L);
		mecanico.setNombre("Lopez");
		mecanico.setEspecialidad(TipoServicio.CAMBIO_ACEITE);

		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));

		// Act y Assert
		assertThrows(EspecialidadIncorrectaException.class, () -> {
			servicioCitas.agendarCita(1L, "LOP-687", TipoServicio.REPARACION_MOTOR, fechaCita);
		});
		verify(repositorioCitas, never()).save(any());
	}
	// ==========================================
	// Pregunta 2: Horario de servicios pesados
	// ==========================================

	@Test
	@DisplayName("Reparación de motor a las 07:00 debe lanzar HorarioNoPermitidoException")
	void deberiaLanzarExcepcionAlAgendarReparacionMotorA0700() {
		// Arrange
		LocalDateTime fechaHora = LocalDateTime.of(2026, 9, DIA, 7, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(1L);
		mecanico.setNombre(NOMBRE_MECANICO);
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));

		// Act & Assert
		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(1L, PLACA, TipoServicio.REPARACION_MOTOR, fechaHora);
		});

		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Reparación de motor a las 08:00 debe agendarse con éxito")
	void permitirAgendarReparacionMotorA0800() {
		// Arrange
		LocalDateTime ahora = LocalDateTime.of(2026, 9, DIA - 1, 8, 0);
		LocalDateTime fechaHora = LocalDateTime.of(2026, 9, DIA, 8, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(1L);
		mecanico.setNombre(NOMBRE_MECANICO);
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(List.of());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act
		Cita resultado = servicioCitas.agendarCita(1L, PLACA, TipoServicio.REPARACION_MOTOR, fechaHora);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		assertEquals(TipoServicio.REPARACION_MOTOR.getDuracionHoras(), resultado.getDuracionHoras());
		verify(repositorioCitas).save(any(Cita.class));
	}

	@Test
	@DisplayName("Reparación de motor a las 11:00 debe agendarse con éxito")
	void permitirAgendarReparacionMotorA1100() {
		// Arrange
		LocalDateTime ahora = LocalDateTime.of(2026, 9, DIA - 1, 8, 0);
		LocalDateTime fechaHora = LocalDateTime.of(2026, 9, DIA, 11, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(1L);
		mecanico.setNombre(NOMBRE_MECANICO);
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(List.of());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act
		Cita resultado = servicioCitas.agendarCita(1L, PLACA, TipoServicio.REPARACION_MOTOR, fechaHora);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		assertEquals(TipoServicio.REPARACION_MOTOR.getDuracionHoras(), resultado.getDuracionHoras());
		verify(repositorioCitas).save(any(Cita.class));
	}

	@Test
	@DisplayName("Reparación de motor a las 12:00 debe lanzar HorarioNoPermitidoException")
	void deberiaLanzarExcepcionAlAgendarReparacionMotorA1200() {
		// Arrange
		LocalDateTime fechaHora = LocalDateTime.of(2026, 9, DIA, 12, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(1L);
		mecanico.setNombre(NOMBRE_MECANICO);
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));

		// Act & Assert
		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(1L, PLACA, TipoServicio.REPARACION_MOTOR, fechaHora);
		});

		verify(repositorioCitas, never()).save(any());
	}

	// ==========================================
	// PREGUNTA 03: CANCELACIÓN DE CITAS
	// ==========================================

	@Test
	@DisplayName("Cancelar cita cuando faltan 24 horas")
	void cancelarCitaCon24HorasDeAnticipacion() {
		// Arrange
		Long citaId = 1L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 10, 0);
		LocalDateTime ahora24h = fechaCita.minusHours(24);

		Cita cita = new Cita();
		cita.setId(citaId);
		cita.setPlacaVehiculo(PLACA);
		cita.setTipoServicio(TipoServicio.CAMBIO_ACEITE);
		cita.setFechaHoraInicio(fechaCita);
		cita.setEstado(EstadoCita.PROGRAMADA);

		when(repositorioCitas.findById(citaId)).thenReturn(Optional.of(cita));
		when(proveedorFechaHora.ahora()).thenReturn(ahora24h);
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act
		servicioCitas.cancelarCita(citaId);

		// Assert
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
	}

	@Test
	@DisplayName("Cancelar cita cuando faltan 2 horas")
	void cancelarCitaCon2HorasDeAnticipacion() {
		// Arrange
		Long citaId = 2L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 10, 0);
		LocalDateTime ahora2h = fechaCita.minusHours(2);

		Cita cita = new Cita();
		cita.setId(citaId);
		cita.setPlacaVehiculo(PLACA);
		cita.setTipoServicio(TipoServicio.CAMBIO_ACEITE);
		cita.setFechaHoraInicio(fechaCita);
		cita.setEstado(EstadoCita.PROGRAMADA);

		when(repositorioCitas.findById(citaId)).thenReturn(Optional.of(cita));
		when(proveedorFechaHora.ahora()).thenReturn(ahora2h);
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act
		servicioCitas.cancelarCita(citaId);

		// Assert
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
	}

	@Test
	@DisplayName("Intentar cancelar cita ya atendida debe lanzar excepcion")
	void lanzarExcepcionAlCancelarCitaAtendida() {
		// Arrange
		Long citaId = 3L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, DIA, 10, 0);

		Cita cita = new Cita();
		cita.setId(citaId);
		cita.setPlacaVehiculo(PLACA);
		cita.setTipoServicio(TipoServicio.CAMBIO_ACEITE);
		cita.setFechaHoraInicio(fechaCita);
		cita.setEstado(EstadoCita.ATENDIDA);

		when(repositorioCitas.findById(citaId)).thenReturn(Optional.of(cita));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> {
			servicioCitas.cancelarCita(citaId);
		});
	}


	@Test
	@DisplayName("Una cita que empieza justo cuando termina otra se acepta")
	void agendarCitaContigua() {
		// Arrange
		// TODO: una cita existente que termina a las 10:00 y la nueva que empieza a las 10:00

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar con 24 horas o mas de anticipacion no genera penalidad")
	void cancelarConAnticipacionSuficiente() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO: penalidad 0, estado CANCELADA, notificacion
	}

	@Test
	@DisplayName("Cancelar con menos de 24 horas aplica una penalidad de 50.00")
	void cancelarConAvisoTardio() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita inexistente lanza CitaNoEncontradaException")
	void cancelarCitaInexistente() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita que ya fue cancelada lanza CitaNoCancelableException")
	void cancelarCitaYaCancelada() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico disponible retorna el primero sin citas superpuestas")
	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
		// Arrange
		// TODO: dos mecanicos de la misma especialidad, el primero ocupado

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
	void buscarMecanicoSinDisponibilidad() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}
}
