import { useState } from 'react';
import { View, TextInput, Pressable } from 'react-native';
import { useUserStore } from '../store/userStore';
import { useAppointmentsStore } from '../store/appointmentsStore';
import { useErrorStore } from '../store/errorStore';
import { parseGraphQLError } from '../utils/errorHandler';
import { FloatingCard } from './FloatingCard';
import { AppText } from './AppText';
import { DatePickerField } from './DatePickerField';
import { WhatsAppButton } from './WhatsAppButton';
import { ConfirmModal } from './ConfirmModal';
import type { RequestAppointmentInput } from '../types/appointments';

// Construye una fecha YYYY-MM-DD desde un objeto Date
function toFechaStr(d: Date): string {
  const anio = d.getFullYear();
  const mes = (d.getMonth() + 1).toString().padStart(2, '0');
  const dia = d.getDate().toString().padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

// Construye una hora HH:mm desde un objeto Date
function toHoraStr(d: Date): string {
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
}

export function AppointmentRequestForm() {
  const patient = useUserStore(function (s) { return s.patient; });
  const requestAppointment = useAppointmentsStore(function (s) { return s.requestAppointment; });
  const showError = useErrorStore(function (s) { return s.showError; });

  // Estado del formulario
  const [telefono, setTelefono] = useState(patient?.phone ?? '');
  const [email, setEmail] = useState(patient?.email ?? '');
  const [fechaDate, setFechaDate] = useState<Date>(new Date());
  const [horaDate, setHoraDate] = useState<Date>(() => {
    const d = new Date();
    d.setHours(9, 0, 0, 0);
    return d;
  });
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [exito, setExito] = useState(false);
  const [errorFecha, setErrorFecha] = useState('');
  const [errorMotivo, setErrorMotivo] = useState('');

  // Fecha minima: hoy
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);

  function validar(): boolean {
    let valido = true;

    // Telefono O email requerido
    const telTrim = telefono.trim();
    const emailTrim = email.trim();
    if (!telTrim && !emailTrim) {
      showError({
        title: 'Error',
        subtitle: 'Contacto invalido',
        message: 'Debes indicar un telefono o email valido para solicitar una cita.',
        code: 'APPOINTMENT_REQUEST_INVALID_CONTACT',
      });
      return false;
    }

    // Fecha no puede ser pasada
    const fechaElegida = new Date(fechaDate);
    fechaElegida.setHours(0, 0, 0, 0);
    if (fechaElegida < hoy) {
      setErrorFecha('La fecha debe ser futura.');
      valido = false;
    } else {
      setErrorFecha('');
    }

    // Motivo minimo 10 caracteres
    if (motivo.trim().length < 10) {
      setErrorMotivo('Describe el motivo (minimo 10 caracteres).');
      valido = false;
    } else {
      setErrorMotivo('');
    }

    return valido;
  }

  async function handleEnviar() {
    if (!validar()) return;

    setEnviando(true);
    try {
      const input: RequestAppointmentInput = {
        fechaPreferida: toFechaStr(fechaDate),
        horaPreferida: toHoraStr(horaDate),
        motivo: motivo.trim(),
        telefono: telefono.trim() || null,
        email: email.trim() || null,
      };
      await requestAppointment(input);

      // Limpiar formulario y mostrar modal de exito
      setMotivo('');
      setExito(true);
    } catch (err) {
      const appError = parseGraphQLError(err);
      showError(appError);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <FloatingCard className="mb-4">
        <AppText variant="subtitle" weight="semibold" className="text-text-primary dark:text-text-primary-dark mb-4">
          Pedir cita nueva
        </AppText>

        {/* Telefono */}
        <View className="mb-3">
          <AppText variant="caption" weight="medium" className="text-text-secondary dark:text-text-secondary-dark mb-1">
            Telefono
          </AppText>
          <TextInput
            value={telefono}
            onChangeText={setTelefono}
            keyboardType="phone-pad"
            placeholder="600 000 000"
            placeholderTextColor="#94A3B8"
            className="bg-background dark:bg-background-dark border border-border dark:border-border-dark rounded-xl px-4 py-3 text-text-primary dark:text-text-primary-dark text-base min-h-12"
          />
        </View>

        {/* Email */}
        <View className="mb-3">
          <AppText variant="caption" weight="medium" className="text-text-secondary dark:text-text-secondary-dark mb-1">
            Email
          </AppText>
          <TextInput
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            placeholder="paciente@ejemplo.com"
            placeholderTextColor="#94A3B8"
            className="bg-background dark:bg-background-dark border border-border dark:border-border-dark rounded-xl px-4 py-3 text-text-primary dark:text-text-primary-dark text-base min-h-12"
          />
        </View>

        {/* Fecha preferida */}
        <View className="mb-1">
          <DatePickerField
            label="Fecha preferida"
            value={fechaDate}
            onChange={setFechaDate}
            mode="date"
            minimumDate={hoy}
          />
          {errorFecha !== '' && (
            <AppText variant="caption" className="text-error mt-1">
              {errorFecha}
            </AppText>
          )}
        </View>

        {/* Hora preferida */}
        <View className="mb-3 mt-3">
          <DatePickerField
            label="Hora preferida"
            value={horaDate}
            onChange={setHoraDate}
            mode="time"
          />
        </View>

        {/* Motivo */}
        <View className="mb-1">
          <AppText variant="caption" weight="medium" className="text-text-secondary dark:text-text-secondary-dark mb-1">
            Motivo
          </AppText>
          <TextInput
            value={motivo}
            onChangeText={setMotivo}
            multiline
            numberOfLines={4}
            placeholder="Describe brevemente el motivo de la consulta..."
            placeholderTextColor="#94A3B8"
            textAlignVertical="top"
            className="bg-background dark:bg-background-dark border border-border dark:border-border-dark rounded-xl px-4 py-3 text-text-primary dark:text-text-primary-dark text-base"
            style={{ minHeight: 100 }}
          />
          {errorMotivo !== '' && (
            <AppText variant="caption" className="text-error mt-1">
              {errorMotivo}
            </AppText>
          )}
        </View>

        {/* Boton enviar */}
        <Pressable
          onPress={handleEnviar}
          disabled={enviando}
          className={`mt-4 py-3 rounded-xl justify-center items-center min-h-12 ${enviando ? 'bg-primary-300' : 'bg-primary-600'}`}
        >
          <AppText variant="body" weight="semibold" className="text-white">
            {enviando ? 'Enviando...' : 'Enviar solicitud'}
          </AppText>
        </Pressable>

        {/* Divisor */}
        <View className="border-t border-border dark:border-border-dark my-4" />

        {/* Boton WhatsApp */}
        <WhatsAppButton />
      </FloatingCard>

      {/* Modal de exito */}
      <ConfirmModal
        visible={exito}
        title="Solicitud enviada"
        message="Tu medico revisara la peticion y recibiras una notificacion cuando sea confirmada."
        confirmLabel="Aceptar"
        onConfirm={function () { setExito(false); }}
        onCancel={function () { setExito(false); }}
        singleAction={true}
      />
    </>
  );
}
