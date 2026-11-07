# 개발 및 검증

## 빌드

- JDK 17, Android SDK 32 / Build Tools 32.0.0을 사용합니다. Gradle Wrapper는 7.4입니다.
- `ANDROID_HOME` 또는 로컬 `local.properties`의 `sdk.dir`로 SDK 경로를 설정합니다.
- 별도 Firebase 프로젝트로 실행하려면 `app/google-services.json`을 해당 프로젝트의 설정으로 교체하고 Google 로그인 제공자를 구성해야 합니다.
- 디버그 빌드는 기본 Android 디버그 키를 사용합니다. 기존 설치본과 서명이 다르면 업데이트 설치가 되지 않습니다.
- 릴리스 서명은 `signing.properties.example`을 복사한 로컬 `signing.properties`로 지정합니다. 키와 실제 비밀번호는 Git 제외 대상입니다.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-configuration-cache
```

## 수정한 내용

- **거래 일관성**: 같은 계정의 잔액·회원권·출석 적립·거래 내역·입금 소비를 하나의 Realtime Database 트랜잭션으로 변경했습니다. 관리자 잔액 수동 수정은 화면에서 본 잔액과 서버 잔액이 다르면 거절합니다.
- **중복 방지**: 거래 ID를 `processedOperations`에 함께 기록합니다. 같은 요청을 재실행해도 금액을 다시 반영하지 않습니다. 저장 성공은 Firebase 완료 콜백으로 확인합니다.
- **알림 재처리**: 파싱한 입금을 계정별 로컬 대기 기록에 저장하고 실패 시 재시도합니다. 회원 매칭이 없거나 중복 별칭으로 모호하면 미확인 입금으로 남깁니다.
- **업무 규칙 통합**: 일반·관리자 결제에서 같은 회원권 계산을 사용합니다. 운영진 적립은 날짜·연월·횟수를 확인합니다. 기존 음수 정산 가격과 음수 잔액 허용은 유지합니다.
- **화면 안정성**: 첫 상품 이동, 추가 버튼 이동, Firebase 상품 목록의 Map/List 변환, 빈 가격 입력, 이전 사용자가 남아 잘못 결제되는 문제를 수정했습니다.
- **관리자 진입 제한**: 내부 Activity의 외부 공개를 끄고 PIN 인증 세션을 확인합니다. 프로세스 재시작 시 PIN 로그인이 필요합니다.
- **기록 표시**: 월을 포함한 식별자로 거래 내역을 합치고, 변경된 월의 목록을 교체합니다. 동일 밀리초 거래와 로그인 화면 목록 갱신도 처리합니다.

## 데이터 호환성과 제약

기존 `users`, `deposits`, `history/YYYYMM`, `items` 경로는 유지합니다. 새 거래·미확인 입금 키는 거래 ID를 사용하며 기존 날짜 키도 읽습니다. 회원권은 14자리 만료 시각으로 저장하고, 기존 6자리 월·8자리 날짜도 읽습니다. 8자리는 해당 날짜 마지막까지 유효한 것으로 해석합니다. 과거 관리자 결제로 잘못 저장된 날짜의 원래 의도까지 자동 복구하지는 않습니다.

기존 형제를 함께 갱신하기 위해 **계정 루트에서 트랜잭션**을 실행합니다. 해당 경로에 대한 읽기·쓰기 권한이 필요하며, 운영 Rules는 별도 확인이 필요합니다. 데이터 규모가 커지면 거래 전용 구조나 서버 처리로 분리할 수 있습니다. `processedOperations`는 중복 방지에 사용하므로 임의로 삭제하지 않습니다.

알림 ID는 패키지·알림 키·이벤트 시각·본문으로 계산합니다. 동일 이벤트의 재전달은 차단하지만, 금융기관의 거래번호가 없으므로 원본 앱이 다른 메타데이터로 재발행한 동일 입금까지 보장할 수는 없습니다. 알림 누락이나 문구 변경도 자동 대사 대상이 아닙니다. 알림 권한, 삼성 메시지 및 지원하는 카카오페이·카카오뱅크 문구가 필요합니다.

PIN은 공용 기기의 화면 접근용입니다. 계정 소유권과 데이터 접근 통제는 Firebase Authentication 및 서버 Rules가 담당해야 합니다. 이번 변경은 운영 Rules를 배포하거나 기존 데이터를 수정하지 않습니다.

서명키와 비밀번호는 로컬의 `signing/`, `signing.properties`로 분리했습니다. **기존 Git 이력에 들어간 키와 비밀번호는 이 변경만으로 지워지지 않습니다.** 공개 시에는 정리된 소스로 새 저장소를 만들거나, 백업 후 이력을 정리해야 합니다. 기존 앱의 업데이트 서명에 필요한 키는 보관해야 합니다.

## 테스트

일반 단위 테스트는 동시 갱신 시 재계산, 재전달, 같은 시각의 서로 다른 입금, 미확인 입금 일괄 연결·실패, 회원권, 운영진 적립, 관리자 수정 충돌, 상품 정렬, 파싱 실패, PIN 세션과 로컬 입금 대기 기록을 검증합니다.

Firebase 통합 테스트는 별도로 실행합니다. Firebase CLI와 에뮬레이터용 JDK 21 이상이 필요합니다. Gradle은 JDK 17로 실행합니다.

```powershell
# JAVA_HOME: 에뮬레이터용 JDK 21 이상
# AGITPOS_GRADLE_JAVA_HOME: Gradle용 JDK 17
firebase emulators:exec --project demo-agitpos-review --only database --config tools/firebase-test/firebase.json "powershell.exe -NoProfile -File tools/run-emulator-tests.ps1"
```

통합 테스트는 `127.0.0.1:9009`의 demo 데이터베이스에서 두 클라이언트의 동시 결제·입금, 중복 거래, 서버 권한 거절 및 잘못된 결제를 확인합니다. 테스트 Rules는 개방형이며 **운영에 배포하면 안 됩니다**. 일반 테스트 실행에서는 이 통합 테스트를 건너뜁니다.

### 확인 결과 (2026-11-07)

- 일반 회귀 테스트 26개 통과
- 로컬 Firebase 통합 테스트 3개 통과 (일반 테스트 실행에서는 별도로 건너뜀)
- 디버그 및 서명된 릴리스 APK 빌드 성공
- Android Lint 오류 0건, 경고 178건 및 정보 2건. 경고까지 모두 정리한 상태는 아닙니다.

실제 금융 앱의 알림 수신, 실기기 화면 및 운영 Firebase Rules 검증은 별도입니다.

구현 참고: [Firebase 트랜잭션과 완료 콜백](https://firebase.google.com/docs/database/android/read-and-write#save_data_as_transactions), [Android NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService).
