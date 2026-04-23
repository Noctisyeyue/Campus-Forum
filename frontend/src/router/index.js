import { createRouter, createWebHistory } from 'vue-router'
import FrontHomeView from '../views/front/HomeView.vue'
import AdminHomeView from '../views/admin/AdminHomeView.vue'

const routes = [
  {
    path: '/',
    redirect: '/index',
  },
  {
    path: '/index',
    name: 'front-home',
    component: FrontHomeView,
  },
  {
    path: '/admin',
    name: 'admin-home',
    component: AdminHomeView,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
