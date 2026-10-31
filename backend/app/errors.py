import logging
from http import HTTPStatus

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

logger = logging.getLogger(__name__)

PROBLEM_JSON = "application/problem+json"

# RFC 9110 renamed 422; Python 3.12 still says "Unprocessable Entity".
TITLE_OVERRIDES = {422: "Unprocessable Content"}


class ApiError(Exception):
    def __init__(self, status: int, code: str, detail: str):
        self.status = status
        self.code = code
        self.detail = detail
        super().__init__(detail)


def status_title(status: int) -> str:
    return TITLE_OVERRIDES.get(status, HTTPStatus(status).phrase)


def problem_response(status: int, code: str, detail: str, fields: list[str] | None = None) -> JSONResponse:
    body = {
        "type": "about:blank",
        "title": status_title(status),
        "status": status,
        "detail": detail,
        "code": code,
    }
    if fields is not None:
        body["fields"] = fields
    return JSONResponse(content=body, status_code=status, media_type=PROBLEM_JSON)


async def handle_api_error(request: Request, exc: ApiError) -> JSONResponse:
    return problem_response(exc.status, exc.code, exc.detail)


async def handle_http_exception(request: Request, exc: StarletteHTTPException) -> JSONResponse:
    status = HTTPStatus(exc.status_code)
    return problem_response(exc.status_code, status.name, status_title(exc.status_code))


async def handle_validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
    fields: list[str] = []
    for error in exc.errors():
        name = ".".join(str(part) for part in error["loc"][1:])
        if name and name not in fields:
            fields.append(name)
    return problem_response(422, "VALIDATION_ERROR", "입력값을 확인해 주세요", fields)


async def handle_unexpected_error(request: Request, exc: Exception) -> JSONResponse:
    logger.exception("Unhandled error on %s %s", request.method, request.url.path)
    return problem_response(500, "INTERNAL_ERROR", "잠시 후 다시 시도해 주세요")


def register_error_handlers(app: FastAPI) -> None:
    app.add_exception_handler(ApiError, handle_api_error)
    app.add_exception_handler(StarletteHTTPException, handle_http_exception)
    app.add_exception_handler(RequestValidationError, handle_validation_error)
    app.add_exception_handler(Exception, handle_unexpected_error)