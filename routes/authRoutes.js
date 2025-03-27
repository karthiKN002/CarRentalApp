const express = require('express');
const { admin, db, auth } = require('../firebase');
const router = express.Router();

// Register API (User or Manager)
router.post('/register', async (req, res) => {
  const { email, password, name, role } = req.body;

  if (!email || !password || !name || !['user', 'manager'].includes(role)) {
    return res.status(400).json({ error: 'Invalid input data' });
  }

  try {
    const userRecord = await auth.createUser({
      email,
      password,
      displayName: name
    });

    await db.collection('users').doc(userRecord.uid).set({
      name,
      email,
      role
    });

    res.json({ message: 'User registered successfully', uid: userRecord.uid });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Login API (Token-based)
router.post('/login', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required' });
  }

  try {
    const user = await auth.getUserByEmail(email);
    res.json({ message: 'Login successful', uid: user.uid });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Send OTP
// Send OTP API with Logging
router.post('/send-otp', async (req, res) => {
  const { phoneNumber } = req.body;

  console.log('Received Request to Send OTP:', req.body); // Log Incoming Request

  if (!phoneNumber) {
    console.error('Error: Phone number is missing');
    return res.status(400).json({ error: 'Phone number is required' });
  }

  try {
    console.log('Sending OTP to:', phoneNumber); // Log Phone Number
    const verificationId = await admin.auth().createCustomToken(phoneNumber);
    console.log('OTP Sent. Verification ID:', verificationId); // Log Verification ID
    res.json({ verificationId, message: 'OTP sent successfully' });
  } catch (error) {
    console.error('Error Sending OTP:', error.message); // Log Error
    res.status(500).json({ error: error.message });
  }
});


// Verify OTP
// Verify OTP API with Logging
router.post('/verify-otp', async (req, res) => {
  const { verificationId, otp } = req.body;

  console.log('Received Request to Verify OTP:', req.body); // Log Incoming Request

  if (!verificationId || !otp) {
    console.error('Error: Missing Verification ID or OTP');
    return res.status(400).json({ error: 'Verification ID and OTP are required' });
  }

  try {
    const decodedToken = await admin.auth().verifyIdToken(verificationId);
    console.log('Decoded Token:', decodedToken); // Log Decoded Token Details

    if (decodedToken && decodedToken.phone_number === otp) {
      console.log('OTP Verified Successfully');
      res.json({ message: 'OTP verified successfully' });
    } else {
      console.warn('Invalid OTP'); // Log Invalid OTP Warning
      res.status(400).json({ error: 'Invalid OTP' });
    }
  } catch (error) {
    console.error('Error Verifying OTP:', error.message); // Log Error
    res.status(500).json({ error: error.message });
  }
});


module.exports = router;