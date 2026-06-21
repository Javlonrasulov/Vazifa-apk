import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { AppProvider } from './i18n/LanguageContext';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppProvider>
      <App />
    </AppProvider>
  </React.StrictMode>,
);
