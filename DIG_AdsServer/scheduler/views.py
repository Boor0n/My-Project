# scheduler/views.py
from rest_framework import generics, status
from rest_framework.response import Response
from core.models import Device
from .models import DevicePlaylist, Playlist
from .serializers import PlaylistSerializer
from django.utils import timezone

import logging # <--- ДОБАВЬТЕ ЭТУ СТРОКУ

logger = logging.getLogger(__name__) # <--- ДОБАВЬТЕ ЭТУ СТРОКУ

# Эндпоинт для получения текущего плейлиста для конкретного устройства
class DevicePlaylistView(generics.RetrieveAPIView):
    serializer_class = PlaylistSerializer

    def get_object(self):
        device_id = self.kwargs.get('device_id') # Получаем device_id из URL
        logger.debug(f"Attempting to get playlist for device_id: {device_id}") # <--- ЛОГ
        if not device_id:
            logger.warning("device_id is missing from URL.") # <--- ЛОГ
            return None

        try:
            device = Device.objects.get(device_id=device_id)
            logger.debug(f"Device found: {device.name} ({device.device_id})") # <--- ЛОГ
        except Device.DoesNotExist:
            logger.warning(f"Device with ID {device_id} does not exist.") # <--- ЛОГ
            return None
        except Exception as e:
            logger.error(f"Error fetching device {device_id}: {e}", exc_info=True) # <--- ЛОГ
            return None

        # Находим активный плейлист для устройства
        # Учитываем, что OneToOneField уже автоматически загружает related_name='assigned_playlist'
        device_playlist = None
        try:
            # Убедитесь, что 'assigned_playlist' это related_name в Device
            # Если это OneToOneField в DevicePlaylist, который указывает на Device,
            # то related_name должен быть в DevicePlaylist.
            # Если OneToOneField в Device указывает на DevicePlaylist, то related_name в Device.
            # Предполагаем, что в модели DevicePlaylist есть OneToOneField на Device
            # и related_name на Device это 'assigned_playlist' (что нелогично, обычно наоборот)
            # или Device имеет OneToOneField на DevicePlaylist с related_name 'assigned_playlist'.
            # Давайте уточним: если в DevicePlaylist есть поле `device = models.OneToOneField(Device, on_delete=models.CASCADE, related_name='assigned_playlist')`
            # тогда `device.assigned_playlist` корректно.
            device_playlist = device.assigned_playlist
            logger.debug(f"DevicePlaylist found for device {device.device_id}: {device_playlist.playlist.name if device_playlist else 'None'}") # <--- ЛОГ
        except DevicePlaylist.DoesNotExist: # Это не сработает для RelatedObjectDoesNotExist
             logger.warning(f"No DevicePlaylist assigned to device {device_id}.") # <--- ЛОГ
             return None
        except Exception as e:
            # RelatedObjectDoesNotExist будет пойман здесь
            logger.warning(f"No assigned_playlist found for device {device_id} (Error: {e}).") # <--- ЛОГ
            return None


        # Проверяем, активен ли плейлист и находится ли он в рамках времени
        if device_playlist and device_playlist.is_active:
            now = timezone.now()
            logger.debug(f"DevicePlaylist {device_playlist.pk} is active. Checking time constraints.") # <--- ЛОГ
            if (device_playlist.start_time is None or device_playlist.start_time <= now) and \
               (device_playlist.end_time is None or device_playlist.end_time >= now):
                logger.info(f"Returning active playlist {device_playlist.playlist.name} for device {device_id}.") # <--- ЛОГ
                return device_playlist.playlist # Возвращаем сам объект Playlist
            else:
                logger.info(f"DevicePlaylist {device_playlist.pk} is active but outside time constraints.") # <--- ЛОГ
        else:
            logger.info(f"DevicePlaylist for {device_id} is not active or not found.") # <--- ЛОГ

        return None # Если плейлист не найден, не активен или не в рамках времени

    def retrieve(self, request, *args, **kwargs):
        instance = self.get_object()
        if instance is None:
            logger.info(f"No active playlist found for device {self.kwargs.get('device_id')}. Returning 404.") # <--- ЛОГ
            return Response({"error": "No active playlist found for this device or device not found."},
                            status=status.HTTP_404_NOT_FOUND)
        serializer = self.get_serializer(instance)
        logger.info(f"Successfully serialized playlist {instance.name} for device {self.kwargs.get('device_id')}. Returning 200 OK.") # <--- ЛОГ
        return Response(serializer.data)