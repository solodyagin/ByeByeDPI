<div align="center">
  <p>
    <img src="https://github.com/romanvht/ByeDPIAndroid/raw/master/.github/images/app.svg" alt="Логотип ByeDPI" width="200" />
  </p>
  <h1>ByeByeDPI Android</h1>
  <p>
    Русский |
    <a href="README-en.md">English</a> |
    <a href="README-tr.md">Türkçe</a>
  </p>
  <p>
    <a href="https://github.com/romanvht/ByeByeDPI/releases/latest"><img src="https://img.shields.io/github/v/release/romanvht/ByeByeDPI" alt="Latest Release" /></a>
    <a href="https://github.com/romanvht/ByeByeDPI/releases"><img src="https://img.shields.io/github/downloads/romanvht/ByeByeDPI/total" alt="Downloads" /></a>
    <a href="https://github.com/romanvht/ByeByeDPI/blob/master/LICENSE"><img src="https://img.shields.io/github/license/romanvht/ByeByeDPI" alt="License" /></a>
  </p>
</div>

Приложение для Android, которое локально запускает ByeDPI и перенаправляет весь трафик через него.

Для стабильной работы может потребоваться изменить настройки. Подробнее о различных настройках можно прочитать в [документации ByeDPI](https://github.com/hufrea/byedpi/blob/v0.13/README.md).

Приложение не является VPN. Оно использует VPN-режим на Android для перенаправления трафика, но не передает ничего на удаленный сервер. Оно не шифрует трафик и не скрывает ваш IP-адрес.

Приложения является форком [ByeDPIAndroid](https://github.com/dovecoteescapee/ByeDPIAndroid)

---

### Возможности
* Автозапуск сервиса при старте устройства
* Сохранение списков параметров командной строки
* Улучшена совместимость с Android TV/BOX
* Раздельное туннелирование приложений
* Импорт/экспорт настроек

### Использование
* Для работы автозапуска активируйте пункт в настройках.
* Рекомендуется подключится один раз к VPN, чтобы принять запрос.
* После этого, при загрузке устройства, приложение автоматически запустит сервис в зависимости от настроек (VPN/Proxy)
* Если у вас Android TV/BOX, и при подключении пропадает соединение по Ethernet, активируйте режим белого списка и укажите нужные приложения, которые должны работать через VPN (например, YouTube)
* Комплексная инструкция от комьюнити [ByeByeDPI-Manual](https://github.com/HideakiTaiki/ByeByeDPI-Manual)

### Как использовать ByeByeDPI вместе с AdGuard?
* Запустите ByeByeDPI в режиме прокси.
* Добавьте ByeByeDPI в исключения AdGuard на вкладке "Управление приложениями".
* В настройках AdGuard укажите прокси:
```plaintext
Тип прокси: SOCKS5
Хост: 127.0.0.1
Порт: 1080 (по умолчанию)
```

### Сборка
1. Клонируйте репозиторий с сабмодулями:
```bash
git clone --recurse-submodules
```
2. Запустите скрипт сборки из корня репозитория:
```bash
./gradlew assembleRelease
```
3. APK будет в `app/build/outputs/apk/release/`

> P.S.: hev_socks5_tunnel не соберется под Windows, вам нужно будет использовать WSL

### Зависимости
- [ByeDPI](https://github.com/hufrea/byedpi)
- [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)
