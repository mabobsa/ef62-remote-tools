# EF-62 볼륨 자동 설정

Epson Lifestudio Pop Plus EF-62의 Google TV에서 동작하는 Android TV 앱입니다.

- 프로젝터 부팅 시 백그라운드 서비스 자동 시작
- 상시 볼륨과 YouTube 볼륨을 각각 0~100% 범위에서 설정
- 앱 화면에서 설정 항목을 선택하고 리모컨 좌·우키로 1%씩 변경
- YouTube(`com.google.android.youtube.tv`, `com.google.android.youtube`)가 전면에 있으면 YouTube 볼륨 적용
- 그 외 앱에서는 상시 볼륨 적용
- 1초마다 현재 상태를 확인하므로 리모컨으로 볼륨을 바꿔도 지정 값으로 복원
- 검증된 Wi-Fi 연결이 확인되기 전에는 볼륨을 변경하지 않음
- Wi-Fi 연결이 끊기면 자동 조절을 일시 중지하고 재연결 시 자동 재개
- EF-62 리모컨의 Live TV 버튼을 누르면 쿠팡플레이 실행

## 최초 설정

1. APK를 설치하고 앱을 한 번 실행합니다.
2. **사용 정보 접근 권한 열기**를 선택합니다.
3. 목록에서 **EF-62 볼륨 자동 설정**을 찾아 허용합니다.
4. 앱으로 돌아와 **자동 조절 시작 / 다시 시작**을 누릅니다.
5. 상시 볼륨과 YouTube 볼륨 막대를 선택하고 리모컨 좌·우키로 값을 조절합니다.
6. **YouTube 열어서 테스트**로 설정값 전환을 확인합니다.

설정값은 앱 업데이트와 재부팅 후에도 유지됩니다. 사용 정보 접근 권한이 없으면 YouTube 여부를 알 수 없으므로 상시 볼륨을 적용합니다.

## ADB 설치

Google TV에서 개발자 옵션과 USB/네트워크 디버깅을 켠 뒤 PC에서 실행합니다.

```powershell
adb connect <EF-62의-IP주소>:5555
adb install -r .\EF62-Volume-Auto-v1.3.0.apk
```

Live TV 버튼 리디렉션을 사용하려면 기기 기본 Live TV 앱을 사용자 영역에서 비활성화합니다.

```powershell
adb shell pm disable-user --user 0 com.cltv.fast
```

원래 동작으로 복원할 때는 다음 명령을 사용합니다.

```powershell
adb shell pm enable com.cltv.fast
```

사용 정보 접근 메뉴가 기기 UI에서 열리지 않을 때는 ADB로 허용할 수 있습니다.

```powershell
adb shell appops set kr.yongmin.ef62volume GET_USAGE_STATS allow
```

부팅 자동 시작 시험(프로젝터가 재부팅됩니다):

```powershell
adb reboot
# 재부팅과 무선 디버깅 재연결 후
adb shell dumpsys activity services kr.yongmin.ef62volume
```

`BOOT_COMPLETED`는 보호된 시스템 방송이므로 일반 ADB 셸에서 임의로 전송해 시험할 수 없습니다.

## 빌드

JDK 17과 Android SDK 35가 있는 환경에서:

```powershell
gradle --no-daemon assembleRelease
```

릴리스 출력은 `app/build/outputs/apk/release/app-release-unsigned.apk`입니다.

## 참고

일부 Google TV 펌웨어는 타사 앱의 부팅 후 실행을 제조사 설정에서 별도로 허용해야 할 수 있습니다. 앱 화면의 서비스 상태가 재부팅 뒤에도 `실행 중`인지 확인하세요.
