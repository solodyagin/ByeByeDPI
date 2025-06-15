# ByeByeDPI Android 
Русский | [English](README-en.md) | [Türkçe](README-tr.md)

<div style="text-align: center;">
  <img alt="Логотип ByeDPI" src="https://github.com/romanvht/ByeDPIAndroid/raw/master/.github/images/app.svg" width="100%" height="200px">
</div>

---

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

### Зависимости
- [ByeDPI](https://github.com/hufrea/byedpi)
- [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)
