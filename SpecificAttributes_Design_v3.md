# 设定分类专属属性设计文档

## 概述

本文档定义了StorySetting.specificAttributes字段的详细结构，针对SettingCategoryConfig.java中的6大顶层分类和30个子分类设计专属属性。

---

## 一、世界观设定

### 1.1 地理环境
```json
{
  "area": "面积描述",
  "terrain": ["山地", "平原", "水域"],
  "climate": "气候类型",
  "resources": ["矿藏", "灵植"],
  "dangerLevel": 5,
  "coordinates": "坐标位置",
  "features": ["险峻地形", "天然屏障"],
  "strategicValue": "战略价值描述"
}
```
| 字段             | 类型         | 说明        |
|----------------|------------|-----------|
| area           | TEXT       | 面积描述      |
| terrain        | TAG_LIST   | 地形标签      |
| climate        | TEXT       | 气候类型      |
| resources      | TAG_LIST   | 资源列表      |
| dangerLevel    | SLIDER     | 危险等级 1-10 |
| coordinates    | TEXT       | 坐标位置      |
| features       | TAG_LIST   | 特色标签      |
| strategicValue | TEXT_MULTI | 战略价值      |

### 1.2 时代背景
```json
{
  "eraName": "时代名称",
  "timePeriod": "时间段",
  "techLevel": "科技水平",
  "culturalLevel": "文化程度",
  "mainConflicts": ["冲突1", "冲突2"],
  "socialMood": "社会氛围"
}
```
| 字段            | 类型       | 说明                      |
|---------------|----------|-------------------------|
| eraName       | TEXT     | 时代名称                    |
| timePeriod    | TEXT     | 时间段                     |
| techLevel     | SELECT   | 科技水平：原始/古代/中世纪/近代/现代/未来 |
| culturalLevel | SELECT   | 文化程度：蒙昧/启蒙/发展/繁荣/衰退     |
| mainConflicts | TAG_LIST | 主要冲突                    |
| socialMood    | TEXT     | 社会氛围                    |

### 1.3 历史背景
```json
{
  "majorEvents": [
    {"year": -500, "event": "创世", "significance": 10},
    {"year": 0, "event": "大灾变", "significance": 9}
  ],
  "keyFigures": ["人物1", "人物2"],
  "historicalImpact": "历史影响描述",
  "legacy": "遗留影响"
}
```
| 字段               | 类型         | 说明                 |
|------------------|------------|--------------------|
| majorEvents      | TEXT_MULTI | 重大事件（格式：年份:事件:重要性） |
| keyFigures       | TAG_LIST   | 关键人物               |
| historicalImpact | TEXT_MULTI | 历史影响               |
| legacy           | TEXT_MULTI | 遗留影响               |

### 1.4 文明种族
```json
{
  "raceName": "种族名称",
  "origin": "起源",
  "physicalTraits": ["特征1", "特征2"],
  "culturalTraits": ["文化1", "文化2"],
  "language": "语言特点",
  "religion": "宗教信仰",
  "population": "人口规模",
  "status": "现状：兴旺/衰落/灭绝"
}
```
| 字段             | 类型       | 说明                  |
|----------------|----------|---------------------|
| raceName       | TEXT     | 种族名称                |
| origin         | TEXT     | 起源                  |
| physicalTraits | TAG_LIST | 身体特征                |
| culturalTraits | TAG_LIST | 文化特征                |
| language       | TEXT     | 语言特点                |
| religion       | TEXT     | 宗教信仰                |
| population     | SELECT   | 人口规模：稀少/少量/中等/众多/海量 |
| status         | SELECT   | 现状：兴旺/平稳/衰落/濒危/灭绝   |

### 1.5 文化习俗
```json
{
  "festivals": ["节日1", "节日2"],
  "traditions": ["传统1", "传统2"],
  "taboos": ["禁忌1", "禁忌2"],
  "culturalValues": ["价值观1", "价值观2"],
  "artForms": ["艺术形式1", "艺术形式2"],
  "dietaryHabits": "饮食习惯"
}
```
| 字段             | 类型       | 说明    |
|----------------|----------|-------|
| festivals      | TAG_LIST | 节日    |
| traditions     | TAG_LIST | 传统习俗  |
| taboos         | TAG_LIST | 禁忌    |
| culturalValues | TAG_LIST | 文化价值观 |
| artForms       | TAG_LIST | 艺术形式  |
| dietaryHabits  | TEXT     | 饮食习惯  |

### 1.6 社会制度
```json
{
  "governmentType": "政体类型",
  "socialHierarchy": ["阶层1", "阶层2", "阶层3"],
  "laws": ["法律1", "法律2"],
  "justiceSystem": "司法制度",
  "classMobility": "阶级流动性",
  "corruption": 3,
  "stability": 7
}
```
| 字段              | 类型         | 说明                         |
|-----------------|------------|----------------------------|
| governmentType  | SELECT     | 政体：君主制/贵族制/共和制/民主制/神权制/独裁制 |
| socialHierarchy | TAG_LIST   | 社会阶层                       |
| laws            | TAG_LIST   | 重要法律                       |
| justiceSystem   | TEXT_MULTI | 司法制度                       |
| classMobility   | SELECT     | 阶级流动性：高/中/低/固化             |
| corruption      | SLIDER     | 腐败程度 1-10                  |
| stability       | SLIDER     | 稳定程度 1-10                  |

### 1.7 政治势力
```json
{
  "factionName": "势力名称",
  "factionType": "势力类型",
  "leader": "领导者",
  "territory": "控制区域",
  "militaryStrength": "军事力量",
  "economicStrength": "经济实力",
  "politicalIdeology": "政治理念",
  "allies": ["盟友1", "盟友2"],
  "enemies": ["敌人1", "敌人2"],
  "influence": 7
}
```
| 字段                | 类型       | 说明                     |
|-------------------|----------|------------------------|
| factionName       | TEXT     | 势力名称                   |
| factionType       | SELECT   | 势力类型：王国/帝国/宗门/帮派/组织/联盟 |
| leader            | TEXT     | 领导者                    |
| territory         | TEXT     | 控制区域                   |
| militaryStrength  | SELECT   | 军事力量：薄弱/一般/强大/顶尖       |
| economicStrength  | SELECT   | 经济实力：贫困/温饱/富裕/巨富       |
| politicalIdeology | TEXT     | 政治理念                   |
| allies            | TAG_LIST | 盟友                     |
| enemies           | TAG_LIST | 敌对势力                   |
| influence         | SLIDER   | 影响力 1-10               |

### 1.8 科技发展
```json
{
  "techLevel": "科技等级",
  "mainTechnologies": ["技术1", "技术2"],
  "forbiddenTechs": ["禁忌技术1"],
  "researchDirection": "研究方向",
  "techFeatures": ["科技特色1", "特色2"],
  "developmentStage": "发展阶段"
}
```
| 字段                | 类型         | 说明                |
|-------------------|------------|-------------------|
| techLevel         | TEXT       | 科技等级描述            |
| mainTechnologies  | TAG_LIST   | 主要技术              |
| forbiddenTechs    | TAG_LIST   | 禁忌技术              |
| researchDirection | TEXT_MULTI | 研究方向              |
| techFeatures      | TAG_LIST   | 科技特色              |
| developmentStage  | SELECT     | 发展阶段：萌芽/发展中/成熟/衰退 |

### 1.9 物品资源
```json
{
  "itemName": "物品名称",
  "itemType": "物品类型",
  "rarity": "稀有度",
  "source": "来源",
  "usage": "用途",
  "value": "价值",
  "sideEffects": ["副作用1"],
  "relatedLocations": ["产地1", "产地2"]
}
```
| 字段               | 类型         | 说明                     |
|------------------|------------|------------------------|
| itemName         | TEXT       | 物品名称                   |
| itemType         | SELECT     | 物品类型：材料/装备/丹药/功法/道具/货币 |
| rarity           | SELECT     | 稀有度：普通/稀有/珍稀/传说/神话     |
| source           | TEXT_MULTI | 来源                     |
| usage            | TEXT_MULTI | 用途                     |
| value            | TEXT       | 价值描述                   |
| sideEffects      | TAG_LIST   | 副作用                    |
| relatedLocations | TAG_LIST   | 关联地点                   |

---

## 二、角色设定

### 2.1 主要角色
```json
{
  "roleType": "protagonist",
  "age": 18,
  "gender": "male",
  "appearance": "外貌描述",
  "personalityTraits": ["冷静", "机智"],
  "goals": ["目标1", "目标2"],
  "internalConflicts": ["内心冲突1"],
  "secrets": ["秘密1"],
  "weaknesses": ["弱点1"],
  "strengths": ["优势1"],
  "backgroundStory": "背景故事",
  "relationships": [
    {"character": "林雪", "relation": "妹妹", "description": "关系描述"}
  ],
  "characterArc": "角色成长弧光",
  "signatureItems": ["标志性物品1"],
  "quotes": ["经典台词1"],
  "currentStatus": "活跃",
  "plotImportance": 10
}
```
| 字段                | 类型         | 说明                                                        |
|-------------------|------------|-----------------------------------------------------------|
| roleType          | SELECT     | 角色类型：protagonist:主角/antagonist:反派/supporting:配角/mentor:导师 |
| age               | NUMBER     | 年龄                                                        |
| gender            | SELECT     | 性别：male:男/female:女/other:其他                               |
| appearance        | TEXT_MULTI | 外貌描述                                                      |
| personalityTraits | TAG_LIST   | 性格特征                                                      |
| goals             | TAG_LIST   | 目标                                                        |
| internalConflicts | TAG_LIST   | 内在冲突                                                      |
| secrets           | TAG_LIST   | 隐藏秘密                                                      |
| weaknesses        | TAG_LIST   | 弱点                                                        |
| strengths         | TAG_LIST   | 优势                                                        |
| backgroundStory   | TEXT_MULTI | 背景故事                                                      |
| relationships     | TEXT_MULTI | 关系网（格式：角色:关系:描述）                                          |
| characterArc      | TEXT_MULTI | 角色成长弧光                                                    |
| signatureItems    | TAG_LIST   | 标志性物品                                                     |
| quotes            | TAG_LIST   | 经典台词                                                      |
| currentStatus     | SELECT     | 现状：活跃/退场/死亡/失踪                                            |
| plotImportance    | SLIDER     | 剧情重要性 1-10                                                |

### 2.2 次要角色
```json
{
  "roleType": "supporting",
  "age": 25,
  "gender": "female",
  "appearance": "外貌描述",
  "personalityTraits": ["热心", "善良"],
  "mainFunction": "在剧情中的作用",
  "relationshipToProtagonist": "与主角关系",
  "keyScenes": ["关键场景1"],
  "developmentPotential": "发展空间",
  "screenTime": 5,
  "status": "活跃"
}
```
| 字段                        | 类型         | 说明                                       |
|---------------------------|------------|------------------------------------------|
| roleType                  | SELECT     | 角色类型：supporting:配角/mentor:导师/sidekick:跟班 |
| age                       | NUMBER     | 年龄                                       |
| gender                    | SELECT     | 性别                                       |
| appearance                | TEXT_MULTI | 外貌描述                                     |
| personalityTraits         | TAG_LIST   | 性格特征                                     |
| mainFunction              | TEXT_MULTI | 主要功能                                     |
| relationshipToProtagonist | TEXT       | 与主角关系                                    |
| keyScenes                 | TAG_LIST   | 关键场景                                     |
| developmentPotential      | TEXT_MULTI | 发展空间                                     |
| screenTime                | SLIDER     | 戏份程度 1-10                                |
| status                    | SELECT     | 现状：活跃/退场/死亡                              |

### 2.3 反派角色
```json
{
  "roleType": "antagonist",
  "age": 35,
  "gender": "male",
  "appearance": "外貌描述",
  "personalityTraits": ["冷酷", "狡猾"],
  "evilType": "邪恶类型",
  "goals": ["目标1", "目标2"],
  "methods": ["手段1"],
  "strengths": ["优势1"],
  "weaknesses": ["弱点1"],
  "relationships": [
    {"character": "主角", "relation": "宿敌", "description": "关系描述"}
  ],
  "motive": "动机",
  "threatLevel": 8,
  "currentStatus": "活跃"
}
```
| 字段                | 类型         | 说明                       |
|-------------------|------------|--------------------------|
| roleType          | SELECT     | 角色类型：antagonist:反派       |
| age               | NUMBER     | 年龄                       |
| gender            | SELECT     | 性别                       |
| appearance        | TEXT_MULTI | 外貌描述                     |
| personalityTraits | TAG_LIST   | 性格特征                     |
| evilType          | SELECT     | 邪恶类型：野心型/报复型/狂热型/权欲型/扭曲型 |
| goals             | TAG_LIST   | 目标                       |
| methods           | TAG_LIST   | 作恶手段                     |
| strengths         | TAG_LIST   | 优势                       |
| weaknesses        | TAG_LIST   | 弱点                       |
| relationships     | TEXT_MULTI | 关系网                      |
| motive            | TEXT_MULTI | 动机                       |
| threatLevel       | SLIDER     | 威胁等级 1-10                |
| currentStatus     | SELECT     | 现状：活跃/退场/死亡              |

### 2.4 组织阵营
```json
{
  "groupName": "组织名称",
  "groupType": "组织类型",
  "scale": "规模",
  "leader": "首领",
  "coreMembers": ["核心成员1", "成员2"],
  "commonTraits": ["共同特征1"],
  "hierarchy": "内部层级",
  "goals": ["目标1", "目标2"],
  "codeOfConduct": "行事准则",
  "resources": ["资源1"],
  "influence": 7,
  "alignment": "阵营倾向"
}
```
| 字段            | 类型         | 说明                          |
|---------------|------------|-----------------------------|
| groupName     | TEXT       | 组织名称                        |
| groupType     | SELECT     | 组织类型：宗门/帮派/家族/王国/教派/商会/秘密组织 |
| scale         | SELECT     | 规模：微型/小型/中型/大型/巨型           |
| leader        | TEXT       | 首领                          |
| coreMembers   | TAG_LIST   | 核心成员                        |
| commonTraits  | TAG_LIST   | 共同特征                        |
| hierarchy     | TEXT_MULTI | 内部层级                        |
| goals         | TAG_LIST   | 组织目标                        |
| codeOfConduct | TEXT_MULTI | 行事准则                        |
| resources     | TAG_LIST   | 资源                          |
| influence     | SLIDER     | 影响力 1-10                    |
| alignment     | SELECT     | 阵营：正义/中立/邪恶/灰色              |

---

## 三、地点设定

### 3.1 国家地区
```json
{
  "countryName": "国家名称",
  "area": "面积",
  "population": "人口",
  "governmentType": "政体",
  "capital": "首都",
  "mainCities": ["主要城市1"],
  "terrain": ["地形1", "地形2"],
  "climate": "气候",
  "resources": ["资源1"],
  "economy": "经济状况",
  "military": "军事实力",
  "culture": "文化特色",
  "allies": ["盟友"],
  "enemies": ["敌国"],
  "stability": 7,
  "nationalStrength": 8
}
```
| 字段               | 类型         | 说明                 |
|------------------|------------|--------------------|
| countryName      | TEXT       | 国家名称               |
| area             | TEXT       | 面积                 |
| population       | TEXT       | 人口                 |
| governmentType   | SELECT     | 政体：君主制/贵族制/共和制/... |
| capital          | TEXT       | 首都                 |
| mainCities       | TAG_LIST   | 主要城市               |
| terrain          | TAG_LIST   | 地形                 |
| climate          | TEXT       | 气候                 |
| resources        | TAG_LIST   | 资源                 |
| economy          | SELECT     | 经济：贫困/温饱/发达/强盛     |
| military         | SELECT     | 军事：薄弱/一般/强大/顶尖     |
| culture          | TEXT_MULTI | 文化特色               |
| allies           | TAG_LIST   | 盟友                 |
| enemies          | TAG_LIST   | 敌国                 |
| stability        | SLIDER     | 稳定度 1-10           |
| nationalStrength | SLIDER     | 国力 1-10            |

### 3.2 城市
```json
{
  "cityName": "城市名称",
  "cityType": "城市类型",
  "population": "人口",
  "location": "位置",
  "mainDistricts": ["区域1", "区域2"],
  "features": ["特色1"],
  "economy": "经济",
  "culture": "文化",
  "famousBuildings": ["著名建筑1"],
  "notableResidents": ["知名居民1"],
  "dangerLevel": 3,
  "prosperity": 8
}
```
| 字段               | 类型         | 说明                        |
|------------------|------------|---------------------------|
| cityName         | TEXT       | 城市名称                      |
| cityType         | SELECT     | 城市类型：首都/港口/商业/工业/文化/军事/宗教 |
| population       | TEXT       | 人口                        |
| location         | TEXT       | 位置描述                      |
| mainDistricts    | TAG_LIST   | 主要区域                      |
| features         | TAG_LIST   | 城市特色                      |
| economy          | SELECT     | 经济：萧条/一般/繁荣/繁华            |
| culture          | TEXT_MULTI | 文化特色                      |
| famousBuildings  | TAG_LIST   | 著名建筑                      |
| notableResidents | TAG_LIST   | 知名居民                      |
| dangerLevel      | SLIDER     | 危险程度 1-10                 |
| prosperity       | SLIDER     | 繁华程度 1-10                 |

### 3.3 村庄
```json
{
  "villageName": "村庄名称",
  "location": "位置",
  "population": "人口",
  "mainIndustry": "主要产业",
  "features": ["特色1"],
  "customs": ["习俗1"],
  "notableNPCs": ["知名NPC1"],
  "relationships": ["与主角关系"],
  "development": "发展状况",
  "dangerLevel": 2,
  "isolation": 5
}
```
| 字段            | 类型         | 说明                      |
|---------------|------------|-------------------------|
| villageName   | TEXT       | 村庄名称                    |
| location      | TEXT       | 位置描述                    |
| population    | TEXT       | 人口                      |
| mainIndustry  | SELECT     | 主要产业：农业/渔业/林业/牧业/手工业/商业 |
| features      | TAG_LIST   | 村庄特色                    |
| customs       | TAG_LIST   | 习俗                      |
| notableNPCs   | TAG_LIST   | 知名NPC                   |
| relationships | TEXT_MULTI | 与主要势力关系                 |
| development   | SELECT     | 发展：落后/普通/发达             |
| dangerLevel   | SLIDER     | 危险程度 1-10               |
| isolation     | SLIDER     | 封闭程度 1-10               |

### 3.4 自然景观
```json
{
  "landscapeName": "景观名称",
  "landscapeType": "景观类型",
  "location": "位置",
  "features": ["特征1", "特征2"],
  "dangerLevel": 7,
  "resources": ["资源1"],
  "legends": ["传说1"],
  "visitors": "访客情况",
  "ecology": "生态环境"
}
```
| 字段            | 类型         | 说明                           |
|---------------|------------|------------------------------|
| landscapeName | TEXT       | 景观名称                         |
| landscapeType | SELECT     | 景观类型：山脉/水域/森林/沙漠/草原/冰川/火山/洞穴 |
| location      | TEXT       | 位置描述                         |
| features      | TAG_LIST   | 景观特征                         |
| dangerLevel   | SLIDER     | 危险程度 1-10                    |
| resources     | TAG_LIST   | 资源                           |
| legends       | TAG_LIST   | 传说典故                         |
| visitors      | SELECT     | 访客情况：无人区/探险地/旅游地/禁区          |
| ecology       | TEXT_MULTI | 生态环境                         |

### 3.5 关键场景
```json
{
  "sceneName": "场景名称",
  "sceneType": "场景类型",
  "location": "位置",
  "atmosphere": "氛围",
  "keyItems": ["关键物品1"],
  "importantEvents": ["重要事件1"],
  "associatedCharacters": ["关联角色1"],
  "function": "剧情功能",
  "accessibility": "可达性"
}
```
| 字段                   | 类型         | 说明                     |
|----------------------|------------|------------------------|
| sceneName            | TEXT       | 场景名称                   |
| sceneType            | SELECT     | 场景类型：室内/室外/私密/公开/战斗/会议 |
| location             | TEXT       | 位置描述                   |
| atmosphere           | TEXT_MULTI | 氛围描述                   |
| keyItems             | TAG_LIST   | 关键物品                   |
| importantEvents      | TAG_LIST   | 重要事件                   |
| associatedCharacters | TAG_LIST   | 关联角色                   |
| function             | TEXT_MULTI | 剧情功能                   |
| accessibility        | SELECT     | 可达性：自由出入/需要许可/隐藏/危险    |

### 3.6 建筑设施
```json
{
  "buildingName": "建筑名称",
  "buildingType": "建筑类型",
  "location": "位置",
  "size": "规模",
  "features": ["特色1"],
  "defense": "防御能力",
  "facilities": ["设施1"],
  "owner": "所有者",
  "staff": ["人员1"],
  "function": "功能",
  "security": 8
}
```
| 字段           | 类型         | 说明                           |
|--------------|------------|------------------------------|
| buildingName | TEXT       | 建筑名称                         |
| buildingType | SELECT     | 建筑类型：宫殿/府邸/商铺/客栈/工坊/塔楼/城墙/祭坛 |
| location     | TEXT       | 位置描述                         |
| size         | SELECT     | 规模：小型/中型/大型/巨型               |
| features     | TAG_LIST   | 建筑特色                         |
| defense      | TEXT_MULTI | 防御能力                         |
| facilities   | TAG_LIST   | 内部设施                         |
| owner        | TEXT       | 所有者                          |
| staff        | TAG_LIST   | 人员                           |
| function     | TEXT_MULTI | 功能                           |
| security     | SLIDER     | 安保程度 1-10                    |

### 3.7 特殊空间
```json
{
  "spaceName": "空间名称",
  "spaceType": "空间类型",
  "entryCondition": "进入条件",
  "size": "空间大小",
  "timeFlow": "时间流速",
  "rules": ["规则1"],
  "dangers": ["危险1"],
  "treasures": ["宝物1"],
  "keyFeatures": ["关键特征1"],
  "origin": "起源"
}
```
| 字段             | 类型         | 说明                         |
|----------------|------------|----------------------------|
| spaceName      | TEXT       | 空间名称                       |
| spaceType      | SELECT     | 空间类型：小世界/秘境/遗迹/阵法空间/异次元/梦境 |
| entryCondition | TEXT_MULTI | 进入条件                       |
| size           | SELECT     | 空间大小：小/中/大/无限              |
| timeFlow       | SELECT     | 时间流速：正常/加速/减缓/静止/混乱        |
| rules          | TAG_LIST   | 特殊规则                       |
| dangers        | TAG_LIST   | 危险                         |
| treasures      | TAG_LIST   | 宝物                         |
| keyFeatures    | TAG_LIST   | 关键特征                       |
| origin         | TEXT_MULTI | 起源                         |

---

## 四、剧情设定

### 4.1 主线剧情
```json
{
  "plotType": "main_storyline",
  "storyArc": "起承转合",
  "keyTurningPoints": [
    {"chapter": 10, "event": "第一次觉醒", "significance": 9}
  ],
  "centralConflict": "核心冲突",
  "resolution": "结局走向",
  "themes": ["主题1", "主题2"],
  "pacing": "节奏",
  "length": "预计篇幅"
}
```
| 字段               | 类型         | 说明                                |
|------------------|------------|-----------------------------------|
| plotType         | SELECT     | 剧情类型：main_storyline:主线/subplot:支线 |
| storyArc         | TEXT_MULTI | 起承转合描述                            |
| keyTurningPoints | TEXT_MULTI | 关键转折点（格式：章节:事件:重要性）               |
| centralConflict  | TEXT_MULTI | 核心冲突                              |
| resolution       | TEXT_MULTI | 结局走向                              |
| themes           | TAG_LIST   | 涉及主题                              |
| pacing           | SELECT     | 节奏：慢热/平稳/紧凑/高潮迭起                  |
| length           | SELECT     | 篇幅：短篇/中篇/长篇/超长篇                   |

### 4.2 支线剧情
```json
{
  "plotType": "subplot",
  "subplotName": "支线名称",
  "relatedMainPlot": "关联主线",
  "purpose": "支线作用",
  "startChapter": 15,
  "endChapter": 30,
  "keyCharacters": ["角色1", "角色2"],
  "resolutionImpact": "对主线影响",
  "status": "进行中"
}
```
| 字段               | 类型         | 说明                 |
|------------------|------------|--------------------|
| plotType         | SELECT     | 剧情类型：subplot:支线    |
| subplotName      | TEXT       | 支线名称               |
| relatedMainPlot  | TEXT       | 关联主线               |
| purpose          | TEXT_MULTI | 支线作用               |
| startChapter     | NUMBER     | 起始章节               |
| endChapter       | NUMBER     | 结束章节               |
| keyCharacters    | TAG_LIST   | 关键角色               |
| resolutionImpact | TEXT_MULTI | 对主线影响              |
| status           | SELECT     | 状态：待开启/进行中/已完成/已放弃 |

### 4.3 关键事件
```json
{
  "eventName": "事件名称",
  "eventType": "battle",
  "chapterNumber": "第25章",
  "participants": ["角色1", "角色2"],
  "outcome": "事件结果",
  "consequences": ["后果1", "后果2"],
  "significance": 9,
  "description": "详细描述"
}
```
| 字段            | 类型         | 说明                                                                                     |
|---------------|------------|----------------------------------------------------------------------------------------|
| eventName     | TEXT       | 事件名称                                                                                   |
| eventType     | SELECT     | 事件类型：battle:战斗/discovery:发现/betrayal:背叛/revelation:揭露/decision:抉择/ceremony:仪式/other:其他 |
| chapterNumber | TEXT       | 发生章节                                                                                   |
| participants  | TAG_LIST   | 参与者                                                                                    |
| outcome       | TEXT_MULTI | 事件结果                                                                                   |
| consequences  | TAG_LIST   | 后续影响                                                                                   |
| significance  | SLIDER     | 重要性 1-10                                                                               |
| description   | TEXT_MULTI | 详细描述                                                                                   |

### 4.4 悬念伏笔
```json
{
  "suspenseName": "悬念名称",
  "suspenseStatus": "pending",
  "suspenseIntensity": 8,
  "setupChapterNumber": "第12章",
  "setupDescription": "埋设描述",
  "resolveChapterNumber": "",
  "resolveDescription": "回收描述",
  "clueList": ["线索1", "线索2"],
  "expectedReveal": "预期揭示方式",
  "readerExpectation": 9
}
```
| 字段                   | 类型         | 说明                                                       |
|----------------------|------------|----------------------------------------------------------|
| suspenseName         | TEXT       | 悬念名称                                                     |
| suspenseStatus       | SELECT     | 状态：pending:待回收/reinforced:已强化/resolved:已回收/abandoned:已放弃 |
| suspenseIntensity    | SLIDER     | 悬念强度 1-10                                                |
| setupChapterNumber   | TEXT       | 埋设章节                                                     |
| setupDescription     | TEXT_MULTI | 埋设描述                                                     |
| resolveChapterNumber | TEXT       | 回收章节（可选）                                                 |
| resolveDescription   | TEXT_MULTI | 回收描述                                                     |
| clueList             | TAG_LIST   | 相关线索                                                     |
| expectedReveal       | TEXT_MULTI | 预期揭示方式                                                   |
| readerExpectation    | SLIDER     | 读者期待度 1-10                                               |

### 4.5 章节规划
```json
{
  "chapterNumber": "第1章",
  "chapterTitle": "章节标题",
  "chapterType": "类型",
  "pov": "视角人物",
  "mainContent": "主要内容",
  "wordCount": 3000,
  "keyPlotPoints": ["要点1"],
  "foreshadowing": ["伏笔1"],
  "mood": "氛围"
}
```
| 字段            | 类型         | 说明                           |
|---------------|------------|------------------------------|
| chapterNumber | TEXT       | 章节序号                         |
| chapterTitle  | TEXT       | 章节标题                         |
| chapterType   | SELECT     | 章节类型：序章/开篇/发展/高潮/转折/过渡/结局/尾声 |
| pov           | TEXT       | 视角人物                         |
| mainContent   | TEXT_MULTI | 主要内容概述                       |
| wordCount     | NUMBER     | 预计字数                         |
| keyPlotPoints | TAG_LIST   | 关键情节点                        |
| foreshadowing | TAG_LIST   | 伏笔                           |
| mood          | TEXT       | 章节氛围                         |

### 4.6 矛盾冲突
```json
{
  "conflictName": "冲突名称",
  "conflictType": "冲突类型",
  "parties": ["冲突方1", "冲突方2"],
  "coreIssue": "核心问题",
  "intensity": 8,
  "history": "冲突历史",
  "currentStatus": "现状",
  "potentialResolution": "可能结局",
  "impactOnPlot": "对剧情影响"
}
```
| 字段                  | 类型         | 说明                     |
|---------------------|------------|------------------------|
| conflictName        | TEXT       | 冲突名称                   |
| conflictType        | SELECT     | 冲突类型：个人/势力/理念/利益/情感/生存 |
| parties             | TAG_LIST   | 冲突方                    |
| coreIssue           | TEXT_MULTI | 核心问题                   |
| intensity           | SLIDER     | 激烈程度 1-10              |
| history             | TEXT_MULTI | 冲突历史                   |
| currentStatus       | TEXT_MULTI | 现状描述                   |
| potentialResolution | TEXT_MULTI | 可能结局                   |
| impactOnPlot        | TEXT_MULTI | 对剧情影响                  |

### 4.7 时间线
```json
{
  "eventName": "事件名称",
  "eventType": "事件类型",
  "absoluteTime": "绝对时间",
  "relativeTime": "相对时间",
  "location": "发生地点",
  "participants": ["参与者"],
  "causes": ["起因"],
  "process": "过程",
  "results": ["结果"],
  "connections": ["关联事件"]
}
```
| 字段           | 类型         | 说明               |
|--------------|------------|------------------|
| eventName    | TEXT       | 事件名称             |
| eventType    | SELECT     | 事件类型：历史/当前/未来/预测 |
| absoluteTime | TEXT       | 绝对时间（纪年）         |
| relativeTime | TEXT       | 相对时间（距今多久）       |
| location     | TEXT       | 发生地点             |
| participants | TAG_LIST   | 参与者              |
| causes       | TAG_LIST   | 起因               |
| process      | TEXT_MULTI | 过程描述             |
| results      | TAG_LIST   | 结果               |
| connections  | TAG_LIST   | 关联事件             |

---

## 五、规则体系

### 5.1 力量体系
```json
{
  "systemName": "体系名称",
  "systemType": "体系类型",
  "progressionPath": ["等级1", "等级2", "等级3"],
  "requirements": ["晋升条件1"],
  "coreAbilities": ["核心能力1"],
  "limitations": "限制条件",
  "sideEffects": "副作用",
  "famousPractitioners": ["知名修炼者1"],
  "cultivationResources": ["修炼资源1"],
  "battleStyle": "战斗风格"
}
```
| 字段                   | 类型         | 说明                     |
|----------------------|------------|------------------------|
| systemName           | TEXT       | 体系名称                   |
| systemType           | SELECT     | 体系类型：修真/魔法/异能/武技/科技/混合 |
| progressionPath      | TAG_LIST   | 晋升路径                   |
| requirements         | TAG_LIST   | 晋升条件                   |
| coreAbilities        | TAG_LIST   | 核心能力                   |
| limitations          | TEXT_MULTI | 限制条件                   |
| sideEffects          | TEXT_MULTI | 副作用                    |
| famousPractitioners  | TAG_LIST   | 知名修炼者                  |
| cultivationResources | TAG_LIST   | 修炼资源                   |
| battleStyle          | TEXT_MULTI | 战斗风格                   |

### 5.2 魔法或超能力
```json
{
  "powerName": "能力名称",
  "powerType": "能力类型",
  "source": "能量来源",
  "castingMethod": "施展方式",
  "effects": ["效果1", "效果2"],
  "limitations": ["限制1"],
  "requirements": ["前置条件1"],
  "sideEffects": ["副作用1"],
  "countermeasures": ["克制方法1"],
  "famousUsers": ["知名使用者1"]
}
```
| 字段              | 类型         | 说明                        |
|-----------------|------------|---------------------------|
| powerName       | TEXT       | 能力名称                      |
| powerType       | SELECT     | 能力类型：元素/精神/物质/时空/召唤/辅助/混合 |
| source          | SELECT     | 能量来源：天赋/修炼/道具/契约/血脉/科技    |
| castingMethod   | TEXT_MULTI | 施展方式                      |
| effects         | TAG_LIST   | 效果                        |
| limitations     | TAG_LIST   | 限制                        |
| requirements    | TAG_LIST   | 前置条件                      |
| sideEffects     | TAG_LIST   | 副作用                       |
| countermeasures | TAG_LIST   | 克制方法                      |
| famousUsers     | TAG_LIST   | 知名使用者                     |

### 5.3 战斗系统
```json
{
  "systemName": "战斗系统名称",
  "battleRules": ["规则1", "规则2"],
  "victoryConditions": ["胜利条件1"],
  "attackTypes": ["攻击类型1"],
  "defenseTypes": ["防御类型1"],
  "supportTypes": ["辅助类型1"],
  "specialMechanics": ["特殊机制1"],
  "balanceFactors": ["平衡因素1"]
}
```
| 字段                | 类型         | 说明   |
|-------------------|------------|------|
| systemName        | TEXT       | 系统名称 |
| battleRules       | TAG_LIST   | 战斗规则 |
| victoryConditions | TAG_LIST   | 胜利条件 |
| attackTypes       | TAG_LIST   | 攻击类型 |
| defenseTypes      | TAG_LIST   | 防御类型 |
| supportTypes      | TAG_LIST   | 辅助类型 |
| specialMechanics  | TAG_LIST   | 特殊机制 |
| balanceFactors    | TEXT_MULTI | 平衡因素 |

### 5.4 经济体系
```json
{
  "currencyName": "货币名称",
  "exchangeRate": "汇率",
  "wealthDistribution": "财富分布",
  "economicCycle": "经济周期",
  "mainIndustries": ["主要产业1"],
  "tradeRoutes": ["贸易路线1"],
  "economicFactors": ["经济因素1"],
  "wealthGap": 7,
  "economicStability": 6
}
```
| 字段                 | 类型         | 说明                  |
|--------------------|------------|---------------------|
| currencyName       | TEXT       | 货币名称                |
| exchangeRate       | TEXT_MULTI | 汇率关系                |
| wealthDistribution | TEXT_MULTI | 财富分布                |
| economicCycle      | SELECT     | 经济周期：稳定/波动/通胀/通缩/危机 |
| mainIndustries     | TAG_LIST   | 主要产业                |
| tradeRoutes        | TAG_LIST   | 重要贸易路线              |
| economicFactors    | TAG_LIST   | 影响经济的因素             |
| wealthGap          | SLIDER     | 贫富差距 1-10           |
| economicStability  | SLIDER     | 经济稳定性 1-10          |

### 5.5 时间规则
```json
{
  "timeSystemName": "时间系统名称",
  "timeFlow": "时间流速",
  "timeMeasurement": "时间计量",
  "timeManipulation": "时间操控",
  "paradoxHandling": "悖论处理",
  "timelineBranches": ["时间线分支1"],
  "keyTemporalEvents": ["关键时间事件1"],
  "rules": ["时间规则1"]
}
```
| 字段                | 类型         | 说明                     |
|-------------------|------------|------------------------|
| timeSystemName    | TEXT       | 时间系统名称                 |
| timeFlow          | SELECT     | 时间流速：正常/加速/减缓/静止/倒流/混乱 |
| timeMeasurement   | TEXT_MULTI | 时间计量方式                 |
| timeManipulation  | TEXT_MULTI | 时间操控方式                 |
| paradoxHandling   | TEXT_MULTI | 悖论处理机制                 |
| timelineBranches  | TAG_LIST   | 时间线分支                  |
| keyTemporalEvents | TAG_LIST   | 关键时间事件                 |
| rules             | TAG_LIST   | 时间规则                   |

### 5.6 限制条件
```json
{
  "ruleName": "规则名称",
  "ruleType": "规则类型",
  "affectedEntities": ["适用对象1"],
  "restriction": "限制内容",
  "punishment": "违规惩罚",
  "exceptions": ["例外情况1"],
  "reason": "设立原因",
  "enforcement": "执行机制"
}
```
| 字段               | 类型         | 说明                  |
|------------------|------------|---------------------|
| ruleName         | TEXT       | 规则名称                |
| ruleType         | SELECT     | 规则类型：物理/魔法/社会/道德/宇宙 |
| affectedEntities | TAG_LIST   | 适用对象                |
| restriction      | TEXT_MULTI | 限制内容                |
| punishment       | TEXT_MULTI | 违规惩罚                |
| exceptions       | TAG_LIST   | 例外情况                |
| reason           | TEXT_MULTI | 设立原因                |
| enforcement      | TEXT_MULTI | 执行机制                |

---

## 六、创作控制

### 6.1 主题内核
```json
{
  "themeName": "主题名称",
  "themeCategory": "主题类别",
  "coreMessage": "核心信息",
  "symbolicElements": ["象征元素1"],
  "philosophicalQuestions": ["哲学问题1"],
  "moralDilemmas": ["道德困境1"],
  "explorationDepth": 8,
  "presentationBalance": 7
}
```
| 字段                     | 类型         | 说明                                                                                                 |
|------------------------|------------|----------------------------------------------------------------------------------------------------|
| themeName              | TEXT       | 主题名称                                                                                               |
| themeCategory          | SELECT     | 主题类别：love:爱情/justice:正义/growth:成长/sacrifice:牺牲/freedom:自由/power:权力/identity:身份/revenge:复仇/other:其他 |
| coreMessage            | TEXT_MULTI | 核心信息                                                                                               |
| symbolicElements       | TAG_LIST   | 象征元素                                                                                               |
| philosophicalQuestions | TAG_LIST   | 哲学问题                                                                                               |
| moralDilemmas          | TAG_LIST   | 道德困境                                                                                               |
| explorationDepth       | SLIDER     | 探索深度 1-10                                                                                          |
| presentationBalance    | SLIDER     | 呈现平衡 1-10                                                                                          |

### 6.2 语言风格
```json
{
  "writingStyle": "写作风格",
  "languageFeatures": ["语言特色1"],
  "sentenceStructure": "句式特点",
  "rhetoricalDevices": ["修辞手法1"],
  "dialogueStyle": "对话风格",
  "narrationType": "叙述类型",
  "referenceWorks": ["参考作品1"],
  "avoidElements": ["避免元素1"]
}
```
| 字段                | 类型         | 说明                           |
|-------------------|------------|------------------------------|
| writingStyle      | SELECT     | 写作风格：简洁/华丽/诗意/写实/古典/现代/幽默/严肃 |
| languageFeatures  | TAG_LIST   | 语言特色                         |
| sentenceStructure | TEXT_MULTI | 句式特点                         |
| rhetoricalDevices | TAG_LIST   | 修辞手法                         |
| dialogueStyle     | TEXT_MULTI | 对话风格                         |
| narrationType     | SELECT     | 叙述类型：第一人称/第三人称全知/第三人称限知/多视角  |
| referenceWorks    | TAG_LIST   | 参考作品                         |
| avoidElements     | TAG_LIST   | 避免的元素                        |

### 6.3 情感基调
```json
{
  "overallTone": "整体基调",
  "emotionalRange": ["情感1", "情感2"],
  "pacing": "节奏",
  "atmosphere": "氛围",
  "emotionalPeaks": ["情感高潮1"],
  "emotionalValleys": ["情感低谷1"],
  "climacticMoments": ["高潮时刻1"],
  "resolutionMood": "结局情绪"
}
```
| 字段               | 类型         | 说明                                                                                          |
|------------------|------------|---------------------------------------------------------------------------------------------|
| overallTone      | SELECT     | 整体基调：serious:严肃/humorous:幽默/dark:黑暗/romantic:浪漫/tragic:悲情/comedic:喜剧/melancholic:忧郁/epic:史诗 |
| emotionalRange   | TAG_LIST   | 情感范围                                                                                        |
| pacing           | SELECT     | 节奏：fast:快/medium:中/slow:慢/variable:起伏                                                       |
| atmosphere       | TEXT_MULTI | 整体氛围                                                                                        |
| emotionalPeaks   | TAG_LIST   | 情感高峰                                                                                        |
| emotionalValleys | TAG_LIST   | 情感低谷                                                                                        |
| climacticMoments | TAG_LIST   | 高潮时刻                                                                                        |
| resolutionMood   | TEXT_MULTI | 结局情绪                                                                                        |

### 6.4 叙事视角
```json
{
  "narrativePerspective": "叙事视角",
  "povCharacter": "视角人物",
  "reliability": "可靠性",
  "omniscience": "全知程度",
  "limitedKnowledge": "已知限制",
  "povAdvantages": "视角优势",
  "povLimitations": "视角局限",
  "multiplePovs": ["多视角人物1"]
}
```
| 字段                   | 类型         | 说明                           |
|----------------------|------------|------------------------------|
| narrativePerspective | SELECT     | 叙事视角：第一人称/第三人称全知/第三人称限知/第二人称 |
| povCharacter         | TEXT       | 视角人物                         |
| reliability          | SELECT     | 可靠性：完全可靠/部分可靠/不可靠            |
| omniscience          | SELECT     | 全知程度：全知/受限全知/限制视角            |
| limitedKnowledge     | TEXT_MULTI | 已知限制                         |
| povAdvantages        | TEXT_MULTI | 视角优势                         |
| povLimitations       | TEXT_MULTI | 视角局限                         |
| multiplePovs         | TAG_LIST   | 多视角人物（如果是多视角叙事）              |

### 6.5 节奏控制
```json
{
  "overallPacing": "整体节奏",
  "chapterLength": "章节长度",
  "sceneLength": "场景长度",
  "dialogueRatio": "对话占比",
  "actionRatio": "动作占比",
  "descriptionRatio": "描写占比",
  "climaxFrequency": "高潮频率",
  "tensionMaintained": "张力维持"
}
```
| 字段                | 类型     | 说明                                             |
|-------------------|--------|------------------------------------------------|
| overallPacing     | SELECT | 整体节奏：slow:慢热/gradual:渐入佳境/steady:平稳推进/fast:快节奏 |
| chapterLength     | SELECT | 章节长度：短/中/长/不定                                  |
| sceneLength       | SELECT | 场景长度：短/中/长                                     |
| dialogueRatio     | SELECT | 对话占比：少/中/多                                     |
| actionRatio       | SELECT | 动作占比：少/中/多                                     |
| descriptionRatio  | SELECT | 描写占比：简洁/适中/详细                                  |
| climaxFrequency   | SELECT | 高潮频率：稀少/适中/频繁                                  |
| tensionMaintained | SLIDER | 张力维持程度 1-10                                    |

---

## 字段类型说明

| 类型              | 说明             | 控件              |
|-----------------|----------------|-----------------|
| TEXT            | 单行文本           | EditText        |
| TEXT_MULTI      | 多行文本           | EditText (多行)   |
| NUMBER          | 数字             | EditText (数字键盘) |
| SLIDER          | 滑块 1-10        | SeekBar         |
| SELECT          | 下拉选择           | Spinner         |
| TAG_LIST        | 标签列表           | ChipGroup       |
| STRUCTURED_LIST | 结构化列表（按固定格式输入） | 多行EditText      |

---
