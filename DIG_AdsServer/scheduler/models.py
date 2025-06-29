from django.db import models
from core.models import Device, Content # Импортируем модели Device и Content из core

class Playlist(models.Model):
    name = models.CharField(max_length=100, unique=True, help_text="Название плейлиста")
    description = models.TextField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.name

class PlaylistItem(models.Model):
    playlist = models.ForeignKey(Playlist, on_delete=models.CASCADE, related_name='items')
    content = models.ForeignKey(Content, on_delete=models.CASCADE)
    order = models.PositiveIntegerField(help_text="Порядок показа контента в плейлисте (начиная с 0)")

    class Meta:
        ordering = ['order'] # Сортировка элементов по порядку
        unique_together = ('playlist', 'order') # Гарантирует уникальность порядка в каждом плейлисте

    def __str__(self):
        return f"{self.playlist.name} - {self.content.name} (Порядок: {self.order})"

class DevicePlaylist(models.Model):
    # OneToOneField гарантирует, что у одного Device может быть только один активный Playlist
    device = models.OneToOneField(Device, on_delete=models.CASCADE, unique=True, related_name='assigned_playlist')
    playlist = models.ForeignKey(Playlist, on_delete=models.CASCADE)
    start_time = models.DateTimeField(blank=True, null=True, help_text="Время начала действия плейлиста (опционально)")
    end_time = models.DateTimeField(blank=True, null=True, help_text="Время окончания действия плейлиста (опционально)")
    is_active = models.BooleanField(default=True, help_text="Активно ли это назначение плейлиста")

    def __str__(self):
        return f"{self.device.name} - {self.playlist.name}"