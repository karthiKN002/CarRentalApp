const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://carrentalmanagementapp.firebaseio.com"
});

const db = admin.firestore();
const auth = admin.auth();

module.exports = { admin, db, auth };
