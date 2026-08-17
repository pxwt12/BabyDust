# i18n Guidelines

Locales for v1:

- `zh-CN`
- `zh-TW`
- `en-US`

Rules:

1. Do not hardcode display strings in page components.
2. Use short labels for tab bars and fixed-format controls.
3. Design buttons for English labels first, then verify Chinese layouts.
4. Use locale-aware formatters for dates, numbers, pregnancy weeks and baby ages.
5. Medical content requires source tracking per locale; do not rely only on machine translation for public-release content.
