# Recommended Repository Structure

```text
.
├─ 00_개발과제.md
├─ README.md
├─ docs/
│  ├─ product-context.md
│  ├─ domain-context.md
│  ├─ architecture-context.md
│  ├─ ai-context.md
│  ├─ implementation-backlog.md
│  └─ api-spec.md
├─ backend/
│  ├─ pom.xml
│  └─ src/
│     ├─ main/
│     │  ├─ java/com/taxworkbench/
│     │  │  ├─ domain/
│     │  │  ├─ application/
│     │  │  ├─ infrastructure/
│     │  │  └─ interfaces/
│     │  └─ resources/
│     └─ test/
├─ frontend/
│  ├─ package.json
│  └─ src/
│     ├─ app/
│     ├─ pages/
│     ├─ features/
│     ├─ components/
│     ├─ lib/
│     └─ styles/
└─ .gitignore
```

## Notes

- Keep the package boundary visible in both backend and frontend
- Avoid a monolithic `service` or `hooks` directory where unrelated rules accumulate
- Add `docs/api-spec.md` once endpoint contracts are fixed
- If the previous source tree is restored later, align it to this shape rather than mixing multiple patterns
