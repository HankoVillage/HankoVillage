"""Check an uploaded photo and save a clean copy of it.

Rules (SW-05 4.3, decisions 11/10 and 11/21):
- JPEG only, at most 1 MiB, square, each side 200 to 1024 pixels
- re-encode: apply the EXIF orientation, then drop all EXIF (location data)
- sides larger than 800 px are reduced to 800 px
- thumbnail: 320 px, or the stored size if that is smaller
"""

from io import BytesIO
from uuid import uuid4

from PIL import Image, ImageOps, UnidentifiedImageError

# 1MB = 1,000,000 bytes (SI), so the "1MB" shown to users is exact.
MAX_UPLOAD_BYTES = 1_000_000
MIN_SIDE = 200
MAX_SIDE = 1024
STORED_SIDE = 800
THUMB_SIDE = 320
JPEG_QUALITY = 85
THUMB_SUFFIX = "_thumb"


class PhotoTooLarge(Exception):
    pass


class InvalidPhoto(Exception):
    pass


def _encode_jpeg(image: Image.Image, icc_profile: bytes | None) -> bytes:
    buffer = BytesIO()
    # No exif= argument, so nothing from the original EXIF block (GPS etc.) is written back.
    # The colour profile is kept so yarn colours look the same as in the original.
    options = {"icc_profile": icc_profile} if icc_profile else {}
    image.save(buffer, format="JPEG", quality=JPEG_QUALITY, optimize=True, **options)
    return buffer.getvalue()


def process_photo(data: bytes) -> tuple[bytes, bytes]:
    """Return (stored JPEG, thumbnail JPEG) or raise PhotoTooLarge / InvalidPhoto."""
    if len(data) > MAX_UPLOAD_BYTES:
        raise PhotoTooLarge()
    try:
        # formats=["JPEG"]: Pillow does not even try PNG, PSD, PDF and the other decoders.
        with Image.open(BytesIO(data), formats=["JPEG"]) as opened:
            # MPO (multi-picture JPEG) is opened by the JPEG plugin but reports "MPO": refuse it.
            if opened.format != "JPEG":
                raise InvalidPhoto()
            # size comes from the header, so this check runs before the pixels are decoded.
            width, height = opened.size
            if width != height or not MIN_SIDE <= width <= MAX_SIDE:
                raise InvalidPhoto()
            opened.load()
            # Keep the profile only for RGB originals: a CMYK or grayscale profile
            # would describe the wrong colours once the pixels are converted to RGB.
            icc_profile = opened.info.get("icc_profile") if opened.mode == "RGB" else None
            image = ImageOps.exif_transpose(opened).convert("RGB")
    except (UnidentifiedImageError, OSError, SyntaxError, Image.DecompressionBombError) as exc:
        raise InvalidPhoto() from exc

    if image.width > STORED_SIDE:
        image = image.resize((STORED_SIDE, STORED_SIDE), Image.Resampling.LANCZOS)
    thumb_side = min(THUMB_SIDE, image.width)
    thumbnail = image.resize((thumb_side, thumb_side), Image.Resampling.LANCZOS)
    return _encode_jpeg(image, icc_profile), _encode_jpeg(thumbnail, icc_profile)


def new_photo_key(prefix: str, user_id: int) -> str:
    """Random key such as yarns/12/3f2a...; the user's own folder makes cleanup by user easy."""
    return f"{prefix}/{user_id}/{uuid4().hex}"


def thumb_key(key: str) -> str:
    return key + THUMB_SUFFIX


def photo_urls(path: str, key: str | None) -> dict[str, str] | None:
    """URLs for an API response, or None when there is no photo.

    v= changes with every upload, so the app's image cache never shows an old photo.
    Use this from the yarn, pattern and profile responses as well.
    """
    if key is None:
        return None
    version = key.rsplit("/", 1)[-1][:12]
    return {
        "image_url": f"{path}?v={version}",
        "thumbnail_url": f"{path}?size=thumb&v={version}",
    }
