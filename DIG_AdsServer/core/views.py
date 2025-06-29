from rest_framework import generics, status
from rest_framework.response import Response
from rest_framework.views import APIView
from .models import Device
from .serializers import DeviceSerializer
from django.db import models

# Эндпоинт для регистрации нового устройства
class DeviceRegisterView(APIView):
    def post(self, request, *args, **kwargs):
        # ТВ при первом запуске отправляет свой уникальный device_id
        device_id_str = request.data.get('device_id')
        name = request.data.get('name', f"Новое устройство {device_id_str[:8]}") # Имя по умолчанию

        if not device_id_str:
            return Response({"error": "Device ID is required"}, status=status.HTTP_400_BAD_REQUEST)

        try:
            # Пытаемся получить существующее устройство
            device = Device.objects.get(device_id=device_id_str)
            # Обновляем last_seen, если устройство уже есть
            device.name = name # Можно обновить имя, если изменилось
            device.last_seen = models.DateTimeField(auto_now=True) # Обновляем вручную, т.к. auto_now=True работает только при save
            device.save()
            serializer = DeviceSerializer(device)
            return Response(serializer.data, status=status.HTTP_200_OK)
        except Device.DoesNotExist:
            # Если устройства нет, создаем новое
            device = Device.objects.create(device_id=device_id_str, name=name)
            serializer = DeviceSerializer(device)
            return Response(serializer.data, status=status.HTTP_201_CREATED)

# Эндпоинт для обновления статуса устройства (heartbeat)
class DeviceStatusView(APIView):
    def post(self, request, *args, **kwargs):
        device_id_str = request.data.get('device_id')
        if not device_id_str:
            return Response({"error": "Device ID is required"}, status=status.HTTP_400_BAD_REQUEST)
        try:
            device = Device.objects.get(device_id=device_id_str)
            device.last_seen = models.DateTimeField(auto_now=True) # Обновляем вручную
            device.save()
            serializer = DeviceSerializer(device)
            return Response(serializer.data, status=status.HTTP_200_OK)
        except Device.DoesNotExist:
            return Response({"error": "Device not found"}, status=status.HTTP_404_NOT_FOUND)