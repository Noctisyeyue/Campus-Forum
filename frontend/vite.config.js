import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 本地开发服务器配置（仅 npm run dev 时生效，生产打包会被忽略）
  server: {
    proxy: {
      // 把 /api 开头的请求转发到本地后端，模拟生产环境 Nginx 的反向代理
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 图片访问同理转发到后端（后端的 MinIO 图片代理接口）
      '/images': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
