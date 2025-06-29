from django.urls import path
from .views import DeviceRegisterView, DeviceStatusView

urlpatterns = [
    path('register/', DeviceRegisterView.as_view(), name='device_register'),
    path('status/', DeviceStatusView.as_view(), name='device_status'),
]