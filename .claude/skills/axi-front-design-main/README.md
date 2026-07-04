# axi-front-design

**A Claude Code skill that turns HTML into high-fidelity design artifacts.**

中文 | [English](#english)

---

## 简介

`axi-front-design` 是一个 Claude Code Skill，让 Claude 扮演**资深设计师**而非通用前端工程师——用 HTML
产出落地页、幻灯片、交互原型、动画、信息图、移动端 Mockup 等高保真设计稿。

核心差异：**先问后做、引用真实设计上下文、给变体而非唯一解、严格规避 AI 俗套。**

---

## 能做什么

| 输出类型             | 触发示例                            |
|------------------|---------------------------------|
| 落地页 / 营销页        | `做个落地页`、`design a landing page` |
| 幻灯片 / Pitch Deck | `做张幻灯片`、`make a deck`           |
| 交互原型             | `做个交互原型`、`prototype this UI`    |
| 宣传动画             | `做个产品动效`                        |
| 信息图 / 海报         | `做张信息图`、`design a poster`       |
| 设计系统 / UI Kit    | `design system`、`hi-fi mockup`  |

---

## 安装

将 `SKILL.md` 放到你的 Claude Code skills 目录：

```
skills/
└── axi-front-design/
    └── SKILL.md
```

在 `.claude/settings.json` 中注册：

```json
{
  "skills": [
    {
      "name": "axi-front-design",
      "path": "skills/axi-front-design/SKILL.md"
    }
  ]
}
```

---

## 使用

```
/axi-front-design 帮我做一个 SaaS 产品落地页
```

或者直接描述需求，满足以下任一触发词时会自动激活：

> `做个落地页` / `设计海报` / `做张幻灯片` / `做个原型` /  
> `design a landing page` / `make a deck` / `prototype this UI`

---

## 工作流

```
提问 → 找设计上下文 → 确立设计系统 → 早交付草稿 → 迭代变体 → 验收
```

1. **先问问题** — 用弹窗收集受众、保真度、变体维度、约束
2. **读设计上下文** — 从 codebase 抠 tokens（颜色、字体、间距），不靠记忆
3. **立系统** — 先在文件头写下颜色/字体/间距决策，再写组件
4. **早给看** — 骨架完成就让用户打开浏览器，不憋大招
5. **给变体** — 默认 3+ 个方向，让用户在原子级别拼配

---

## 设计原则（核心约束）

**反 AI 俗套，严格执行：**

- 不用滥用渐变背景（紫粉蓝那套）
- 不用没必要的 emoji
- 不用 Inter / Roboto / Arial 等过度使用的字体
- 不用 SVG 手画插画（用占位符 + 真实素材）
- 不堆没意义的数字、icon、stats
- 不加废话 section，空白是设计意图

---

## 不适用场景

- 纯文字内容 / 逻辑代码（用常规对话）
- 需要真实图像生成（应改用图像生成模型）
- 需要原生 PPTX / Figma 文件（HTML 优先，不导出其他格式）

---

## License

MIT

---

<a name="english"></a>

## English

`axi-front-design` is a Claude Code Skill that makes Claude act as a **senior designer**, not a generic frontend
engineer. It produces high-fidelity HTML artifacts: landing pages, slide decks, interactive prototypes, motion demos,
infographics, and mobile mockups.

**Key behaviors:**

- Asks questions before building (via popup, not lists)
- Reads real design tokens from your codebase instead of guessing
- Delivers 3+ variants across different visual/interaction dimensions
- Avoids AI design clichés (gradients, emoji, Lorem Ipsum filler sections)

### Install

Drop `SKILL.md` into your `skills/axi-front-design/` folder and register it in `.claude/settings.json`.

### Invoke

```
/axi-front-design make a SaaS landing page
```

### License

MIT
