import { mount } from 'svelte'
import './app.css'
import Portal from './Portal.svelte'

const app = mount(Portal, {
  target: document.getElementById('app'),
})

export default app
