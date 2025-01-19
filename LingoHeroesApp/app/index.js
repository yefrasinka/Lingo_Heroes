
//delete auth email
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});
const uid = 'YQwhTUMLLlXeOmQUk4CmbJBrDYo2'; // UID użytkownika, którego chcesz usunąć

admin.auth().deleteUser(uid)
  .then(() => {
    console.log('Użytkownik usunięty.');
  })
  .catch((error) => {
    console.error('Błąd usuwania użytkownika:', error);
  });