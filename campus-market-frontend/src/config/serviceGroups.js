import {
  Shop, TrendCharts, Reading, Calendar, ShoppingCart, Lightning, Box,
  Van, Tickets, Connection, Location,
  ChatLineSquare, Medal, CollectionTag, Present,
  DataLine, Star, Odometer, Wallet,
  User, ChatDotRound, Bell, Document
} from '@element-plus/icons-vue'

/**
 * 侧栏功能分组：侧栏只保留分组入口，点进 /hub/:key 后再选择具体功能。
 * 原有页面与路由全部保留，只是收进分组，避免侧栏按钮过多。
 * theme 用于聚合页的主题配色，tag 是卡片上的短标签，featured 为推荐卡片。
 */
export const serviceGroups = [
  {
    key: 'market',
    label: '闲置集市',
    subtitle: '行情、教材、租赁、求购、盲盒，闲置好物都在这里',
    navHint: '行情 · 教材 · 盲盒',
    navDesc: '看行情定价、淘教材、租相机礼服，发求购或许愿盲盒，让闲置找到下一位主人',
    intro: '从开学置办到毕业清仓，课本、数码和生活用品都能在这里找到下一位主人。先看行情再定价，想省钱就逛教材与租赁，想省事就发求购、开个盲盒。',
    highlights: ['毕业清仓', '同校面交', '低价淘好物'],
    tip: '不确定定价时，先去「校园行情」看看同款最近卖了多少。',
    theme: { from: '#FF7A45', to: '#E1251B', soft: '#FFF3EF', accent: '#E1251B' },
    icon: Shop,
    badge: '集',
    isNew: true,
    children: [
      { label: '校园行情', path: '/market', icon: TrendCharts, tag: '定价参考', desc: '看看同款闲置最近的挂牌价与成交量，定价、砍价都有底' },
      { label: '教材循环', path: '/textbook', icon: Reading, tag: '按课程找', desc: '按课程和学院找教材，学长学姐的旧书直接接力，省钱也省心' },
      { label: '租赁专区', path: '/rental', icon: Calendar, tag: '短期省心', desc: '相机、礼服、帐篷只用一两次？短租比买新的更划算', isNew: true },
      { label: '求购专区', path: '/wanted', icon: ShoppingCart, tag: '卖家上门', desc: '想要什么直接发求购，有货的同学会主动来报价', isNew: true },
      { label: '求购热力', path: '/demand', icon: Lightning, tag: '校园趋势', desc: '看看最近校园里哪些东西被频繁求购，也许你手上正好有' },
      {
        label: '闲置盲盒',
        path: '/blindbox',
        icon: Box,
        tag: '惊喜开盒',
        desc: '许愿品类和预算，卖家随机打包，拆盒的惊喜和实惠一起拿下',
        featuredDesc: '许愿一个品类和预算，卖家随机打包发货。可能是整套文具，也可能是一摞课外书，拆开才知道——但一定在你的预算内。',
        isNew: true,
        featured: true
      }
    ]
  },
  {
    key: 'campus',
    label: '校园互助',
    subtitle: '闪送、跑腿、拼单、面交，小事互相搭把手',
    navHint: '闪送 · 跑腿 · 拼单',
    navDesc: '急件闪送、取快递带饭、拼单拼车、约点面交，校园小事都能找到同学搭把手',
    intro: '上课忘带东西、快递没人取、想拼单又凑不齐人，这些校园里的小麻烦都能在这里找到帮手。发布一件小事，附近的同学顺路就帮你办了。',
    highlights: ['顺路帮带', '拼单更省', '面交有约'],
    tip: '急件优先用「校园闪送」，写清楼栋和期望送达时间更容易被接单。',
    theme: { from: '#4C9AFF', to: '#2563EB', soft: '#EEF5FF', accent: '#2563EB' },
    icon: Van,
    badge: '帮',
    isNew: true,
    children: [
      {
        label: '校园闪送',
        path: '/flash-delivery',
        icon: Van,
        tag: '校内直达',
        desc: '急用的文件、钥匙、药品，发布后由校内同学顺路快速送达',
        featuredDesc: '上课发现 U 盘落宿舍、打印店排队来不及？发布闪送，路过的同学顺路帮你带，比等快递快得多。',
        isNew: true,
        featured: true
      },
      { label: '任务大厅', path: '/task', icon: Tickets, tag: '顺手赚零花', desc: '取快递、带饭、占座、打印，发布任务或接单赚点零花', isNew: true },
      { label: '拼单搭子', path: '/together', icon: Connection, tag: '凑单更省', desc: '奶茶、外卖、拼车都能凑单，还能找到一起自习运动的搭子', isNew: true },
      { label: '面交助手', path: '/pickup', icon: Location, tag: '安全面交', desc: '推荐人多好找的面交点，约好时间地点，交易更安心' }
    ]
  },
  {
    key: 'community',
    label: '校园社区',
    subtitle: '论坛、评审、校友标签与公益，认识同频的人',
    navHint: '论坛 · 标签 · 公益',
    navDesc: '逛论坛聊校园热点、参与闲置评审、贴校友标签找同频同学，还能捐闲置做公益',
    intro: '不只买卖，也能认识同频的人。在论坛聊校园新鲜事，去评审团把关闲置品质，用校友标签找到考研、摄影、健身的同路人，还能把闲置捐给更需要的地方。',
    highlights: ['找同频同学', '闲置评审', '公益循环'],
    tip: '先设置校友标签，发帖和交易时更容易被同需求的同学看到。',
    theme: { from: '#9B7BFF', to: '#6D4AFF', soft: '#F4F0FF', accent: '#6D4AFF' },
    icon: ChatLineSquare,
    badge: '区',
    isNew: true,
    children: [
      { label: '校园论坛', path: '/forum', icon: ChatLineSquare, tag: '校园热点', desc: '聊校园话题、问经验、组活动，看看同学们最近都在讨论什么', isNew: true },
      { label: '评审团', path: '/jury', icon: Medal, tag: '品质把关', desc: '参与闲置品质评审，帮大家避坑，也能让自己的好物被看见', isNew: true },
      {
        label: '校友标签',
        path: '/alumni',
        icon: CollectionTag,
        tag: '同频同学',
        desc: '考研、新生、摄影、健身……按标签快速找到同需求的同学',
        featuredDesc: '设置考研、摄影、健身等标签，同频的同学和他们的在售闲置会主动出现在眼前，找搭子不用再靠缘分。',
        isNew: true,
        featured: true
      },
      { label: '公益捐赠', path: '/donation', icon: Present, tag: '让爱循环', desc: '把闲置捐给有需要的同学或社团，让物品继续发挥价值', isNew: true }
    ]
  },
  {
    key: 'data',
    label: '我的数据',
    subtitle: '积分、碳足迹、账单与曝光，看清自己的校园经营',
    navHint: '积分 · 减碳 · 账单',
    navDesc: '做任务攒积分换好物，累计减碳生成年度海报，营收账单与曝光看板帮你看清经营',
    intro: '把每一次交易都变成看得见的收获：积分能换好物、解锁勋章，二手交易会累计减碳并生成年度海报，营收账单和曝光看板则帮你看清钱花在哪、商品为什么没人问。',
    highlights: ['积分兑换', '年度减碳海报', '经营复盘'],
    tip: '曝光高但咨询少，多半是价格或描述的问题，去「曝光看板」对照调整。',
    theme: { from: '#35C08A', to: '#136B4C', soft: '#EAF8F1', accent: '#136B4C' },
    icon: DataLine,
    badge: '数',
    isNew: true,
    children: [
      { label: '积分中心', path: '/points', icon: Star, tag: '兑换好物', desc: '做任务攒积分，兑换优惠券和校园好物，还能解锁环保勋章', isNew: true },
      {
        label: '碳足迹',
        path: '/carbon',
        icon: Odometer,
        tag: '年度海报',
        desc: '每笔二手交易折算减碳量，累计环保贡献并生成年度海报',
        featuredDesc: '每一次二手成交都会折算成减碳量，攒够一年还能生成专属环保海报，既是环保记录，也是朋友圈里的小骄傲。',
        isNew: true,
        featured: true
      },
      { label: '营收账单', path: '/ledger', icon: Wallet, tag: '一键对账', desc: '卖出收入、跑腿支出、积分变动分门别类，支持导出对账' },
      { label: '曝光看板', path: '/exposure', icon: DataLine, tag: '找出问题', desc: '浏览量、收藏、咨询次数和来源宿舍区，帮你找到没人问的原因' }
    ]
  },
  {
    key: 'me',
    label: '个人中心',
    subtitle: '消息、提醒、草稿与个人资料',
    navHint: '消息 · 草稿 · 我的',
    navDesc: '消息实时沟通不漏回，草稿自动保存不丢失，关键词订阅让新上架第一时间提醒你',
    intro: '消息、提醒、草稿和个人主页集中在这里。买家卖家的对话不漏回，发布到一半的闲置自动存草稿，关键词订阅会在有新货时第一时间提醒你。',
    highlights: ['消息不漏', '草稿自动保存', '订阅提醒'],
    tip: '发布较多闲置时先用草稿箱慢慢整理，检查好再一键发布。',
    theme: { from: '#38BDF8', to: '#0E7490', soft: '#ECFAFF', accent: '#0E7490' },
    icon: User,
    badge: '我',
    children: [
      { label: '消息', path: '/message', icon: ChatDotRound, tag: '实时沟通', desc: '和买家卖家实时沟通，议价、约面交都在一个会话里完成' },
      { label: '消息提醒', path: '/subscribe', icon: Bell, tag: '新货提醒', desc: '订阅关键词和感兴趣的品类，有新闲置上架第一时间通知' },
      {
        label: '草稿箱',
        path: '/drafts',
        icon: Document,
        tag: '慢慢整理',
        desc: '发布到一半退出也不丢，毕业清仓可以慢慢整理再一次发完',
        featuredDesc: '毕业清仓几十件闲置，不必一口气发完。先存草稿，慢慢改标题、补图片，整理好再统一发布，发布过程不再手忙脚乱。',
        featured: true
      },
      { label: '我的', path: '/profile', icon: User, tag: '资料管理', desc: '个人主页、在售闲置、订单与信用记录，一处管理' }
    ]
  }
]

/** 按 key 取分组，找不到返回 null */
export const getServiceGroup = (key) => serviceGroups.find(g => g.key === key) || null
