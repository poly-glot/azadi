import express from 'express';
import nunjucks from 'nunjucks';
import { readFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const app = express();
const PORT = 3000;

// Parse form data
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Configure Nunjucks
nunjucks.configure(join(__dirname, 'templates'), {
  autoescape: true,
  express: app,
  watch: true,
});

// Serve static assets from frontend src (fallback when Vite is not running)
app.use('/assets/css', express.static(join(__dirname, '../src/css')));
app.use('/assets/js', express.static(join(__dirname, '../src/js')));
app.use('/assets/img', express.static(join(__dirname, '../src/img')));

// Load mock data
const agreements = JSON.parse(readFileSync(join(__dirname, 'data/agreements.json'), 'utf-8'));
const documents = JSON.parse(readFileSync(join(__dirname, 'data/documents.json'), 'utf-8'));

// Vite dev server URL — set when running via `npm run preview` (Vite + mock together)
const VITE_URL = process.env.VITE_URL || 'http://localhost:5173';

// Middleware to inject common template data
app.use(async (req, res, next) => {
  res.locals.currentUrl = req.path;
  res.locals.cspNonce = 'mock-nonce';
  res.locals.isAuthenticated = req.path !== '/' && req.path !== '/login';
  // Check if Vite is running to enable HMR CSS/JS loading
  try {
    await fetch(`${VITE_URL}/@vite/client`, { method: 'HEAD' });
    res.locals.viteUrl = VITE_URL;
  } catch {}
  next();
});

// Routes
app.get('/', (req, res) => {
  res.render('login.njk', {
    title: 'Login',
  });
});

app.post('/login', (req, res) => {
  // Simple mock login
  res.redirect('/my-account');
});

app.get('/my-account', (req, res) => {
  const agreement = agreements[0];
  res.render('my-account.njk', {
    title: 'My Account',
    customerName: agreement.customerName,
    agreement,
    unreadDocuments: 2,
  });
});

app.get('/finance/make-a-payment', (req, res) => {
  res.render('make-a-payment.njk', {
    title: 'Make a Payment',
    customerName: agreements[0].customerName,
    stripePublicKey: 'pk_test_mock_key',
    unreadDocuments: 2,
  });
});

app.get('/my-documents', (req, res) => {
  res.render('my-account.njk', {
    title: 'My Documents',
    customerName: agreements[0].customerName,
    documents,
    unreadDocuments: 2,
    pageContent: 'documents',
  });
});

app.get('/logout', (req, res) => {
  res.redirect('/');
});

// Catch-all for other authenticated pages
app.get('/{*path}', (req, res) => {
  res.render('my-account.njk', {
    title: 'Page',
    customerName: agreements[0].customerName,
    agreement: agreements[0],
    unreadDocuments: 2,
  });
});

app.listen(PORT, () => {
  console.log(`Mock server running at http://localhost:${PORT}`);
});
