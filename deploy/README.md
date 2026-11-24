# 서버 배포 절차

한코마을 API 서버(api.hankovillage.com)에 코드를 반영하는 순서. 서버 작업은 이 문서대로만 한다.

## 1. 원칙

- 코드는 노트북에서만 고치고 시험하고 커밋한다. GitHub PR이 main에 merge된 것만 서버에 반영한다.
- 서버에서는 `git pull`과 실행만 한다. 서버에서 코드를 고치거나 커밋하지 않는다.
- 서버에만 있는 것: 진짜 `.env`, nginx 설정 원본, 인증서, 방화벽, OS 패키지. 이것들은 저장소에 올리지 않는다(nginx 설정은 비밀 값이 없어서 사본을 `deploy/nginx/`에 보관).
- sudo는 서버 OS 관리(apt, nginx, 방화벽)에만 쓰고, 쓰기 전에 팀에 알린다.

### 하지 말 것

| 명령 | 이유 |
|---|---|
| `docker compose down -v` | `-v`가 DB 볼륨(`db_data`)을 지운다. 데이터가 사라진다 |
| `docker compose config` (`--quiet` 없이) | `.env`의 비밀번호와 비밀키가 화면에 그대로 나온다 |
| `cat .env` | 같은 이유 |
| 서버에서 코드 수정·커밋 | 저장소와 서버가 달라져서 다음 `git pull`이 꼬인다 |

## 2. 접속

노트북 `~/.ssh/config`에 별칭을 만들어 두고 접속한다.

```
Host hankovillage
    HostName <서버 공인 IP>
    User ubuntu
    IdentityFile ~/.ssh/<키 파일 이름>
```

```bash
ssh hankovillage
```

키 비밀번호를 여러 번 입력하지 않으려면 접속 전에 한 번 실행한다(터미널 창을 닫으면 사라진다).

```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/<키 파일 이름>
```

## 3. 배포 전 점검 (주차 시작 전, 서버 작업 전)

```bash
# 커널 보안 수정 확인: 웹에서 Ubuntu CVE 페이지의 linux-oracle 상태 확인
uname -r
test -f /run/reboot-required && echo "reboot required" || echo "no reboot required"
df -h /
```

| 확인 | 기준 |
|---|---|
| `reboot required` | 바로 재부팅하지 않는다. 재부팅 시점을 정한 뒤 진행하고, 재부팅 후 `docker compose ps`로 두 컨테이너가 자동으로 다시 떴는지 확인 |
| `df -h /`의 Use% | 80%를 넘으면 원인부터 확인 |

## 4. 일반 배포 (DB 구조 변경 없음)

```bash
cd ~/hankovillage
git status --short          # 아무것도 안 나와야 한다
git switch main
git pull
docker compose config --quiet && echo "config ok"
docker compose up -d --build
docker compose ps           # db: healthy, api: Up
curl -s https://api.hankovillage.com/health; echo
```

기대 결과: `{"status":"ok","db":"ok"}`

`git status`에 무언가 나오면 멈춘다. 서버에서 누군가 파일을 고쳤다는 뜻이다.

## 5. 새 환경 변수가 추가된 배포

`.env.example`에 새 이름이 생겼는데 서버 `.env`에 없으면 api가 시작하지 않는다(설정 검사가 막는다). 배포 전에 이름만 비교한다. 값은 화면에 나오지 않는다.

```bash
cd ~/hankovillage
diff <(grep -o '^[A-Z0-9_]*=' .env.example | sort) <(grep -o '^[A-Z0-9_]*=' .env | sort)
```

`<`로 나온 이름이 서버 `.env`에 없는 이름이다. 비밀키는 서버에서 직접 만들어 추가한다. 노트북 값을 복사하지 않는다.

```bash
python3 -c 'import secrets; print(secrets.token_urlsafe(48))'   # 값 생성
```

JWT 비밀키 두 개(`JWT_ACCESS_SECRET`, `JWT_SIGNUP_SECRET`)는 서로 다른 값이어야 하고 43자 이상이어야 한다.

## 6. DB 구조 변경(마이그레이션)이 포함된 배포

### 6-1. 적용 전 확인

- PR의 마이그레이션 파일을 열어 무엇이 바뀌는지 본다.
- 기존 표에 NOT NULL 칸을 추가하는 마이그레이션이면 그 표의 행 수를 먼저 확인한다. 행이 있으면 기본값 없이 적용할 수 없다.

```bash
docker compose exec db sh -c 'psql -P pager=off -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) FROM 표이름;"'
```

### 6-2. 적용 전 백업

```bash
mkdir -p ~/backups
(umask 077; docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > ~/backups/before-$(date +%Y%m%d-%H%M).dump)
ls -l ~/backups
```

- `umask 077`: 백업 파일을 주인만 읽을 수 있게 만든다(회원 정보가 들어 있다).
- 백업은 저장소 폴더 밖(`~/backups`)에 둔다.

### 6-3. 적용

```bash
docker compose up -d --build
docker compose exec api alembic current
docker compose exec api alembic upgrade head
docker compose exec api alembic current
curl -s https://api.hankovillage.com/health; echo
```

`alembic current` 결과가 PR의 최신 revision 번호와 같아야 한다.

## 7. nginx 설정

서버 원본: `/etc/nginx/sites-available/hankovillage`, 저장소 사본: `deploy/nginx/hankovillage.conf`. 두 파일은 항상 같아야 한다.

### 같은지 확인 (노트북에서)

```bash
diff <(ssh hankovillage cat /etc/nginx/sites-available/hankovillage) deploy/nginx/hankovillage.conf && echo "same as server"
```

### 저장소 사본으로 서버 설정 바꾸기 (서버에서, sudo 사용)

```bash
cd ~/hankovillage
diff /etc/nginx/sites-available/hankovillage deploy/nginx/hankovillage.conf   # 바뀌는 줄 확인
cp /etc/nginx/sites-available/hankovillage ~/hankovillage.conf.bak
sudo cp deploy/nginx/hankovillage.conf /etc/nginx/sites-available/hankovillage
sudo nginx -t
sudo systemctl reload nginx
```

`nginx -t`가 실패하면 reload하지 말고 백업으로 되돌린다.

```bash
sudo cp ~/hankovillage.conf.bak /etc/nginx/sites-available/hankovillage
```

certbot이 서버 설정을 고친 경우(인증서 재발급 설정 변경 등)에는 반대로 서버 원본을 노트북으로 가져와 커밋한다.

```bash
scp hankovillage:/etc/nginx/sites-available/hankovillage deploy/nginx/hankovillage.conf
```

## 8. 로그

api·db 컨테이너 로그는 `docker-compose.yml`의 `x-logging` 설정으로 컨테이너마다 10MB 파일 3개까지만 남는다. 적용 확인:

```bash
docker inspect --format '{{.Name}} {{.HostConfig.LogConfig}}' $(docker compose ps -q)
```

기대 결과: 두 줄 모두 `{json-file map[max-file:3 max-size:10m]}`

최근 로그 보기:

```bash
docker compose logs --tail 50 api
```

## 9. 되돌리기

### 코드만 되돌리기 (DB 구조 변경이 없었을 때)

```bash
git log --oneline -5                 # 되돌아갈 커밋 확인
git switch --detach <커밋 번호>
docker compose up -d --build
```

문제를 고친 PR이 merge되면 `git switch main && git pull`로 돌아온다.

### DB 구조까지 되돌리기

1. 먼저 `alembic downgrade -1`이 데이터를 지우는지 마이그레이션 파일의 `downgrade()`를 확인한다(칸을 지우면 그 칸의 데이터가 사라진다).
2. 확인한 뒤 실행한다.

```bash
docker compose exec api alembic downgrade -1
```

3. 데이터가 망가졌으면 6-2에서 만든 백업으로 복원한다. 복원은 팀과 상의한 뒤 진행한다.

## 10. DB 백업 (매일 04:00 KST)

`deploy/backup.sh`가 하는 일: DB 전체를 `~/backups/daily-<UTC시각>.dump`로 저장(주인만 읽기) → 파일 확인 → `hanko-backup` 버킷에 올리고 크기·MD5 확인 → 서버에는 최근 3개만 남김 → 만료 후 30일 지난 재발급 토큰 삭제 → `~/backups/backup.log`에 한 줄씩 기록, 실패하면 OCI 알림 메일.

- 백업 대상은 DB뿐이다. 사진 파일(버킷)은 포함되지 않는다.
- 서버는 `hanko-backup`에 **올리기·목록만** 할 수 있다(읽기·덮어쓰기·삭제 불가). 14일 지난 백업은 버킷의 수명 주기 규칙이 지운다.
- 알림 토픽 OCID는 서버 `.env`의 `BACKUP_ALERT_TOPIC_ID`에만 둔다.
- 새벽 4시 근처에는 배포(api 재빌드)를 하지 않는다. 업로드와 알림이 api 컨테이너에서 실행된다.

### 처음 설치 (서버, sudo 사용 → 팀에 먼저 알린다)

```bash
sudo cp deploy/systemd/hanko-backup.service deploy/systemd/hanko-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now hanko-backup.timer
systemctl list-timers hanko-backup.timer
```

### 확인

```bash
tail -n 20 ~/backups/backup.log        # 최근 결과
journalctl -u hanko-backup.service -n 30 --no-pager
deploy/backup.sh                       # 손으로 한 번 실행 (프로젝트 폴더에서)
```

### 복구 시험 (운영 DB는 건드리지 않음)

```bash
f=$(ls -1t ~/backups/daily-*.dump | head -1)
docker compose exec -T db sh -c 'createdb -U "$POSTGRES_USER" restore_test'
docker compose exec -T db sh -c 'pg_restore -U "$POSTGRES_USER" -d restore_test --no-owner' < "$f"
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d restore_test -P pager=off -c "SELECT (SELECT count(*) FROM users) AS users, (SELECT count(*) FROM projects) AS projects, (SELECT version_num FROM alembic_version) AS version;"'
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -P pager=off -c "SELECT (SELECT count(*) FROM users) AS users, (SELECT count(*) FROM projects) AS projects, (SELECT version_num FROM alembic_version) AS version;"'
docker compose exec -T db sh -c 'dropdb -U "$POSTGRES_USER" restore_test'
```

`dropdb`에는 반드시 `restore_test`만 쓴다.

### 실제 복구

1. 팀과 상의하고, 지금 상태도 `before-restore-<날짜>.dump`로 먼저 백업한다(6-2 명령).
2. 3일 안의 백업이면 `~/backups/daily-*.dump`를 쓴다.
3. 그보다 오래된 백업이 필요하면, OCI 콘솔 정책에 아래 문장을 **잠깐** 추가해 서버가 내려받을 수 있게 하고, 복구가 끝나면 바로 지운다.

```
Allow dynamic-group hanko-app-server to read objects in compartment hankovillage where target.bucket.name='hanko-backup'
```

4. **지금 DB가 읽히면** 복구하기 전에 지금 있는 회원 id 목록을 저장한다. 백업 뒤에 탈퇴한 회원을 찾는 데 쓴다(id만 저장, 개인정보 없음).

```bash
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -c "SELECT id FROM users ORDER BY id"' > ~/backups/users-before-restore.txt
wc -l ~/backups/users-before-restore.txt   # 지금 회원 수와 같은지
```

5. api를 멈추고 복구한다. 아래 6번 정리가 끝나기 전에는 api를 켜지 않는다(탈퇴한 회원이 다시 로그인하거나, 로그아웃한 토큰이 다시 쓰이는 것을 막기 위해).

```bash
f=~/backups/daily-<복구할 시각>.dump
docker compose stop api
docker compose exec -T db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' < "$f"
```

6. 복구한 DB를 정리한다. 백업은 최대 14일 전 상태라서, 그 사이에 로그아웃한 로그인 토큰과 탈퇴한 회원이 되살아난다. 처리방침의 "탈퇴하면 지체 없이 파기"와 맞추기 위해 둘 다 다시 지운다.

```bash
# 6-1. 로그인 토큰 전부 삭제 (모든 회원이 한 번 다시 로그인)
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DELETE FROM refresh_tokens"'

# 6-2. 백업 뒤에 탈퇴한 회원 찾기: 4번 목록에 없는 회원. 먼저 개수만 본다
[ -s ~/backups/users-before-restore.txt ] && ids=$(paste -sd, ~/backups/users-before-restore.txt) && echo "목록 회원 수: $(wc -l < ~/backups/users-before-restore.txt)"
docker compose exec -T db sh -c "psql -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -At -c \"SELECT count(*) FROM users WHERE id NOT IN ($ids)\""

# 6-3. 개수를 확인한 뒤 삭제. 탈퇴 API와 같은 방식(users 한 줄 삭제 → 외래 키가 프로젝트·도안·실·토큰을 함께 삭제, 글·댓글은 작성자만 비움)
docker compose exec -T db sh -c "psql -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -c \"DELETE FROM users WHERE id NOT IN ($ids)\""
```

- 6-2에서 `ids`가 비어 있으면 SQL 문법 오류로 멈춘다. 이때 6-3을 실행하지 않는다.
- 6-2의 개수가 예상보다 많으면(예: 지금 회원보다 많음) 4번 목록이 잘못된 것이니 멈추고 팀과 확인한다.
- 이 회원들의 사진 파일은 탈퇴할 때 이미 버킷에서 지워졌으므로 따로 지울 것이 없다.

7. api를 켜고 확인한다.

```bash
docker compose start api
curl -s https://api.hankovillage.com/health
```

8. 회원에게 알린다. 약관 제11조 2항("장애 등으로 기록이 손실된 경우 지체 없이 회원에게 알리고 백업을 이용해 복구")에 따른 단계다.
- 앱 공지와 홈페이지 공지에 올린다.
- 적을 내용: 장애가 난 날짜, 복구한 백업의 시각(KST), 그 시각 이후의 가입·기록·사진 변경이 사라졌다는 것, 모든 회원이 한 번 다시 로그인해야 한다는 것, 일부 사진은 다시 등록해야 할 수 있다는 것, 문의처(contact@hankovillage.com).
- 시각은 파일 이름의 UTC에 9시간을 더해 KST로 적는다(예: `daily-20261124T190000Z` → 11/25 04:00 KST).

9. 한계와 후속 조치
- 지금 DB가 읽히지 않아 4번 목록을 만들 수 없으면, 백업 뒤에 탈퇴한 회원을 찾을 방법이 없다. api 컨테이너 로그의 탈퇴 기록(`Account deleted: user_id=...`)은 컨테이너를 다시 만들면 사라지고, nginx 로그에는 회원 id가 없다. 이 경우 복구 날짜와 사정을 팀에 기록한다(다음 학기 "탈퇴 기록 파일"에서 처리).
- 백업 뒤에 사진을 바꾸거나 지운 회원은, DB가 이미 지워진 사진 파일을 가리켜 사진이 깨져 보일 수 있다. 8번 공지에 다시 등록하라는 안내를 넣는다(다음 학기 "DB–버킷 정합성 점검", "사진 버전 관리"에서 처리).
- 복구 뒤 `~/backups/users-before-restore.txt`는 지운다.
