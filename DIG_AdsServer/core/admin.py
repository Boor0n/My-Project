# core/admin.py
from django.contrib import admin
from .models import Device, Content, RegistrationKey

admin.site.register(Device)
admin.site.register(Content)
admin.site.register(RegistrationKey)