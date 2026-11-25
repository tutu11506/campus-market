import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { ElLoading } from 'element-plus'
import 'element-plus/dist/index.css'
import {
  AlarmClock,
  ArrowLeft,
  ArrowRight,
  ArrowUpBold,
  Bell,
  Box,
  Calendar,
  ChatDotRound,
  ChatLineSquare,
  Check,
  CircleCheck,
  Clock,
  Coin,
  CollectionTag,
  Connection,
  DataLine,
  Document,
  Download,
  EditPen,
  FolderOpened,
  FullScreen,
  Goods,
  HomeFilled,
  House,
  Lightning,
  Link,
  Loading,
  Location,
  Lock,
  MagicStick,
  Medal,
  Odometer,
  Phone,
  Picture,
  Plus,
  Present,
  Reading,
  Refresh,
  School,
  Search,
  Share,
  Shop,
  ShoppingCart,
  Star,
  StarFilled,
  Tickets,
  TrendCharts,
  Upload,
  User,
  Van,
  View,
  Wallet,
  Warning
} from '@element-plus/icons-vue'
import './style.css'

const app = createApp(App)

// Element Plus 组件由 unplugin-vue-components 在各 SFC 内按需引入，这里不再全量注册
// v-loading 指令（列表加载态）
app.directive('loading', ElLoading.directive)

// 只注册模板中实际用到的图标；全量注册会把 300+ 个图标打进首屏包体
const icons = {
  AlarmClock,
  ArrowLeft,
  ArrowRight,
  ArrowUpBold,
  Bell,
  Box,
  Calendar,
  ChatDotRound,
  ChatLineSquare,
  Check,
  CircleCheck,
  Clock,
  Coin,
  CollectionTag,
  Connection,
  DataLine,
  Document,
  Download,
  EditPen,
  FolderOpened,
  FullScreen,
  Goods,
  HomeFilled,
  House,
  Lightning,
  Link,
  Loading,
  Location,
  Lock,
  MagicStick,
  Medal,
  Odometer,
  Phone,
  Picture,
  Plus,
  Present,
  Reading,
  Refresh,
  School,
  Search,
  Share,
  Shop,
  ShoppingCart,
  Star,
  StarFilled,
  Tickets,
  TrendCharts,
  Upload,
  User,
  Van,
  View,
  Wallet,
  Warning
}
Object.entries(icons).forEach(([name, component]) => {
  app.component(name, component)
})

app.use(router)
app.mount('#app')
