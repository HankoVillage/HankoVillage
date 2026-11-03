import logging

import jwt
from jwt import PyJWKClient
from jwt.exceptions import PyJWKClientConnectionError, PyJWKClientError, PyJWKSetError

from app.config import settings
from app.errors import ApiError

logger = logging.getLogger(__name__)

KAKAO_ISSUER = "https://kauth.kakao.com"
KAKAO_JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json"

# Seconds. Temporary value: must stay well below nginx proxy_read_timeout (60s).
JWKS_TIMEOUT = 5
# Kakao asks services to cache public keys; too many requests can be blocked.
JWKS_CACHE_SECONDS = 3600
# Allowed clock difference between our server and the provider, in seconds.
CLOCK_LEEWAY = 60

kakao_jwks = PyJWKClient(KAKAO_JWKS_URL, lifespan=JWKS_CACHE_SECONDS, timeout=JWKS_TIMEOUT)


def invalid_social_token() -> ApiError:
    return ApiError(401, "INVALID_SOCIAL_TOKEN", "소셜 로그인을 다시 해 주세요")


def social_provider_unavailable() -> ApiError:
    return ApiError(503, "SOCIAL_PROVIDER_UNAVAILABLE", "잠시 후 다시 시도해 주세요")


def verify_id_token(token: str, jwks: PyJWKClient, issuer: str, audience: str, provider: str) -> str:
    # Checks signature, iss, aud, exp and returns sub. Never log the token itself.
    try:
        kid = jwt.get_unverified_header(token).get("kid")
    except jwt.InvalidTokenError as exc:
        raise invalid_social_token() from exc
    if not isinstance(kid, str) or not kid:
        raise invalid_social_token()

    try:
        jwks.get_signing_keys()
    except (PyJWKClientError, PyJWKSetError, ValueError) as exc:
        logger.warning("%s public keys could not be loaded: %s", provider, type(exc).__name__)
        raise social_provider_unavailable() from exc

    try:
        signing_key = jwks.get_signing_key(kid)
    except PyJWKClientConnectionError as exc:
        logger.warning("%s public keys could not be refreshed", provider)
        raise social_provider_unavailable() from exc
    except (PyJWKSetError, ValueError) as exc:
        logger.warning("%s public keys could not be loaded: %s", provider, type(exc).__name__)
        raise social_provider_unavailable() from exc
    except PyJWKClientError as exc:
        raise invalid_social_token() from exc

    try:
        payload = jwt.decode(
            token,
            signing_key.key,
            algorithms=["RS256"],
            audience=audience,
            issuer=issuer,
            leeway=CLOCK_LEEWAY,
            options={"require": ["iss", "aud", "sub", "iat", "exp"]},
        )
    except jwt.InvalidTokenError as exc:
        raise invalid_social_token() from exc

    sub = payload["sub"]
    if not isinstance(sub, str) or not sub:
        raise invalid_social_token()
    return sub


def verify_kakao_token(token: str) -> str:
    return verify_id_token(token, kakao_jwks, KAKAO_ISSUER, settings.kakao_native_app_key, "Kakao")
