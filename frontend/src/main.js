import { mount } from 'svelte'
import './app.css'
import FinancialModule from './FinancialModule.svelte'

const app = mount(FinancialModule, {
  target: document.getElementById('app'),
})

export default app
