// Enmascara el DNI mostrando primeros 4 digitos y ultima letra
// Ej: 12345678Z → 1234****Z
export function maskDni(dni: string | null): string {
  if (!dni) return '—';
  if (dni.length < 2) return '****';
  return `${dni.slice(0, 4)}****${dni.slice(-1)}`;
}

// Enmascara el numero de Seguridad Social mostrando primeros 6 digitos
// Ej: 280000000001 → 2800 00 ****
export function maskSsn(ssn: string | null): string {
  if (!ssn) return '—';
  const digits = ssn.replace(/\D/g, '');
  if (digits.length < 6) return '****';
  return `${digits.slice(0, 4)} ${digits.slice(4, 6)} ****`;
}

// Calcula la edad en anos a partir de una fecha YYYY-MM-DD
export function calcularEdad(birthDate: string | null): number | null {
  if (!birthDate) return null;
  const hoy = new Date();
  const nacimiento = new Date(birthDate);
  let edad = hoy.getFullYear() - nacimiento.getFullYear();
  const mes = hoy.getMonth() - nacimiento.getMonth();
  if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
    edad -= 1;
  }
  return edad;
}
