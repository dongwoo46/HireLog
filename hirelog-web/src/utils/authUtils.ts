/**
 * Get cookie value by name
 */
export function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
  return null;
}

/**
 * Check if a JWT token is expired
 */
export function isTokenExpired(token: string): boolean {
  try {
    const payloadBase64 = token.split('.')[1];
    if (!payloadBase64) return true;

    const payloadJson = atob(payloadBase64);
    const payload = JSON.parse(payloadJson);

    if (!payload.exp) return false;

    // exp matches seconds, Date.now() matches milliseconds
    const currentTime = Math.floor(Date.now() / 1000);
    return payload.exp < currentTime;
  } catch (error) {
    console.error('Error decoding token:', error);
    return true;
  }
}
