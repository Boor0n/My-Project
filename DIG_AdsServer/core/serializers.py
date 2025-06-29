from rest_framework import serializers
from .models import Device, Content

class DeviceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Device
        fields = ['device_id', 'name', 'location', 'is_active', 'last_seen',
                  'ip_address', 'mac_address', 'app_version', 'serial_number'] # Добавляем новые поля
        read_only_fields = ['device_id', 'last_seen'] # ТВ не изменяет эти поля

class ContentSerializer(serializers.ModelSerializer):
    file_url = serializers.SerializerMethodField() # Это поле будет отдавать полный URL к файлу

    class Meta:
        model = Content
        fields = ['id', 'name', 'content_type', 'file_url', 'duration_seconds']

    def get_file_url(self, obj):
        # Возвращаем полный URL к файлу контента
        request = self.context.get('request')
        if request is not None:
            return request.build_absolute_uri(obj.file.url)
        return obj.file.url # Если запроса нет (например, из консоли), просто URL
    
class DeviceRegisterSerializer(serializers.Serializer):
    registration_key = serializers.CharField(max_length=50)
    # Эти поля будут отправляться ТВ при регистрации/обновлении
    device_id = serializers.UUIDField(required=False) # Может быть пустой при первой регистрации
    name = serializers.CharField(max_length=100, required=False)
    ip_address = serializers.IPAddressField(required=False)
    mac_address = serializers.CharField(max_length=17, required=False)
    app_version = serializers.CharField(max_length=20, required=False)
    serial_number = serializers.CharField(max_length=100, required=False)