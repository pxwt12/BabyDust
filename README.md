# BabyDust / 接好孕

Multi-channel pregnancy, delivery, postpartum and baby-care record system.

## Structure

- `apps/miniprogram` - uni-app + Vue3 + TypeScript mini-program client.
- `apps/admin` - Vue3 operations console.
- `services/api` - Spring Boot Java multi-channel API.
- `deploy` - Docker Compose and Nginx deployment files.
- `docs` - product and API design documents.

## First Run

Backend:

```powershell
cd services/api
mvn test
mvn spring-boot:run
```

Frontend commands should use `npm.cmd` on Windows PowerShell if script execution blocks `npm.ps1`.

```powershell
cd apps/miniprogram
npm.cmd install
npm.cmd run dev:mp-weixin
```

Admin:

```powershell
cd apps/admin
npm.cmd install
npm.cmd run dev
```

Docker deployment:

```powershell
cd deploy
docker compose up -d --build
```
