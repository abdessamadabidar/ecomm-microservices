export async function request(path, { body, method = body === undefined ? 'GET' : 'POST', onLog } = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8000);
  const started = performance.now();
  let responseBody = null;
  let status = null;
  let failure = '';

  try {
    const response = await fetch(path, {
      method,
      headers: body === undefined ? { Accept: 'application/json' } : { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
      cache: 'no-store',
    });
    status = response.status;
    const raw = await response.text();
    try { responseBody = raw ? JSON.parse(raw) : null; }
    catch { responseBody = raw; }
    if (!response.ok) {
      const message = responseBody && typeof responseBody === 'object' && (responseBody.message || responseBody.error);
      throw new Error(message ? String(message) : `HTTP ${status}: ${response.statusText || 'Request failed'}`);
    }
    if (raw && typeof responseBody === 'string') throw new Error('The server returned a non-JSON response. Check the gateway route.');
    return responseBody;
  } catch (error) {
    failure = error.name === 'AbortError' ? 'Request timed out after 8 seconds. Check that the service is running.' :
      error instanceof TypeError ? 'Cannot reach the gateway. Check that it is running and try again.' : error.message;
    throw new Error(failure);
  } finally {
    clearTimeout(timeout);
    if (method !== 'GET') onLog?.({ method, path, body: body ?? null, response: responseBody, status, error: failure,
      duration: Math.round(performance.now() - started), at: new Date().toISOString() });
  }
}
