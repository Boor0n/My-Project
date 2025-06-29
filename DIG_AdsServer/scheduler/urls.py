#scheduler\urls.py

from django.urls import path
from .views import DevicePlaylistView

urlpatterns = [
    path('<uuid:device_id>/', DevicePlaylistView.as_view(), name='device_playlist'),
    # <uuid:device_id> означает, что Django будет ожидать UUID в этом месте URL
]
#
