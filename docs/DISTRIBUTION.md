# 배포 준비 기록

대상: **Show Interaction 0.2.1 / Minecraft Java 26.2 / Fabric**

현재는 공개 전 준비 단계입니다. GitHub 저장소 공개 전환, GitHub Release, Modrinth/CurseForge 등록,
태그 푸시 및 다운로드 페이지 게시를 수행하지 않습니다. CI는 비공개 저장소의 빌드 결과만 보관합니다.

## 준비한 정보

| 항목 | 값 |
| --- | --- |
| 이름 | Show Interaction |
| 제작자 | RUOK0214 |
| 라이선스 | MIT, Copyright (c) 2026 RUOK0214 |
| 실행 대상 | 클라이언트 전용 |
| 게임 / 로더 | Minecraft Java 26.2 / Fabric Loader 0.19.5 이상 |
| 필수 의존성 | Fabric API 0.160.0+26.2 또는 26.2용 호환 버전 |
| 선택 의존성 | Mod Menu 20.0.2, 26.2용 |
| Java | 25 이상 |
| 설정 진입 | `/showinteraction`, 또는 Mod Menu의 설정 버튼 |
| 설치 파일 | `show-interaction-0.2.1.jar` |
| 언어 | 한국어·영어 UI 및 Mod Menu 설명 |
| 아이콘 | 직접 작성한 256×256 블록 윤곽 아이콘, 게임 이미지 미사용 |

Mod Menu는 포함하지 않습니다. 파일 자체에 제작자·MIT·연락 링크·아이콘을 넣고,
Mod Menu가 설치되어 있을 때만 해당 설정 진입점을 사용합니다.

## 게시용 소개 초안

### 한국어

**짧은 소개:** 방금 설치·제거·조절·확인한 블록을 잠시 강조하는 방송용 클라이언트 모드.

Show Interaction은 방금 조작한 블록에 색상 윤곽을 표시합니다. 중계기 단계를 바꿨을 때,
여러 상자를 연달아 열 때, 블록을 설치하거나 제거할 때 시청자가 변화 지점을 따라가기 쉽습니다.
설치·제거·조절·상자 열기·확인을 색으로 구분하며, 가장 최근 조작을 더 선명하게 보여 줍니다.
`/showinteraction`에서 전체 표시, 투명도, 반응별 표시 여부와 시간을 조절할 수 있습니다.
Mod Menu를 설치하면 모드 목록에서도 설정을 열 수 있습니다. 서버에 설치할 필요는 없습니다.

### English

**Summary:** Brief colored outlines highlight the blocks you just used, built for streams and recordings.

Show Interaction highlights recent block placement, removal, adjustments, container opening and inspection.
Help viewers follow changes to repeater delays or see which chest you just checked. The latest interaction
stands out, with configurable visibility, transparency and individual reaction durations.
Open settings with `/showinteraction` or the optional Mod Menu configuration button.
Client-side only; no server installation required.

## 파일 구성 및 재생성

`gradlew.bat prepareDistribution` 또는 `./gradlew prepareDistribution`으로 로컬 ZIP을 재생성합니다.
동일한 작업으로 설치 파일 안의 버전·MIT·아이콘·번역·선택 의존성·불필요한 포함 파일을 검사합니다.

- ZIP 루트: 설치용 JAR, INSTALL.txt, README.md, CHANGELOG.md, LICENSE, THIRD_PARTY_NOTICES.md, SHA256SUMS.txt
- `developer/`: 설치하지 않는 소스 JAR
- `docs/`: 실제 검증 범위를 기록한 VERIFICATION.md

아이콘은 저장소 루트에서 `java tools/GenerateIcon.java`로 재생성합니다.

## 공개 전에 남은 일

- 멀티플레이, 여러 상자 연속 열기 및 다른 렌더링 모드의 미검증 범위를 검토합니다.
- 플랫폼을 정한 뒤 실제 다운로드/문의 링크를 확정합니다. 현재 GitHub 링크는 비공개 저장소입니다.
- 선택한 플랫폼에서 요구하는 소개·스크린샷·표시 항목을 확인합니다.
- 공개 게시 여부는 사용자가 결정합니다. MIT 선택은 게시 또는 저장소 공개의 승인을 의미하지 않습니다.

참고: [Fabric 메타데이터](https://wiki.fabricmc.net/documentation:fabric_mod_json_spec),
[Mod Menu API](https://github.com/TerraformersMC/ModMenu/blob/v20.0.2/README.md),
[MIT 원문](https://opensource.org/license/mit).
