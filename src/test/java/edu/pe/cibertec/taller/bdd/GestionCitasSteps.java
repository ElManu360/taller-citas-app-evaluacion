package edu.pe.cibertec.taller.bdd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Exception excepcionCapturada;
	private Cita citaResultado;

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);

		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 16, 8, 0));
	}

	@Given("que un mecanico esta libre para el dia {int} a las {int}:00")
	public void queUnMecanicoEstaLibre(int dia, int hora) {
		Mecanico mecanico = new Mecanico();
		mecanico.setId(1L);
		mecanico.setNombre("Juan Lopez");
		mecanico.setEspecialidad(TipoServicio.MANTENIMIENTO_LIGERO);

		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
	}

	@When("intento agendar un MANTENIMIENTO_LIGERO para la placa {string} el dia {int} a las {int}:00")
	public void intentoAgendarMantenimientoLigero(String placa, int dia, int hora) {
		try {
			LocalDateTime fechaHora = LocalDateTime.of(2026, 9, dia, hora, 0);
			citaResultado = servicioCitas.agendarCita(1L, placa, TipoServicio.MANTENIMIENTO_LIGERO, fechaHora);
		} catch (Exception e) {
			excepcionCapturada = e;
		}
	}

	@Then("la cita queda registrada exitosamente")
	public void laCitaQuedaRegistradaExitosamente() {
		assertNull(excepcionCapturada);
		assertNotNull(citaResultado);
		assertEquals(EstadoCita.PROGRAMADA, citaResultado.getEstado());
	}

	@Given("que el mecanico ya tiene una cita ocupada de {int}:00 a {int}:00 el dia {int}")
	public void queElMecanicoYaTieneUnaCitaOcupada(int horaInicio, int horaFin, int dia) {
		Mecanico mecanico = new Mecanico();
		mecanico.setId(1L);
		mecanico.setNombre("Juan Lopez");
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
	}

	@When("intento agendar una REPARACION_MOTOR para la placa {string} el dia {int} a las {int}:00")
	public void intentoAgendarReparacionMotor(String placa, int dia, int hora) {
		try {
			LocalDateTime fechaHora = LocalDateTime.of(2026, 9, dia, hora, 0);
			citaResultado = servicioCitas.agendarCita(1L, placa, TipoServicio.REPARACION_MOTOR, fechaHora);
		} catch (Exception e) {
			excepcionCapturada = e;
		}
	}

	@Then("el sistema no permite agendar la cita por solapamiento de horario")
	public void elSistemaNoPermiteAgendarCita() {
		assertNotNull(excepcionCapturada);
	}

	@Then("el sistema registra la cita correctamente")
	public void elSistemaRegistraLaCitaCorrectamente() {
		assertNotNull(citaResultado);
	}
}