from typing import Literal

from pydantic import SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy.engine import URL

MIN_SECRET_LENGTH = 43
PLACEHOLDER_PREFIX = "change-me"
GOOGLE_CLIENT_ID_SUFFIX = ".apps.googleusercontent.com"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(hide_input_in_errors=True)

    postgres_user: str
    postgres_password: SecretStr
    postgres_db: str
    db_host: str = "db"
    db_port: int = 5432

    jwt_access_secret: SecretStr
    jwt_signup_secret: SecretStr

    kakao_native_app_key: str
    google_web_client_id: str

    # Photo storage: "local" keeps files in a folder (laptop), "oci" uses Object Storage (server).
    storage_backend: Literal["local", "oci"] = "local"
    local_storage_dir: str = "/app/storage"
    oci_region: str = "ap-tokyo-1"
    oci_private_bucket: str = "hanko-private"

    # Daily DB backup (deploy/backup.sh). The server may only create and list objects in this bucket.
    oci_backup_bucket: str = "hanko-backup"
    # OCI Notifications topic for backup failure emails. Empty on laptops: alerts are skipped.
    backup_alert_topic_id: str | None = None

    @property
    def database_url(self) -> URL:
        # URL.create escapes special characters (@ : / #) in the password,
        # and str() of the result hides the password as ***.
        return URL.create(
            drivername="postgresql+psycopg",
            username=self.postgres_user,
            password=self.postgres_password.get_secret_value(),
            host=self.db_host,
            port=self.db_port,
            database=self.postgres_db,
        )

    @model_validator(mode="after")
    def check_postgres_password(self) -> "Settings":
        value = self.postgres_password.get_secret_value()
        if not value or value.startswith(PLACEHOLDER_PREFIX):
            raise ValueError("POSTGRES_PASSWORD is still empty or the placeholder value from .env.example. Replace it with a random value.")
        return self

    @model_validator(mode="after")
    def check_jwt_secrets(self) -> "Settings":
        access = self.jwt_access_secret.get_secret_value()
        signup = self.jwt_signup_secret.get_secret_value()
        for name, value in (("JWT_ACCESS_SECRET", access), ("JWT_SIGNUP_SECRET", signup)):
            if value.startswith(PLACEHOLDER_PREFIX):
                raise ValueError(f"{name} is still the placeholder value from .env.example. Replace it with a random value.")
            if len(value) < MIN_SECRET_LENGTH:
                raise ValueError(f"{name} is too short (minimum {MIN_SECRET_LENGTH} characters). Generate one with: python3 -c 'import secrets; print(secrets.token_urlsafe(48))'")
        if access == signup:
            raise ValueError("JWT_ACCESS_SECRET and JWT_SIGNUP_SECRET must be different. Replace one of them with a new random value.")
        return self

    @model_validator(mode="after")
    def check_kakao_native_app_key(self) -> "Settings":
        if not self.kakao_native_app_key or self.kakao_native_app_key.startswith(PLACEHOLDER_PREFIX):
            raise ValueError("KAKAO_NATIVE_APP_KEY is not set. Copy the native app key from Kakao Developers into .env.")
        return self

    @model_validator(mode="after")
    def check_google_web_client_id(self) -> "Settings":
        value = self.google_web_client_id
        if not value or value.startswith(PLACEHOLDER_PREFIX) or not value.endswith(GOOGLE_CLIENT_ID_SUFFIX):
            raise ValueError(f"GOOGLE_WEB_CLIENT_ID is not set. Copy the Web application client ID (ends with {GOOGLE_CLIENT_ID_SUFFIX}) from Google Cloud Console into .env.")
        return self


settings = Settings()