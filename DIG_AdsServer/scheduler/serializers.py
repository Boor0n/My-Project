from rest_framework import serializers
from .models import Playlist, PlaylistItem, DevicePlaylist
from core.serializers import ContentSerializer # Импортируем сериализатор контента

class PlaylistItemSerializer(serializers.ModelSerializer):
    content = ContentSerializer(read_only=True) # Включаем данные контента в элемент плейлиста

    class Meta:
        model = PlaylistItem
        fields = ['id', 'content', 'order']

class PlaylistSerializer(serializers.ModelSerializer):
    items = PlaylistItemSerializer(many=True, read_only=True) # Список элементов плейлиста

    class Meta:
        model = Playlist
        fields = ['id', 'name', 'description', 'items']

class DevicePlaylistSerializer(serializers.ModelSerializer):
    playlist = PlaylistSerializer(read_only=True) # Включаем данные плейлиста
    device = serializers.SlugRelatedField(slug_field='device_id', read_only=True) # Только ID устройства

    class Meta:
        model = DevicePlaylist
        fields = ['device', 'playlist', 'start_time', 'end_time', 'is_active']