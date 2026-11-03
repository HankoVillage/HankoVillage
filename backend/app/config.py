from pydantic import SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

MIN_SECRET_LENGTH = 43
PLACEHOLDER_PREFIX = "change-me"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(hide_input_in_errors=True)

    postgres_user: str
    postgres_password: str
    postgres_db: str
    db_host: str = "db"
    db_port: int = 5432

    jwt_access_secret: SecretStr
    jwt_signup_secret: SecretStr

    kakao_native_app_key: str

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg://{self.postgres_user}:{self.postgres_password}"
            f"@{self.db_host}:{self.db_port}/{self.postgres_db}"
        )

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


settings = Settings()