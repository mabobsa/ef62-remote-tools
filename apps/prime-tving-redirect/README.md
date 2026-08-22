# EF-62 Prime 버튼 → TVING

EF-62 Google TV 리모컨의 Prime Video 전용 버튼을 누르면 TVING을 실행하는 중계 앱입니다.

이 앱은 Prime Video와 동일한 패키지명(`com.amazon.amazonvideo.livingroom`)을 사용하므로 원본 Prime Video를 먼저 제거해야 설치할 수 있습니다.

## 빌드

JDK 17, Android SDK 35, Gradle 8.9 환경에서 실행합니다.

```powershell
gradle --no-daemon assembleRelease
```

## 설치

```powershell
adb uninstall com.amazon.amazonvideo.livingroom
adb install .\EF62-Prime-Button-TVING-v1.0.0.apk
```

## Prime Video 복구

```powershell
adb uninstall com.amazon.amazonvideo.livingroom
```

그 다음 Google Play 스토어에서 Prime Video를 다시 설치합니다. 백업한 원본 split APK가 있다면 `adb install-multiple`로도 복원할 수 있습니다.

```powershell
adb install-multiple .\base.apk .\split_config.armeabi_v7a.apk .\split_config.ko.apk .\split_config.xhdpi.apk
```

중계 앱은 Prime Video와 패키지명이 같지만 서명이 다르므로, Google Play 스토어의 Prime Video 자동 업데이트는 실패할 수 있습니다. Prime Video 항목의 자동 업데이트를 꺼 두는 것을 권장합니다.
