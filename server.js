require('dotenv').config();
const express = require('express');
const Stripe = require('stripe');
const { admin } = require('./firebase');
const authRoutes = require('./routes/authRoutes');

const app = express();
const stripe = Stripe(process.env.STRIPE_SECRET_KEY);

if (!process.env.STRIPE_SECRET_KEY || !process.env.STRIPE_PUBLISHABLE_KEY) {
  console.error('Stripe keys are missing. Please set STRIPE_SECRET_KEY and STRIPE_PUBLISHABLE_KEY in the .env file.');
  process.exit(1);
}

app.use(express.json());
app.use('/api/auth', authRoutes);

// Payment Sheet API
app.post('/payment-sheet', async (req, res) => {
  try {
    const { email, name, totalAmount } = req.body;

    if (!email || !name || !totalAmount || totalAmount <= 0) {
      return res.status(400).json({ error: 'Invalid input data. Please provide valid email, name, and totalAmount.' });
    }

    const customer = await stripe.customers.create({ email, name });

    const ephemeralKey = await stripe.ephemeralKeys.create(
      { customer: customer.id },
      { apiVersion: '2022-11-15' }
    );

    const paymentIntent = await stripe.paymentIntents.create({
      amount: totalAmount,
      currency: 'cad',
      customer: customer.id,
      automatic_payment_methods: { enabled: true },
    });

    res.json({
      paymentIntent: paymentIntent.client_secret,
      ephemeralKey: ephemeralKey.secret,
      customer: customer.id,
      publishableKey: process.env.STRIPE_PUBLISHABLE_KEY,
    });
  } catch (error) {
    console.error('Payment Error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Firebase Test API
app.post('/test-firebase', async (req, res) => {
  const { userId } = req.body;

  try {
    const userRecord = await admin.auth().getUser(userId);
    res.json({ user: userRecord });
  } catch (error) {
    console.error('Firebase Error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Health Check Endpoint
app.get('/', (req, res) => {
  res.send('Server is running!');
});

const PORT = process.env.PORT || 4242;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
