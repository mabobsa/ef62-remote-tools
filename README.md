# EF-62 Remote Tools

Epson Lifestudio Pop Plus EF-62의 Google TV 환경을 위한 Android TV 도구 모음입니다.

## 포함된 앱

| 앱 | 기능 | 패키지 |
| --- | --- | --- |
| `volume-controller` | 부팅 후 자동 실행, 일반/YouTube 볼륨 자동 적용, Live TV 버튼으로 쿠팡플레이 실행 | `kr.yongmin.ef62volume` |
| `prime-tving-redirect` | Prime Video 전용 버튼으로 TVING 실행 | `com.amazon.amazonvideo.livingroom` |

### 볼륨 컨트롤러

- 일반 앱과 YouTube 볼륨을 각각 설정
- 리모컨 좌·우키로 1% 단위 조절
- 1초마다 지정 볼륨 복원
- 부팅 및 앱 업데이트 후 포그라운드 서비스 자동 시작
- `livetvx://cltv.dev/...` 링크를 받아 쿠팡플레이 실행

### Prime 버튼 리디렉터

EF-62의 Google TV 런처는 Prime 버튼을 누르면 다음 대상을 직접 실행합니다.

```text
Action: com.amazon.amazonvideo.livingroom.AMAZON_BUTTON
Component: com.amazon.amazonvideo.livingroom/com.amazon.ignition.IgnitionActivity
```

리디렉터는 동일한 패키지와 Activity 이름으로 이 신호를 받은 뒤 TVING을 실행합니다.

## 요구 사항

- Android Studio 또는 JDK 17
- Android SDK 35
- Gradle 8.9
- 무선 또는 USB ADB 연결

## 빌드

각 앱 디렉터리에서 실행합니다.

```powershell
gradle --no-daemon assembleRelease
```

또는 저장소 루트에서 두 앱을 한 번에 빌드합니다.

```powershell
.\build-all.ps1
```

생성되는 APK는 서명되지 않은 릴리스 APK입니다. 기기에 업데이트 설치하려면 본인이 안전하게 보관하는 동일한 키로 계속 서명해야 합니다.

## EF-62 설치 설정

자세한 설치 및 복구 명령은 각 앱 문서를 참고하세요.

- [볼륨 컨트롤러와 Live TV → 쿠팡플레이](apps/volume-controller/README.md)
- [Prime Video 버튼 → TVING](apps/prime-tving-redirect/README.md)
- [기기 조사 결과](docs/DEVICE_FINDINGS.md)

## 주의 사항

- Prime 리디렉터는 Prime Video와 같은 패키지명을 사용하므로 원본 Prime Video를 먼저 제거해야 합니다.
- Prime Video 원본 APK, 서명 키, 비밀번호, 로컬 Android SDK/Gradle/JDK는 이 저장소에 포함하지 않습니다.
- Prime 리디렉터 설치 중에는 Play 스토어의 Prime Video 자동 업데이트를 꺼 두는 것을 권장합니다.
- 기기 펌웨어 업데이트로 Google TV 런처의 버튼 처리 방식이 바뀌면 리디렉션이 동작하지 않을 수 있습니다.

## 검증 기기

- Epson Lifestudio Pop Plus EF-62
- 모델 식별자: `HB73`
- Android 14 / API 34
