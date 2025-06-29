# mysignageproject/urls.py

from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from django.http import HttpResponse

from scheduler.views import DevicePlaylistView # <--- ДОЛЖЕН БЫТЬ ЗДЕСЬ
# from core.views import SOME_CORE_VIEW_FOR_TEST # Если вы используете test_view_func

def test_view_func(request, device_id):
    return HttpResponse(f"Test is successful for device: {device_id}", status=200)

urlpatterns = [
    path('admin/', admin.site.urls),

    # === ПЕРЕМЕЩЕННЫЙ ВВЕРХ ПЛЕЙЛИСТ ДЛЯ ТЕСТА ===
    path('api/v1/playlist/<uuid:device_id>/', DevicePlaylistView.as_view(), name='device_playlist_direct_test'), # <--- ПЕРЕМЕЩЕНО СЮДА
    # ===============================================

    path('api/v1/device/', include('core.urls')),
    # path('api/v1/playlist/', include('scheduler.urls')), # <--- ВСЕ ЕЩЕ ЗАКОММЕНТИРОВАНО/УДАЛЕНО

    # === ВРЕМЕННЫЙ ТЕСТОВЫЙ URL (оставьте его) ===
    path('test-path/<uuid:device_id>/', test_view_func),
    # ===============================================
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)