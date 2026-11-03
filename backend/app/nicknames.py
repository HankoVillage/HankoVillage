import re
import unicodedata

# 2-10 characters: complete Hangul syllables, ASCII letters, ASCII digits.
NICKNAME_PATTERN = re.compile(r"[가-힣A-Za-z0-9]{2,10}")


def normalize_nickname(value: str) -> str | None:
    # Some keyboards send Hangul as separate jamo (NFD); NFC joins them first.
    nickname = unicodedata.normalize("NFC", value)
    if NICKNAME_PATTERN.fullmatch(nickname) is None:
        return None
    return nickname
