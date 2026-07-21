Feature: Gestion de citas del taller mecanico

  Scenario: Registro exitoso de MANTENIMIENTO_LIGERO
    Given que un mecanico esta libre para el dia 17 a las 10:00
    When intento agendar un MANTENIMIENTO_LIGERO para la placa "LOP-687" el dia 17 a las 10:00
    Then la cita queda registrada exitosamente

  Scenario: Intento de registro con mecanico ocupado iniciando a las 11:00
    Given que el mecanico ya tiene una cita ocupada de 10:00 a 12:00 el dia 17
    When intento agendar una REPARACION_MOTOR para la placa "LOP-687" el dia 17 a las 11:00
    Then el sistema no permite agendar la cita por solapamiento de horario

  Scenario: Intento de registro con mecanico disponible iniciando a las 12:00
    Given que el mecanico ya tiene una cita ocupada de 10:00 a 12:00 el dia 17
    When intento agendar una REPARACION_MOTOR para la placa "LOP-687" el dia 17 a las 12:00
    Then el sistema registra la cita correctamente