import { mount } from 'svelte'
import './app.css'
import FinancialModule from './FinancialModule.svelte'

const app = mount(FinancialModule, {
  target: document.getElementById('app'),
})

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then(registration => {
        console.log('ServiceWorker зарегистрирован в области:', registration.scope);
      })
      .catch(error => {
        console.error('Ошибка регистрации ServiceWorker:', error);
      });
  });
}

export default app
