# 团队协作规范

## Git 分支策略

- `main` — 生产环境分支
- `develop` — 开发主分支
- `feature/*` — 功能分支
- `hotfix/*` — 紧急修复分支

### 提交规范

```bash
git commit -m "feat: 添加用户登录功能"
git commit -m "fix: 修复订单金额计算错误"
git commit -m "docs: 更新 API 文档"
```

## 代码评审清单

- [ ] 代码符合团队风格指南
- [ ] 单元测试已通过
- [ ] 无安全漏洞
- [ ] 性能无明显退化
- [ ] 文档已同步更新
