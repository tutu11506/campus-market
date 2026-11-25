import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 组件在各自 SFC 内按需引入：只有当前路由用到的组件才会打进对应分包，
    // 样式仍统一走全局 Element Plus CSS（importStyle: false 避免重复注入）
    Components({
      dirs: [],
      dts: false,
      resolvers: [ElementPlusResolver({ importStyle: false })],
    }),
  ],
  resolve: {
    alias: [
      // 把 element-plus 精确指向按需出口，只打包实际用到的组件（详见 src/element-plus-on-demand.js）
      {
        find: /^element-plus$/,
        replacement: fileURLToPath(new URL('./src/element-plus-on-demand.js', import.meta.url)),
      },
    ],
  },
  build: {
    rollupOptions: {
      output: {
        // 只把仅在后台统计页用到的 echarts 单独拆出，其余交给 Rollup 按路由自动分包，
        // 保证首屏只加载首页真正用到的 Element Plus 组件
        manualChunks(id) {
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
        },
      },
    },
  },
  server: {
    host: '0.0.0.0',
    allowedHosts: ['.ngrok-free.dev', '.ngrok-free.app', '.ngrok.app', '.ngrok.io'],
    proxy: {
      // 前端统一走 /api，转发到后端 8081 并去掉 /api 前缀
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
