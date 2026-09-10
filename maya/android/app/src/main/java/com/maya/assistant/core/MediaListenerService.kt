package com.maya.assistant.core

import android.service.notification.NotificationListenerService

/**
 * Android requires an app to be a registered "Notification Listener" before
 * MediaSessionManager.getActiveSessions() will return anything - this is the
 * same permission Bluetooth-headset and smartwatch apps use purely for
 * media-button control. We deliberately do NOT override onNotificationPosted
 * or read notification text here; this class exists only to unlock the
 * media-session API, not to read your messages.
 *
 * The user must explicitly enable this once at:
 * Settings > Apps > Special app access > Notification access > Maya
 */
class MediaListenerService : NotificationListenerService()
