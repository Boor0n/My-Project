from django.contrib import admin
from .models import Playlist, PlaylistItem, DevicePlaylist

# Это позволит добавлять элементы плейлиста прямо в форме создания плейлиста
class PlaylistItemInline(admin.TabularInline):
    model = PlaylistItem
    extra = 1 # Количество пустых строк для добавления новых элементов

# Регистрируем Playlist с возможностью добавлять PlaylistItem'ы
@admin.register(Playlist)
class PlaylistAdmin(admin.ModelAdmin):
    inlines = [PlaylistItemInline]

admin.site.register(DevicePlaylist)