# EF-62 전용 버튼 조사 결과

검증 기기는 Epson EF-62 (`HB73`), Android 14 / API 34입니다.

## Live TV 버튼

Google TV 런처가 다음 형식의 링크를 실행합니다.

```text
android.intent.action.VIEW
livetvx://cltv.dev/...
```

기본 처리 앱은 `com.cltv.fast/com.iwedia.cltv.MainActivity`입니다. 기본 앱을 사용자 영역에서 비활성화하면 볼륨 컨트롤러의 `LiveTvRedirectActivity`가 링크를 받아 설정 화면에서 선택한 쿠팡플레이(`com.coupang.mobile.play`) 또는 디즈니+(`com.disney.disneyplus`)를 엽니다.

```powershell
adb shell pm disable-user --user 0 com.cltv.fast
```

원래 Live TV 앱을 복구합니다.

```powershell
adb shell pm enable com.cltv.fast
```

## Prime Video 버튼

Google TV LauncherX가 다음 명시적 대상을 실행합니다.

```text
Action: com.amazon.amazonvideo.livingroom.AMAZON_BUTTON
Package: com.amazon.amazonvideo.livingroom
Component: com.amazon.ignition.IgnitionActivity
```

따라서 다른 패키지의 일반 Accessibility/버튼 매퍼 앱으로는 대체하기 어렵습니다. 이 저장소의 중계 앱은 원본 Prime Video를 제거한 뒤 같은 패키지와 Activity 이름을 차지하는 방식으로 동작합니다.

## 검증된 대상 앱

| 앱 | 패키지 | TV 시작 Activity |
| --- | --- | --- |
| YouTube | `com.google.android.youtube.tv` | 기기별 런처 Activity |
| 쿠팡플레이 | `com.coupang.mobile.play` | `com.coupang.play.features.provision.ProvisioningActivity` |
| 디즈니+ | `com.disney.disneyplus` | `com.bamtechmedia.dominguez.main.MainActivity` |
| TVING | `net.cj.em.tving` | `.ui.main.MainActivity` |

## 펌웨어 의존성

전용 버튼 처리는 Google TV 런처와 제조사 시스템 오버레이에 의존합니다. 다른 EF-62 펌웨어나 다른 프로젝터 모델에서는 패키지, 링크 또는 Activity 이름이 다를 수 있습니다.
