// pages/pet/pet.js
const app = getApp();

const TYPE_CONFIG = {
  DRAGON: {
    type: 'DRAGON',
    icon: '🐉',
    eggEmoji: '🟢',
    eggName: '青玉龙灵之卵',
    name: '木木小龙',
    tag: '燃脂闺蜜',
    food: '🍎',
    quote: '今天每走1000步，小龙饭碗里就多添一颗甜苹果🍎～',
    themeBg: '#ECFDF5',
    themeColor: '#047857',
    defaultName: '木木',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 木木',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_dragon_stage1.png',
        quote: '薄荷绿软萌小奶龙，趴在小草垫上最爱吃甜苹果！',
        desc: '初始软萌形态，小巧可爱，每天陪你开启运动燃脂与轻盈生活！'
      },
      {
        rank: 2,
        name: '元气陪伴 · 碧霄灵龙',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_dragon_stage2.png',
        quote: '戴上粉色蝴蝶结发带，背上小黄包，今天也要美美冲鸭！',
        desc: '进阶为元气灵龙，长出漂亮的翡翠龙角与修长羽翼，活力满满！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 青天应龙',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_dragon_stage3.png',
        quote: '樱花花环加身，手捧热香茶，做你永远最贴心的自律闺蜜！',
        desc: '终极形态！身披紫青流云与金辉光芒，守护你的好体态！'
      }
    ]
  },
  TOTORO: {
    type: 'TOTORO',
    icon: '🍃',
    eggEmoji: '⚪',
    eggName: '灵木龙猫之卵',
    name: '呼噜龙猫',
    tag: '治愈松弛',
    food: '🥝',
    quote: '吃饱睡好才是正经事，慢慢来，宝宝超棒的～',
    themeBg: '#F1F5F9',
    themeColor: '#334155',
    defaultName: '呼噜噜',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 龙猫仔',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_totoro_stage1.png',
        quote: '毛茸茸圆球小兽捧星星，抱着新鲜奇异果打盹～',
        desc: '纯真可爱的小龙猫，最喜欢趴在草坡上晒太阳打呼噜。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 森林使者',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_totoro_stage2.png',
        quote: '绿叶小斗篷加身，穿上小雨靴，手握四叶草守护你的好心情！',
        desc: '进阶为森林守护者，身披橡木绿叶斗篷，充满治愈力量！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 治愈神鹿',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_totoro_stage3.png',
        quote: '戴上浆果贝雷帽，穿上爱心毛衣，手提暖灯温暖相伴！',
        desc: '终极形态！头戴浆果贝雷帽，手提温暖小灯，庇佑你的身心平衡！'
      }
    ]
  },
  CAT: {
    type: 'CAT',
    icon: '🐱',
    eggEmoji: '🟡',
    eggName: '元气灵猫之卵',
    name: '元气小橘',
    tag: '轻盈体态',
    food: '🍊',
    quote: '动作要轻盈，体态要挺拔，今天也超级美喵～',
    themeBg: '#FFF7ED',
    themeColor: '#C2410C',
    defaultName: '小橘',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 奶橘',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_cat_stage1.png',
        quote: '奶橘圆球大眼萌猫，抱着大甜橙满地打滚求摸头喵～',
        desc: '活泼好动的小猫咪，最懂女孩子的身材焦虑与体态美。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 灵猫使',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_cat_stage2.png',
        quote: '戴上草莓遮阳帽，系上招财赤红小金铃，步态如风轻盈美美喵！',
        desc: '进阶为赤焰灵猫，系上赤红小铃铛，周身环绕金色星火！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 天焰金猫',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_cat_stage3.png',
        quote: '戴上雏菊贝雷帽与小围巾，手握小水壶，做你专属的优雅化身喵！',
        desc: '终极形态！戴上优雅雏菊贝雷帽与小围巾，体态轻盈！'
      }
    ]
  },
  DOG: {
    type: 'DOG',
    icon: '🐶',
    eggEmoji: '🟤',
    eggName: '忠义玄犬之卵',
    name: '旺财柴柴',
    tag: '户外元气',
    food: '🍓',
    quote: '甩甩尾巴给主人充充电，随时陪你散步吹晚风汪！',
    themeBg: '#FEF3C7',
    themeColor: '#B45309',
    defaultName: '旺财',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 柴柴幼崽',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_dog_stage1.png',
        quote: '吐舌憨萌柴犬宝宝，抱着大草莓，最爱陪主人慢跑！',
        desc: '忠诚可爱的柴犬幼崽，陪伴你度过每一个自律清晨。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 疾风柴柴',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_dog_stage2.png',
        quote: '系上红白格子领巾，穿上运动跑鞋，陪你跑出满满的多巴胺汪！',
        desc: '进阶为健壮潇洒的疾风小猎犬，系着飘扬的红色领巾，奔跑如风！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 烈焰圣犬',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_dog_stage3.png',
        quote: '花环头饰加身，手握金光闪闪奖章，做永远守护你的小太阳！',
        desc: '终极形态！身披暖阳花环与金色奖章，给你无限元气！'
      }
    ]
  },
  QILIN: {
    type: 'QILIN',
    icon: '✨',
    eggEmoji: '🟣',
    eggName: '祥瑞天麟之卵',
    name: '仙贝小麟',
    tag: '好运加持',
    food: '🍑',
    quote: '自律者自带祥瑞，爱自己会吸引宇宙一切美好✨',
    themeBg: '#F5F3FF',
    themeColor: '#6D28D9',
    defaultName: '仙贝',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 麟宝宝',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_qilin_stage1.png',
        quote: '淡紫圆滚小仙麟，双手捧着仙桃送好运～',
        desc: '天生灵秀的紫曜幼麟，踏云而生，带来健康与好运。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 踏云仙麟',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_qilin_stage2.png',
        quote: '系上彩虹小围巾，戴上星光发卡，脚踏白云朵陪你变好！',
        desc: '进阶为踏云仙麟，水晶金角璀璨生辉，踏祥云而行！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 乾坤麒麟圣皇',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_qilin_stage3.png',
        quote: '晶莹星花王冠加身！把全宇宙满满的好运与光芒都送给你！',
        desc: '终极形态！头戴星花王冠，至高祥瑞守护你的自律之路！'
      }
    ]
  },
  RABBIT: {
    type: 'RABBIT',
    icon: '🐰',
    eggEmoji: '🌸',
    eggName: '粉樱甜兔之卵',
    name: '糯糯小兔',
    tag: '甜美治愈',
    food: '🍓',
    quote: '蹦蹦跳跳甩掉卡路里，今天也要开心自律哦～',
    themeBg: '#FFF1F2',
    themeColor: '#E11D48',
    defaultName: '糯糯',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 糯糯兔',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_rabbit_stage1.png',
        quote: '粉白长耳软萌垂耳兔，两颊粉扑扑，最爱抱大草莓！',
        desc: '软萌纯真的小兔兔，每天陪你跳操、喝水与健康轻食！'
      },
      {
        rank: 2,
        name: '元气陪伴 · 甜心兔',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_rabbit_stage2.png',
        quote: '戴上粉色郁金香遮阳帽，背上小黄包，元气满满冲鸭！',
        desc: '进阶为甜心小兔，戴着可爱遮阳帽与小背包，充满活力！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 治愈仙兔',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_rabbit_stage3.png',
        quote: '头戴樱花水晶花冠，手捧热茶，做你永远的贴心小棉袄！',
        desc: '终极形态！身披星芒与樱花花冠，用无条件的爱守护你！'
      }
    ]
  },
  PANDA: {
    type: 'PANDA',
    icon: '🐼',
    eggEmoji: '🎋',
    eggName: '青竹翠玉之卵',
    name: '墩墩熊猫',
    tag: '松弛无忧',
    food: '🎋',
    quote: '多吃新鲜绿叶蔬菜，每天都像大熊猫一样快乐无忧～',
    themeBg: '#F0FDF4',
    themeColor: '#15803D',
    defaultName: '墩墩',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 小墩墩',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_panda_stage1.png',
        quote: '黑白圆滚滚小团子，抱着鲜嫩脆竹笋打滚～',
        desc: '慢条斯理的国宝小熊猫，教你享受健康轻食与松弛节奏。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 功夫墩',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_panda_stage2.png',
        quote: '披上翠绿斗篷，穿上探险鞋，吃饱动起来超健康！',
        desc: '进阶为元气小熊猫，活力满满，督促你规律三餐与运动！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 仙境竹仙',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_panda_stage3.png',
        quote: '头戴翠竹神冠，手提暖灯，守护你的一生从容与健康！',
        desc: '终极形态！化身仙境竹神，给你最踏实安宁的松弛力量！'
      }
    ]
  },
  BEAR: {
    type: 'BEAR',
    icon: '🐻',
    eggEmoji: '🍯',
    eggName: '暖阳焦糖之卵',
    name: '焦糖暖熊',
    tag: '温暖依靠',
    food: '🍯',
    quote: '给你一个超大的暖心熊抱，今天无论如何你都很棒！',
    themeBg: '#FFFBEB',
    themeColor: '#B45309',
    defaultName: '波波',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 小暖熊',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_bear_stage1.png',
        quote: '毛茸茸的焦糖色小熊仔，抱着蜂蜜罐甜甜地笑～',
        desc: '温暖厚实的泰迪小熊，给你最充沛的安全感与陪伴。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 探险熊',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_bear_stage2.png',
        quote: '系上红格子领巾，穿上小皮鞋，陪你探索更棒的自己！',
        desc: '进阶为探险暖熊，精力充沛，随时给你充气打劲！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 守护大白熊',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_bear_stage3.png',
        quote: '花冠加身，做你永远最温暖有力的靠山与后盾！',
        desc: '终极形态！头戴璀璨花环，给你无限的包容与爱！'
      }
    ]
  },
  PENGUIN: {
    type: 'PENGUIN',
    icon: '🐧',
    eggEmoji: '❄️',
    eggName: '极地冰晶之卵',
    name: '皮皮企鹅',
    tag: '清爽轻盈',
    food: '🐟',
    quote: '摇摇摆摆走一万步，甩掉油腻，今天又是清爽的一天！',
    themeBg: '#F0F9FF',
    themeColor: '#0369A1',
    defaultName: '皮皮',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 小皮皮',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_penguin_stage1.png',
        quote: '圆滚滚的冰蓝小企鹅，扑棱小短翅求抱抱～',
        desc: '清凉可爱的小企鹅，最喜欢陪你喝足八杯水、保持清爽！'
      },
      {
        rank: 2,
        name: '元气陪伴 · 破浪企鹅',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_penguin_stage2.png',
        quote: '戴上探险帽背上行囊，迈着小碎步轻快前行！',
        desc: '进阶为破浪小企鹅，轻快敏捷，甩掉疲劳与水肿！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 极光圣企鹅',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_penguin_stage3.png',
        quote: '头戴冰晶花冠，手捧极光暖茶，轻盈变美永不停步！',
        desc: '终极形态！周身环绕极光与冰晶星芒，轻盈优雅！'
      }
    ]
  },
  REDPANDA: {
    type: 'REDPANDA',
    icon: '🍁',
    eggEmoji: '🍂',
    eggName: '红枫栗栗之卵',
    name: '栗栗小熊猫',
    tag: '元气萌主',
    food: '🍎',
    quote: '摇一摇毛茸茸的大尾巴，今天也是活力满满的一天！',
    themeBg: '#FFF7ED',
    themeColor: '#EA580C',
    defaultName: '栗栗',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 栗栗仔',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_redpanda_stage1.png',
        quote: '红褐色毛茸茸的小熊猫，大眼睛扑闪扑闪抱苹果～',
        desc: '活泼好动的小熊猫宝宝，每天陪你快乐轻食与自律打卡！'
      },
      {
        rank: 2,
        name: '元气陪伴 · 枫叶小熊猫',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_redpanda_stage2.png',
        quote: '戴上甜美遮阳帽，系上小金铃铛，活力无限！',
        desc: '进阶为枫叶小熊猫，戴上帽子与小背包，充满探险精神！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 灵枫守护仙',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_redpanda_stage3.png',
        quote: '头戴雏菊贝雷帽与小围巾，守护你的自信与好身材！',
        desc: '终极形态！戴上优雅贝雷帽与暖心水壶，做你最骄傲的搭子！'
      }
    ]
  },
  RACCOON: {
    type: 'RACCOON',
    icon: '🦝',
    eggEmoji: '🌰',
    eggName: '暖灰软糖之卵',
    name: '软糖小浣熊',
    tag: '机灵贴心',
    food: '🍇',
    quote: '洗洗小爪子吃健康餐，自律让生活变得超级有仪式感！',
    themeBg: '#F1F5F9',
    themeColor: '#475569',
    defaultName: '软糖',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 软糖仔',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_raccoon_stage1.png',
        quote: '带眼罩花纹的暖灰小浣熊，乖巧地抱着小星星～',
        desc: '机灵可爱的小浣熊，特别注重生活习惯与饮食仪式感。'
      },
      {
        rank: 2,
        name: '元气陪伴 · 侦探小浣熊',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_raccoon_stage2.png',
        quote: '披上绿叶斗篷，穿上小雨靴，做你身边的健康小侦探！',
        desc: '进阶为小侦探，敏锐感知你的心情与打卡进度！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 森林暖灯使',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_raccoon_stage3.png',
        quote: '戴上复古贝雷帽，穿上爱心毛衣，手提暖灯照亮自律路！',
        desc: '终极形态！复古优雅，用爱与包容守护你的每一天！'
      }
    ]
  },
  OTTER: {
    type: 'OTTER',
    icon: '🦦',
    eggEmoji: '🌊',
    eggName: '清波水灵之卵',
    name: '嘟嘟小水獭',
    tag: '松弛治愈',
    food: '🐚',
    quote: '仰泳漂在水面上晒太阳，今天也要喝足八杯水哦～',
    themeBg: '#F0F9FF',
    themeColor: '#0284C7',
    defaultName: '嘟嘟',
    stages: [
      {
        rank: 1,
        name: '破壳萌新 · 小嘟嘟',
        stageTitle: '阶段1 · 破壳萌新',
        reqText: 'Lv.1 破壳解锁',
        image: '/images/pets/pet_otter_stage1.png',
        quote: '可可色圆滚滚小水獭，双手捧着新鲜大草莓～',
        desc: '超级松弛治愈的小水獭，最喜欢督促你多喝水、多放松！'
      },
      {
        rank: 2,
        name: '元气陪伴 · 水手小水獭',
        stageTitle: '阶段2 · 元气陪伴',
        reqText: 'Lv.5 解锁',
        image: '/images/pets/pet_otter_stage2.png',
        quote: '系上红白领巾穿上跑鞋，迈着轻盈步伐甩掉水肿！',
        desc: '进阶为活力小水手，充满朝气，陪你跑出好状态！'
      },
      {
        rank: 3,
        name: '蜕变闺蜜 · 碧波仙水獭',
        stageTitle: '阶段3 · 蜕变闺蜜',
        reqText: 'Lv.10 解锁',
        image: '/images/pets/pet_otter_stage3.png',
        quote: '樱花花环加身，手捧热香茶，带来如水般的温柔与从容！',
        desc: '终极形态！周身环绕水灵星芒，做你永远最松弛的灵魂搭子！'
      }
    ]
  }
};

const POLAROID_QUOTES = [
  '今日份轻盈已到账 🍃',
  '慢慢来，每一次坚持都在发光 ✨',
  '自律是爱自己的最高形式 ❤️',
  '好好吃饭，好好喝水，好好生活 🌸',
  '今天也是体态轻盈、心情美好的一天 🌟',
  '和搭子一起变轻变好的日常 🍃'
];

Page({
  data: {
    loading: true,
    petSystemEnabled: true,
    hasPet: false,
    pet: null,
    selectedType: 'DRAGON',
    currentTypeInfo: TYPE_CONFIG['DRAGON'],
    petName: '木木',
    adopting: false,
    isFeeding: false,
    isTouched: false,
    heartAnim: false,
    foodIcon: '🍎',

    // 破壳仪式
    hatchingStep: 0,

    // 3 阶成长系统
    petStageRank: 1,
    currentStageInfo: null,
    activePetImage: '/images/pets/pet_dragon_stage1.png',
    nextStageGoalText: 'Lv.5 解锁元气陪伴形态',
    petStageProgressText: '1/5',
    showEvolutionModal: false,
    showDexModal: false,

    // 📸 自律拍立得小红书分享
    showPolaroidModal: false,
    polaroidQuote: '今日份轻盈已到账 🍃',
    polaroidDateText: '',

    // AI 动态交互
    aiThinking: false,
    petDialogue: '',

    // 勋章馆
    showBadgeModal: false,
    selectedBadge: null,
    unlockedBadgeCount: 1,
    badgeList: [
      { id: 'hatch', icon: '🥚', name: '破壳启航', req: '领养搭子', desc: '成功孵化唤醒属于你的第一只自律闺蜜搭子！', unlocked: true },
      { id: 'streak_7', icon: '🔥', name: '自律之星', req: '连续 7 天', desc: '连续陪伴打卡满 7 天，养成自律生活好习惯！', unlocked: false },
      { id: 'feed_20', icon: '🥣', name: '贴心投喂', req: '投喂 20 次', desc: '累计为搭子投喂 20 次健康零食，爱意满满！', unlocked: false },
      { id: 'calorie_5k', icon: '🏃', name: '燃脂达人', req: '消耗 5000kcal', desc: '通过自律运动累计为身体燃脂 5000 大卡！', unlocked: false },
      { id: 'evo_stage2', icon: '🌸', name: '元气陪伴', req: '达到 Lv.5', desc: '搭子成长蜕变，成功解锁元气陪伴高阶形态！', unlocked: false },
      { id: 'evo_stage3', icon: '💖', name: '蜕变闺蜜', req: '达到 Lv.10', desc: '搭子达成 Lv.10 蜕变闺蜜，身披光芒守护！', unlocked: false }
    ],

    // 每日赚粮任务
    foodTasks: {
      checkin: false,
      exercise: false,
      diet: false,
      water: false,
      weight: false
    },
    earnedFoodCount: 0,

    candidateNames: ['木木', '小燃', '豆豆', '卡卡', '饭团', '泡泡', '嘟嘟'],
                        types: [
      { type: 'DRAGON', icon: '🐉', eggEmoji: '🟢', name: '木木小龙' },
      { type: 'TOTORO', icon: '🍃', eggEmoji: '⚪', name: '呼噜龙猫' },
      { type: 'CAT', icon: '🐱', eggEmoji: '🟡', name: '元气小橘' },
      { type: 'DOG', icon: '🐶', eggEmoji: '🟤', name: '旺财柴柴' },
      { type: 'QILIN', icon: '✨', eggEmoji: '🟣', name: '仙贝小麟' },
      { type: 'RABBIT', icon: '🐰', eggEmoji: '🌸', name: '糯糯小兔' },
      { type: 'PANDA', icon: '🐼', eggEmoji: '🎋', name: '墩墩熊猫' },
      { type: 'BEAR', icon: '🐻', eggEmoji: '🍯', name: '焦糖暖熊' },
      { type: 'PENGUIN', icon: '🐧', eggEmoji: '❄️', name: '皮皮企鹅' },
      { type: 'REDPANDA', icon: '🍁', eggEmoji: '🍂', name: '栗栗小熊猫' },
      { type: 'RACCOON', icon: '🦝', eggEmoji: '🌰', name: '软糖小浣熊' },
      { type: 'OTTER', icon: '🦦', eggEmoji: '🌊', name: '嘟嘟小水獭' }
    ]
  },

  onLoad(options) {
    this.checkToggleAndLoad();
    this.initPolaroidDate();
  },

  onShow() {
    this.checkToggleAndLoad();
    this.updateCustomTabBar();
    this.fetchFoodTasks();
  },

  initPolaroidDate() {
    const d = new Date();
    const m = d.getMonth() + 1;
    const day = d.getDate();
    this.setData({ polaroidDateText: `${m}月${day}日` });
  },

  onPullDownRefresh() {
    this.checkToggleAndLoad(() => {
      this.fetchFoodTasks();
      wx.stopPullDownRefresh();
    });
  },

  onRefreshPage() {
    this.checkToggleAndLoad();
    this.fetchFoodTasks();
  },

  updateCustomTabBar() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().updateTabs('pages/pet/pet', (app && app.globalData && app.globalData.features));
    }
  },

  checkToggleAndLoad(callback) {
    let env = 'release';
    try {
      const accountInfo = wx.getAccountInfoSync();
      env = (accountInfo && accountInfo.miniProgram && accountInfo.miniProgram.envVersion) || 'release';
    } catch (e) {}

    wx.request({
      url: `${app.globalData.baseUrl}/config/features?env=${env}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const enabled = res.data.data.pet_system !== false;
          this.setData({ petSystemEnabled: enabled });
          if (app.globalData) { app.globalData.features = res.data.data; }
          this.updateCustomTabBar();
          if (!enabled) {
            this.setData({ loading: false });
            if (callback) callback();
            return;
          }
        }
        app.login((user) => {
          this.fetchPetInfo(callback);
        });
      },
      fail: (err) => {
        app.login((user) => {
          this.fetchPetInfo(callback);
        });
      }
    });
  },

  fetchPetInfo(callback) {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      this.setData({ loading: false });
      if (callback) callback();
      return;
    }

    wx.request({
      url: `${app.globalData.baseUrl}/pet/my?userId=${user.id}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const pet = res.data.data;
          const info = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
          this.setData({
            hasPet: true,
            pet: pet,
            petDialogue: pet.dialogue || '',
            currentTypeInfo: info,
            foodIcon: info.food || '🍎',
            loading: false
          });
          this.calculateEvolutionAndBadges(pet);
        } else if (res.data && res.data.code === 403) {
          this.setData({
            petSystemEnabled: false,
            loading: false
          });
        } else {
          this.setData({
            hasPet: false,
            pet: null,
            loading: false
          });
        }
      },
      fail: (err) => {
        console.error('Fetch pet failed', err);
        this.setData({ loading: false });
      },
      complete: () => {
        if (callback) callback();
      }
    });
  },

  fetchFoodTasks() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) return;

    wx.request({
      url: `${app.globalData.baseUrl}/pet/food-tasks?userId=${user.id}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const tasks = res.data.data.tasks || {};
          let count = 0;
          Object.values(tasks).forEach(v => { if (v) count++; });
          this.setData({
            foodTasks: tasks,
            earnedFoodCount: count
          });
        }
      }
    });
  },

  onDailyCheckin() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '正在签到...' });
    wx.request({
      url: `${app.globalData.baseUrl}/pet/checkin?userId=${user.id}`,
      method: 'POST',
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          const data = res.data.data;
          wx.showToast({ title: data.message || '签到成功！', icon: 'none', duration: 2500 });
          wx.vibrateShort({ type: 'medium' });
          this.fetchPetInfo();
          this.fetchFoodTasks();
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '签到失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络异常', icon: 'none' });
      }
    });
  },

  calculateEvolutionAndBadges(pet) {
    const lvl = pet.level || 1;
    const typeInfo = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
    const stages = typeInfo.stages || [];

    let rank = 1;
    let nextGoal = 'Lv.5 解锁元气陪伴形态';
    let progressText = `${lvl}/5`;

    if (lvl >= 10) {
      rank = 3;
      nextGoal = '已达终极蜕变闺蜜 ✨';
      progressText = 'MAX';
    } else if (lvl >= 5) {
      rank = 2;
      nextGoal = 'Lv.10 解锁蜕变闺蜜形态';
      progressText = `${lvl}/10`;
    }

    const currentStage = stages[rank - 1] || stages[0];

    const badges = [...this.data.badgeList];
    badges[0].unlocked = true;
    badges[1].unlocked = (pet.streakDays || 0) >= 7;
    badges[2].unlocked = (pet.intimacy || 0) >= 200;
    badges[3].unlocked = (pet.level || 1) >= 3;
    badges[4].unlocked = lvl >= 5;
    badges[5].unlocked = lvl >= 10;

    const count = badges.filter(b => b.unlocked).length;

    this.setData({
      petStageRank: rank,
      currentStageInfo: currentStage,
      activePetImage: currentStage.image,
      nextStageGoalText: nextGoal,
      petStageProgressText: progressText,
      badgeList: badges,
      unlockedBadgeCount: count
    });
  },

  onSelectType(e) {
    const type = e.currentTarget.dataset.type;
    const info = TYPE_CONFIG[type] || TYPE_CONFIG['DRAGON'];
    this.setData({
      selectedType: type,
      currentTypeInfo: info,
      petName: info.defaultName,
      foodIcon: info.food || '🍎',
      hatchingStep: 0
    });
    wx.vibrateShort({ type: 'light' });
  },

  onTapEgg() {
    let nextStep = this.data.hatchingStep + 1;
    if (nextStep > 2) nextStep = 2;
    this.setData({ hatchingStep: nextStep });
    wx.vibrateShort({ type: 'medium' });
  },

  onInputName(e) {
    this.setData({ petName: e.detail.value });
  },

  onSelectCandidateName(e) {
    const name = e.currentTarget.dataset.name;
    this.setData({ petName: name });
    wx.vibrateShort({ type: 'light' });
  },

  onRandomName() {
    const candidates = ['木木', '小燃', '豆豆', '卡卡', '饭团', '元宝', '可乐', '泡泡', '嘟嘟'];
    const randomIdx = Math.floor(Math.random() * candidates.length);
    this.setData({ petName: candidates[randomIdx] });
    wx.vibrateShort({ type: 'light' });
  },

  onStartHatchCeremony() {
    const user = app.globalData.userInfo;
    if (!user || !user.id) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    const name = this.data.petName ? this.data.petName.trim() : '小搭子';
    if (!name) {
      wx.showToast({ title: '请为搭子起个名字', icon: 'none' });
      return;
    }

    this.setData({ adopting: true, hatchingStep: 1 });
    wx.vibrateShort({ type: 'medium' });

    setTimeout(() => {
      this.setData({ hatchingStep: 2 });
      wx.vibrateShort({ type: 'heavy' });
    }, 600);

    setTimeout(() => {
      this.submitAdopt(user.id, name);
    }, 1200);
  },

  submitAdopt(userId, name) {
    wx.request({
      url: `${app.globalData.baseUrl}/pet/create`,
      method: 'POST',
      data: {
        userId: userId,
        name: name,
        petType: this.data.selectedType,
        avatarUrl: this.data.currentTypeInfo.stages[0].image
      },
      success: (res) => {
        this.setData({ adopting: false });
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '破壳成功！🎉', icon: 'success' });
          const pet = res.data.data;
          const info = TYPE_CONFIG[pet.petType] || TYPE_CONFIG['DRAGON'];
          this.setData({
            hasPet: true,
            pet: pet,
            currentTypeInfo: info,
            foodIcon: info.food || '🍎',
            hatchingStep: 0
          });
          this.calculateEvolutionAndBadges(pet);
          this.fetchFoodTasks();
          wx.vibrateShort({ type: 'heavy' });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '孵化失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ adopting: false });
        wx.showToast({ title: '网络异常，请重试', icon: 'none' });
      }
    });
  },

  onFeedPet() {
    const user = app.globalData.userInfo;
    if (!user || !user.id || !this.data.pet) return;

    if (this.data.pet.foodCount <= 0) {
      wx.showModal({
        title: '零食袋空空啦',
        content: '小家伙的零食袋空空啦！完成下方「赚粮任务」（运动/记餐/喝水打卡）就能免费获得食物哦～',
        confirmText: '去签到',
        cancelText: '稍后再说',
        success: (modalRes) => {
          if (modalRes.confirm && !this.data.foodTasks.checkin) {
            this.onDailyCheckin();
          }
        }
      });
      return;
    }

    const oldLevel = this.data.pet.level || 1;
    this.setData({ isFeeding: true });
    wx.vibrateShort({ type: 'medium' });

    wx.request({
      url: `${app.globalData.baseUrl}/pet/feed?userId=${user.id}`,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const updated = res.data.data;
          const newLevel = updated.level || 1;

          this.setData({ pet: updated });
          this.calculateEvolutionAndBadges(updated);

          if ((oldLevel < 5 && newLevel >= 5) || (oldLevel < 10 && newLevel >= 10)) {
            setTimeout(() => {
              this.setData({ showEvolutionModal: true });
              wx.vibrateShort({ type: 'heavy' });
            }, 600);
          } else {
            wx.showToast({ title: '投喂成功！+10 能量 ✨', icon: 'none' });
          }
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '投喂失败', icon: 'none' });
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络异常', icon: 'none' });
      },
      complete: () => {
        setTimeout(() => {
          this.setData({ isFeeding: false });
        }, 1200);
      }
    });
  },

  onTapPet() {
    if (!this.data.pet || this.data.isFeeding) return;

    this.setData({
      isTouched: true,
      heartAnim: true
    });
    wx.vibrateShort({ type: 'light' });

    setTimeout(() => {
      this.setData({
        isTouched: false,
        heartAnim: false
      });
    }, 600);

    this.callAiInteraction('TOUCH', '');
  },

  onTapQuickPrompt(e) {
    const prompt = e.currentTarget.dataset.prompt;
    if (!prompt) return;
    this.callAiInteraction('CHAT', prompt);
  },

  callAiInteraction(actionType, userMessage) {
    const user = app.globalData.userInfo;
    if (!user || !user.id || !this.data.pet) return;

    this.setData({ aiThinking: true });

    wx.request({
      url: `${app.globalData.baseUrl}/pet/interact`,
      method: 'POST',
      data: {
        userId: user.id,
        actionType: actionType,
        userMessage: userMessage
      },
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const vo = res.data.data;
          this.setData({
            petDialogue: vo.dialogue,
            aiThinking: false
          });
          wx.vibrateShort({ type: 'light' });
        } else {
          this.setData({ aiThinking: false });
        }
      },
      fail: (err) => {
        console.warn('AI interact request failed, using fallback', err);
        this.setData({ aiThinking: false });
      }
    });
  },

  /* 📸 自律拍立得小红书分享 */
  onOpenPolaroidModal() {
    const randomQuote = POLAROID_QUOTES[Math.floor(Math.random() * POLAROID_QUOTES.length)];
    this.setData({
      showPolaroidModal: true,
      polaroidQuote: randomQuote
    });
    wx.vibrateShort({ type: 'medium' });
  },

  onClosePolaroidModal() {
    this.setData({ showPolaroidModal: false });
  },

  onSavePolaroidToAlbum() {
    wx.showLoading({ title: '正在生成拍立得...' });
    setTimeout(() => {
      wx.hideLoading();
      wx.showModal({
        title: '📸 拍立得已就绪',
        content: '拍立得卡片已生成！您可以直接截图保存，分享至小红书或微信朋友圈，晒出你的松弛感自律日常～✨',
        showCancel: false,
        confirmText: '知道啦 💖'
      });
      wx.vibrateShort({ type: 'heavy' });
    }, 600);
  },

  onOpenDexModal() {
    this.setData({ showDexModal: true });
  },

  onCloseDexModal() {
    this.setData({ showDexModal: false });
  },

  onOpenBadgeModal() {
    const first = this.data.badgeList[0];
    this.setData({
      showBadgeModal: true,
      selectedBadge: first
    });
  },

  onCloseBadgeModal() {
    this.setData({ showBadgeModal: false });
  },

  onTapBadgeItem(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({ selectedBadge: item });
    wx.vibrateShort({ type: 'light' });
  },

  onCloseEvolutionModal() {
    this.setData({ showEvolutionModal: false });
  },

  onGoExercise() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoDiet() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoWater() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoWeight() { wx.switchTab({ url: '/pages/index/index' }); },
  onGoHome() { wx.switchTab({ url: '/pages/index/index' }); },
  noBubble() {}
});
