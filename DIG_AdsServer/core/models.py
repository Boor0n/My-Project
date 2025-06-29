from django.db import models
import uuid # Для генерации уникальных ID устройств

class Device(models.Model):
    device_id = models.UUIDField(default=uuid.uuid4, unique=True, editable=False, db_index=True)
    name = models.CharField(max_length=100, help_text="Название сервисного центра или ТВ")
    location = models.CharField(max_length=255, blank=True, null=True, help_text="Адрес или местоположение")
    is_active = models.BooleanField(default=True, help_text="Активно ли устройство")
    last_seen = models.DateTimeField(auto_now=True, help_text="Время последнего подключения к серверу")

    # Новые поля для мониторинга
    ip_address = models.GenericIPAddressField(blank=True, null=True, help_text="IP-адрес устройства")
    mac_address = models.CharField(max_length=17, blank=True, null=True, unique=False, help_text="MAC-адрес устройства (если доступен)")
    app_version = models.CharField(max_length=20, blank=True, null=True, help_text="Версия приложения Digital Signage на устройстве")
    # Возможно, серийный номер устройства для надежной идентификации
    serial_number = models.CharField(max_length=100, blank=True, null=True, unique=True, help_text="Серийный номер ТВ")

    def __str__(self):
        return self.name

class Content(models.Model):
    CONTENT_TYPES = [
        ('image', 'Изображение'),
        ('video', 'Видео'),
    ]
    name = models.CharField(max_length=255, help_text="Название контента (для админки)")
    content_type = models.CharField(max_length=10, choices=CONTENT_TYPES, help_text="Тип контента")
    file = models.FileField(upload_to='content/', help_text="Загрузите файл изображения или видео")
    duration_seconds = models.PositiveIntegerField(
        blank=True, null=True,
        help_text="Длительность показа для изображений в секундах (для видео не требуется)"
    )
    upload_date = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.name

    def save(self, *args, **kwargs):
        if self.content_type == 'video':
            self.duration_seconds = None # Для видео длительность определяется проигрывателем
        super().save(*args, **kwargs)


class RegistrationKey(models.Model):
    key = models.CharField(max_length=50, unique=True, help_text="Уникальный ключ для регистрации нового устройства")
    is_used = models.BooleanField(default=False, help_text="Был ли этот ключ уже использован")
    device = models.OneToOneField('Device', on_delete=models.SET_NULL, null=True, blank=True,
                                  help_text="Устройство, которое зарегистрировалось с этим ключом")
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.key       