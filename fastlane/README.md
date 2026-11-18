# fastlane 스토어 메타데이터

```
metadata/android/{ko-KR,en-US}/  title, short_description, full_description, images/icon.png(512px)
metadata/ios/{ko,en-US}/         name, subtitle, promotional_text, description, keywords, privacy_url, support_url
check_lengths.py                 글자 수 제한 확인 (초과 시 exit 1)
```

## 원본 문서

- 스토어 문구: ICEC-Project-Knowledge `docs/release/store-listing.md`
- 개인정보처리방침: ICEC-Project-Knowledge `docs/release/privacy-policy.md` (`site/privacy/`에 반영)

## 갱신 방법

1. 원본 문서를 먼저 고칩니다.
2. 바뀐 문구를 이 폴더의 `.txt`에 옮깁니다.
3. `python3 fastlane/check_lengths.py`를 실행합니다.

## 미정 사항

- `privacy_url`, `support_url`은 GitHub Pages(`site/`, `.github/workflows/pages.yml`) 배포 전입니다. 배포 전에는 열리지 않습니다.
- `site/`의 `{판매자}`, `{문의 이메일}`, `{개인정보 담당자}`, `{시행일}`을 확정 후 채워야 Pages 배포가 통과합니다.
