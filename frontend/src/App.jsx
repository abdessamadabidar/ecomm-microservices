import { useCallback, useEffect, useRef, useState } from 'react';
import { request } from './api.js';

const emptyResource = () => ({ data: [], error: '', loaded: false });
const numberFormat = new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const amount = (value) => Number.isFinite(Number(value)) ? numberFormat.format(Number(value)) : '—';
const shortId = (value) => String(value ?? '').slice(0, 8);
const time = (value) => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Unknown time' : date.toLocaleTimeString();
};
const pretty = (value) => JSON.stringify(value, null, 2);

function Badge({ children, tone = 'neutral', id }) {
  return <span id={id} className={`badge ${tone}`}>{children}</span>;
}

function StatusBadge({ status }) {
  const tone = ({ CONFIRMED: 'good', COMPLETED: 'good', PENDING_PAYMENT: 'pending', PAYMENT_FAILED: 'bad', FAILED: 'bad' })[status] || 'neutral';
  return <Badge tone={tone}>{String(status || 'Unknown').replaceAll('_', ' ')}</Badge>;
}

function ResourceStatus({ name, resource }) {
  const text = resource.error ? resource.loaded ? 'Stale data' : 'Unavailable' : resource.loaded ? 'Connected' : 'Connecting';
  return <Badge id={`${name}-status`} tone={resource.error ? 'bad' : resource.loaded ? 'good' : 'neutral'}>{text}</Badge>;
}

function ResourceError({ name, resource }) {
  return <p id={`${name}-error`} className="error-message" role="status" hidden={!resource.error}>
    {resource.error}{resource.error && resource.loaded ? ' Showing the last successful response.' : ''}
  </p>;
}

function Flow() {
  const nodes = [
    ['API gateway', 'Receives browser requests', '/api/products · /api/orders'],
    ['Order service', 'Looks up the product over HTTP', '→ product-service'],
    ['Payment service', 'Processes the order amount', 'payment-completed / failed'],
    ['Order + notification', 'Update status and log a notification', 'Notifications also read order-created'],
  ];
  return <section className="panel" aria-labelledby="flow-title">
    <div className="section-heading"><div><h2 id="flow-title">How the services connect</h2><p className="small muted">The configured flow. Select an order below to inspect its observed data.</p></div><Badge>HTTP + Kafka</Badge></div>
    <div className="flow">{nodes.map(([title, description, path], index) => <div className="flow-fragment" key={title}>
      <div className="flow-node"><span className="node-number">0{index + 1}</span><h3>{title}</h3><p>{description}</p><code>{path}</code></div>
      {index < 3 && <div className="flow-link"><span>{['HTTP', 'order-created', 'Kafka'][index]}</span><span aria-hidden="true">→</span></div>}
    </div>)}</div>
    <p className="flow-note">The existing payment demo accepts totals up to 5,000 and fails larger totals. Notifications are simulated in service logs. Amounts have no configured currency.</p>
  </section>;
}

function ProductPanel({ resource, onSubmit, onUse, submitting, result }) {
  return <section id="products" className="panel" aria-labelledby="products-title">
    <div className="section-heading"><div><h2 id="products-title">Products <span id="product-count" className="count">{resource.data.length}</span></h2><p className="small muted">Product service · catalog and stock</p></div><ResourceStatus name="products" resource={resource} /></div>
    <form id="product-form" className="form-grid" onSubmit={onSubmit}>
      <label className="wide">Name<input name="name" placeholder="Mechanical keyboard" required maxLength="255" /></label>
      <label>Unit price<input name="price" type="number" min="0.01" step="0.01" placeholder="75.00" required /></label>
      <label>Stock<input name="quantity" type="number" min="0" max="2147483647" step="1" placeholder="10" required /></label>
      <label className="wide">Description <span className="muted">(optional)</span><input name="description" maxLength="255" placeholder="A product to try the service flow" /></label>
      <div className="form-footer wide"><button type="submit" disabled={submitting}>{submitting ? 'Creating…' : 'Create product'}</button><span id="product-result" className={`small form-result ${result.error ? 'error' : 'success'}`} role="status">{result.message}</span></div>
    </form>
    <ResourceError name="products" resource={resource} />
    <div className="table-scroll"><table><caption className="sr-only">Product catalog</caption><thead><tr><th>Product</th><th>Price</th><th>Stock</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody id="product-rows">
      {resource.data.length ? resource.data.map((product) => <tr key={product.id}><td><strong className="product-name">{product.name}</strong><code title={product.id}>{shortId(product.id)}</code></td><td>{amount(product.price)}</td><td>{product.quantity}</td><td><button type="button" className="secondary small" data-product={product.id} disabled={product.quantity < 1} onClick={() => onUse(product.id)}>Use</button></td></tr>) :
        <tr><td colSpan="4" className="empty">{resource.error ? 'Products could not be loaded.' : 'No products yet. Create one above to get started.'}</td></tr>}
    </tbody></table></div>
  </section>;
}

function OrderForm({ products, chosenProduct, setChosenProduct, onSubmit, submitting, result, quantityRef }) {
  const [quantity, setQuantity] = useState('1');
  const product = products.find((item) => item.id === chosenProduct);
  return <section className="panel" aria-labelledby="create-order-title">
    <div className="section-heading"><div><h2 id="create-order-title">Place an order</h2><p className="small muted">Start the HTTP → Kafka flow</p></div><Badge>POST /api/orders</Badge></div>
    <form id="order-form" className="form-grid" onSubmit={onSubmit}>
      <label className="wide">Product<select id="order-product" name="productId" required value={chosenProduct} disabled={!products.length} onChange={(event) => setChosenProduct(event.target.value)}>
        <option value="">{products.length ? 'Select a product' : 'Create a product first'}</option>
        {products.map((item) => <option value={item.id} disabled={item.quantity < 1} key={item.id}>{item.name} · {amount(item.price)} · {item.quantity} in stock</option>)}
      </select></label>
      <label>Quantity<input id="order-quantity" ref={quantityRef} name="quantity" type="number" min="1" max={product?.quantity || 2147483647} step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></label>
      <div className="estimate"><span className="small muted">Estimated total</span><strong id="order-total">{product && Number(quantity) > 0 ? amount(product.price * Number(quantity)) : '—'}</strong></div>
      <label className="wide">Customer email<input name="customerEmail" type="email" placeholder="demo@example.com" required maxLength="255" /></label>
      <div className="form-footer wide"><button id="place-order" type="submit" disabled={submitting || !product || product.quantity < 1}>{submitting ? 'Placing order…' : 'Place order'}</button><span id="order-result" className={`small form-result ${result.error ? 'error' : 'success'}`} role="status">{result.message}</span></div>
    </form>
    <div className="hint"><strong>Try both outcomes</strong><p>Place an order with a total of 5,000 or less, then one above 5,000. Watch the payment event and order status change.</p><p>The current backend checks stock but does not reduce it after ordering.</p></div>
  </section>;
}

function Orders({ resource, products, selectedOrder, setSelectedOrder, events }) {
  const order = resource.data.find((item) => item.id === selectedOrder);
  const matching = events.filter((item) => item.orderId === selectedOrder);
  const created = matching.some((item) => item.topic === 'order-created');
  const payment = matching.find((item) => item.topic === 'payment-completed' || item.topic === 'payment-failed');
  return <section id="orders" className="panel" aria-labelledby="orders-title">
    <div className="section-heading"><div><h2 id="orders-title">Orders <span id="order-count" className="count">{resource.data.length}</span></h2><p className="small muted">Select an order to filter events and inspect the response.</p></div><ResourceStatus name="orders" resource={resource} /></div>
    <ResourceError name="orders" resource={resource} />
    <div className="table-scroll"><table><caption className="sr-only">Orders and payment status</caption><thead><tr><th>Order</th><th>Product</th><th>Customer</th><th>Qty</th><th>Total</th><th>Status</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody id="order-rows">
      {resource.data.length ? resource.data.map((item) => <tr key={item.id} className={item.id === selectedOrder ? 'selected-row' : ''}><td><code title={item.id}>{shortId(item.id)}</code></td><td><span className="product-name">{products.find((p) => p.id === item.productId)?.name || shortId(item.productId)}</span></td><td>{item.customerEmail}</td><td>{item.quantity}</td><td>{amount(item.totalAmount)}</td><td><StatusBadge status={item.status} /></td><td><button type="button" className="secondary small" data-order={item.id} aria-pressed={item.id === selectedOrder} onClick={() => setSelectedOrder(item.id)}>{item.id === selectedOrder ? 'Selected' : 'Inspect'}</button></td></tr>) :
        <tr><td colSpan="7" className="empty">{resource.error ? 'Orders could not be loaded.' : 'No orders yet. Place an order to start the flow.'}</td></tr>}
    </tbody></table></div>
    <div id="selected-order" className="order-detail" hidden={!selectedOrder}>{selectedOrder && <>
      <p><strong>Order</strong> <code>{selectedOrder}</code></p>
      <div className="observations"><Badge tone={created ? 'good' : 'neutral'}>{created ? 'order-created observed' : 'No order-created in retained history'}</Badge><Badge tone={payment ? payment.topic === 'payment-failed' ? 'bad' : 'good' : 'neutral'}>{payment ? `${payment.topic} observed` : 'No payment event in retained history'}</Badge>{order && <StatusBadge status={order.status} />}</div>
      {order ? <details data-key="order-json"><summary>Order service response</summary><pre>{pretty(order)}</pre></details> : <p className="muted">This order is not in the current order response.</p>}
    </>}</div>
  </section>;
}

function Events({ resource, selectedOrder, setSelectedOrder }) {
  const events = resource.data.filter((event) => !selectedOrder || event.orderId === selectedOrder);
  return <section id="events" className="panel" aria-labelledby="events-title">
    <div className="section-heading"><div><h2 id="events-title">Kafka events <span id="event-count" className="count">{events.length}</span></h2><p className="small muted">Actual payloads received by the notification service. Newest received first.</p></div><ResourceStatus name="events" resource={resource} /></div>
    <ResourceError name="events" resource={resource} />
    <div className="event-toolbar"><span id="event-filter" className="small">{selectedOrder ? `Order ${selectedOrder}` : 'Showing all orders'}</span><button id="clear-selection" type="button" className="secondary small" hidden={!selectedOrder} onClick={() => setSelectedOrder(null)}>Show all orders</button></div>
    <div id="event-list">{events.length ? events.map((event) => <details className="event-item" data-key={event.id} key={event.id}>
      <summary><span className="event-title">{event.topic}</span><Badge tone={event.topic === 'payment-failed' ? 'bad' : 'good'}>Received</Badge><span className="event-meta">{event.source} → {event.consumer} · {shortId(event.orderId)} · {time(event.receivedAt)}</span></summary>
      <div className="payload-body"><p>Order: <code>{event.orderId}</code><br />Received: {event.receivedAt}<br />Topic: <code>{event.topic}</code> · Order key: <code>{event.orderId}</code></p><pre>{pretty(event.payload)}</pre></div>
    </details>) : <p className="empty">{resource.error ? 'Event feed unavailable. Check the notification service and gateway route.' : selectedOrder ? 'No retained events for this order yet. Auto-refresh will check for new messages.' : 'No events received yet. Place an order with the Kafka consumers running.'}</p>}</div>
    <p className="footnote">Latest 200 observations per notification instance; history resets on restart. This confirms receipt by the notification service, not by every consumer. Order status is fetched separately.</p>
  </section>;
}

function Requests({ requests, clear }) {
  return <section className="panel" aria-labelledby="requests-title">
    <div className="section-heading"><div><h2 id="requests-title">Browser requests</h2><p className="small muted">Inspect the request and response for products and orders created here. Background refreshes are omitted.</p></div><button id="clear-requests" type="button" className="secondary small" onClick={clear}>Clear</button></div>
    <div id="request-list">{requests.length ? requests.map((entry) => <details className="request-item" data-key={entry.id} key={entry.id}>
      <summary><span className="event-title">POST {entry.path}</span><Badge tone={entry.error ? 'bad' : 'good'}>{entry.status || 'Network error'}</Badge><span className="event-meta">{entry.duration} ms · {time(entry.at)}</span></summary>
      <div className="payload-body">{entry.error && <p>{entry.error}</p>}<p>Request</p><pre>{pretty(entry.body)}</pre><p>Response</p><pre>{pretty(entry.response)}</pre></div>
    </details>) : <p className="empty">Create a product or place an order to see its HTTP payload.</p>}</div>
  </section>;
}

export default function App() {
  const [resources, setResources] = useState({ products: emptyResource(), orders: emptyResource(), events: emptyResource() });
  const resourcesRef = useRef(resources);
  const refreshRef = useRef(null);
  const requestId = useRef(0);
  const quantityRef = useRef(null);
  const [refreshing, setRefreshing] = useState(false);
  const [refreshStatus, setRefreshStatus] = useState('Connecting…');
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [chosenProduct, setChosenProduct] = useState('');
  const [requests, setRequests] = useState([]);
  const [submittingProduct, setSubmittingProduct] = useState(false);
  const [submittingOrder, setSubmittingOrder] = useState(false);
  const [productResult, setProductResult] = useState({ message: '', error: false });
  const [orderResult, setOrderResult] = useState({ message: '', error: false });

  const updateResource = useCallback((name, change) => {
    const next = { ...resourcesRef.current, [name]: { ...resourcesRef.current[name], ...change } };
    resourcesRef.current = next;
    setResources(next);
  }, []);
  const addRequest = useCallback((entry) => setRequests((previous) => [{ ...entry, id: String(++requestId.current) }, ...previous].slice(0, 20)), []);

  const refresh = useCallback(() => {
    if (refreshRef.current) return refreshRef.current;
    setRefreshing(true);
    setRefreshStatus('Refreshing…');
    const task = Promise.all(['products', 'orders', 'events'].map(async (name) => {
      try {
        const data = await request(`/api/${name}`);
        if (!Array.isArray(data)) throw new Error(`Expected a list from /api/${name}. Check the service route.`);
        updateResource(name, { data, error: '', loaded: true });
        return false;
      } catch (error) {
        updateResource(name, { error: error.message });
        return true;
      }
    })).then((failures) => {
      const count = failures.filter(Boolean).length;
      setRefreshStatus(`${count ? `${count} API${count > 1 ? 's' : ''} unavailable · Checked` : 'Updated'} ${time(new Date())}`);
    }).finally(() => { refreshRef.current = null; setRefreshing(false); });
    refreshRef.current = task;
    return task;
  }, [updateResource]);

  useEffect(() => { refresh(); }, [refresh]);
  useEffect(() => {
    if (!autoRefresh) return undefined;
    const id = setInterval(() => { if (!document.hidden) refresh(); }, 3000);
    const visible = () => { if (!document.hidden) refresh(); };
    document.addEventListener('visibilitychange', visible);
    return () => { clearInterval(id); document.removeEventListener('visibilitychange', visible); };
  }, [autoRefresh, refresh]);

  async function submit(event, kind) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    if (kind === 'product' ? submittingProduct : submittingOrder) return;
    const values = Object.fromEntries(new FormData(form));
    const body = kind === 'product' ? { name: values.name.trim(), description: values.description.trim(), price: Number(values.price), quantity: Number(values.quantity) } :
      { productId: values.productId, quantity: Number(values.quantity), customerEmail: values.customerEmail.trim() };
    const resultSetter = kind === 'product' ? setProductResult : setOrderResult;
    if (kind === 'product' && !body.name) { resultSetter({ message: 'Enter a product name.', error: true }); return; }
    const submittingSetter = kind === 'product' ? setSubmittingProduct : setSubmittingOrder;
    submittingSetter(true);
    resultSetter({ message: '', error: false });
    try {
      const result = await request(`/api/${kind === 'product' ? 'products' : 'orders'}`, { body, onLog: addRequest });
      if (!result?.id) throw new Error('The server did not return a record ID. Refresh before trying again.');
      if (refreshRef.current) await refreshRef.current;
      const name = kind === 'product' ? 'products' : 'orders';
      updateResource(name, { data: [result, ...resourcesRef.current[name].data.filter((item) => item.id !== result.id)] });
      if (kind === 'product') { form.reset(); setChosenProduct(result.id); }
      else setSelectedOrder(result.id);
      resultSetter({ message: `${kind === 'product' ? 'Product' : 'Order'} ${shortId(result.id)} created.`, error: false });
      await refresh();
    } catch (error) {
      resultSetter({ message: error.message, error: true });
    } finally { submittingSetter(false); }
  }

  return <>
    <header className="topbar"><a className="brand" href="#main"><span className="brand-mark" aria-hidden="true">↔</span> ecomm <span className="muted">/ service dashboard</span></a><nav aria-label="Sections"><a href="#products">Products</a><a href="#orders">Orders</a><a href="#events">Events</a></nav></header>
    <main id="main">
      <div className="page-heading"><div><p className="eyebrow">MICROSERVICES EXPLORER</p><h1>Follow the data.</h1><p className="muted">Create a product, place an order, and see what travels between services.</p></div><div className="refresh-controls"><label className="check-label"><input id="auto-refresh" type="checkbox" checked={autoRefresh} onChange={(event) => { setAutoRefresh(event.target.checked); if (event.target.checked) refresh(); }} /> Auto-refresh · 3s</label><button id="refresh" type="button" className="secondary" disabled={refreshing} onClick={refresh}>Refresh now</button><span id="refresh-status" className="small muted" role="status">{refreshStatus}</span></div></div>
      <Flow />
      <div className="workspace"><ProductPanel resource={resources.products} onSubmit={(event) => submit(event, 'product')} onUse={(id) => { setChosenProduct(id); quantityRef.current?.focus(); }} submitting={submittingProduct} result={productResult} /><OrderForm products={resources.products.data} chosenProduct={chosenProduct} setChosenProduct={setChosenProduct} onSubmit={(event) => submit(event, 'order')} submitting={submittingOrder} result={orderResult} quantityRef={quantityRef} /></div>
      <Orders resource={resources.orders} products={resources.products.data} selectedOrder={selectedOrder} setSelectedOrder={setSelectedOrder} events={resources.events.data} />
      <Events resource={resources.events} selectedOrder={selectedOrder} setSelectedOrder={setSelectedOrder} />
      <Requests requests={requests} clear={() => setRequests([])} />
      <footer>Same-origin APIs through the frontend proxy · No sign-in</footer>
    </main>
  </>;
}
