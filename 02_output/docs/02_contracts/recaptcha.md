# reCAPTCHA contract

> Written in: phase 2 · Source: `docs/02_specification.md` 6.3, SR-01, SR-02, AR-07 · Agent: writes

## Widget (frontend)

When `GET /api/form-config` returns `captchaTestMode: false`, the frontend loads `https://www.google.com/recaptcha/api.js?render=explicit`, renders a reCAPTCHA v2 checkbox with `captchaSiteKey` and sends `grecaptcha.getResponse()` as `captchaToken`; after a rejected submission the widget is reset. With `captchaTestMode: true` it shows "reCAPTCHA test mode" and sends `test-pass`. The site key is the frontend's only key (AR-07).

## Verify call (backend)

| Item | Value |
|---|---|
| Request | `POST <RECAPTCHA_VERIFY_URL>`, `application/x-www-form-urlencoded`, fields `secret=<RECAPTCHA_SECRET_KEY>`, `response=<captchaToken>` |
| Default URL | `https://www.google.com/recaptcha/api/siteverify` |
| Accepted | HTTP 200 and JSON body with `"success": true` |
| Rejected | `"success": false`, any other status, unparsable body, timeout (5 s connect, 5 s read) or connection error |
| Test mode | `CAPTCHA_TEST_MODE=true`: no call; `test-pass` accepted, every other token rejected |
| Production guard | `APP_ENVIRONMENT=production` refuses to start with test mode on, or with an empty site or secret key (SR-02) |

The production path is tested against a mocked verification endpoint returning success and failure (DoD-P05).
