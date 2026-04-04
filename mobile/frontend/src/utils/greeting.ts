// Genera el saludo basado en la hora local del dispositivo
export function getGreeting(name: string): string {
  const hours = new Date().getHours();
  const timeGreeting = hours < 12 ? 'Buenos dias' : 'Buenas tardes';
  return `${timeGreeting} ${name}, vamos a empezar la rutina :)`;
}
