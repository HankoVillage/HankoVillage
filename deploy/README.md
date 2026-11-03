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
